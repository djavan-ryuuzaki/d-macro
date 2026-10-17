package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed interface ResultadoAcao {
    @Serializable @SerialName("ok")
    data object Ok : ResultadoAcao

    @Serializable @SerialName("pendente")
    data object Pendente : ResultadoAcao

    @Serializable @SerialName("erro")
    data class Erro(val motivo: String) : ResultadoAcao

    @Serializable @SerialName("bloqueado_teto")
    data object BloqueadoPorTeto : ResultadoAcao
}

@Serializable
sealed interface Desfecho {
    @Serializable @SerialName("executada")
    data object Executada : Desfecho

    @Serializable @SerialName("barrada")
    data class BarradaPorRestricao(val restricao: Restricao) : Desfecho

    @Serializable @SerialName("bloqueada_permissao")
    data class BloqueadaPorPermissao(val faltando: Set<Permissao>) : Desfecho
}

enum class TipoAcao { Chamada, Sms, Email, WhatsApp }

/** Notificação que causou o Disparo (ausente em Testes). */
data class OrigemDisparo(val pacote: String, val nomeApp: String, val titulo: String, val texto: String)

/** Resultado de uma Ação para um Destinatário. */
data class ItemRegistro(
    val id: String,
    val indiceAcao: Int,
    val tipo: TipoAcao,
    val destinatario: String,
    val rotulo: String?,
    val resultado: ResultadoAcao,
)

data class RegistroExecucao(
    val id: String,
    val macroId: String,
    val macroNome: String,
    val instante: Instant,
    val teste: Boolean,
    val origem: OrigemDisparo?,
    val desfecho: Desfecho,
    val itens: List<ItemRegistro>,
)
