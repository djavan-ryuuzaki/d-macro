package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RestricaoTest {

    private val zona = ZoneId.of("America/Sao_Paulo")

    /** 2026-10-12 é uma segunda-feira. */
    private fun em(dia: Int, hora: Int, minuto: Int = 0): Instant =
        LocalDateTime.of(2026, 10, dia, hora, minuto).atZone(zona).toInstant()

    private fun Restricao.permiteEm(instante: Instant, ultimaExecucao: Instant? = null) =
        permite(ContextoRestricao(instante, zona, ultimaExecucao))

    @Test
    fun `faixa de horario no mesmo dia inclui o inicio e exclui o fim`() {
        val faixa = FaixaHorario(LocalTime.of(8, 0), LocalTime.of(18, 0))
        assertTrue(faixa.permiteEm(em(12, 8, 0)))
        assertTrue(faixa.permiteEm(em(12, 17, 59)))
        assertFalse(faixa.permiteEm(em(12, 18, 0)))
        assertFalse(faixa.permiteEm(em(12, 7, 59)))
    }

    @Test
    fun `faixa que passa da meia-noite`() {
        val faixa = FaixaHorario(LocalTime.of(22, 0), LocalTime.of(6, 0))
        assertTrue(faixa.permiteEm(em(12, 23, 0)))
        assertTrue(faixa.permiteEm(em(13, 2, 0)))
        assertFalse(faixa.permiteEm(em(12, 12, 0)))
        assertFalse(faixa.permiteEm(em(13, 6, 0)))
    }

    @Test
    fun `dia da semana e avaliado no instante do disparo`() {
        val segunda = DiasSemana(setOf(DayOfWeek.MONDAY))
        assertTrue(segunda.permiteEm(em(12, 23, 0)))
        assertFalse(segunda.permiteEm(em(13, 2, 0)))
    }

    @Test
    fun `limite de frequencia barra dentro da janela e libera ao completar os minutos`() {
        val limite = LimiteFrequencia(5)
        val execucao = em(12, 10, 0)
        assertTrue(limite.permiteEm(em(12, 10, 4), ultimaExecucao = null))
        assertFalse(limite.permiteEm(em(12, 10, 4), ultimaExecucao = execucao))
        assertTrue(limite.permiteEm(em(12, 10, 5), ultimaExecucao = execucao))
    }

    @Test
    fun `restricoes sao avaliadas em ordem e a primeira reprovada e devolvida`() {
        val faixa = FaixaHorario(LocalTime.of(8, 0), LocalTime.of(18, 0))
        val dias = DiasSemana(setOf(DayOfWeek.TUESDAY))
        val contexto = ContextoRestricao(em(12, 20, 0), zona, null)
        assertEquals(faixa, primeiraReprovada(listOf(faixa, dias), contexto))
        assertEquals(dias, primeiraReprovada(listOf(dias, faixa), contexto))
        assertNull(primeiraReprovada(emptyList(), contexto))
    }
}
