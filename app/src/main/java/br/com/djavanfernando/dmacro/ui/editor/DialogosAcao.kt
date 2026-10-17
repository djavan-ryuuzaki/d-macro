package br.com.djavanfernando.dmacro.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.ContextoMarcadores
import br.com.djavanfernando.dmacro.engine.EfetuarChamada
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarSms
import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.engine.EnviarWhatsApp
import br.com.djavanfernando.dmacro.engine.numeroWhatsApp
import br.com.djavanfernando.dmacro.engine.Marcadores
import br.com.djavanfernando.dmacro.engine.cortarSms
import br.com.djavanfernando.dmacro.platform.SmsAndroid
import br.com.djavanfernando.dmacro.ui.comum.CampoComMarcadores
import br.com.djavanfernando.dmacro.ui.comum.DialogoTelaCheia
import br.com.djavanfernando.dmacro.ui.comum.EditorEmails
import br.com.djavanfernando.dmacro.ui.comum.EditorTelefones
import br.com.djavanfernando.dmacro.ui.comum.Secao
import java.time.Instant
import java.time.ZoneId

@Composable
fun DialogoChamada(inicial: EfetuarChamada?, onCancelar: () -> Unit, onConfirmar: (EfetuarChamada) -> Unit) {
    var destinatario by remember { mutableStateOf(inicial?.destinatario) }
    DialogoTelaCheia(
        titulo = stringResource(R.string.acao_chamada),
        podeConfirmar = destinatario != null,
        onCancelar = onCancelar,
        onConfirmar = { destinatario?.let { onConfirmar(EfetuarChamada(it)) } },
    ) {
        Secao(stringResource(R.string.destinatario))
        EditorTelefones(listOfNotNull(destinatario), { destinatario = it.lastOrNull() }, maximo = 1)
        Text(stringResource(R.string.dica_chamada), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DialogoSms(inicial: EnviarSms?, onCancelar: () -> Unit, onConfirmar: (EnviarSms) -> Unit) {
    val context = LocalContext.current
    var acao by remember { mutableStateOf(inicial ?: EnviarSms(emptyList(), "")) }
    var limiteTexto by remember { mutableStateOf(acao.limiteCaracteres.toString()) }

    DialogoTelaCheia(
        titulo = stringResource(R.string.acao_sms),
        podeConfirmar = acao.destinatarios.isNotEmpty() && acao.mensagem.isNotBlank() && acao.limiteCaracteres >= 2,
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(acao) },
    ) {
        Secao(stringResource(R.string.destinatarios))
        EditorTelefones(acao.destinatarios, { acao = acao.copy(destinatarios = it) })

        Secao(stringResource(R.string.mensagem))
        CampoComMarcadores(stringResource(R.string.mensagem), acao.mensagem, { acao = acao.copy(mensagem = it) })

        OutlinedTextField(
            value = limiteTexto,
            onValueChange = { t ->
                limiteTexto = t.filter(Char::isDigit).take(4)
                limiteTexto.toIntOrNull()?.let { acao = acao.copy(limiteCaracteres = it) }
            },
            label = { Text(stringResource(R.string.limite_caracteres)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        val exemplo = cortarSms(
            Marcadores.renderizar(acao.mensagem, ContextoMarcadores.exemplo(Instant.now(), ZoneId.systemDefault())),
            acao.limiteCaracteres.coerceAtLeast(2),
        )
        val partes = SmsAndroid.contarPartes(context, exemplo)
        Text(
            pluralStringResource(R.plurals.estimativa_partes, partes, partes, exemplo.length),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun DialogoEmail(
    inicial: EnviarEmail?,
    contaConfigurada: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: (EnviarEmail) -> Unit,
) {
    var acao by remember { mutableStateOf(inicial ?: EnviarEmail(emptyList(), "", "")) }
    DialogoTelaCheia(
        titulo = stringResource(R.string.acao_email),
        podeConfirmar = acao.destinatarios.isNotEmpty() && acao.corpo.isNotBlank(),
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(acao) },
    ) {
        if (!contaConfigurada) {
            Text(stringResource(R.string.aviso_sem_conta), color = MaterialTheme.colorScheme.error)
        }
        Secao(stringResource(R.string.destinatarios))
        EditorEmails(acao.destinatarios, { acao = acao.copy(destinatarios = it) })

        Secao(stringResource(R.string.mensagem))
        CampoComMarcadores(stringResource(R.string.assunto), acao.assunto, { acao = acao.copy(assunto = it) }, linhas = 1)
        CampoComMarcadores(stringResource(R.string.corpo), acao.corpo, { acao = acao.copy(corpo = it) }, linhas = 8)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoWhatsApp(
    inicial: EnviarWhatsApp?,
    instalados: Set<AppWhatsApp>,
    onCancelar: () -> Unit,
    onConfirmar: (EnviarWhatsApp) -> Unit,
) {
    var acao by remember {
        val base = inicial ?: EnviarWhatsApp(emptyList(), "")
        mutableStateOf(if (base.app !in instalados && instalados.isNotEmpty()) base.copy(app = instalados.first()) else base)
    }
    DialogoTelaCheia(
        titulo = stringResource(R.string.acao_whatsapp),
        podeConfirmar = acao.destinatarios.isNotEmpty() && acao.mensagem.isNotBlank() && acao.app in instalados,
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(acao) },
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(stringResource(R.string.aviso_whatsapp_desbloqueada), Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (instalados.isEmpty()) {
            Text(stringResource(R.string.aviso_sem_whatsapp), color = MaterialTheme.colorScheme.error)
        } else if (instalados.size > 1) {
            Secao(stringResource(R.string.enviar_por))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppWhatsApp.entries.filter { it in instalados }.forEach { app ->
                    FilterChip(selected = acao.app == app, onClick = { acao = acao.copy(app = app) }, label = { Text(rotuloApp(app)) })
                }
            }
        }

        Secao(stringResource(R.string.destinatarios))
        EditorTelefones(acao.destinatarios, { acao = acao.copy(destinatarios = it) })
        acao.destinatarios.forEach { d ->
            val numero = numeroWhatsApp(d.numero)
            Text(
                if (numero != null) stringResource(R.string.previa_numero_whatsapp, d.rotulo ?: d.numero, "+$numero")
                else stringResource(R.string.numero_whatsapp_sem_ddd, d.rotulo ?: d.numero),
                style = MaterialTheme.typography.bodySmall,
                color = if (numero != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }

        Secao(stringResource(R.string.mensagem))
        CampoComMarcadores(stringResource(R.string.mensagem), acao.mensagem, { acao = acao.copy(mensagem = it) })
    }
}

@Composable
fun rotuloApp(app: AppWhatsApp): String = stringResource(
    when (app) {
        AppWhatsApp.Pessoal -> R.string.whatsapp_pessoal
        AppWhatsApp.Business -> R.string.whatsapp_business
    },
)
