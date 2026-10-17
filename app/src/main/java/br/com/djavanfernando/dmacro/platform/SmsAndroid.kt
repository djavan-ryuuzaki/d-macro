package br.com.djavanfernando.dmacro.platform

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.SmsSender
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Envia pelo chip padrão e espera o sentIntent de todas as partes. */
class SmsAndroid(private val context: Context) : SmsSender {

    override suspend fun enviar(numero: String, texto: String): ResultadoAcao =
        withTimeoutOrNull(TIMEOUT_MS) { enviarEsperando(numero, texto) }
            ?: ResultadoAcao.Erro("sem confirmação do envio")

    private suspend fun enviarEsperando(numero: String, texto: String): ResultadoAcao =
        suspendCancellableCoroutine { cont ->
            val manager = smsManager(context)
            val partes = manager.divideMessage(texto)
            val acao = "${context.packageName}.SMS_ENVIADO.${UUID.randomUUID()}"
            var restantes = partes.size
            var erro: String? = null
            val desregistrado = AtomicBoolean(false)

            lateinit var receptor: BroadcastReceiver
            fun desregistrar() {
                if (desregistrado.compareAndSet(false, true)) context.unregisterReceiver(receptor)
            }
            receptor = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (resultCode != Activity.RESULT_OK && erro == null) erro = descreverErro(resultCode)
                    restantes--
                    if (restantes <= 0) {
                        desregistrar()
                        if (cont.isActive) cont.resume(erro?.let(ResultadoAcao::Erro) ?: ResultadoAcao.Ok)
                    }
                }
            }
            ContextCompat.registerReceiver(context, receptor, IntentFilter(acao), ContextCompat.RECEIVER_NOT_EXPORTED)
            cont.invokeOnCancellation { desregistrar() }

            val intents = ArrayList(
                partes.indices.map { i ->
                    PendingIntent.getBroadcast(
                        context, i, Intent(acao).setPackage(context.packageName),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
                    )
                },
            )
            try {
                manager.sendMultipartTextMessage(numero, null, partes, intents, null)
            } catch (e: Exception) {
                desregistrar()
                if (cont.isActive) cont.resume(ResultadoAcao.Erro(e.message ?: e.javaClass.simpleName))
            }
        }

    private fun descreverErro(codigo: Int): String = when (codigo) {
        SmsManager.RESULT_ERROR_NO_SERVICE -> "sem serviço"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "rádio desligado"
        SmsManager.RESULT_ERROR_NULL_PDU -> "mensagem inválida"
        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "falha genérica"
        else -> "erro $codigo"
    }

    companion object {
        private const val TIMEOUT_MS = 60_000L

        fun smsManager(context: Context): SmsManager =
            if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java)
            else @Suppress("DEPRECATION") SmsManager.getDefault()

        /** Quantas partes o SMS terá (estimativa exibida no editor). */
        fun contarPartes(context: Context, texto: String): Int =
            if (texto.isEmpty()) 0 else runCatching { smsManager(context).divideMessage(texto).size }.getOrDefault(1)
    }
}
