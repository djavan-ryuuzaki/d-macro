package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Dados do momento do Disparo de que as Restrições precisam. */
data class ContextoRestricao(
    val instante: Instant,
    val zona: ZoneId,
    /** Início da última Execução (não-Teste) desta Macro, se houver. */
    val ultimaExecucao: Instant?,
)

@Serializable
sealed interface Restricao {
    fun permite(contexto: ContextoRestricao): Boolean
}

/** Faixa [inicio, fim). Se fim < inicio, a faixa passa da meia-noite. */
@Serializable
@SerialName("faixa_horario")
data class FaixaHorario(
    @Serializable(with = LocalTimeSerializer::class) val inicio: LocalTime,
    @Serializable(with = LocalTimeSerializer::class) val fim: LocalTime,
) : Restricao {
    override fun permite(contexto: ContextoRestricao): Boolean {
        val agora = contexto.instante.atZone(contexto.zona).toLocalTime()
        val depoisDoInicio = !agora.isBefore(inicio)
        val antesDoFim = agora.isBefore(fim)
        return if (fim.isBefore(inicio)) depoisDoInicio || antesDoFim else depoisDoInicio && antesDoFim
    }
}

@Serializable
@SerialName("dias_semana")
data class DiasSemana(val dias: Set<DayOfWeek>) : Restricao {
    override fun permite(contexto: ContextoRestricao): Boolean =
        contexto.instante.atZone(contexto.zona).dayOfWeek in dias
}

@Serializable
@SerialName("limite_frequencia")
data class LimiteFrequencia(val minutos: Int = MINUTOS_PADRAO) : Restricao {
    override fun permite(contexto: ContextoRestricao): Boolean {
        val ultima = contexto.ultimaExecucao ?: return true
        return !contexto.instante.isBefore(ultima.plus(Duration.ofMinutes(minutos.toLong())))
    }

    companion object {
        const val MINUTOS_PADRAO = 5
    }
}

/** Avalia as Restrições em ordem e devolve a primeira que barra o Disparo, ou null se todas passam. */
fun primeiraReprovada(restricoes: List<Restricao>, contexto: ContextoRestricao): Restricao? =
    restricoes.firstOrNull { !it.permite(contexto) }

object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString())
}
