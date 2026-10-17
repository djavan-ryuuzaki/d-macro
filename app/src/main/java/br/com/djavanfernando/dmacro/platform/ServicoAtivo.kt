package br.com.djavanfernando.dmacro.platform

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.data.ConfiguracoesRepository
import br.com.djavanfernando.dmacro.di.EscopoApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Serviço em primeiro plano opcional que mantém o processo vivo em aparelhos agressivos com bateria. */
class ServicoAtivo : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificacao = NotificationCompat.Builder(this, Canais.SERVICO)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(getString(R.string.servico_titulo))
            .setContentText(getString(R.string.servico_texto))
            .setContentIntent(Canais.abrirApp(this))
            .setOngoing(true)
            .build()
        val tipo = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, ID, notificacao, tipo)
        return START_STICKY
    }

    companion object {
        private const val ID = 1

        fun iniciar(context: Context) {
            context.startForegroundService(Intent(context, ServicoAtivo::class.java))
        }

        fun parar(context: Context) {
            context.stopService(Intent(context, ServicoAtivo::class.java))
        }
    }
}

/** No boot: religa o serviço em primeiro plano (se ativado) e pede reconexão do listener. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var configuracoes: ConfiguracoesRepository
    @Inject lateinit var permissoes: PermissoesAndroid
    @Inject @EscopoApp lateinit var escopo: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendente = goAsync()
        escopo.launch {
            try {
                if (configuracoes.servicoPrimeiroPlano.first()) ServicoAtivo.iniciar(context)
                if (permissoes.acessoNotificacoesConcedido()) {
                    NotificationListenerService.requestRebind(permissoes.componenteListener())
                }
            } finally {
                pendente.finish()
            }
        }
    }
}
