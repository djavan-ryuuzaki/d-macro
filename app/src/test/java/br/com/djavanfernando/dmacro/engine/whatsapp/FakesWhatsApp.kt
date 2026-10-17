package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant

class AparelhoFake : EstadoAparelho {
    val bloqueada = MutableStateFlow(false)
    val emChamada = MutableStateFlow(false)
    override fun telaBloqueada() = bloqueada.value
    override fun chamadaEmAndamento() = emChamada.value
    override suspend fun aguardarLiberado() {
        combine(bloqueada, emChamada) { b, c -> !b && !c }.first { it }
    }
}

class PrazosFake : PrazoItem {
    val agendados = mutableListOf<Pair<String, Instant>>()
    override fun agendar(idItem: String, prazo: Instant) { agendados += idItem to prazo }
}

/**
 * Simula o WhatsApp: abrir um link mostra a conversa depois de [leiturasAteAbrir] leituras;
 * tocar em Enviar esvazia o campo, a menos que [envioTrava] esteja ligado.
 */
class TelaFake : TelaWhatsApp {
    sealed interface Evento {
        data class Abriu(val app: AppWhatsApp, val numero: String, val texto: String) : Evento
        data class Tocou(val no: NoTela) : Evento
        data object Voltou : Evento
        data object Inicio : Evento
    }

    val eventos = mutableListOf<Evento>()
    var instalados = mutableSetOf(AppWhatsApp.Pessoal)
    var disponivel = true
    var leiturasAteAbrir = 1
    val semWhatsApp = mutableSetOf<String>()
    /** Rascunho que já estava digitado na conversa; o WhatsApp mantém o rascunho em vez do texto do link. */
    val rascunhos = mutableMapOf<String, String>()
    var envioTrava = false
    /** Conversa que nunca aparece (WhatsApp mudou a tela, por exemplo). */
    var nuncaAbre = false
    /** Leituras em que a tela "Enviar para…" aparece de passagem antes da conversa. */
    var leiturasComSeletorAntes = 0

    val enviados get() = eventos.filterIsInstance<Evento.Tocou>().filter { it.no.idView == ID_ENVIAR }

    private var tela: List<NoTela> = emptyList()
    private var proxima: List<NoTela> = emptyList()
    private var leiturasRestantes = 0
    private var seletorRestante = 0

    override fun instalado(app: AppWhatsApp) = app in instalados
    override fun disponivel() = disponivel

    override fun abrirConversa(app: AppWhatsApp, numero: String, texto: String) {
        eventos += Evento.Abriu(app, numero, texto)
        tela = emptyList()
        leiturasRestantes = leiturasAteAbrir
        seletorRestante = leiturasComSeletorAntes
        proxima = when {
            nuncaAbre -> emptyList()
            numero in semWhatsApp -> SELETOR
            else -> conversa(campo = rascunhos[numero] ?: texto, comEnviar = true)
        }
    }

    override fun ler(): List<NoTela> {
        if (seletorRestante > 0) {
            seletorRestante--
            return SELETOR
        }
        if (leiturasRestantes > 0 && --leiturasRestantes == 0) tela = proxima
        return tela
    }

    override fun tocar(no: NoTela) {
        eventos += Evento.Tocou(no)
        if (no.idView == ID_ENVIAR && !envioTrava) tela = conversa(campo = "Mensagem", comEnviar = false)
    }

    override fun voltar() { eventos += Evento.Voltou; tela = emptyList() }
    override fun irParaInicio() { eventos += Evento.Inicio; tela = emptyList() }

    private fun conversa(campo: String, comEnviar: Boolean) = listOfNotNull(
        NoTela(0, "com.whatsapp:id/entry", "android.widget.EditText", campo, null, true),
        if (comEnviar) NoTela(1, ID_ENVIAR, "android.widget.ImageButton", null, "Enviar", true) else null,
    )

    companion object {
        const val ID_ENVIAR = "com.whatsapp:id/send"
        val SELETOR = listOf(NoTela(0, "com.whatsapp:id/contact_picker_layout", null, null, null, false))
    }
}
