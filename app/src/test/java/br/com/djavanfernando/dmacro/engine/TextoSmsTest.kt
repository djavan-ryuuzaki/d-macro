package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoSmsTest {

    @Test
    fun `texto acima do limite e cortado e termina com reticencias`() {
        val cortado = cortarSms("a".repeat(400), 300)
        assertEquals(300, cortado.length)
        assertEquals("a".repeat(299) + "…", cortado)
    }

    @Test
    fun `texto dentro do limite fica intacto`() {
        assertEquals("a".repeat(300), cortarSms("a".repeat(300), 300))
        assertEquals("curto", cortarSms("curto", 300))
    }
}
