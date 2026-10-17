package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.engine.Desfecho
import br.com.djavanfernando.dmacro.engine.ItemRegistro
import br.com.djavanfernando.dmacro.engine.LogFake
import br.com.djavanfernando.dmacro.engine.RegistroExecucao
import br.com.djavanfernando.dmacro.engine.RelogioDeTeste
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.TipoAcao
import br.com.djavanfernando.dmacro.engine.whatsapp.TelaFake.Evento
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FilaWhatsAppTest {

    private val relogio = RelogioDeTeste(Instant.parse("2026-10-17T12:00:00Z"))
    private val aparelho = AparelhoFake()
    private val tela = TelaFake()
    private val prazos = PrazosFake()
    private val log = LogFake()
    private val prazo = relogio.instant().plus(Duration.ofMinutes(30))

    private fun TestScope.fila() = FilaWhatsApp(backgroundScope, relogio, aparelho, tela, prazos, log)

    /** Registro com itens pendentes, como o Motor grava antes de a Fila terminar. */
    private suspend fun registrarPendentes(vararg ids: String) {
        log.registrar(
            RegistroExecucao(
                "r1", "m1", "Macro", relogio.instant(), false, null, Desfecho.Executada,
                ids.mapIndexed { i, id -> ItemRegistro(id, 0, TipoAcao.WhatsApp, "n$i", null, ResultadoAcao.Pendente) },
            ),
        )
    }

    private fun resultado(id: String) = log.registros.single().itens.single { it.id == id }.resultado

    private suspend fun FilaWhatsApp.enviar(id: String, numero: String = "5541900000001", texto: String = "Alarme") =
        enfileirar(id, numero, texto, AppWhatsApp.Pessoal, prazo)

    @Test
    fun `envia abrindo a conversa, tocando em Enviar e volta para a tela inicial`() = runTest {
        registrarPendentes("a")
        val fila = fila()

        assertEquals(ResultadoAcao.Pendente, fila.enviar("a", texto = "Alarme: Sensor"))
        runCurrent(); advanceTimeBy(5_000); runCurrent()

        assertEquals(Evento.Abriu(AppWhatsApp.Pessoal, "5541900000001", "Alarme: Sensor"), tela.eventos.first())
        assertEquals(1, tela.enviados.size)
        assertEquals(Evento.Inicio, tela.eventos.last())
        assertEquals(ResultadoAcao.Ok, resultado("a"))
        assertEquals(listOf("a" to prazo), prazos.agendados)
    }

    @Test
    fun `itens enfileirados juntos saem um depois do outro e so voltam ao inicio no fim`() = runTest {
        registrarPendentes("a", "b")
        val fila = fila()

        fila.enviar("a", numero = "5541911111111")
        fila.enviar("b", numero = "5541922222222")
        runCurrent(); advanceTimeBy(10_000); runCurrent()

        assertEquals(
            listOf("Abriu 5541911111111", "Tocou", "Abriu 5541922222222", "Tocou", "Inicio"),
            tela.eventos.map {
                when (it) {
                    is Evento.Abriu -> "Abriu ${it.numero}"
                    is Evento.Tocou -> "Tocou"
                    Evento.Voltou -> "Voltou"
                    Evento.Inicio -> "Inicio"
                }
            },
        )
        assertEquals(ResultadoAcao.Ok, resultado("a"))
        assertEquals(ResultadoAcao.Ok, resultado("b"))
    }

    @Test
    fun `com a tela bloqueada espera e envia logo depois do desbloqueio`() = runTest {
        registrarPendentes("a")
        aparelho.bloqueada.value = true
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(Duration.ofMinutes(10).toMillis()); runCurrent()
        assertEquals(emptyList<Evento>(), tela.eventos)
        assertEquals(ResultadoAcao.Pendente, resultado("a"))

        aparelho.bloqueada.value = false
        runCurrent(); advanceTimeBy(5_000); runCurrent()
        assertEquals(ResultadoAcao.Ok, resultado("a"))
    }

    @Test
    fun `tela bloqueada ate o fim do prazo falha sem abrir o WhatsApp`() = runTest {
        registrarPendentes("a")
        aparelho.bloqueada.value = true
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(Duration.ofMinutes(30).toMillis() + 1); runCurrent()

        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_BLOQUEADA), resultado("a"))
        assertEquals(emptyList<Evento>(), tela.eventos)
    }

    @Test
    fun `durante uma chamada espera ela terminar`() = runTest {
        registrarPendentes("a")
        aparelho.emChamada.value = true
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(60_000); runCurrent()
        assertEquals(emptyList<Evento>(), tela.eventos)

        aparelho.emChamada.value = false
        runCurrent(); advanceTimeBy(5_000); runCurrent()
        assertEquals(ResultadoAcao.Ok, resultado("a"))
    }

    @Test
    fun `numero sem WhatsApp sai da tela com Voltar, registra o erro e segue para o proximo`() = runTest {
        registrarPendentes("a", "b")
        tela.semWhatsApp += "5541900000009"
        val fila = fila()

        fila.enviar("a", numero = "5541900000009")
        fila.enviar("b")
        runCurrent(); advanceTimeBy(10_000); runCurrent()

        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_SEM_WHATSAPP), resultado("a"))
        assertEquals(ResultadoAcao.Ok, resultado("b"))
        assertEquals(Evento.Voltou, tela.eventos[1])
        assertEquals(1, tela.enviados.size)
    }

    @Test
    fun `conversa nao reconhecida em 15 segundos falha e a fila continua`() = runTest {
        registrarPendentes("a")
        tela.nuncaAbre = true
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(14_000); runCurrent()
        assertEquals(ResultadoAcao.Pendente, resultado("a"))

        advanceTimeBy(2_000); runCurrent()
        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_NAO_RECONHECIDA), resultado("a"))
        assertEquals(Evento.Inicio, tela.eventos.last())
    }

    @Test
    fun `campo que nao esvazia depois do toque falha sem tocar de novo`() = runTest {
        registrarPendentes("a")
        tela.envioTrava = true
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(10_000); runCurrent()

        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_NAO_CONFIRMADO), resultado("a"))
        assertEquals(1, tela.enviados.size)
    }

    @Test
    fun `rascunho diferente no campo nunca e enviado`() = runTest {
        registrarPendentes("a")
        tela.rascunhos["5541900000001"] = "texto pessoal do usuário"
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(5_000); runCurrent()

        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_RASCUNHO), resultado("a"))
        assertEquals(0, tela.enviados.size)
    }

    @Test
    fun `WhatsApp nao instalado ou servico desligado falham na hora sem entrar na fila`() = runTest {
        val fila = fila()

        assertEquals(
            ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_NAO_INSTALADO),
            fila.enfileirar("a", "5541900000001", "oi", AppWhatsApp.Business, prazo),
        )
        tela.disponivel = false
        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_SERVICO_DESLIGADO), fila.enviar("b"))
        runCurrent(); advanceTimeBy(5_000); runCurrent()

        assertEquals(emptyList<Evento>(), tela.eventos)
        assertEquals(emptyList<Pair<String, Instant>>(), prazos.agendados)
    }

    @Test
    fun `servico desligado enquanto o item esperava falha sem abrir o WhatsApp`() = runTest {
        registrarPendentes("a")
        aparelho.bloqueada.value = true
        val fila = fila()

        fila.enviar("a")
        tela.disponivel = false
        aparelho.bloqueada.value = false
        runCurrent(); advanceTimeBy(5_000); runCurrent()

        assertEquals(ResultadoAcao.Erro(FilaWhatsApp.MOTIVO_SERVICO_DESLIGADO), resultado("a"))
        assertEquals(emptyList<Evento>(), tela.eventos)
    }

    @Test
    fun `tela Enviar para que aparece de passagem antes da conversa nao e numero sem WhatsApp`() = runTest {
        registrarPendentes("a")
        tela.leiturasComSeletorAntes = 3
        val fila = fila()

        fila.enviar("a")
        runCurrent(); advanceTimeBy(10_000); runCurrent()

        assertEquals(ResultadoAcao.Ok, resultado("a"))
        assertEquals(false, tela.eventos.contains(Evento.Voltou))
    }
}
