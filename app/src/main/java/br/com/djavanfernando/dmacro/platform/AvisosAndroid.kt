package br.com.djavanfernando.dmacro.platform

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.Avisos
import br.com.djavanfernando.dmacro.engine.TipoTeto
import br.com.djavanfernando.dmacro.ui.MainActivity

object Canais {
    const val AVISOS = "avisos"
    const val SERVICO = "servico"

    fun criar(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(AVISOS, context.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_HIGH),
        )
        nm.createNotificationChannel(
            NotificationChannel(SERVICO, context.getString(R.string.canal_servico), NotificationManager.IMPORTANCE_MIN),
        )
    }

    fun abrirApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
}

class AvisosAndroid(private val context: Context, private val permissoes: PermissoesAndroid) : Avisos {

    @SuppressLint("MissingPermission")
    override fun tetoAtingido(tipo: TipoTeto) {
        if (!permissoes.podePostarNotificacoes()) return
        val oQue = context.getString(
            when (tipo) {
                TipoTeto.Sms -> R.string.teto_sms
                TipoTeto.Chamada -> R.string.teto_chamadas
                TipoTeto.WhatsApp -> R.string.teto_whatsapp
            },
        )
        val texto = context.getString(R.string.aviso_teto_texto, tipo.maximoPorHora, oQue)
        val notificacao = NotificationCompat.Builder(context, Canais.AVISOS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.aviso_teto_titulo))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(Canais.abrirApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_TETO + tipo.ordinal, notificacao)
    }

    private companion object { const val ID_TETO = 1000 }
}
