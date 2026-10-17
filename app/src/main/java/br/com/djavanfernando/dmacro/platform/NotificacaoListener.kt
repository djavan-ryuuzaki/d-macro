package br.com.djavanfernando.dmacro.platform

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import br.com.djavanfernando.dmacro.di.EscopoApp
import br.com.djavanfernando.dmacro.engine.EventoNotificacao
import br.com.djavanfernando.dmacro.engine.Motor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Converte notificações do sistema em [EventoNotificacao] e entrega ao [Motor]. */
@AndroidEntryPoint
class NotificacaoListener : NotificationListenerService() {

    @Inject lateinit var motor: Motor
    @Inject lateinit var pacotesVistos: PacotesVistos
    @Inject @EscopoApp lateinit var escopo: CoroutineScope

    override fun onListenerConnected() {
        _conectado.value = true
    }

    override fun onListenerDisconnected() {
        _conectado.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != packageName) pacotesVistos.registrar(sbn.packageName)
        val evento = paraEvento(this, sbn)
        escopo.launch { motor.onNotificacao(evento) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val chave = sbn.key
        escopo.launch { motor.onNotificacaoRemovida(chave) }
    }

    companion object {
        private val _conectado = MutableStateFlow(false)
        val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

        fun paraEvento(context: Context, sbn: StatusBarNotification): EventoNotificacao {
            val n = sbn.notification
            val extras = n.extras
            val texto = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            return EventoNotificacao(
                pacote = sbn.packageName,
                nomeApp = rotuloDoApp(context, sbn.packageName),
                titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                texto = texto?.toString().orEmpty(),
                chave = sbn.key,
                emCurso = n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0,
                resumoDeGrupo = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                instante = Instant.now(),
            )
        }

        fun rotuloDoApp(context: Context, pacote: String): String = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pacote, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pacote
        }
    }
}
