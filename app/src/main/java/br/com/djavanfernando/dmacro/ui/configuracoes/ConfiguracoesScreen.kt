package br.com.djavanfernando.dmacro.ui.configuracoes

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.data.ConfiguracoesRepository
import br.com.djavanfernando.dmacro.platform.ContaEmail
import br.com.djavanfernando.dmacro.platform.ContaEmailRepository
import br.com.djavanfernando.dmacro.platform.PresetEmail
import br.com.djavanfernando.dmacro.platform.ResultadoSmtp
import br.com.djavanfernando.dmacro.platform.Seguranca
import br.com.djavanfernando.dmacro.platform.ServicoAtivo
import br.com.djavanfernando.dmacro.platform.SmtpCliente
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ConfiguracoesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contas: ContaEmailRepository,
    private val smtp: SmtpCliente,
    private val configuracoes: ConfiguracoesRepository,
) : ViewModel() {

    val contaSalva: ContaEmail? get() = contas.obterSemSenha()
    val servicoPrimeiroPlano: StateFlow<Boolean> =
        configuracoes.servicoPrimeiroPlano.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun salvar(conta: ContaEmail) {
        contas.salvar(conta)
        mensagens.tryEmit(context.getString(R.string.conta_salva))
    }

    fun remover() {
        contas.remover()
        mensagens.tryEmit(context.getString(R.string.conta_removida))
    }

    /** Envia um e-mail de teste da conta salva para o próprio usuário. */
    fun testar() {
        val conta = contas.obter() ?: return
        viewModelScope.launch {
            mensagens.emit(context.getString(R.string.enviando_teste))
            val resultado = withContext(Dispatchers.IO) {
                smtp.enviar(conta, conta.usuario, context.getString(R.string.email_teste_assunto), context.getString(R.string.email_teste_corpo))
            }
            mensagens.emit(
                when (resultado) {
                    ResultadoSmtp.Ok -> context.getString(R.string.teste_ok, conta.usuario)
                    is ResultadoSmtp.FalhaAutenticacao -> context.getString(R.string.teste_falha_autenticacao)
                    is ResultadoSmtp.FalhaTemporaria -> context.getString(R.string.teste_falha, resultado.motivo)
                    is ResultadoSmtp.FalhaPermanente -> context.getString(R.string.teste_falha, resultado.motivo)
                },
            )
        }
    }

    fun definirServicoPrimeiroPlano(ligado: Boolean) {
        viewModelScope.launch {
            configuracoes.definirServicoPrimeiroPlano(ligado)
            if (ligado) ServicoAtivo.iniciar(context) else ServicoAtivo.parar(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfiguracoesScreen(vm: ConfiguracoesViewModel = hiltViewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val servico by vm.servicoPrimeiroPlano.collectAsState()
    LaunchedEffect(Unit) { vm.mensagens.collect { snackbar.showSnackbar(it) } }

    val salva = remember { vm.contaSalva }
    var temConta by remember { mutableStateOf(salva != null) }
    val presetInicial = PresetEmail.entries.firstOrNull { it != PresetEmail.Personalizado && it.servidor == salva?.servidor }
        ?: if (salva != null) PresetEmail.Personalizado else PresetEmail.Gmail
    var preset by remember { mutableStateOf(presetInicial) }
    var servidor by remember { mutableStateOf(salva?.servidor ?: preset.servidor) }
    var porta by remember { mutableStateOf((salva?.porta ?: preset.porta).toString()) }
    var seguranca by remember { mutableStateOf(salva?.seguranca ?: preset.seguranca) }
    var usuario by remember { mutableStateOf(salva?.usuario.orEmpty()) }
    var senha by remember { mutableStateOf("") }

    val portaNum = porta.toIntOrNull()
    val valido = servidor.isNotBlank() && portaNum != null && usuario.isNotBlank() && (senha.isNotEmpty() || temConta)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.aba_configuracoes)) }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.conta_email), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.conta_email_desc), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetEmail.entries.forEach { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = {
                            preset = p
                            if (p != PresetEmail.Personalizado) {
                                servidor = p.servidor; porta = p.porta.toString(); seguranca = p.seguranca
                            }
                        },
                        label = { Text(p.rotulo) },
                    )
                }
            }
            if (preset == PresetEmail.Personalizado) {
                OutlinedTextField(servidor, { servidor = it }, label = { Text(stringResource(R.string.servidor_smtp)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    porta, { porta = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.porta)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Seguranca.entries.forEach { s ->
                        FilterChip(selected = seguranca == s, onClick = { seguranca = s }, label = { Text(s.name) })
                    }
                }
            }
            OutlinedTextField(
                usuario, { usuario = it },
                label = { Text(stringResource(R.string.usuario_email)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                senha, { senha = it },
                label = { Text(stringResource(if (temConta) R.string.senha_salva else R.string.senha_app)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (preset == PresetEmail.Gmail) {
                Text(stringResource(R.string.dica_gmail), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = valido,
                    onClick = {
                        vm.salvar(ContaEmail(servidor, portaNum!!, seguranca, usuario, senha))
                        senha = ""
                        temConta = true
                    },
                ) { Text(stringResource(R.string.salvar)) }
                OutlinedButton(enabled = temConta, onClick = vm::testar) { Text(stringResource(R.string.enviar_teste)) }
                if (temConta) TextButton(onClick = { vm.remover(); temConta = false }) { Text(stringResource(R.string.remover)) }
            }

            HorizontalDivider()

            Card {
                Row(
                    Modifier
                        .toggleable(value = servico, role = Role.Switch, onValueChange = vm::definirServicoPrimeiroPlano)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.servico_opcao), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.servico_opcao_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = servico, onCheckedChange = null)
                }
            }
        }
    }
}
