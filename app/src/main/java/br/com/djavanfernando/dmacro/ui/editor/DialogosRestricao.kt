package br.com.djavanfernando.dmacro.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.engine.DiasSemana
import br.com.djavanfernando.dmacro.engine.FaixaHorario
import br.com.djavanfernando.dmacro.engine.LimiteFrequencia
import br.com.djavanfernando.dmacro.ui.comum.DialogoTelaCheia
import br.com.djavanfernando.dmacro.ui.comum.Secao
import br.com.djavanfernando.dmacro.ui.comum.nomeDia
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun DialogoFaixaHorario(inicial: FaixaHorario?, onCancelar: () -> Unit, onConfirmar: (FaixaHorario) -> Unit) {
    var faixa by remember { mutableStateOf(inicial ?: FaixaHorario(LocalTime.of(8, 0), LocalTime.of(18, 0))) }
    var editando by remember { mutableStateOf<Boolean?>(null) } // true = início, false = fim

    DialogoTelaCheia(
        titulo = stringResource(R.string.restricao_faixa),
        podeConfirmar = faixa.inicio != faixa.fim,
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(faixa) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { editando = true }) { Text(stringResource(R.string.inicio_hora, faixa.inicio.toString())) }
            OutlinedButton(onClick = { editando = false }) { Text(stringResource(R.string.fim_hora, faixa.fim.toString())) }
        }
        if (faixa.fim.isBefore(faixa.inicio)) {
            Text(stringResource(R.string.dica_meia_noite), style = MaterialTheme.typography.bodySmall)
        }
        if (faixa.inicio == faixa.fim) {
            Text(stringResource(R.string.erro_faixa_igual), color = MaterialTheme.colorScheme.error)
        }
    }

    editando?.let { inicio ->
        SeletorHora(
            inicial = if (inicio) faixa.inicio else faixa.fim,
            onCancelar = { editando = null },
            onConfirmar = { hora ->
                faixa = if (inicio) faixa.copy(inicio = hora) else faixa.copy(fim = hora)
                editando = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorHora(inicial: LocalTime, onCancelar: () -> Unit, onConfirmar: (LocalTime) -> Unit) {
    val estado = rememberTimePickerState(inicial.hour, inicial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onCancelar,
        text = { TimePicker(estado) },
        confirmButton = { TextButton(onClick = { onConfirmar(LocalTime.of(estado.hour, estado.minute)) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoDiasSemana(inicial: DiasSemana?, onCancelar: () -> Unit, onConfirmar: (DiasSemana) -> Unit) {
    var dias by remember { mutableStateOf(inicial?.dias ?: (DayOfWeek.MONDAY..DayOfWeek.FRIDAY).toSet()) }
    DialogoTelaCheia(
        titulo = stringResource(R.string.restricao_dias),
        podeConfirmar = dias.isNotEmpty(),
        onCancelar = onCancelar,
        onConfirmar = { onConfirmar(DiasSemana(dias)) },
    ) {
        Secao(stringResource(R.string.dias_permitidos))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { dia ->
                FilterChip(
                    selected = dia in dias,
                    onClick = { dias = if (dia in dias) dias - dia else dias + dia },
                    label = { Text(nomeDia(dia)) },
                )
            }
        }
        Text(stringResource(R.string.dica_dias), style = MaterialTheme.typography.bodySmall)
    }
}

private operator fun DayOfWeek.rangeTo(fim: DayOfWeek) = DayOfWeek.entries.filter { it.value in value..fim.value }

@Composable
fun DialogoLimiteFrequencia(inicial: LimiteFrequencia?, onCancelar: () -> Unit, onConfirmar: (LimiteFrequencia) -> Unit) {
    var texto by remember { mutableStateOf((inicial?.minutos ?: LimiteFrequencia.MINUTOS_PADRAO).toString()) }
    val minutos = texto.toIntOrNull()
    DialogoTelaCheia(
        titulo = stringResource(R.string.restricao_limite),
        podeConfirmar = minutos != null && minutos >= 1,
        onCancelar = onCancelar,
        onConfirmar = { minutos?.let { onConfirmar(LimiteFrequencia(it)) } },
    ) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it.filter(Char::isDigit).take(4) },
            label = { Text(stringResource(R.string.minutos)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Text(stringResource(R.string.dica_limite), style = MaterialTheme.typography.bodySmall)
    }
}
