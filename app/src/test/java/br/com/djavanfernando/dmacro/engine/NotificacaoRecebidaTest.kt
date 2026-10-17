package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotificacaoRecebidaTest {

    private fun evento(
        pacote: String = "com.banco.x",
        titulo: String = "Banco X",
        texto: String = "Compra aprovada R$ 5.000",
        emCurso: Boolean = false,
    ) = EventoNotificacao(
        pacote = pacote,
        nomeApp = "Banco X",
        titulo = titulo,
        texto = texto,
        chave = "0|$pacote|1",
        emCurso = emCurso,
        resumoDeGrupo = false,
        instante = Instant.parse("2026-10-11T12:00:00Z"),
    )

    private val gatilhoBanco = NotificacaoRecebida(
        filtroTitulo = FiltroTexto(ModoFiltro.Contem, listOf("banco")),
        filtroTexto = FiltroTexto(ModoFiltro.Contem, listOf("aprovada")),
    )

    @Test
    fun `filtros de titulo e texto precisam passar os dois`() {
        assertTrue(gatilhoBanco.satisfeitoPor(evento()))
        assertFalse(gatilhoBanco.satisfeitoPor(evento(texto = "Compra negada R$ 5.000")))
        assertFalse(gatilhoBanco.satisfeitoPor(evento(titulo = "Loja Y")))
    }

    @Test
    fun `selecao de apps restringe o pacote de origem`() {
        val gatilho = NotificacaoRecebida(apps = SelecaoApps.Somente(setOf("com.banco.x", "com.whatsapp")))
        assertTrue(gatilho.satisfeitoPor(evento(pacote = "com.banco.x")))
        assertTrue(gatilho.satisfeitoPor(evento(pacote = "com.whatsapp")))
        assertFalse(gatilho.satisfeitoPor(evento(pacote = "com.outro")))
        assertTrue(NotificacaoRecebida(apps = SelecaoApps.Todos).satisfeitoPor(evento(pacote = "com.outro")))
    }

    @Test
    fun `notificacoes em curso sao ignoradas somente quando a opcao esta ligada`() {
        assertFalse(NotificacaoRecebida(ignorarEmCurso = true).satisfeitoPor(evento(emCurso = true)))
        assertTrue(NotificacaoRecebida(ignorarEmCurso = false).satisfeitoPor(evento(emCurso = true)))
        assertTrue(NotificacaoRecebida(ignorarEmCurso = true).satisfeitoPor(evento(emCurso = false)))
    }

    @Test
    fun `ignorar maiusculas do gatilho se aplica aos dois filtros`() {
        val sensivel = gatilhoBanco.copy(ignorarMaiusculas = false)
        assertFalse(sensivel.satisfeitoPor(evento()))
        assertTrue(sensivel.satisfeitoPor(evento(titulo = "meu banco")))
    }
}
