package br.com.djavanfernando.dmacro.engine

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class MotorTest {

    private val relogio = RelogioDeTeste(Instant.parse("2026-10-12T12:00:00Z")) // 09:00 em São Paulo
    private val sms = SmsFake()
    private val chamadas = ChamadaFake()
    private val email = EmailFake()
    private val whatsapp = WhatsAppFake()
    private val log = LogFake()
    private val teto = TetoFake()
    private val macros = MacrosFake()
    private val config = ConfiguracoesFake()
    private val permissoes = PermissoesFake()
    private val avisos = AvisosFake()

    private fun TestScope.motor() = Motor(
        pacoteProprio = "br.com.djavanfernando.dmacro",
        relogio = relogio,
        escopo = backgroundScope,
        macros = macros,
        configuracoes = config,
        permissoes = permissoes,
        sms = sms,
        chamadas = chamadas,
        email = email,
        whatsapp = whatsapp,
        log = log,
        teto = teto,
        avisos = avisos,
    )

    private fun evento(
        titulo: String = "Alarme",
        texto: String = "Sensor da sala",
        chave: String = "k1",
        pacote: String = "com.alarme",
    ) = EventoNotificacao(
        pacote = pacote, nomeApp = "Alarme Casa", titulo = titulo, texto = texto, chave = chave,
        emCurso = false, resumoDeGrupo = false, instante = relogio.instant(),
    )

    private val gatilhoAlarme = NotificacaoRecebida(filtroTitulo = FiltroTexto(ModoFiltro.Contem, listOf("alarme")))

    private fun macro(
        id: String = "m1",
        acoes: List<Acao> = listOf(EnviarSms(listOf(Telefone("111", "Ana"), Telefone("222")), "{app}: {texto}")),
        restricoes: List<Restricao> = emptyList(),
        gatilhos: List<Gatilho> = listOf(gatilhoAlarme),
    ) = Macro(id = id, nome = "Macro $id", ativada = true, gatilhos = gatilhos, acoes = acoes, restricoes = restricoes)

    @Test
    fun `notificacao que satisfaz o gatilho envia SMS a cada destinatario com marcadores e registra a execucao`() = runTest {
        macros.macros = listOf(macro())

        motor().onNotificacao(evento())

        assertEquals(
            listOf(SmsFake.Enviado("111", "Alarme Casa: Sensor da sala"), SmsFake.Enviado("222", "Alarme Casa: Sensor da sala")),
            sms.enviados,
        )
        val registro = log.registros.single()
        assertEquals(Desfecho.Executada, registro.desfecho)
        assertEquals("m1", registro.macroId)
        assertEquals(listOf("111", "222"), registro.itens.map { it.destinatario })
        assertEquals(listOf(ResultadoAcao.Ok, ResultadoAcao.Ok), registro.itens.map { it.resultado })
    }

    @Test
    fun `chave geral desligada impede qualquer disparo`() = runTest {
        macros.macros = listOf(macro())
        config.chaveGeral = false

        motor().onNotificacao(evento())

        assertEquals(emptyList<SmsFake.Enviado>(), sms.enviados)
        assertEquals(emptyList<RegistroExecucao>(), log.registros)
    }

    @Test
    fun `notificacoes do proprio app e resumos de grupo sao ignorados`() = runTest {
        macros.macros = listOf(macro(gatilhos = listOf(NotificacaoRecebida())))
        val motor = motor()

        motor.onNotificacao(evento(pacote = "br.com.djavanfernando.dmacro"))
        motor.onNotificacao(evento().copy(resumoDeGrupo = true))

        assertEquals(emptyList<RegistroExecucao>(), log.registros)
    }

    @Test
    fun `atualizacoes da mesma notificacao disparam uma unica vez ate ela ser removida`() = runTest {
        macros.macros = listOf(macro(acoes = listOf(EnviarSms(listOf(Telefone("111")), "oi"))))
        val motor = motor()

        repeat(5) { motor.onNotificacao(evento(chave = "k1")) }
        assertEquals(1, log.registros.size)

        motor.onNotificacao(evento(chave = "k2"))
        assertEquals(2, log.registros.size)

        motor.onNotificacaoRemovida("k1")
        motor.onNotificacao(evento(chave = "k1"))
        assertEquals(3, log.registros.size)
    }

    @Test
    fun `sem impedir multiplos disparos cada atualizacao dispara`() = runTest {
        macros.macros = listOf(macro(gatilhos = listOf(gatilhoAlarme.copy(impedirMultiplosDisparos = false))))
        val motor = motor()

        repeat(3) { motor.onNotificacao(evento(chave = "k1")) }

        assertEquals(3, log.registros.size)
    }

    @Test
    fun `restricao reprovada barra o disparo e o registro mostra qual foi`() = runTest {
        val faixa = FaixaHorario(LocalTime.of(18, 0), LocalTime.of(22, 0))
        macros.macros = listOf(macro(restricoes = listOf(faixa)))

        motor().onNotificacao(evento())

        assertEquals(emptyList<SmsFake.Enviado>(), sms.enviados)
        val registro = log.registros.single()
        assertEquals(Desfecho.BarradaPorRestricao(faixa), registro.desfecho)
        assertEquals(emptyList<ItemRegistro>(), registro.itens)
    }

    @Test
    fun `limite de frequencia conta a partir da ultima execucao`() = runTest {
        macros.macros = listOf(macro(restricoes = listOf(LimiteFrequencia(5))))
        val motor = motor()

        motor.onNotificacao(evento(chave = "a"))
        relogio.avancar(Duration.ofMinutes(4))
        motor.onNotificacao(evento(chave = "b"))
        relogio.avancar(Duration.ofMinutes(1))
        motor.onNotificacao(evento(chave = "c"))

        assertEquals(
            listOf(Desfecho.Executada, Desfecho.BarradaPorRestricao(LimiteFrequencia(5)), Desfecho.Executada),
            log.registros.map { it.desfecho },
        )
    }

    @Test
    fun `dois eventos simultaneos para a mesma macro com limite resultam em uma execucao`() = runTest {
        macros.macros = listOf(macro(restricoes = listOf(LimiteFrequencia(5))))
        sms.atrasoMs = 1_000
        val motor = motor()

        val a = launch { motor.onNotificacao(evento(chave = "a")) }
        val b = launch { motor.onNotificacao(evento(chave = "b")) }
        a.join(); b.join()

        assertEquals(1, log.registros.count { it.desfecho == Desfecho.Executada })
        assertEquals(2, log.registros.size)
    }

    @Test
    fun `macro sem permissao de SMS e bloqueada e o registro diz o motivo`() = runTest {
        macros.macros = listOf(macro(acoes = listOf(EnviarSms(listOf(Telefone("111")), "oi"), EfetuarChamada(Telefone("999")))))
        permissoes.negadas += Permissao.Sms

        motor().onNotificacao(evento())

        assertEquals(emptyList<SmsFake.Enviado>(), sms.enviados)
        assertEquals(emptyList<String>(), chamadas.chamados)
        assertEquals(Desfecho.BloqueadaPorPermissao(setOf(Permissao.Sms)), log.registros.single().desfecho)
    }

    @Test
    fun `teto global bloqueia o 21o SMS da hora, avisa o usuario e libera depois da janela`() = runTest {
        val destinatarios = (1..25).map { Telefone("$it") }
        macros.macros = listOf(macro(acoes = listOf(EnviarSms(destinatarios, "oi"))))
        val motor = motor()

        motor.onNotificacao(evento(chave = "a"))

        assertEquals(20, sms.enviados.size)
        val resultados = log.registros.single().itens.map { it.resultado }
        assertEquals(List(20) { ResultadoAcao.Ok } + List(5) { ResultadoAcao.BloqueadoPorTeto }, resultados)
        assertEquals(listOf(TipoTeto.Sms), avisos.tetosAtingidos)

        relogio.avancar(Duration.ofMinutes(61))
        motor.onNotificacao(evento(chave = "b"))
        assertEquals(40, sms.enviados.size)
    }

    @Test
    fun `teto global limita chamadas a 5 por hora`() = runTest {
        macros.macros = listOf(macro(acoes = List(6) { EfetuarChamada(Telefone("99$it")) }))

        motor().onNotificacao(evento())

        assertEquals(5, chamadas.chamados.size)
        assertEquals(ResultadoAcao.BloqueadoPorTeto, log.registros.single().itens.last().resultado)
        assertEquals(listOf(TipoTeto.Chamada), avisos.tetosAtingidos)
    }

    @Test
    fun `teste ignora gatilho, restricoes e chave geral e usa valores de exemplo`() = runTest {
        config.chaveGeral = false
        val m = macro(
            acoes = listOf(EnviarSms(listOf(Telefone("111")), "{app}|{titulo}|{texto}|{hora}")),
            restricoes = listOf(FaixaHorario(LocalTime.of(18, 0), LocalTime.of(22, 0))),
        )

        motor().testar(m)

        assertEquals(listOf(SmsFake.Enviado("111", "App de exemplo|Título de exemplo|Texto de exemplo|09:00")), sms.enviados)
        val registro = log.registros.single()
        assertEquals(true, registro.teste)
        assertEquals(null, registro.origem)
        assertEquals(Desfecho.Executada, registro.desfecho)
    }

    @Test
    fun `teste nao conta para o limite de frequencia`() = runTest {
        val m = macro(restricoes = listOf(LimiteFrequencia(5)))
        macros.macros = listOf(m)
        val motor = motor()

        motor.testar(m)
        motor.onNotificacao(evento())

        assertEquals(listOf(true, false), log.registros.map { it.teste })
        assertEquals(Desfecho.Executada, log.registros.last().desfecho)
    }

    @Test
    fun `teste respeita permissoes e o teto global`() = runTest {
        val semPermissao = macro(acoes = listOf(EfetuarChamada(Telefone("999"))))
        permissoes.negadas += Permissao.Telefone
        val motor = motor()

        motor.testar(semPermissao)
        assertEquals(Desfecho.BloqueadaPorPermissao(setOf(Permissao.Telefone)), log.registros.last().desfecho)

        repeat(20) { teto.registrarUso(TipoTeto.Sms, relogio.instant()) }
        motor.testar(macro(acoes = listOf(EnviarSms(listOf(Telefone("111")), "oi"))))
        assertEquals(ResultadoAcao.BloqueadoPorTeto, log.registros.last().itens.single().resultado)
    }

    @Test
    fun `falha em uma acao nao interrompe as seguintes`() = runTest {
        macros.macros = listOf(
            macro(
                acoes = listOf(
                    EfetuarChamada(Telefone("999")),
                    EnviarSms(listOf(Telefone("111"), Telefone("222")), "oi"),
                    EnviarEmail(listOf(Email("ana@x.com")), "Alerta {app}", "{titulo} às {hora}"),
                ),
            ),
        )
        sms.falhas["111"] = 2

        motor().onNotificacao(evento())
        advanceTimeBy(61_000); runCurrent() // passa da nova tentativa do SMS

        assertEquals(listOf("999"), chamadas.chamados)
        assertEquals(listOf("222"), sms.enviados.map { it.numero })
        assertEquals(
            EmailFake.Enfileirado(email.enfileirados.single().idItem, "ana@x.com", "Alerta Alarme Casa", "Alarme às 09:00"),
            email.enfileirados.single(),
        )
        assertEquals(
            listOf(ResultadoAcao.Ok, ResultadoAcao.Erro("sem sinal"), ResultadoAcao.Ok, ResultadoAcao.Pendente),
            log.registros.single().itens.map { it.resultado },
        )
        assertEquals(email.enfileirados.single().idItem, log.registros.single().itens[3].id)
    }

    @Test
    fun `SMS que falha e tentado de novo apos 1 minuto`() = runTest {
        macros.macros = listOf(macro(acoes = listOf(EnviarSms(listOf(Telefone("111")), "oi"))))
        sms.falhas["111"] = 1

        motor().onNotificacao(evento())
        assertEquals(ResultadoAcao.Pendente, log.registros.single().itens.single().resultado)

        advanceTimeBy(59_000); runCurrent()
        assertEquals(emptyList<SmsFake.Enviado>(), sms.enviados)

        advanceTimeBy(1_000); runCurrent()
        assertEquals(listOf(SmsFake.Enviado("111", "oi")), sms.enviados)
        assertEquals(ResultadoAcao.Ok, log.registros.single().itens.single().resultado)
    }

    @Test
    fun `whatsapp enfileira cada destinatario com marcadores, numero internacional e prazo de 30 minutos`() = runTest {
        val acao = EnviarWhatsApp(listOf(Telefone("(11) 98765-4321", "Ana"), Telefone("+1 415 555 0123")), "{titulo}: {texto}", AppWhatsApp.Business)
        macros.macros = listOf(macro(acoes = listOf(acao)))

        motor().onNotificacao(evento())

        val prazo = relogio.instant().plus(Duration.ofMinutes(30))
        val itens = log.registros.single().itens
        assertEquals(
            listOf(
                WhatsAppFake.Enfileirado(itens[0].id, "5511987654321", "Alarme: Sensor da sala", AppWhatsApp.Business, prazo),
                WhatsAppFake.Enfileirado(itens[1].id, "14155550123", "Alarme: Sensor da sala", AppWhatsApp.Business, prazo),
            ),
            whatsapp.enfileirados,
        )
        assertEquals(listOf(TipoAcao.WhatsApp, TipoAcao.WhatsApp), itens.map { it.tipo })
        assertEquals(listOf("(11) 98765-4321", "+1 415 555 0123"), itens.map { it.destinatario })
        assertEquals(listOf(ResultadoAcao.Pendente, ResultadoAcao.Pendente), itens.map { it.resultado })
    }

    @Test
    fun `teto global limita whatsapp a 20 por hora e avisa uma vez`() = runTest {
        repeat(19) { teto.registrarUso(TipoTeto.WhatsApp, relogio.instant()) }
        macros.macros = listOf(macro(acoes = listOf(EnviarWhatsApp(listOf(Telefone("11911110000"), Telefone("11922220000"), Telefone("11933330000")), "oi"))))

        motor().onNotificacao(evento())

        assertEquals(listOf("5511911110000"), whatsapp.enfileirados.map { it.numero })
        assertEquals(
            listOf(ResultadoAcao.Pendente, ResultadoAcao.BloqueadoPorTeto, ResultadoAcao.BloqueadoPorTeto),
            log.registros.single().itens.map { it.resultado },
        )
        assertEquals(listOf(TipoTeto.WhatsApp), avisos.tetosAtingidos)
    }

    @Test
    fun `macro com whatsapp sem o servico de acessibilidade e bloqueada`() = runTest {
        macros.macros = listOf(macro(acoes = listOf(EnviarWhatsApp(listOf(Telefone("11911110000")), "oi"))))
        permissoes.negadas += Permissao.Acessibilidade

        motor().onNotificacao(evento())

        assertEquals(Desfecho.BloqueadaPorPermissao(setOf(Permissao.Acessibilidade)), log.registros.single().desfecho)
        assertEquals(emptyList<WhatsAppFake.Enfileirado>(), whatsapp.enfileirados)
    }

    @Test
    fun `teste de whatsapp usa os valores de exemplo`() = runTest {
        motor().testar(macro(acoes = listOf(EnviarWhatsApp(listOf(Telefone("11911110000")), "{app} / {titulo}"))))

        assertEquals("App de exemplo / Título de exemplo", whatsapp.enfileirados.single().texto)
        assertEquals(true, log.registros.single().teste)
    }

    @Test
    fun `whatsapp so entra na fila depois que o registro existe e um erro imediato atualiza o item`() = runTest {
        val idsNoRegistro = mutableListOf<Boolean>()
        val recusa = object : WhatsAppSender {
            override suspend fun enfileirar(idItem: String, numero: String, texto: String, app: AppWhatsApp, prazo: Instant): ResultadoAcao {
                idsNoRegistro += log.registros.any { r -> r.itens.any { it.id == idItem } }
                return ResultadoAcao.Erro("WhatsApp não instalado")
            }
        }
        macros.macros = listOf(macro(acoes = listOf(EnviarWhatsApp(listOf(Telefone("11911110000")), "oi"), EnviarSms(listOf(Telefone("111")), "oi"))))

        Motor("br.com.djavanfernando.dmacro", relogio, backgroundScope, macros, config, permissoes, sms, chamadas, email, recusa, log, teto, avisos)
            .onNotificacao(evento())

        assertEquals(listOf(true), idsNoRegistro)
        assertEquals(ResultadoAcao.Erro("WhatsApp não instalado"), log.registros.single().itens[0].resultado)
    }
}
