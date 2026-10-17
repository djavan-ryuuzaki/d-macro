package br.com.djavanfernando.dmacro.platform

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmtpClienteTest {

    private val setup = ServerSetup(3125, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
    private val servidor = GreenMail(setup).withConfiguration(GreenMailConfiguration.aConfig().withUser("ana@exemplo.com", "ana@exemplo.com", "segredo"))
    private val cliente = SmtpCliente(timeoutMs = 5_000)

    private fun conta(senha: String = "segredo", porta: Int = 3125) = ContaEmail(
        servidor = "127.0.0.1", porta = porta, seguranca = Seguranca.Nenhuma,
        usuario = "ana@exemplo.com", senha = senha,
    )

    @Before fun iniciar() = servidor.start()
    @After fun parar() = servidor.stop()

    @Test
    fun `envia e-mail com assunto e corpo em UTF-8 do usuario para o destinatario`() {
        val resultado = cliente.enviar(conta(), "bia@exemplo.com", "Alerta às 09:00 — ç", "Corpo com acentuação: ã é ü")

        assertEquals(ResultadoSmtp.Ok, resultado)
        val recebida = servidor.receivedMessages.single()
        assertEquals("Alerta às 09:00 — ç", recebida.subject)
        assertEquals("ana@exemplo.com", recebida.from.single().toString())
        assertEquals("bia@exemplo.com", recebida.allRecipients.single().toString())
        assertTrue(GreenMailUtil.getBody(recebida).isNotEmpty())
        assertEquals("Corpo com acentuação: ã é ü", recebida.content.toString().trim())
    }

    @Test
    fun `senha errada e falha de autenticacao`() {
        val resultado = cliente.enviar(conta(senha = "errada"), "bia@exemplo.com", "a", "b")
        assertTrue("foi $resultado", resultado is ResultadoSmtp.FalhaAutenticacao)
    }

    @Test
    fun `servidor fora do ar e falha temporaria`() {
        val resultado = cliente.enviar(conta(porta = 3999), "bia@exemplo.com", "a", "b")
        assertTrue("foi $resultado", resultado is ResultadoSmtp.FalhaTemporaria)
    }
}
