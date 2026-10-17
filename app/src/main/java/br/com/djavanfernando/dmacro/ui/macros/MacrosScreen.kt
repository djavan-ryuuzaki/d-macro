package br.com.djavanfernando.dmacro.ui.macros

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.validar
import br.com.djavanfernando.dmacro.ui.comum.resumo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    onAbrirMacro: (String) -> Unit,
    onAbrirSaude: () -> Unit,
    vm: MacrosViewModel = hiltViewModel(),
) {
    val macros by vm.macros.collectAsState()
    val chaveGeral by vm.chaveGeral.collectAsState()
    val saude by vm.saude.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    val nomeNovaMacro = stringResource(R.string.nova_macro)
    val msgIncompleta = stringResource(R.string.macro_incompleta)
    val pedirNotificacoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LifecycleResumeEffect(Unit) {
        vm.atualizarSaude()
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Text(stringResource(if (chaveGeral) R.string.ativado else R.string.pausado), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = chaveGeral, onCheckedChange = vm::definirChaveGeral, modifier = Modifier.padding(end = 12.dp))
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.criar(nomeNovaMacro, onAbrirMacro) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.nova_macro)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!chaveGeral) {
                item { Aviso(stringResource(R.string.chave_geral_desligada)) {} }
            }
            if (saude?.temProblemaCritico(macros.filter { it.ativada }) == true) {
                item { Aviso(stringResource(R.string.banner_saude), onAbrirSaude) }
            }
            if (macros.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.sem_macros), textAlign = TextAlign.Center)
                    }
                }
            }
            items(macros, key = { it.id }) { macro ->
                ItemMacro(
                    macro = macro,
                    comProblema = saude?.problemasDa(macro)?.isNotEmpty() == true,
                    onClick = { onAbrirMacro(macro.id) },
                    onAtivar = { ligar ->
                        if (ligar && validar(macro).isNotEmpty()) {
                            escopo.launch { snackbar.showSnackbar(msgIncompleta) }
                        } else {
                            vm.definirAtivada(macro, ligar)
                            if (ligar && Build.VERSION.SDK_INT >= 33 && saude?.podePostarNotificacoes == false) {
                                pedirNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Aviso(texto: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null)
            Spacer(Modifier.width(12.dp))
            Text(texto)
        }
    }
}

@Composable
private fun ItemMacro(macro: Macro, comProblema: Boolean, onClick: () -> Unit, onAtivar: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (comProblema) {
                        Icon(Icons.Filled.Warning, stringResource(R.string.macro_com_problema), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(macro.nome.ifBlank { stringResource(R.string.sem_nome) }, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    macro.gatilhos.firstOrNull()?.let { resumo(it) } ?: stringResource(R.string.sem_gatilhos),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                )
            }
            Switch(checked = macro.ativada, onCheckedChange = onAtivar)
        }
    }
}
