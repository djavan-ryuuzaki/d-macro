package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ValidacaoMacroTest {

    private val gatilhoValido = NotificacaoRecebida(filtroTexto = FiltroTexto(ModoFiltro.Contem, listOf("alarme")))
    private val smsValido = EnviarSms(listOf(Telefone("+55 11 99999-0000", "Ana")), "Alerta: {texto}")

    private fun macro(
        gatilhos: List<Gatilho> = listOf(gatilhoValido),
        acoes: List<Acao> = listOf(smsValido),
        restricoes: List<Restricao> = emptyList(),
    ) = Macro(id = "m1", nome = "Teste", gatilhos = gatilhos, acoes = acoes, restricoes = restricoes)

    @Test
    fun `macro completa pode ser ativada`() {
        assertTrue(validar(macro()).isEmpty())
    }

    @Test
    fun `macro sem gatilho e sem acao nao pode ser ativada`() {
        assertEquals(
            listOf(ProblemaMacro.SemGatilho, ProblemaMacro.SemAcao),
            validar(macro(gatilhos = emptyList(), acoes = emptyList())),
        )
    }

    @Test
    fun `filtro que compara texto precisa de ao menos um termo nao vazio`() {
        val gatilho = NotificacaoRecebida(filtroTitulo = FiltroTexto(ModoFiltro.Igual, listOf("  ")))
        assertEquals(listOf(ProblemaMacro.FiltroSemTermos), validar(macro(gatilhos = listOf(gatilho))))
    }

    @Test
    fun `selecao de apps especificos nao pode ser vazia`() {
        val gatilho = NotificacaoRecebida(apps = SelecaoApps.Somente(emptySet()))
        assertEquals(listOf(ProblemaMacro.SemApps), validar(macro(gatilhos = listOf(gatilho))))
    }

    @Test
    fun `acoes de mensagem precisam de destinatarios validos e texto`() {
        val acoes = listOf(
            EnviarSms(emptyList(), "oi"),
            EnviarSms(listOf(Telefone("abc")), "oi"),
            EnviarSms(listOf(Telefone("11999990000")), "  "),
            EnviarEmail(listOf(Email("nao-e-email")), "Assunto", "corpo"),
            EnviarEmail(listOf(Email("ana@exemplo.com")), "Assunto", ""),
            EfetuarChamada(Telefone("12")),
        )
        assertEquals(
            listOf(
                ProblemaMacro.SemDestinatario(0),
                ProblemaMacro.DestinatarioInvalido(1, "abc"),
                ProblemaMacro.MensagemVazia(2),
                ProblemaMacro.DestinatarioInvalido(3, "nao-e-email"),
                ProblemaMacro.MensagemVazia(4),
                ProblemaMacro.DestinatarioInvalido(5, "12"),
            ),
            validar(macro(acoes = acoes)),
        )
    }

    @Test
    fun `restricoes precisam de valores validos`() {
        val restricoes = listOf(DiasSemana(emptySet()), LimiteFrequencia(0), DiasSemana(setOf(DayOfWeek.MONDAY)))
        assertEquals(
            listOf(ProblemaMacro.RestricaoInvalida(0), ProblemaMacro.RestricaoInvalida(1)),
            validar(macro(restricoes = restricoes)),
        )
    }

    @Test
    fun `whatsapp precisa de numero que converte para o formato internacional`() {
        val acao = EnviarWhatsApp(listOf(Telefone("(11) 98765-4321"), Telefone("98765-4321", "Sem DDD")), "Alerta")
        assertEquals(
            listOf(ProblemaMacro.NumeroSemDdi(0, "98765-4321")),
            validar(macro(acoes = listOf(acao))),
        )
    }

    @Test
    fun `whatsapp precisa de destinatario e mensagem`() {
        assertEquals(
            listOf(ProblemaMacro.SemDestinatario(0), ProblemaMacro.MensagemVazia(0)),
            validar(macro(acoes = listOf(EnviarWhatsApp(emptyList(), " ")))),
        )
    }
}
