package br.com.djavanfernando.dmacro.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.com.djavanfernando.dmacro.data.RegistroRepository
import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.engine.whatsapp.EstadoAparelho
import br.com.djavanfernando.dmacro.engine.whatsapp.FilaWhatsApp
import br.com.djavanfernando.dmacro.engine.whatsapp.NoTela
import br.com.djavanfernando.dmacro.engine.whatsapp.PrazoItem
import br.com.djavanfernando.dmacro.engine.whatsapp.TelaWhatsApp
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Serviço de acessibilidade do D-Macro. Não reage a eventos: só existe para que a [TelaWhatsAppAndroid]
 * possa abrir o WhatsApp em segundo plano, ler a janela ativa e tocar em Enviar durante um envio.
 * Nada do que ele lê é gravado.
 */
class DMacroAcessibilidade : AccessibilityService() {
    override fun onServiceConnected() { instancia = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() {
        if (instancia === this) instancia = null
        super.onDestroy()
    }

    companion object {
        @Volatile var instancia: DMacroAcessibilidade? = null
            private set
    }
}

/** Quais WhatsApp estão instalados (visíveis graças ao `<queries>` do manifesto). */
fun whatsAppsInstalados(context: Context): Set<AppWhatsApp> = AppWhatsApp.entries.filter { app ->
    try {
        context.packageManager.getPackageInfo(app.pacote, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}.toSet()

/** Abre as configurações de acessibilidade, destacando o serviço do D-Macro quando o sistema aceita. */
fun intentConfigAcessibilidade(context: Context): Intent {
    val componente = android.content.ComponentName(context, DMacroAcessibilidade::class.java).flattenToString()
    return Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .putExtra(":settings:fragment_args_key", componente)
        .putExtra(":settings:show_fragment_args", android.os.Bundle().apply { putString(":settings:fragment_args_key", componente) })
}

class TelaWhatsAppAndroid(private val context: Context) : TelaWhatsApp {

    /** Nós da última leitura, na mesma ordem dos índices de [NoTela]. */
    private var ultimaLeitura: List<AccessibilityNodeInfo> = emptyList()

    private fun servico() = DMacroAcessibilidade.instancia ?: error(FilaWhatsApp.MOTIVO_SERVICO_DESLIGADO)

    override fun instalado(app: AppWhatsApp) = app in whatsAppsInstalados(context)

    override fun disponivel() = DMacroAcessibilidade.instancia != null

    override fun abrirConversa(app: AppWhatsApp, numero: String, texto: String) {
        val link = Uri.Builder().scheme("https").authority("api.whatsapp.com").path("send")
            .appendQueryParameter("phone", numero)
            .appendQueryParameter("text", texto)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, link)
            .setPackage(app.pacote)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Iniciar pelo contexto do serviço vinculado pelo sistema é o que libera abrir telas em segundo plano.
        servico().startActivity(intent)
    }

    @Synchronized
    override fun ler(): List<NoTela> {
        val raiz = servico().rootInActiveWindow
        if (raiz == null || raiz.packageName?.toString() !in PACOTES) {
            ultimaLeitura = emptyList()
            return emptyList()
        }
        val nos = mutableListOf<AccessibilityNodeInfo>()
        fun visitar(no: AccessibilityNodeInfo) {
            nos += no
            for (i in 0 until no.childCount) no.getChild(i)?.let(::visitar)
        }
        visitar(raiz)
        ultimaLeitura = nos
        return nos.mapIndexed { i, n ->
            NoTela(
                indice = i,
                idView = n.viewIdResourceName,
                classe = n.className?.toString(),
                texto = n.text?.toString(),
                descricao = n.contentDescription?.toString(),
                clicavel = n.isClickable,
            )
        }
    }

    @Synchronized
    override fun tocar(no: NoTela) {
        var alvo: AccessibilityNodeInfo? = ultimaLeitura.getOrNull(no.indice) ?: error("nó ${no.indice} fora da última leitura")
        while (alvo != null && !alvo.isClickable) alvo = alvo.parent
        check(alvo?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "o toque não foi aceito" }
    }

    override fun voltar() { servico().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }

    override fun irParaInicio() { servico().performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }

    private companion object {
        val PACOTES = AppWhatsApp.entries.map { it.pacote }.toSet()
    }
}

class EstadoAparelhoAndroid(context: Context) : EstadoAparelho {
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    private val energia = context.getSystemService(PowerManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)

    /** Tela apagada conta como bloqueada: o WhatsApp não desenha nada para ser tocado. */
    override fun telaBloqueada() = keyguard.isKeyguardLocked || !energia.isInteractive

    /** Qualquer modo de áudio diferente do normal: chamada comum, chamada por app, tocando ou triagem. */
    override fun chamadaEmAndamento() = audio.mode != AudioManager.MODE_NORMAL

    /**
     * Verifica a cada segundo e exige que o aparelho continue liberado por mais um instante,
     * para não abrir o WhatsApp por cima de uma chamada que a Macro acabou de iniciar.
     */
    override suspend fun aguardarLiberado() {
        while (true) {
            if (liberado()) {
                delay(ESTABILIDADE_MS)
                if (liberado()) return
            }
            delay(INTERVALO_MS)
        }
    }

    private fun liberado() = !telaBloqueada() && !chamadaEmAndamento()

    private companion object {
        const val INTERVALO_MS = 1_000L
        const val ESTABILIDADE_MS = 1_500L
    }
}

/** Vigia do prazo: marca o item como falho se ele ainda estiver pendente, mesmo que o processo tenha morrido. */
class PrazoWhatsAppWorkManager(private val context: Context, private val relogio: Clock) : PrazoItem {
    override fun agendar(idItem: String, prazo: Instant) {
        val vigia = OneTimeWorkRequestBuilder<WhatsAppPrazoWorker>()
            .setInputData(workDataOf(WhatsAppPrazoWorker.ID_ITEM to idItem))
            .setInitialDelay(Duration.between(relogio.instant(), prazo).plusSeconds(30).toMillis(), TimeUnit.MILLISECONDS)
            .addTag("whatsapp")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("whatsapp-prazo-$idItem", ExistingWorkPolicy.KEEP, vigia)
    }
}

@HiltWorker
class WhatsAppPrazoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val registros: RegistroRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val idItem = inputData.getString(ID_ITEM) ?: return Result.failure()
        registros.falharSePendente(idItem, FilaWhatsApp.MOTIVO_PRAZO)
        return Result.success()
    }

    companion object { const val ID_ITEM = "idItem" }
}
