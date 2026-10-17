package br.com.djavanfernando.dmacro.ui.editor

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.Acao
import br.com.djavanfernando.dmacro.engine.DiasSemana
import br.com.djavanfernando.dmacro.engine.EfetuarChamada
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarSms
import br.com.djavanfernando.dmacro.engine.EnviarWhatsApp
import br.com.djavanfernando.dmacro.engine.FaixaHorario
import br.com.djavanfernando.dmacro.engine.LimiteFrequencia
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.NotificacaoRecebida
import br.com.djavanfernando.dmacro.engine.Restricao
import br.com.djavanfernando.dmacro.engine.validar
import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.platform.PermissoesAndroid
import br.com.djavanfernando.dmacro.platform.intentConfigAcessibilidade
import br.com.djavanfernando.dmacro.platform.whatsAppsInstalados
import br.com.djavanfernando.dmacro.ui.comum.descricao
import br.com.djavanfernando.dmacro.ui.comum.exibicao
import br.com.djavanfernando.dmacro.ui.comum.resumo
import br.com.djavanfernando.dmacro.ui.comum.tituloAcao
import br.com.djavanfernando.dmacro.ui.comum.tituloRestricao
import br.com.djavanfernando.dmacro.ui.theme.CoresCartao
import kotlinx.coroutines.launch

/** O que está sendo editado num diálogo. Índice nulo = item novo. */
private sealed interface Edicao {
    data class Gatilho(val indice: Int?) : Edicao
    data class AcaoEd(val indice: Int?, val tipo: TipoAcaoNova) : Edicao
    data class RestricaoEd(val indice: Int?, val tipo: TipoRestricaoNova) : Edicao
}

private enum class TipoAcaoNova { Chamada, Sms, Email, WhatsApp }
private enum class TipoRestricaoNova { Faixa, Dias, Limite }

private fun Acao.tipoNovo() = when (this) {
    is EfetuarChamada -> TipoAcaoNova.Chamada
    is EnviarSms -> TipoAcaoNova.Sms
    is EnviarEmail -> TipoAcaoNova.Email
    is EnviarWhatsApp -> TipoAcaoNova.WhatsApp
}

private fun Restricao.tipoNovo() = when (this) {
    is FaixaHorario -> TipoRestricaoNova.Faixa
    is DiasSemana -> TipoRestricaoNova.Dias
    is LimiteFrequencia -> TipoRestricaoNova.Limite
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onVoltar: () -> Unit, vm: EditorViewModel = hiltViewModel()) {
    val macro by vm.macro.collectAsState()
    val contaConfigurada by vm.contaConfigurada.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val msgTesteIniciado = stringResource(R.string.teste_iniciado)

    var edicao by remember { mutableStateOf<Edicao?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmarTeste by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var pedirAcessoNotificacoes by remember { mutableStateOf(false) }
    var divulgarAcessibilidade by remember { mutableStateOf(false) }
    var problemasVisiveis by remember { mutableStateOf(false) }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun abrir(nova: Edicao) {
        when (nova) {
            is Edicao.Gatilho -> if (!vm.permissoes.acessoNotificacoesConcedido()) pedirAcessoNotificacoes = true
            is Edicao.AcaoEd -> when (nova.tipo) {
                TipoAcaoNova.Sms -> if (!vm.permissoes.temPermissao(Manifest.permission.SEND_SMS)) pedirPermissao.launch(Manifest.permission.SEND_SMS)
                TipoAcaoNova.Chamada -> if (!vm.permissoes.temPermissao(Manifest.permission.CALL_PHONE)) pedirPermissao.launch(Manifest.permission.CALL_PHONE)
                TipoAcaoNova.WhatsApp -> if (!vm.permissoes.servicoAcessibilidadeAtivo()) divulgarAcessibilidade = true
                TipoAcaoNova.Email -> Unit
            }
            is Edicao.RestricaoEd -> Unit
        }
        edicao = nova
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editar_macro)) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar)) }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.mais_opcoes)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.testar)) },
                            enabled = macro?.acoes?.isNotEmpty() == true,
                            onClick = { menu = false; confirmarTeste = true },
                        )
                        DropdownMenuItem(text = { Text(stringResource(R.string.excluir)) }, onClick = { menu = false; confirmarExclusao = true })
                    }
                },
            )
        },
    ) { padding ->
        val m = macro ?: return@Scaffold
        val problemas = validar(m)
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = m.nome,
                onValueChange = vm::renomear,
                label = { Text(stringResource(R.string.nome_da_macro)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val alternarAtivacao: (Boolean) -> Unit = { ligar ->
                if (!vm.definirAtivada(ligar)) {
                    problemasVisiveis = true
                } else if (ligar && Build.VERSION.SDK_INT >= 33 && !vm.permissoes.podePostarNotificacoes()) {
                    pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            Row(
                Modifier.fillMaxWidth().toggleable(value = m.ativada, role = Role.Switch, onValueChange = alternarAtivacao),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(if (m.ativada) R.string.ativada else R.string.desativada), Modifier.weight(1f))
                Switch(checked = m.ativada, onCheckedChange = null)
            }
            if (problemas.isNotEmpty() && (problemasVisiveis || m.gatilhos.isNotEmpty() || m.acoes.isNotEmpty())) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.nao_pode_ativar), style = MaterialTheme.typography.titleSmall)
                        problemas.forEach { Text("• " + descricao(it), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            CartaoSecao(
                titulo = stringResource(R.string.gatilhos),
                quantidade = m.gatilhos.size,
                cor = CoresCartao.gatilhos,
                vazio = stringResource(R.string.sem_gatilhos),
                onAdicionar = { abrir(Edicao.Gatilho(null)) },
            ) {
                m.gatilhos.forEachIndexed { i, g ->
                    ItemCartao(
                        titulo = stringResource(R.string.gatilho_notificacao),
                        subtitulo = resumo(g),
                        onClick = { abrir(Edicao.Gatilho(i)) },
                        onRemover = { vm.removerGatilho(i) },
                    )
                }
            }

            CartaoSecao(
                titulo = stringResource(R.string.acoes),
                quantidade = m.acoes.size,
                cor = CoresCartao.acoes,
                vazio = stringResource(R.string.sem_acoes),
                tipos = listOf(
                    stringResource(R.string.acao_chamada) to { abrir(Edicao.AcaoEd(null, TipoAcaoNova.Chamada)) },
                    stringResource(R.string.acao_sms) to { abrir(Edicao.AcaoEd(null, TipoAcaoNova.Sms)) },
                    stringResource(R.string.acao_email) to { abrir(Edicao.AcaoEd(null, TipoAcaoNova.Email)) },
                    stringResource(R.string.acao_whatsapp) to { abrir(Edicao.AcaoEd(null, TipoAcaoNova.WhatsApp)) },
                ),
            ) {
                m.acoes.forEachIndexed { i, a ->
                    ItemCartao(
                        titulo = "${i + 1}. ${tituloAcao(a)}",
                        subtitulo = resumo(a),
                        onClick = { edicao = Edicao.AcaoEd(i, a.tipoNovo()) },
                        onRemover = { vm.removerAcao(i) },
                        onSubir = if (i > 0) ({ vm.moverAcao(i, -1) }) else null,
                        onDescer = if (i < m.acoes.lastIndex) ({ vm.moverAcao(i, 1) }) else null,
                    )
                }
            }

            CartaoSecao(
                titulo = stringResource(R.string.restricoes),
                quantidade = m.restricoes.size,
                cor = CoresCartao.restricoes,
                vazio = stringResource(R.string.sem_restricoes),
                tipos = listOf(
                    stringResource(R.string.restricao_faixa) to { abrir(Edicao.RestricaoEd(null, TipoRestricaoNova.Faixa)) },
                    stringResource(R.string.restricao_dias) to { abrir(Edicao.RestricaoEd(null, TipoRestricaoNova.Dias)) },
                    stringResource(R.string.restricao_limite) to { abrir(Edicao.RestricaoEd(null, TipoRestricaoNova.Limite)) },
                ),
            ) {
                m.restricoes.forEachIndexed { i, r ->
                    ItemCartao(
                        titulo = tituloRestricao(r),
                        subtitulo = resumo(r),
                        onClick = { edicao = Edicao.RestricaoEd(i, r.tipoNovo()) },
                        onRemover = { vm.removerRestricao(i) },
                    )
                }
            }
        }

        val instaladosWhatsApp = remember(edicao) { whatsAppsInstalados(context) }
        DialogoDaEdicao(m, edicao, vm, contaConfigurada, instaladosWhatsApp) { edicao = null }

        if (confirmarTeste) {
            AlertDialog(
                onDismissRequest = { confirmarTeste = false },
                title = { Text(stringResource(R.string.testar_titulo)) },
                text = { Text(descricaoTeste(m)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmarTeste = false
                        vm.testar()
                        escopo.launch { snackbar.showSnackbar(msgTesteIniciado) }
                    }) { Text(stringResource(R.string.executar)) }
                },
                dismissButton = { TextButton(onClick = { confirmarTeste = false }) { Text(stringResource(R.string.cancelar)) } },
            )
        }
        if (confirmarExclusao) {
            AlertDialog(
                onDismissRequest = { confirmarExclusao = false },
                title = { Text(stringResource(R.string.excluir_titulo, m.nome)) },
                confirmButton = { TextButton(onClick = { vm.excluir(onVoltar) }) { Text(stringResource(R.string.excluir)) } },
                dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text(stringResource(R.string.cancelar)) } },
            )
        }
        if (divulgarAcessibilidade) {
            AlertDialog(
                onDismissRequest = { divulgarAcessibilidade = false },
                title = { Text(stringResource(R.string.divulgacao_titulo)) },
                text = { Text(stringResource(R.string.divulgacao_texto)) },
                confirmButton = {
                    TextButton(onClick = {
                        divulgarAcessibilidade = false
                        context.startActivity(intentConfigAcessibilidade(context))
                    }) { Text(stringResource(R.string.abrir_configuracoes)) }
                },
                dismissButton = { TextButton(onClick = { divulgarAcessibilidade = false }) { Text(stringResource(R.string.agora_nao)) } },
            )
        }
        if (pedirAcessoNotificacoes) {
            AlertDialog(
                onDismissRequest = { pedirAcessoNotificacoes = false },
                title = { Text(stringResource(R.string.acesso_notificacoes_titulo)) },
                text = { Text(stringResource(R.string.acesso_notificacoes_texto)) },
                confirmButton = {
                    TextButton(onClick = {
                        pedirAcessoNotificacoes = false
                        context.startActivity(Intent(PermissoesAndroid.ACAO_CONFIG_LISTENER))
                    }) { Text(stringResource(R.string.abrir_configuracoes)) }
                },
                dismissButton = { TextButton(onClick = { pedirAcessoNotificacoes = false }) { Text(stringResource(R.string.depois)) } },
            )
        }
    }
}

@Composable
private fun descricaoTeste(m: Macro): String {
    val linhas = m.acoes.map { a ->
        when (a) {
            is EfetuarChamada -> stringResource(R.string.teste_ligar, a.destinatario.exibicao())
            is EnviarSms -> pluralStringResource(R.plurals.teste_sms, a.destinatarios.size, a.destinatarios.size)
            is EnviarEmail -> pluralStringResource(R.plurals.teste_email, a.destinatarios.size, a.destinatarios.size)
            is EnviarWhatsApp -> pluralStringResource(R.plurals.teste_whatsapp, a.destinatarios.size, a.destinatarios.size)
        }
    }
    return stringResource(R.string.testar_texto) + "\n\n" + linhas.joinToString("\n") { "• $it" }
}

@Composable
private fun DialogoDaEdicao(
    m: Macro,
    edicao: Edicao?,
    vm: EditorViewModel,
    contaConfigurada: Boolean,
    instaladosWhatsApp: Set<AppWhatsApp>,
    fechar: () -> Unit,
) {
    when (edicao) {
        null -> Unit
        is Edicao.Gatilho -> DialogoNotificacaoRecebida(
            inicial = edicao.indice?.let { m.gatilhos[it] as NotificacaoRecebida } ?: NotificacaoRecebida(),
            carregarApps = vm::apps,
            onCancelar = fechar,
            onConfirmar = { vm.salvarGatilho(edicao.indice, it); fechar() },
        )
        is Edicao.AcaoEd -> {
            val atual = edicao.indice?.let { m.acoes[it] }
            val salvar: (Acao) -> Unit = { vm.salvarAcao(edicao.indice, it); fechar() }
            when (edicao.tipo) {
                TipoAcaoNova.Chamada -> DialogoChamada(atual as? EfetuarChamada, fechar, salvar)
                TipoAcaoNova.Sms -> DialogoSms(atual as? EnviarSms, fechar, salvar)
                TipoAcaoNova.Email -> DialogoEmail(atual as? EnviarEmail, contaConfigurada, fechar, salvar)
                TipoAcaoNova.WhatsApp -> DialogoWhatsApp(atual as? EnviarWhatsApp, instaladosWhatsApp, fechar, salvar)
            }
        }
        is Edicao.RestricaoEd -> {
            val atual = edicao.indice?.let { m.restricoes[it] }
            val salvar: (Restricao) -> Unit = { vm.salvarRestricao(edicao.indice, it); fechar() }
            when (edicao.tipo) {
                TipoRestricaoNova.Faixa -> DialogoFaixaHorario(atual as? FaixaHorario, fechar, salvar)
                TipoRestricaoNova.Dias -> DialogoDiasSemana(atual as? DiasSemana, fechar, salvar)
                TipoRestricaoNova.Limite -> DialogoLimiteFrequencia(atual as? LimiteFrequencia, fechar, salvar)
            }
        }
    }
}

@Composable
private fun CartaoSecao(
    titulo: String,
    quantidade: Int,
    cor: Color,
    vazio: String,
    onAdicionar: (() -> Unit)? = null,
    tipos: List<Pair<String, () -> Unit>> = emptyList(),
    itens: @Composable () -> Unit,
) {
    var escolhendo by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.15f))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$titulo ($quantidade)", style = MaterialTheme.typography.titleMedium, color = cor, modifier = Modifier.weight(1f))
            Row {
                IconButton(onClick = { if (onAdicionar != null) onAdicionar() else escolhendo = true }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.adicionar), tint = cor)
                }
                DropdownMenu(expanded = escolhendo, onDismissRequest = { escolhendo = false }) {
                    tipos.forEach { (rotulo, acao) ->
                        DropdownMenuItem(text = { Text(rotulo) }, onClick = { escolhendo = false; acao() })
                    }
                }
            }
        }
        HorizontalDivider(color = cor.copy(alpha = 0.4f))
        if (quantidade == 0) {
            Text(vazio, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        } else {
            Column { itens() }
        }
    }
}

@Composable
private fun ItemCartao(
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    onRemover: () -> Unit,
    onSubir: (() -> Unit)? = null,
    onDescer: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall)
        }
        if (onSubir != null) IconButton(onClick = onSubir) { Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.subir)) }
        if (onDescer != null) IconButton(onClick = onDescer) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.descer)) }
        IconButton(onClick = onRemover) { Icon(Icons.Filled.Delete, stringResource(R.string.remover)) }
    }
}
