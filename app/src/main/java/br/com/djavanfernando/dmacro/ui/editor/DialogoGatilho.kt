package br.com.djavanfernando.dmacro.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.FiltroTexto
import br.com.djavanfernando.dmacro.engine.ModoFiltro
import br.com.djavanfernando.dmacro.engine.NotificacaoRecebida
import br.com.djavanfernando.dmacro.engine.SelecaoApps
import br.com.djavanfernando.dmacro.platform.AppInstalado
import br.com.djavanfernando.dmacro.platform.PermissoesAndroid
import br.com.djavanfernando.dmacro.ui.comum.DialogoTelaCheia
import br.com.djavanfernando.dmacro.ui.comum.EditorTermos
import br.com.djavanfernando.dmacro.ui.comum.Secao
import br.com.djavanfernando.dmacro.ui.comum.rotuloModo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DialogoNotificacaoRecebida(
    inicial: NotificacaoRecebida,
    carregarApps: () -> List<AppInstalado>,
    onCancelar: () -> Unit,
    onConfirmar: (NotificacaoRecebida) -> Unit,
) {
    var g by remember { mutableStateOf(inicial) }
    var escolhendoApps by remember { mutableStateOf(false) }

    DialogoTelaCheia(
        titulo = stringResource(R.string.gatilho_notificacao),
        podeConfirmar = true,
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(g) },
    ) {
        if (PermissoesAndroid.temNotificacoesAprimoradas) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(stringResource(R.string.aviso_android15), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        Secao(stringResource(R.string.aplicativos))
        OpcaoRadio(stringResource(R.string.todos_os_apps), g.apps == SelecaoApps.Todos, Modifier.fillMaxWidth()) {
            g = g.copy(apps = SelecaoApps.Todos)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val apps = g.apps
            OpcaoRadio(
                if (apps is SelecaoApps.Somente) pluralStringResource(R.plurals.resumo_n_apps, apps.pacotes.size, apps.pacotes.size)
                else stringResource(R.string.escolher_apps),
                apps is SelecaoApps.Somente,
                Modifier.weight(1f),
            ) { escolhendoApps = true }
            OutlinedButton(onClick = { escolhendoApps = true }) { Text(stringResource(R.string.escolher)) }
        }

        EditorFiltro(stringResource(R.string.filtro_titulo), g.filtroTitulo) { g = g.copy(filtroTitulo = it) }
        EditorFiltro(stringResource(R.string.filtro_texto), g.filtroTexto) { g = g.copy(filtroTexto = it) }

        Secao(stringResource(R.string.opcoes))
        Opcao(stringResource(R.string.ignorar_maiusculas), g.ignorarMaiusculas) { g = g.copy(ignorarMaiusculas = it) }
        Opcao(stringResource(R.string.ignorar_em_curso), g.ignorarEmCurso) { g = g.copy(ignorarEmCurso = it) }
        Opcao(stringResource(R.string.impedir_multiplos), g.impedirMultiplosDisparos) { g = g.copy(impedirMultiplosDisparos = it) }
    }

    if (escolhendoApps) {
        val atuais = (g.apps as? SelecaoApps.Somente)?.pacotes.orEmpty()
        DialogoApps(
            selecionados = atuais,
            carregarApps = carregarApps,
            onCancelar = { escolhendoApps = false },
            onConfirmar = { pacotes ->
                g = g.copy(apps = if (pacotes.isEmpty()) SelecaoApps.Todos else SelecaoApps.Somente(pacotes))
                escolhendoApps = false
            },
        )
    }
}

@Composable
private fun EditorFiltro(titulo: String, filtro: FiltroTexto, onMudar: (FiltroTexto) -> Unit) {
    Secao(titulo)
    Column {
        ModoFiltro.entries.chunked(2).forEach { linha ->
            Row {
                linha.forEach { modo ->
                    OpcaoRadio(stringResource(rotuloModo(modo)), filtro.modo == modo, Modifier.weight(1f)) {
                        onMudar(filtro.copy(modo = modo))
                    }
                }
            }
        }
    }
    if (filtro.modo != ModoFiltro.Qualquer) {
        EditorTermos(filtro.termos) { onMudar(filtro.copy(termos = it)) }
        Text(stringResource(R.string.dica_termos), style = MaterialTheme.typography.bodySmall)
    }
}

/** Linha inteira clicável com um RadioButton. */
@Composable
private fun OpcaoRadio(texto: String, selecionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.selectable(selected = selecionado, onClick = onClick, role = Role.RadioButton).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        Text(texto)
    }
}

@Composable
fun Opcao(texto: String, marcado: Boolean, onMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onMudar(!marcado) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = marcado, onCheckedChange = onMudar)
        Text(texto)
    }
}

@Composable
private fun DialogoApps(
    selecionados: Set<String>,
    carregarApps: () -> List<AppInstalado>,
    onCancelar: () -> Unit,
    onConfirmar: (Set<String>) -> Unit,
) {
    var escolha by remember { mutableStateOf(selecionados) }
    var busca by remember { mutableStateOf("") }
    val apps by produceState<List<AppInstalado>?>(null) { value = withContext(Dispatchers.IO) { carregarApps() } }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.escolher_apps)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(busca, { busca = it }, label = { Text(stringResource(R.string.buscar)) }, singleLine = true)
                val lista = apps
                if (lista == null) {
                    Text(stringResource(R.string.carregando))
                } else {
                    LazyColumn(Modifier.heightIn(max = 400.dp)) {
                        items(lista.filter { busca.isBlank() || it.nome.contains(busca, true) || it.pacote.contains(busca, true) }) { app ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    escolha = if (app.pacote in escolha) escolha - app.pacote else escolha + app.pacote
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = app.pacote in escolha, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                app.icone?.let {
                                    Image(it.toBitmap(48, 48).asImageBitmap(), null, Modifier.size(32.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(app.nome)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirmar(escolha) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}
