package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroTextoTest {

    @Test
    fun `Qualquer aceita qualquer valor, inclusive vazio`() {
        val filtro = FiltroTexto(ModoFiltro.Qualquer)
        assertTrue(filtro.aceita("qualquer coisa", ignorarMaiusculas = false))
        assertTrue(filtro.aceita("", ignorarMaiusculas = false))
    }

    @Test
    fun `Contem aceita quando o valor contem algum dos termos`() {
        val filtro = FiltroTexto(ModoFiltro.Contem, listOf("urgente", "emergência"))
        assertTrue(filtro.aceita("Isto é urgente!", ignorarMaiusculas = false))
        assertTrue(filtro.aceita("houve uma emergência", ignorarMaiusculas = false))
        assertFalse(filtro.aceita("tudo tranquilo", ignorarMaiusculas = false))
    }

    @Test
    fun `Igual aceita somente quando o valor e identico a algum termo`() {
        val filtro = FiltroTexto(ModoFiltro.Igual, listOf("Alarme", "Pânico"))
        assertTrue(filtro.aceita("Alarme", ignorarMaiusculas = false))
        assertTrue(filtro.aceita("Pânico", ignorarMaiusculas = false))
        assertFalse(filtro.aceita("Alarme disparado", ignorarMaiusculas = false))
    }

    @Test
    fun `Nao contem aceita somente quando o valor nao contem nenhum dos termos`() {
        val filtro = FiltroTexto(ModoFiltro.NaoContem, listOf("promoção", "oferta"))
        assertTrue(filtro.aceita("Sua entrega chegou", ignorarMaiusculas = false))
        assertFalse(filtro.aceita("Grande oferta hoje", ignorarMaiusculas = false))
        assertFalse(filtro.aceita("promoção relâmpago", ignorarMaiusculas = false))
    }

    @Test
    fun `ignorar maiusculas vale para todos os modos que comparam texto`() {
        assertTrue(FiltroTexto(ModoFiltro.Contem, listOf("URGENTE")).aceita("é urgente", ignorarMaiusculas = true))
        assertFalse(FiltroTexto(ModoFiltro.Contem, listOf("URGENTE")).aceita("é urgente", ignorarMaiusculas = false))
        assertTrue(FiltroTexto(ModoFiltro.Igual, listOf("alarme")).aceita("ALARME", ignorarMaiusculas = true))
        assertFalse(FiltroTexto(ModoFiltro.Igual, listOf("alarme")).aceita("ALARME", ignorarMaiusculas = false))
        assertFalse(FiltroTexto(ModoFiltro.NaoContem, listOf("Oferta")).aceita("grande OFERTA", ignorarMaiusculas = true))
        assertTrue(FiltroTexto(ModoFiltro.NaoContem, listOf("Oferta")).aceita("grande OFERTA", ignorarMaiusculas = false))
    }

    @Test
    fun `acentos nao sao normalizados`() {
        assertFalse(FiltroTexto(ModoFiltro.Contem, listOf("urgência")).aceita("urgencia", ignorarMaiusculas = true))
    }
}
