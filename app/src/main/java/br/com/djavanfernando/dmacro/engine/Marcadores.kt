package br.com.djavanfernando.dmacro.engine

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Dados do evento que satisfez o Gatilho, disponíveis aos Marcadores. */
data class ContextoMarcadores(
    val app: String,
    val titulo: String,
    val texto: String,
    val instante: Instant,
    val zona: ZoneId,
) {
    companion object {
        fun de(evento: EventoNotificacao, zona: ZoneId) =
            ContextoMarcadores(evento.nomeApp, evento.titulo, evento.texto, evento.instante, zona)

        /** Valores fictícios usados pelo Teste. */
        fun exemplo(instante: Instant, zona: ZoneId) =
            ContextoMarcadores("App de exemplo", "Título de exemplo", "Texto de exemplo", instante, zona)
    }
}

/** Corta o texto do SMS para no máximo [limite] caracteres, terminando com "…" quando há corte. */
fun cortarSms(texto: String, limite: Int): String =
    if (texto.length <= limite) texto else texto.take(limite - 1) + "…"

object Marcadores {
    /** Marcadores disponíveis, na ordem em que o editor os oferece. */
    val todos = listOf("{app}", "{titulo}", "{texto}", "{data}", "{hora}")

    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")
    private val PADRAO = Regex("""\{(app|titulo|texto|data|hora)\}""")

    /** Substitui em uma única passada, para que valores do evento nunca sejam lidos como Marcadores. */
    fun renderizar(modelo: String, contexto: ContextoMarcadores): String {
        val momento = contexto.instante.atZone(contexto.zona)
        return PADRAO.replace(modelo) { m ->
            when (m.groupValues[1]) {
                "app" -> contexto.app
                "titulo" -> contexto.titulo
                "texto" -> contexto.texto
                "data" -> momento.format(DATA)
                else -> momento.format(HORA)
            }
        }
    }
}
