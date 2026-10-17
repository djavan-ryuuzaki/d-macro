package br.com.djavanfernando.dmacro.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.Acao
import br.com.djavanfernando.dmacro.engine.Desfecho
import br.com.djavanfernando.dmacro.engine.DiasSemana
import br.com.djavanfernando.dmacro.engine.Destinatario
import br.com.djavanfernando.dmacro.engine.EfetuarChamada
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarSms
import br.com.djavanfernando.dmacro.engine.EnviarWhatsApp
import br.com.djavanfernando.dmacro.engine.FaixaHorario
import br.com.djavanfernando.dmacro.engine.FiltroTexto
import br.com.djavanfernando.dmacro.engine.Gatilho
import br.com.djavanfernando.dmacro.engine.LimiteFrequencia
import br.com.djavanfernando.dmacro.engine.ModoFiltro
import br.com.djavanfernando.dmacro.engine.NotificacaoRecebida
import br.com.djavanfernando.dmacro.engine.Permissao
import br.com.djavanfernando.dmacro.engine.ProblemaMacro
import br.com.djavanfernando.dmacro.engine.Restricao
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.SelecaoApps
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")

@Composable
fun resumo(gatilho: Gatilho): String = when (gatilho) {
    is NotificacaoRecebida -> {
        val apps = when (val a = gatilho.apps) {
            SelecaoApps.Todos -> stringResource(R.string.resumo_todos_apps)
            is SelecaoApps.Somente -> pluralStringResource(R.plurals.resumo_n_apps, a.pacotes.size, a.pacotes.size)
        }
        val filtros = listOfNotNull(
            resumoFiltro(R.string.campo_titulo, gatilho.filtroTitulo),
            resumoFiltro(R.string.campo_texto, gatilho.filtroTexto),
        )
        stringResource(R.string.resumo_notificacao, apps) + filtros.joinToString("") { "\n• $it" }
    }
}

@Composable
private fun resumoFiltro(campo: Int, filtro: FiltroTexto): String? {
    if (filtro.modo == ModoFiltro.Qualquer) return null
    val termos = filtro.termos.filter { it.isNotBlank() }.joinToString(", ") { "\"$it\"" }
    return "${stringResource(campo)} ${stringResource(rotuloModo(filtro.modo)).lowercase()} $termos"
}

fun rotuloModo(modo: ModoFiltro): Int = when (modo) {
    ModoFiltro.Qualquer -> R.string.modo_qualquer
    ModoFiltro.Igual -> R.string.modo_igual
    ModoFiltro.Contem -> R.string.modo_contem
    ModoFiltro.NaoContem -> R.string.modo_nao_contem
}

fun Destinatario.exibicao(): String = rotulo?.let { "$it ($valor)" } ?: valor

@Composable
fun tituloAcao(acao: Acao): String = when (acao) {
    is EfetuarChamada -> stringResource(R.string.acao_chamada)
    is EnviarSms -> stringResource(R.string.acao_sms)
    is EnviarEmail -> stringResource(R.string.acao_email)
    is EnviarWhatsApp -> stringResource(R.string.acao_whatsapp)
}

@Composable
fun resumo(acao: Acao): String = when (acao) {
    is EfetuarChamada -> acao.destinatario.exibicao()
    is EnviarSms -> acao.destinatarios.joinToString(", ") { it.exibicao() } + "\n\"${acao.mensagem}\""
    is EnviarEmail -> acao.destinatarios.joinToString(", ") { it.exibicao() } + "\n${acao.assunto}"
    is EnviarWhatsApp -> acao.destinatarios.joinToString(", ") { it.exibicao() } + "\n\"${acao.mensagem}\""
}

fun nomeDia(dia: DayOfWeek): String = dia.getDisplayName(TextStyle.SHORT, ptBr)

@Composable
fun tituloRestricao(restricao: Restricao): String = when (restricao) {
    is FaixaHorario -> stringResource(R.string.restricao_faixa)
    is DiasSemana -> stringResource(R.string.restricao_dias)
    is LimiteFrequencia -> stringResource(R.string.restricao_limite)
}

@Composable
fun resumo(restricao: Restricao): String = when (restricao) {
    is FaixaHorario -> {
        val base = stringResource(R.string.resumo_faixa, restricao.inicio.toString(), restricao.fim.toString())
        if (restricao.fim.isBefore(restricao.inicio)) base + " " + stringResource(R.string.resumo_faixa_meia_noite) else base
    }
    is DiasSemana -> DayOfWeek.entries.filter { it in restricao.dias }.joinToString(", ") { nomeDia(it) }
    is LimiteFrequencia -> stringResource(R.string.resumo_limite, restricao.minutos)
}

@Composable
fun descricao(problema: ProblemaMacro): String = when (problema) {
    ProblemaMacro.SemGatilho -> stringResource(R.string.problema_sem_gatilho)
    ProblemaMacro.SemAcao -> stringResource(R.string.problema_sem_acao)
    ProblemaMacro.SemApps -> stringResource(R.string.problema_sem_apps)
    ProblemaMacro.FiltroSemTermos -> stringResource(R.string.problema_filtro_sem_termos)
    is ProblemaMacro.SemDestinatario -> stringResource(R.string.problema_sem_destinatario, problema.acao + 1)
    is ProblemaMacro.DestinatarioInvalido -> stringResource(R.string.problema_destinatario_invalido, problema.acao + 1, problema.valor)
    is ProblemaMacro.MensagemVazia -> stringResource(R.string.problema_mensagem_vazia, problema.acao + 1)
    is ProblemaMacro.NumeroSemDdi -> stringResource(R.string.problema_numero_sem_ddd, problema.acao + 1, problema.valor)
    is ProblemaMacro.RestricaoInvalida -> stringResource(R.string.problema_restricao_invalida, problema.restricao + 1)
}

@Composable
fun nome(permissao: Permissao): String = when (permissao) {
    Permissao.Sms -> stringResource(R.string.permissao_sms)
    Permissao.Telefone -> stringResource(R.string.permissao_telefone)
    Permissao.Acessibilidade -> stringResource(R.string.permissao_acessibilidade)
}

@Composable
fun descricao(resultado: ResultadoAcao): String = when (resultado) {
    ResultadoAcao.Ok -> stringResource(R.string.resultado_ok)
    ResultadoAcao.Pendente -> stringResource(R.string.resultado_pendente)
    ResultadoAcao.BloqueadoPorTeto -> stringResource(R.string.resultado_teto)
    is ResultadoAcao.Erro -> stringResource(R.string.resultado_erro, resultado.motivo)
}

@Composable
fun descricao(desfecho: Desfecho): String = when (desfecho) {
    Desfecho.Executada -> stringResource(R.string.desfecho_executada)
    is Desfecho.BarradaPorRestricao ->
        stringResource(R.string.desfecho_barrada, "${tituloRestricao(desfecho.restricao)}: ${resumo(desfecho.restricao)}")
    is Desfecho.BloqueadaPorPermissao ->
        stringResource(R.string.desfecho_bloqueada, desfecho.faltando.map { nome(it) }.joinToString(", "))
}
