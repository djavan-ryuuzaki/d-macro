package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures capturadas do WhatsApp 2.26.40.76 em pt-BR, num Samsung com Android 16, e anonimizadas. */
class ReconhecimentoTelaTest {

    private fun no(i: Int, id: String? = null, classe: String? = null, texto: String? = null, desc: String? = null, clicavel: Boolean = false) =
        NoTela(i, id, classe, texto, desc, clicavel)

    @Test
    fun `conversa aberta pelo link com o texto no campo esta pronta para enviar`() {
        val estado = reconhecer(LeitorDump.ler("conversa-com-texto.xml"), AppWhatsApp.Pessoal)

        estado as EstadoTela.ConversaPronta
        assertEquals("com.whatsapp:id/send", estado.enviar.idView)
        assertEquals("Teste D-Macro", estado.campo.texto)
    }

    @Test
    fun `depois do envio o botao Enviar some e o campo mostra a dica`() {
        val nos = LeitorDump.ler("depois-de-enviar.xml")

        assertEquals(EstadoTela.Aguardar, reconhecer(nos, AppWhatsApp.Pessoal))
        assertEquals("Mensagem", campoDe(nos, AppWhatsApp.Pessoal)?.texto)
    }

    @Test
    fun `numero sem WhatsApp abre a tela Enviar para e deve ser abandonada sem tocar em nada`() {
        assertEquals(EstadoTela.NumeroSemWhatsApp(null), reconhecer(LeitorDump.ler("numero-sem-whatsapp.xml"), AppWhatsApp.Pessoal))
    }

    @Test
    fun `aviso de numero sem WhatsApp em dialogo e fechado pelo botao que so cancela`() {
        val cancelar = no(2, texto = "Cancelar", clicavel = true)
        val nos = listOf(
            no(0, classe = "android.widget.FrameLayout"),
            no(1, id = "android:id/message", texto = "O número de telefone +55 41 90000-0001 compartilhado via link não está no WhatsApp."),
            cancelar,
            no(3, id = "android:id/button1", classe = "android.widget.Button", texto = "OK", clicavel = true),
        )

        assertEquals(EstadoTela.NumeroSemWhatsApp(cancelar), reconhecer(nos, AppWhatsApp.Pessoal))
    }

    @Test
    fun `sem os ids conhecidos o botao e achado pela descricao exata e o campo pela classe`() {
        val enviar = no(2, id = "com.whatsapp:id/novo_botao", classe = "android.widget.ImageButton", desc = "Send", clicavel = true)
        val campo = no(1, id = "com.whatsapp:id/novo_campo", classe = "android.widget.EditText", texto = "Alarme")
        val nos = listOf(no(0), campo, enviar)

        assertEquals(EstadoTela.ConversaPronta(enviar, campo), reconhecer(nos, AppWhatsApp.Pessoal))
    }

    @Test
    fun `descricao que so contem a palavra enviar nao e o botao Enviar`() {
        val nos = listOf(
            no(0, classe = "android.widget.EditText", texto = "Alarme"),
            no(1, classe = "android.widget.ImageButton", desc = "Mensagem de voz, Botão. Solte para enviar.", clicavel = true),
        )

        assertEquals(EstadoTela.Aguardar, reconhecer(nos, AppWhatsApp.Pessoal))
    }

    @Test
    fun `WhatsApp Business usa os ids do proprio pacote`() {
        val campo = no(0, id = "com.whatsapp.w4b:id/entry", classe = "android.widget.EditText", texto = "Oi")
        val enviar = no(1, id = "com.whatsapp.w4b:id/send", classe = "android.widget.ImageButton", clicavel = true)

        assertEquals(EstadoTela.ConversaPronta(enviar, campo), reconhecer(listOf(campo, enviar), AppWhatsApp.Business))
    }

    @Test
    fun `tela vazia ou de outro app manda aguardar`() {
        assertEquals(EstadoTela.Aguardar, reconhecer(emptyList(), AppWhatsApp.Pessoal))
        assertTrue(campoDe(emptyList(), AppWhatsApp.Pessoal) == null)
    }

    @Test
    fun `painel de convite por SMS do WhatsApp 2_26 so pode ser fechado por Agora nao, nunca pelo botao de convite`() {
        val agoraNao = no(4, texto = "Agora não", classe = "android.widget.Button", clicavel = true)
        val nos = listOf(
            no(0, texto = "Enviar convite por SMS"),
            no(1, texto = "+55 41 90000-0001 não está no WhatsApp. Deseja convidar essa pessoa para o WhatsApp?"),
            no(2, classe = "android.widget.EditText", texto = "Vamos conversar no WhatsApp"),
            no(3, texto = "Enviar convite por SMS", classe = "android.widget.Button", clicavel = true),
            agoraNao,
        )

        assertEquals(EstadoTela.NumeroSemWhatsApp(agoraNao), reconhecer(nos, AppWhatsApp.Pessoal))
    }
}
