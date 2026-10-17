package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumeroWhatsAppTest {

    @Test
    fun `numero com mais usa os digitos como estao`() {
        assertEquals("5511987654321", numeroWhatsApp("+55 (11) 98765-4321"))
        assertEquals("14155550123", numeroWhatsApp("+1 415 555 0123"))
    }

    @Test
    fun `numero com DDD sem DDI recebe 55`() {
        assertEquals("5511987654321", numeroWhatsApp("(11) 98765-4321"))
        assertEquals("551132654321", numeroWhatsApp("11 3265-4321"))
    }

    @Test
    fun `zeros a esquerda sao removidos antes de acrescentar o DDI`() {
        assertEquals("5511987654321", numeroWhatsApp("011 98765-4321"))
    }

    @Test
    fun `numero que ja comeca com 55 e tem 12 ou 13 digitos fica como esta`() {
        assertEquals("5511987654321", numeroWhatsApp("5511987654321"))
        assertEquals("551132654321", numeroWhatsApp("551132654321"))
    }

    @Test
    fun `numero sem DDD ou de formato desconhecido nao converte`() {
        assertNull(numeroWhatsApp("98765-4321"))
        assertNull(numeroWhatsApp("190"))
        assertNull(numeroWhatsApp("123456789012345"))
        assertNull(numeroWhatsApp("+12"))
        assertNull(numeroWhatsApp(""))
    }
}
