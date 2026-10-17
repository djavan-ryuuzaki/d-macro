package br.com.djavanfernando.dmacro.ui.comum

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.Email
import br.com.djavanfernando.dmacro.engine.Marcadores
import br.com.djavanfernando.dmacro.engine.Telefone

/** Diálogo em tela cheia usado para configurar Gatilhos, Ações e Restrições. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoTelaCheia(
    titulo: String,
    podeConfirmar: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo) },
                    navigationIcon = {
                        IconButton(onClick = onCancelar) { Icon(Icons.Filled.Close, stringResource(R.string.cancelar)) }
                    },
                    actions = {
                        TextButton(onClick = onConfirmar, enabled = podeConfirmar) { Text(stringResource(R.string.ok)) }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { conteudo() }
        }
    }
}

@Composable
fun Secao(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** Lista de termos de um Filtro: chips removíveis + campo para adicionar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorTermos(termos: List<String>, onMudar: (List<String>) -> Unit) {
    var novo by remember { mutableStateOf("") }
    fun adicionar() {
        val t = novo.trim()
        if (t.isNotEmpty() && t !in termos) onMudar(termos + t)
        novo = ""
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        termos.forEach { termo ->
            InputChip(
                selected = false,
                onClick = { onMudar(termos - termo) },
                label = { Text(termo) },
                trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.remover)) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = novo,
            onValueChange = { novo = it },
            label = { Text(stringResource(R.string.termo)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = ::adicionar, enabled = novo.isNotBlank()) { Text(stringResource(R.string.adicionar)) }
    }
}

/** Campo de texto com botões para inserir Marcadores na posição do cursor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampoComMarcadores(rotulo: String, valor: String, onMudar: (String) -> Unit, linhas: Int = 4) {
    var campo by remember { mutableStateOf(TextFieldValue(valor, TextRange(valor.length))) }
    if (campo.text != valor) campo = TextFieldValue(valor, TextRange(valor.length))
    Column {
        OutlinedTextField(
            value = campo,
            onValueChange = { campo = it; onMudar(it.text) },
            label = { Text(rotulo) },
            minLines = if (linhas > 1) 2 else 1,
            maxLines = linhas,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Marcadores.todos.forEach { marcador ->
                AssistChip(
                    onClick = {
                        val inicio = campo.selection.min
                        val texto = campo.text.replaceRange(inicio, campo.selection.max, marcador)
                        campo = TextFieldValue(texto, TextRange(inicio + marcador.length))
                        onMudar(texto)
                    },
                    label = { Text(marcador) },
                )
            }
        }
    }
}

private class EscolherContato(private val tipo: Uri) : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) = Intent(Intent.ACTION_PICK, tipo)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

private fun lerContato(context: Context, uri: Uri, colunaValor: String): Pair<String, String?>? =
    context.contentResolver.query(uri, arrayOf(colunaValor, ContactsContract.Contacts.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) to c.getString(1) else null }

/** Editor de Destinatários de telefone: seletor da agenda (sem READ_CONTACTS) ou digitação manual. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorTelefones(destinatarios: List<Telefone>, onMudar: (List<Telefone>) -> Unit, maximo: Int = Int.MAX_VALUE) {
    val context = LocalContext.current
    val seletor = rememberLauncherForActivityResult(EscolherContato(ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        lerContato(context, uri, ContactsContract.CommonDataKinds.Phone.NUMBER)?.let { (numero, nome) ->
            onMudar((destinatarios + Telefone(numero, nome)).takeLast(maximo))
        }
    }
    EditorDestinatarios(
        itens = destinatarios.map { it.exibicao() },
        onRemover = { i -> onMudar(destinatarios.filterIndexed { j, _ -> j != i }) },
        rotuloCampo = stringResource(R.string.numero),
        teclado = KeyboardType.Phone,
        onAdicionarManual = { onMudar((destinatarios + Telefone(it)).takeLast(maximo)) },
        onAgenda = { seletor.launch(Unit) },
    )
}

@Composable
fun EditorEmails(destinatarios: List<Email>, onMudar: (List<Email>) -> Unit) {
    val context = LocalContext.current
    val seletor = rememberLauncherForActivityResult(EscolherContato(ContactsContract.CommonDataKinds.Email.CONTENT_URI)) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        lerContato(context, uri, ContactsContract.CommonDataKinds.Email.ADDRESS)?.let { (endereco, nome) ->
            onMudar(destinatarios + Email(endereco, nome))
        }
    }
    EditorDestinatarios(
        itens = destinatarios.map { it.exibicao() },
        onRemover = { i -> onMudar(destinatarios.filterIndexed { j, _ -> j != i }) },
        rotuloCampo = stringResource(R.string.endereco_email),
        teclado = KeyboardType.Email,
        onAdicionarManual = { onMudar(destinatarios + Email(it)) },
        onAgenda = { seletor.launch(Unit) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorDestinatarios(
    itens: List<String>,
    onRemover: (Int) -> Unit,
    rotuloCampo: String,
    teclado: KeyboardType,
    onAdicionarManual: (String) -> Unit,
    onAgenda: () -> Unit,
) {
    var manual by remember { mutableStateOf("") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itens.forEachIndexed { i, item ->
            InputChip(
                selected = false,
                onClick = { onRemover(i) },
                label = { Text(item) },
                trailingIcon = { Icon(Icons.Filled.Close, stringResource(R.string.remover)) },
            )
        }
    }
    OutlinedButton(onClick = onAgenda) {
        Icon(Icons.Filled.Contacts, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.escolher_da_agenda))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = manual,
            onValueChange = { manual = it },
            label = { Text(rotuloCampo) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { onAdicionarManual(manual.trim()); manual = "" },
            enabled = manual.isNotBlank(),
        ) { Text(stringResource(R.string.adicionar)) }
    }
}
