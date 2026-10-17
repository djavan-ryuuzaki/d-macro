package br.com.djavanfernando.dmacro.ui.registros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.data.RegistroRepository
import br.com.djavanfernando.dmacro.engine.Desfecho
import br.com.djavanfernando.dmacro.engine.RegistroExecucao
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.TipoAcao
import br.com.djavanfernando.dmacro.ui.comum.descricao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
private fun RegistroExecucao.horario() = instante.atZone(ZoneId.systemDefault()).format(FORMATO)

@HiltViewModel
class RegistrosViewModel @Inject constructor(private val repo: RegistroRepository) : ViewModel() {
    val registros: StateFlow<List<RegistroExecucao>> =
        repo.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun limpar() {
        viewModelScope.launch { repo.limpar() }
    }
}

@HiltViewModel
class RegistroViewModel @Inject constructor(estado: SavedStateHandle, repo: RegistroRepository) : ViewModel() {
    val registro: StateFlow<RegistroExecucao?> =
        repo.observar(checkNotNull<String>(estado["id"])).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private enum class Selo(val texto: Int) { Executada(R.string.selo_executada), Barrada(R.string.selo_barrada), Bloqueada(R.string.selo_bloqueada) }

private fun RegistroExecucao.selo() = when (desfecho) {
    Desfecho.Executada -> Selo.Executada
    is Desfecho.BarradaPorRestricao -> Selo.Barrada
    is Desfecho.BloqueadaPorPermissao -> Selo.Bloqueada
}

@Composable
private fun resumoResultados(r: RegistroExecucao): String {
    if (r.desfecho != Desfecho.Executada) return descricao(r.desfecho)
    val ok = r.itens.count { it.resultado == ResultadoAcao.Ok }
    val pendentes = r.itens.count { it.resultado == ResultadoAcao.Pendente }
    val falhas = r.itens.size - ok - pendentes
    return stringResource(R.string.resumo_resultados, ok, pendentes, falhas)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosScreen(onAbrir: (String) -> Unit, vm: RegistrosViewModel = hiltViewModel()) {
    val registros by vm.registros.collectAsState()
    var confirmarLimpeza by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aba_registros)) },
                actions = {
                    IconButton(onClick = { confirmarLimpeza = true }, enabled = registros.isNotEmpty()) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.limpar_registros))
                    }
                },
            )
        },
    ) { padding ->
        if (registros.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.sem_registros)) }
        }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(registros, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { onAbrir(r.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.macroNome, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(r.horario(), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = { onAbrir(r.id) }, label = { Text(stringResource(r.selo().texto)) })
                            if (r.teste) {
                                Spacer(Modifier.width(6.dp))
                                AssistChip(onClick = { onAbrir(r.id) }, label = { Text(stringResource(R.string.selo_teste)) })
                            }
                        }
                        Text(resumoResultados(r), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (confirmarLimpeza) {
        AlertDialog(
            onDismissRequest = { confirmarLimpeza = false },
            title = { Text(stringResource(R.string.limpar_registros_titulo)) },
            confirmButton = { TextButton(onClick = { vm.limpar(); confirmarLimpeza = false }) { Text(stringResource(R.string.limpar)) } },
            dismissButton = { TextButton(onClick = { confirmarLimpeza = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(onVoltar: () -> Unit, vm: RegistroViewModel = hiltViewModel()) {
    val registro by vm.registro.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(registro?.macroNome.orEmpty()) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar)) } },
            )
        },
    ) { padding ->
        val r = registro ?: return@Scaffold
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(r.horario() + if (r.teste) " · " + stringResource(R.string.selo_teste) else "", style = MaterialTheme.typography.labelLarge)
            Text(descricao(r.desfecho), style = MaterialTheme.typography.titleMedium)

            r.origem?.let { o ->
                HorizontalDivider()
                Text(stringResource(R.string.notificacao_origem), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.origem_app, o.nomeApp, o.pacote))
                Text(stringResource(R.string.origem_titulo, o.titulo))
                Text(stringResource(R.string.origem_texto, o.texto))
            }

            if (r.itens.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.resultados), style = MaterialTheme.typography.titleSmall)
                r.itens.forEach { item ->
                    val tipo = stringResource(
                        when (item.tipo) {
                            TipoAcao.Chamada -> R.string.acao_chamada
                            TipoAcao.Sms -> R.string.acao_sms
                            TipoAcao.Email -> R.string.acao_email
                            TipoAcao.WhatsApp -> R.string.acao_whatsapp
                        },
                    )
                    val quem = item.rotulo?.let { "$it (${item.destinatario})" } ?: item.destinatario
                    Text("${item.indiceAcao + 1}. $tipo → $quem")
                    Text(
                        descricao(item.resultado),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.resultado is ResultadoAcao.Erro || item.resultado == ResultadoAcao.BloqueadoPorTeto)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
