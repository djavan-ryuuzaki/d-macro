package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MarcadoresTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val contexto = ContextoMarcadores(
        app = "Banco X",
        titulo = "Compra",
        texto = "R$ 50,00 aprovada",
        instante = LocalDateTime.of(2026, 10, 12, 9, 5).atZone(zona).toInstant(),
        zona = zona,
    )

    @Test
    fun `substitui todos os marcadores`() {
        assertEquals(
            "Banco X: Compra - R$ 50,00 aprovada (12/10/2026 09:05)",
            Marcadores.renderizar("{app}: {titulo} - {texto} ({data} {hora})", contexto),
        )
    }

    @Test
    fun `marcador desconhecido permanece literal`() {
        assertEquals("{foo} Compra", Marcadores.renderizar("{foo} {titulo}", contexto))
    }

    @Test
    fun `valor do evento contendo chaves nao e reinterpretado como marcador`() {
        val ctx = contexto.copy(titulo = "{texto}", texto = "segredo")
        assertEquals("{texto} / segredo", Marcadores.renderizar("{titulo} / {texto}", ctx))
    }

    @Test
    fun `contexto de exemplo para o Teste`() {
        val ctx = ContextoMarcadores.exemplo(contexto.instante, zona)
        assertEquals(
            "App de exemplo|Título de exemplo|Texto de exemplo|12/10/2026|09:05",
            Marcadores.renderizar("{app}|{titulo}|{texto}|{data}|{hora}", ctx),
        )
    }

    @Test
    fun `contexto a partir de um evento de notificacao`() {
        val evento = EventoNotificacao(
            pacote = "com.banco.x", nomeApp = "Banco X", titulo = "Compra", texto = "R$ 50,00 aprovada",
            chave = "k", emCurso = false, resumoDeGrupo = false, instante = contexto.instante,
        )
        assertEquals(contexto, ContextoMarcadores.de(evento, zona))
    }
}
