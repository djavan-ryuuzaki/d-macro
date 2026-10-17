package br.com.djavanfernando.dmacro.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Coração do D-Macro: recebe eventos, decide quais Macros disparam, avalia Restrições e executa Ações.
 * Disparos de Macros diferentes rodam em paralelo; Disparos da mesma Macro passam por uma fila.
 */
class Motor(
    private val pacoteProprio: String,
    private val relogio: Clock,
    /** Escopo de longa duração para trabalho que sobrevive ao Disparo (novas tentativas de SMS). */
    private val escopo: CoroutineScope,
    private val macros: FonteMacros,
    private val configuracoes: ConfiguracoesMotor,
    private val permissoes: VerificadorPermissoes,
    private val sms: SmsSender,
    private val chamadas: CallPlacer,
    private val email: EmailSender,
    private val whatsapp: WhatsAppSender,
    private val log: ExecutionLog,
    private val teto: ContadorTeto,
    private val avisos: Avisos,
    private val novoId: () -> String = { UUID.randomUUID().toString() },
) {

    /** Chaves de notificação já disparadas, por (Macro, índice do Gatilho). Vive só em memória. */
    private data class ChaveDisparada(val macroId: String, val gatilho: Int, val chave: String)
    private val chavesDisparadas = mutableSetOf<ChaveDisparada>()
    private val travaChaves = Mutex()

    private val travaTeto = Mutex()
    private val filas = mutableMapOf<String, Mutex>()
    private fun filaDa(macroId: String): Mutex = synchronized(filas) { filas.getOrPut(macroId) { Mutex() } }

    suspend fun onNotificacao(evento: EventoNotificacao) {
        if (evento.pacote == pacoteProprio || evento.resumoDeGrupo) return
        if (!configuracoes.chaveGeralLigada()) return
        val disparadas = macros.macrosAtivadas().filter { macro -> disparou(macro, evento) }
        coroutineScope {
            disparadas.forEach { macro -> launch { processarDisparo(macro, evento) } }
        }
    }

    /** Teste: executa as Ações com valores de exemplo, ignorando Gatilhos, Restrições e a Chave geral. */
    suspend fun testar(macro: Macro) = filaDa(macro.id).withLock {
        val instante = relogio.instant()
        val faltando = permissoesFaltando(macro)
        if (faltando.isNotEmpty()) {
            log.registrar(registro(macro, instante, teste = true, null, Desfecho.BloqueadaPorPermissao(faltando), emptyList()))
            return@withLock
        }
        executarAcoes(macro, instante, teste = true, null, ContextoMarcadores.exemplo(instante, relogio.zone))
    }

    suspend fun onNotificacaoRemovida(chave: String) {
        travaChaves.withLock { chavesDisparadas.removeAll { it.chave == chave } }
    }

    /** Um Gatilho satisfeito dispara, exceto se já disparou para esta mesma notificação. */
    private suspend fun disparou(macro: Macro, evento: EventoNotificacao): Boolean = travaChaves.withLock {
        var algum = false
        macro.gatilhos.forEachIndexed { i, gatilho ->
            if (gatilho is NotificacaoRecebida && gatilho.satisfeitoPor(evento)) {
                if (!gatilho.impedirMultiplosDisparos) {
                    algum = true
                } else if (chavesDisparadas.add(ChaveDisparada(macro.id, i, evento.chave))) {
                    algum = true
                }
            }
        }
        algum
    }

    private suspend fun processarDisparo(macro: Macro, evento: EventoNotificacao) = filaDa(macro.id).withLock {
        val instante = relogio.instant()
        val origem = OrigemDisparo(evento.pacote, evento.nomeApp, evento.titulo, evento.texto)

        val faltando = permissoesFaltando(macro)
        if (faltando.isNotEmpty()) {
            log.registrar(registro(macro, instante, teste = false, origem, Desfecho.BloqueadaPorPermissao(faltando), emptyList()))
            return@withLock
        }

        val contextoRestricao = ContextoRestricao(instante, relogio.zone, log.ultimaExecucao(macro.id))
        val reprovada = primeiraReprovada(macro.restricoes, contextoRestricao)
        if (reprovada != null) {
            log.registrar(registro(macro, instante, teste = false, origem, Desfecho.BarradaPorRestricao(reprovada), emptyList()))
            return@withLock
        }

        executarAcoes(macro, instante, teste = false, origem, ContextoMarcadores.de(evento, relogio.zone))
    }

    private suspend fun executarAcoes(
        macro: Macro,
        instante: Instant,
        teste: Boolean,
        origem: OrigemDisparo?,
        contexto: ContextoMarcadores,
    ) {
        val itens = mutableListOf<ItemRegistro>()
        val novasTentativas = mutableListOf<suspend () -> Unit>()
        /** Envios que só podem começar depois que o Registro existe, porque atualizam seus itens. */
        val enviosAposRegistro = mutableListOf<suspend () -> Unit>()
        val tetosAvisados = mutableSetOf<TipoTeto>()

        /** Reserva um uso do Teto global; avisa o usuário uma vez por Execução quando o teto é atingido. */
        suspend fun reservar(tipo: TipoTeto): Boolean {
            val ok = travaTeto.withLock {
                val agora = relogio.instant()
                val livre = teto.usosDesde(tipo, agora.minus(JANELA_TETO)) < tipo.maximoPorHora
                if (livre) teto.registrarUso(tipo, agora)
                livre
            }
            if (!ok && tetosAvisados.add(tipo)) avisos.tetoAtingido(tipo)
            return ok
        }

        macro.acoes.forEachIndexed { i, acao ->
            when (acao) {
                is EfetuarChamada -> {
                    val d = acao.destinatario
                    val resultado = if (reservar(TipoTeto.Chamada)) chamadas.ligar(d.numero) else ResultadoAcao.BloqueadoPorTeto
                    itens += ItemRegistro(novoId(), i, TipoAcao.Chamada, d.numero, d.rotulo, resultado)
                }
                is EnviarSms -> {
                    val texto = cortarSms(Marcadores.renderizar(acao.mensagem, contexto), acao.limiteCaracteres)
                    acao.destinatarios.forEach { d ->
                        val id = novoId()
                        if (!reservar(TipoTeto.Sms)) {
                            itens += ItemRegistro(id, i, TipoAcao.Sms, d.numero, d.rotulo, ResultadoAcao.BloqueadoPorTeto)
                            return@forEach
                        }
                        var resultado = sms.enviar(d.numero, texto)
                        if (resultado is ResultadoAcao.Erro) {
                            resultado = ResultadoAcao.Pendente
                            novasTentativas += {
                                delay(ESPERA_NOVA_TENTATIVA_SMS)
                                log.atualizarItem(id, sms.enviar(d.numero, texto))
                            }
                        }
                        itens += ItemRegistro(id, i, TipoAcao.Sms, d.numero, d.rotulo, resultado)
                    }
                }
                is EnviarEmail -> {
                    val assunto = Marcadores.renderizar(acao.assunto, contexto)
                    val corpo = Marcadores.renderizar(acao.corpo, contexto)
                    val prazo = instante.plus(PRAZO_EMAIL)
                    acao.destinatarios.forEach { d ->
                        val id = novoId()
                        val resultado = email.enfileirar(id, d.endereco, assunto, corpo, prazo)
                        itens += ItemRegistro(id, i, TipoAcao.Email, d.endereco, d.rotulo, resultado)
                    }
                }
                is EnviarWhatsApp -> {
                    val texto = Marcadores.renderizar(acao.mensagem, contexto)
                    val prazo = instante.plus(PRAZO_WHATSAPP)
                    acao.destinatarios.forEach { d ->
                        val id = novoId()
                        val numero = numeroWhatsApp(d.numero)
                        val resultado = when {
                            numero == null -> ResultadoAcao.Erro(MOTIVO_NUMERO_SEM_DDI)
                            !reservar(TipoTeto.WhatsApp) -> ResultadoAcao.BloqueadoPorTeto
                            else -> {
                                enviosAposRegistro += {
                                    val r = whatsapp.enfileirar(id, numero, texto, acao.app, prazo)
                                    if (r != ResultadoAcao.Pendente) log.atualizarItem(id, r)
                                }
                                ResultadoAcao.Pendente
                            }
                        }
                        itens += ItemRegistro(id, i, TipoAcao.WhatsApp, d.numero, d.rotulo, resultado)
                    }
                }
            }
        }

        log.registrar(registro(macro, instante, teste, origem, Desfecho.Executada, itens))
        enviosAposRegistro.forEach { it() }
        novasTentativas.forEach { escopo.launch { it() } }
    }

    private fun permissoesFaltando(macro: Macro): Set<Permissao> =
        permissoesNecessarias(macro).filterNot { permissoes.concedida(it) }.toSet()

    private fun registro(
        macro: Macro,
        instante: Instant,
        teste: Boolean,
        origem: OrigemDisparo?,
        desfecho: Desfecho,
        itens: List<ItemRegistro>,
    ) = RegistroExecucao(novoId(), macro.id, macro.nome, instante, teste, origem, desfecho, itens)

    companion object {
        const val ESPERA_NOVA_TENTATIVA_SMS = 60_000L
        val PRAZO_EMAIL: Duration = Duration.ofMinutes(30)
        val PRAZO_WHATSAPP: Duration = Duration.ofMinutes(30)
        const val MOTIVO_NUMERO_SEM_DDI = "número sem DDD"
        val JANELA_TETO: Duration = Duration.ofHours(1)
    }
}

/** Permissões de plataforma que as Ações da Macro exigem. */
fun permissoesNecessarias(macro: Macro): Set<Permissao> = macro.acoes.mapNotNull { acao ->
    when (acao) {
        is EnviarSms -> Permissao.Sms
        is EfetuarChamada -> Permissao.Telefone
        is EnviarEmail -> null
        is EnviarWhatsApp -> Permissao.Acessibilidade
    }
}.toSet()
