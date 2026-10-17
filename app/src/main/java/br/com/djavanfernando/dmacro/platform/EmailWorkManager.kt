package br.com.djavanfernando.dmacro.platform

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.com.djavanfernando.dmacro.data.RegistroRepository
import br.com.djavanfernando.dmacro.engine.EmailSender
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Cada e-mail vira um trabalho do WorkManager com restrição de rede e backoff exponencial.
 * Um segundo trabalho, sem restrição de rede, marca o item como falho quando o prazo se esgota.
 */
class EmailWorkManager(
    private val context: Context,
    private val contas: ContaEmailRepository,
    private val relogio: Clock,
) : EmailSender {

    override suspend fun enfileirar(idItem: String, endereco: String, assunto: String, corpo: String, prazo: Instant): ResultadoAcao {
        if (contas.obter() == null) return ResultadoAcao.Erro(SEM_CONTA)

        val dados = workDataOf(
            EmailWorker.ID_ITEM to idItem,
            EmailWorker.ENDERECO to endereco,
            EmailWorker.ASSUNTO to assunto,
            EmailWorker.CORPO to corpo,
            EmailWorker.PRAZO to prazo.toEpochMilli(),
        )
        val envio = OneTimeWorkRequestBuilder<EmailWorker>()
            .setInputData(dados)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        val vigia = OneTimeWorkRequestBuilder<EmailPrazoWorker>()
            .setInputData(dados)
            .setInitialDelay(Duration.between(relogio.instant(), prazo).plusSeconds(5).toMillis(), TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(nomeEnvio(idItem), ExistingWorkPolicy.KEEP, envio)
        wm.enqueueUniqueWork(nomeVigia(idItem), ExistingWorkPolicy.KEEP, vigia)
        return ResultadoAcao.Pendente
    }

    companion object {
        const val SEM_CONTA = "Conta de e-mail não configurada"
        const val TAG = "email"
        fun nomeEnvio(idItem: String) = "email-$idItem"
        fun nomeVigia(idItem: String) = "email-prazo-$idItem"
    }
}

@HiltWorker
class EmailWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contas: ContaEmailRepository,
    private val smtp: SmtpCliente,
    private val registros: RegistroRepository,
    private val relogio: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val idItem = inputData.getString(ID_ITEM) ?: return Result.failure()
        val prazo = Instant.ofEpochMilli(inputData.getLong(PRAZO, 0))
        if (relogio.instant().isAfter(prazo)) return falhar(idItem, PRAZO_ESGOTADO)

        val conta = contas.obter() ?: return falhar(idItem, EmailWorkManager.SEM_CONTA)
        val resultado = withContext(Dispatchers.IO) {
            smtp.enviar(conta, inputData.getString(ENDERECO)!!, inputData.getString(ASSUNTO)!!, inputData.getString(CORPO)!!)
        }
        return when (resultado) {
            ResultadoSmtp.Ok -> {
                registros.atualizarItem(idItem, ResultadoAcao.Ok)
                WorkManager.getInstance(applicationContext).cancelUniqueWork(EmailWorkManager.nomeVigia(idItem))
                Result.success()
            }
            is ResultadoSmtp.FalhaTemporaria -> Result.retry()
            is ResultadoSmtp.FalhaAutenticacao -> falhar(idItem, "autenticação recusada: ${resultado.motivo}")
            is ResultadoSmtp.FalhaPermanente -> falhar(idItem, resultado.motivo)
        }
    }

    private suspend fun falhar(idItem: String, motivo: String): Result {
        registros.atualizarItem(idItem, ResultadoAcao.Erro(motivo))
        WorkManager.getInstance(applicationContext).cancelUniqueWork(EmailWorkManager.nomeVigia(idItem))
        return Result.success()
    }

    companion object {
        const val ID_ITEM = "idItem"
        const val ENDERECO = "endereco"
        const val ASSUNTO = "assunto"
        const val CORPO = "corpo"
        const val PRAZO = "prazo"
        const val PRAZO_ESGOTADO = "prazo de 30 minutos esgotado"
    }
}

/** Marca o e-mail como falho se ele ainda estiver pendente quando o prazo acabar (ex.: sem rede o tempo todo). */
@HiltWorker
class EmailPrazoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val registros: RegistroRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val idItem = inputData.getString(EmailWorker.ID_ITEM) ?: return Result.failure()
        WorkManager.getInstance(applicationContext).cancelUniqueWork(EmailWorkManager.nomeEnvio(idItem))
        registros.falharSePendente(idItem, EmailWorker.PRAZO_ESGOTADO)
        return Result.success()
    }
}
