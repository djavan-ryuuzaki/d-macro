package br.com.djavanfernando.dmacro.engine

/** Motivo pelo qual uma Macro não pode ser ativada. Índices referem-se à posição na lista de Ações ou Restrições. */
sealed interface ProblemaMacro {
    data object SemGatilho : ProblemaMacro
    data object SemAcao : ProblemaMacro
    data object SemApps : ProblemaMacro
    data object FiltroSemTermos : ProblemaMacro
    data class SemDestinatario(val acao: Int) : ProblemaMacro
    data class DestinatarioInvalido(val acao: Int, val valor: String) : ProblemaMacro
    data class MensagemVazia(val acao: Int) : ProblemaMacro
    /** Número de WhatsApp que não converte para o formato internacional (falta o DDD). */
    data class NumeroSemDdi(val acao: Int, val valor: String) : ProblemaMacro
    data class RestricaoInvalida(val restricao: Int) : ProblemaMacro
}

fun validar(macro: Macro): List<ProblemaMacro> = buildList {
    if (macro.gatilhos.isEmpty()) add(ProblemaMacro.SemGatilho)
    if (macro.acoes.isEmpty()) add(ProblemaMacro.SemAcao)

    macro.gatilhos.forEach { gatilho ->
        when (gatilho) {
            is NotificacaoRecebida -> {
                val apps = gatilho.apps
                if (apps is SelecaoApps.Somente && apps.pacotes.isEmpty()) add(ProblemaMacro.SemApps)
                if (!gatilho.filtroTitulo.temTermosSuficientes() || !gatilho.filtroTexto.temTermosSuficientes()) {
                    add(ProblemaMacro.FiltroSemTermos)
                }
            }
        }
    }

    macro.acoes.forEachIndexed { i, acao ->
        val destinatarios: List<Destinatario> = when (acao) {
            is EfetuarChamada -> listOf(acao.destinatario)
            is EnviarSms -> acao.destinatarios
            is EnviarEmail -> acao.destinatarios
            is EnviarWhatsApp -> acao.destinatarios
        }
        if (destinatarios.isEmpty()) add(ProblemaMacro.SemDestinatario(i))
        if (acao is EnviarWhatsApp) {
            destinatarios.filter { numeroWhatsApp(it.valor) == null }.forEach { add(ProblemaMacro.NumeroSemDdi(i, it.valor)) }
        } else {
            destinatarios.filterNot { it.valido() }.forEach { add(ProblemaMacro.DestinatarioInvalido(i, it.valor)) }
        }

        val texto = when (acao) {
            is EfetuarChamada -> null
            is EnviarSms -> acao.mensagem
            is EnviarEmail -> acao.corpo
            is EnviarWhatsApp -> acao.mensagem
        }
        if (texto != null && texto.isBlank()) add(ProblemaMacro.MensagemVazia(i))
    }

    macro.restricoes.forEachIndexed { i, restricao ->
        val valida = when (restricao) {
            is FaixaHorario -> restricao.inicio != restricao.fim
            is DiasSemana -> restricao.dias.isNotEmpty()
            is LimiteFrequencia -> restricao.minutos >= 1
        }
        if (!valida) add(ProblemaMacro.RestricaoInvalida(i))
    }
}

fun podeAtivar(macro: Macro): Boolean = validar(macro).isEmpty()

private fun FiltroTexto.temTermosSuficientes() =
    modo == ModoFiltro.Qualquer || termos.any { it.isNotBlank() }

private val TELEFONE = Regex("""^\+?\d{3,15}$""")
private val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

private fun Destinatario.valido(): Boolean = when (this) {
    is Telefone -> TELEFONE.matches(numero.replace(Regex("""[\s().-]"""), ""))
    is Email -> EMAIL.matches(endereco.trim())
}
