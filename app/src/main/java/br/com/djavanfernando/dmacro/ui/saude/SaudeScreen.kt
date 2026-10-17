package br.com.djavanfernando.dmacro.ui.saude

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.data.MacroRepository
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarWhatsApp
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.Permissao
import br.com.djavanfernando.dmacro.platform.EstadoSaude
import br.com.djavanfernando.dmacro.platform.PermissoesAndroid
import br.com.djavanfernando.dmacro.platform.SaudeApp
import br.com.djavanfernando.dmacro.platform.intentConfigAcessibilidade
import br.com.djavanfernando.dmacro.platform.manifesto
import br.com.djavanfernando.dmacro.ui.editor.rotuloApp
import br.com.djavanfernando.dmacro.ui.comum.nome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SaudeViewModel @Inject constructor(private val saudeApp: SaudeApp, macros: MacroRepository) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoSaude?>(null)
    val estado: StateFlow<EstadoSaude?> = _estado.asStateFlow()
    val macros: StateFlow<List<Macro>> = macros.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun atualizar() {
        saudeApp.reconectarListenerSePreciso()
        _estado.value = saudeApp.ler()
    }
}

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaudeScreen(onAbrirConfiguracoes: () -> Unit, vm: SaudeViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsState()
    val macros by vm.macros.collectAsState()
    val context = LocalContext.current
    var guiaSamsung by remember { mutableStateOf(false) }
    var guiaRestritas by remember { mutableStateOf(false) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.atualizar() }

    LifecycleResumeEffect(Unit) {
        vm.atualizar()
        onPauseOrDispose { }
    }

    fun abrirDetalhesApp() = context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.saude_titulo)) }) }) { padding ->
        val e = estado ?: return@Scaffold
        val acoesWhatsApp = macros.flatMap { m -> m.acoes.filterIsInstance<EnviarWhatsApp>() }
        val acessibilidadeAtiva = e.permissoes[Permissao.Acessibilidade] == true
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ItemSaude(
                ok = e.acessoNotificacoes,
                titulo = stringResource(R.string.saude_acesso_notificacoes),
                descricao = stringResource(R.string.saude_acesso_notificacoes_desc),
                acao = stringResource(R.string.corrigir),
            ) { context.startActivity(Intent(PermissoesAndroid.ACAO_CONFIG_LISTENER)) }

            if (e.acessoNotificacoes) {
                ItemSaude(
                    ok = e.listenerConectado,
                    titulo = stringResource(R.string.saude_listener),
                    descricao = stringResource(R.string.saude_listener_desc),
                    acao = stringResource(R.string.reconectar),
                ) { vm.atualizar() }
            }

            Permissao.entries.forEach { p ->
                val manifesto = p.manifesto ?: return@forEach
                ItemSaude(
                    ok = e.permissoes[p] == true,
                    titulo = stringResource(R.string.saude_permissao, nome(p)),
                    descricao = stringResource(R.string.saude_permissao_desc),
                    acao = stringResource(R.string.conceder),
                ) { pedirPermissao.launch(manifesto) }
            }

            if (acoesWhatsApp.isNotEmpty()) {
                ItemSaude(
                    ok = acessibilidadeAtiva,
                    titulo = stringResource(R.string.saude_acessibilidade),
                    descricao = stringResource(R.string.saude_acessibilidade_desc),
                    acao = stringResource(R.string.corrigir),
                ) { context.startActivity(intentConfigAcessibilidade(context)) }

                acoesWhatsApp.map { it.app }.distinct().forEach { app ->
                    ItemSaude(
                        ok = app in e.whatsAppsInstalados,
                        titulo = stringResource(R.string.saude_whatsapp_instalado, rotuloApp(app)),
                        descricao = stringResource(R.string.saude_whatsapp_instalado_desc),
                        acao = null,
                    ) {}
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= 33 && (!e.acessoNotificacoes || (acoesWhatsApp.isNotEmpty() && !acessibilidadeAtiva))) {
                ItemSaude(
                    ok = null,
                    titulo = stringResource(R.string.saude_restritas),
                    descricao = stringResource(R.string.saude_restritas_desc),
                    acao = stringResource(R.string.ver_guia),
                ) { guiaRestritas = true }
            }

            ItemSaude(
                ok = e.podePostarNotificacoes,
                titulo = stringResource(R.string.saude_post_notificacoes),
                descricao = stringResource(R.string.saude_post_notificacoes_desc),
                acao = stringResource(R.string.conceder),
            ) {
                if (android.os.Build.VERSION.SDK_INT >= 33) pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }

            ItemSaude(
                ok = e.bateriaIgnorada,
                titulo = stringResource(R.string.saude_bateria),
                descricao = stringResource(R.string.saude_bateria_desc),
                acao = stringResource(R.string.corrigir),
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                )
            }

            if (PermissoesAndroid.ehSamsung) {
                ItemSaude(
                    ok = null,
                    titulo = stringResource(R.string.saude_samsung),
                    descricao = stringResource(R.string.saude_samsung_desc),
                    acao = stringResource(R.string.ver_guia),
                ) { guiaSamsung = true }
            }

            if (macros.any { m -> m.acoes.any { it is EnviarEmail } }) {
                ItemSaude(
                    ok = e.contaEmail,
                    titulo = stringResource(R.string.saude_conta_email),
                    descricao = stringResource(R.string.saude_conta_email_desc),
                    acao = stringResource(R.string.configurar),
                    onAcao = onAbrirConfiguracoes,
                )
            }

            if (PermissoesAndroid.temNotificacoesAprimoradas) {
                ItemSaude(
                    ok = null,
                    titulo = stringResource(R.string.saude_android15),
                    descricao = stringResource(R.string.aviso_android15),
                    acao = null,
                ) {}
            }
        }
    }

    if (guiaRestritas) {
        AlertDialog(
            onDismissRequest = { guiaRestritas = false },
            title = { Text(stringResource(R.string.saude_restritas)) },
            text = { Text(stringResource(R.string.guia_restritas)) },
            confirmButton = {
                TextButton(onClick = { guiaRestritas = false; abrirDetalhesApp() }) { Text(stringResource(R.string.abrir_configuracoes)) }
            },
            dismissButton = { TextButton(onClick = { guiaRestritas = false }) { Text(stringResource(R.string.fechar)) } },
        )
    }

    if (guiaSamsung) {
        AlertDialog(
            onDismissRequest = { guiaSamsung = false },
            title = { Text(stringResource(R.string.saude_samsung)) },
            text = { Text(stringResource(R.string.guia_samsung)) },
            confirmButton = {
                TextButton(onClick = { guiaSamsung = false; abrirDetalhesApp() }) { Text(stringResource(R.string.abrir_configuracoes)) }
            },
            dismissButton = { TextButton(onClick = { guiaSamsung = false }) { Text(stringResource(R.string.fechar)) } },
        )
    }
}

/** [ok] nulo = item informativo, sem status. */
@Composable
private fun ItemSaude(ok: Boolean?, titulo: String, descricao: String, acao: String?, onAcao: () -> Unit) {
    val (icone: ImageVector, cor) = when (ok) {
        true -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
        false -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
        null -> Icons.Filled.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, null, tint = cor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(descricao, style = MaterialTheme.typography.bodySmall)
            }
            if (acao != null && ok != true) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onAcao) { Text(acao) }
            }
        }
    }
}
