package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.engine.ExecutionLog
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.WhatsAppSender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Fila do WhatsApp: única no app, envia um item por vez porque só existe uma tela.
 * Cada item espera a tela desbloqueada e sem chamada, abre a conversa, confere o campo, toca em Enviar
 * e confirma. Nunca toca em Enviar duas vezes para o mesmo item.
 */
class FilaWhatsApp(
    escopo: CoroutineScope,
    private val relogio: Clock,
    private val aparelho: EstadoAparelho,
    private val tela: TelaWhatsApp,
    private val prazos: PrazoItem,
    private val log: ExecutionLog,
) : WhatsAppSender {

    private data class Item(val id: String, val numero: String, val texto: String, val app: AppWhatsApp, val prazo: Instant)

    private val canal = Channel<Item>(Channel.UNLIMITED)

    init {
        escopo.launch { consumir() }
    }

    override suspend fun enfileirar(idItem: String, numero: String, texto: String, app: AppWhatsApp, prazo: Instant): ResultadoAcao {
        if (!tela.instalado(app)) return ResultadoAcao.Erro(MOTIVO_NAO_INSTALADO)
        if (!tela.disponivel()) return ResultadoAcao.Erro(MOTIVO_SERVICO_DESLIGADO)
        prazos.agendar(idItem, prazo)
        canal.send(Item(idItem, numero, texto, app, prazo))
        return ResultadoAcao.Pendente
    }

    private suspend fun consumir() {
        while (true) {
            var item: Item? = canal.receive()
            var abriuWhatsApp = false
            while (item != null) {
                abriuWhatsApp = processar(item) || abriuWhatsApp
                item = canal.tryReceive().getOrNull()
            }
            if (abriuWhatsApp) tela.irParaInicio()
        }
    }

    /** Processa um item e grava o resultado. Devolve se chegou a abrir o WhatsApp. */
    private suspend fun processar(item: Item): Boolean {
        var abriu = false
        val resultado = try {
            val restante = Duration.between(relogio.instant(), item.prazo).toMillis()
            val liberado = withTimeoutOrNull(restante) { aparelho.aguardarLiberado(); true }
            when {
                liberado == null -> ResultadoAcao.Erro(motivoDoPrazo())
                !tela.disponivel() -> ResultadoAcao.Erro(MOTIVO_SERVICO_DESLIGADO)
                else -> {
                    abriu = true
                    enviar(item)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoAcao.Erro(e.message ?: e.javaClass.simpleName)
        }
        log.atualizarItem(item.id, resultado)
        return abriu
    }

    private suspend fun enviar(item: Item): ResultadoAcao {
        tela.abrirConversa(item.app, item.numero, item.texto)

        // A tela "Enviar para…" pode aparecer de passagem antes da conversa; só conta se persistir.
        val estado = withTimeoutOrNull<EstadoTela>(ESPERA_CONVERSA_MS) {
            var leiturasSemWhatsApp = 0
            while (true) {
                delay(INTERVALO_LEITURA_MS)
                when (val e = reconhecer(tela.ler(), item.app)) {
                    is EstadoTela.ConversaPronta -> return@withTimeoutOrNull e
                    is EstadoTela.NumeroSemWhatsApp -> if (++leiturasSemWhatsApp >= LEITURAS_SEM_WHATSAPP) return@withTimeoutOrNull e
                    EstadoTela.Aguardar -> leiturasSemWhatsApp = 0
                }
            }
            @Suppress("UNREACHABLE_CODE")
            EstadoTela.Aguardar
        }

        val pronta = when (estado) {
            null, EstadoTela.Aguardar -> return ResultadoAcao.Erro(MOTIVO_NAO_RECONHECIDA)
            is EstadoTela.NumeroSemWhatsApp -> {
                estado.fechar?.let(tela::tocar) ?: tela.voltar()
                return ResultadoAcao.Erro(MOTIVO_SEM_WHATSAPP)
            }
            is EstadoTela.ConversaPronta -> estado
        }
        if (normalizar(pronta.campo.texto) != normalizar(item.texto)) return ResultadoAcao.Erro(MOTIVO_RASCUNHO)

        tela.tocar(pronta.enviar)

        val confirmado = withTimeoutOrNull(ESPERA_CONFIRMACAO_MS) {
            while (!enviado(tela.ler(), item)) delay(INTERVALO_LEITURA_MS)
            true
        }
        return if (confirmado == true) ResultadoAcao.Ok else ResultadoAcao.Erro(MOTIVO_NAO_CONFIRMADO)
    }

    /** O campo continua na tela, mas a mensagem saiu dele ou o botão Enviar sumiu. */
    private fun enviado(nos: List<NoTela>, item: Item): Boolean {
        val campo = campoDe(nos, item.app) ?: return false
        return normalizar(campo.texto) != normalizar(item.texto) || reconhecer(nos, item.app) !is EstadoTela.ConversaPronta
    }

    private fun motivoDoPrazo() = when {
        aparelho.telaBloqueada() -> MOTIVO_BLOQUEADA
        aparelho.chamadaEmAndamento() -> MOTIVO_CHAMADA
        else -> MOTIVO_PRAZO
    }

    private fun normalizar(texto: String?) = texto.orEmpty().replace("\r\n", "\n").trim()

    companion object {
        const val ESPERA_CONVERSA_MS = 15_000L
        const val ESPERA_CONFIRMACAO_MS = 5_000L
        const val INTERVALO_LEITURA_MS = 300L
        /** Leituras seguidas (cerca de 1,5 s) da tela de número recusado antes de desistir. */
        const val LEITURAS_SEM_WHATSAPP = 5

        const val MOTIVO_NAO_INSTALADO = "WhatsApp não instalado"
        const val MOTIVO_SERVICO_DESLIGADO = "serviço de acessibilidade desligado"
        const val MOTIVO_BLOQUEADA = "tela bloqueada até o fim do prazo de 30 minutos"
        const val MOTIVO_CHAMADA = "chamada em andamento até o fim do prazo de 30 minutos"
        const val MOTIVO_PRAZO = "prazo de 30 minutos esgotado"
        const val MOTIVO_SEM_WHATSAPP = "número sem WhatsApp"
        const val MOTIVO_NAO_RECONHECIDA = "tela do WhatsApp não reconhecida"
        const val MOTIVO_NAO_CONFIRMADO = "envio não confirmado"
        const val MOTIVO_RASCUNHO = "a conversa já tinha outro texto digitado; nada foi enviado"
    }
}
