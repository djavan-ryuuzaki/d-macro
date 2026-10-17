package br.com.djavanfernando.dmacro.platform

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import br.com.djavanfernando.dmacro.engine.Permissao
import br.com.djavanfernando.dmacro.engine.VerificadorPermissoes

/** Nome da permissão Android de cada [Permissao] do motor; nulo quando ela é uma tela do sistema. */
val Permissao.manifesto: String?
    get() = when (this) {
        Permissao.Sms -> Manifest.permission.SEND_SMS
        Permissao.Telefone -> Manifest.permission.CALL_PHONE
        Permissao.Acessibilidade -> null
    }

class PermissoesAndroid(private val context: Context) : VerificadorPermissoes {

    override fun concedida(permissao: Permissao): Boolean =
        permissao.manifesto?.let(::temPermissao) ?: servicoAcessibilidadeAtivo()

    fun temPermissao(nome: String): Boolean =
        ContextCompat.checkSelfPermission(context, nome) == PackageManager.PERMISSION_GRANTED

    /** O serviço de acessibilidade do D-Macro está ligado nas configurações do sistema. */
    fun servicoAcessibilidadeAtivo(): Boolean {
        val ativos = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return ativos.split(':').any { it.startsWith("${context.packageName}/") }
    }

    fun acessoNotificacoesConcedido(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun podePostarNotificacoes(): Boolean =
        Build.VERSION.SDK_INT < 33 || temPermissao(Manifest.permission.POST_NOTIFICATIONS)

    fun otimizacaoBateriaIgnorada(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun componenteListener() = ComponentName(context, NotificacaoListener::class.java)

    companion object {
        const val ACAO_CONFIG_LISTENER = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        val ehSamsung: Boolean get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val temNotificacoesAprimoradas: Boolean get() = Build.VERSION.SDK_INT >= 35
    }
}
