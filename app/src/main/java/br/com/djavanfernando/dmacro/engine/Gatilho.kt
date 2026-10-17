package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/** Notificação publicada por outro app, já extraída da plataforma. */
data class EventoNotificacao(
    val pacote: String,
    val nomeApp: String,
    val titulo: String,
    val texto: String,
    val chave: String,
    val emCurso: Boolean,
    val resumoDeGrupo: Boolean,
    val instante: Instant,
)

@Serializable
sealed interface SelecaoApps {
    fun inclui(pacote: String): Boolean

    @Serializable
    @SerialName("todos")
    data object Todos : SelecaoApps {
        override fun inclui(pacote: String) = true
    }

    @Serializable
    @SerialName("somente")
    data class Somente(val pacotes: Set<String>) : SelecaoApps {
        override fun inclui(pacote: String) = pacote in pacotes
    }
}

@Serializable
sealed interface Gatilho

@Serializable
@SerialName("notificacao_recebida")
data class NotificacaoRecebida(
    val apps: SelecaoApps = SelecaoApps.Todos,
    val filtroTitulo: FiltroTexto = FiltroTexto(),
    val filtroTexto: FiltroTexto = FiltroTexto(),
    val ignorarMaiusculas: Boolean = true,
    val ignorarEmCurso: Boolean = true,
    val impedirMultiplosDisparos: Boolean = true,
) : Gatilho {

    /** Avalia os Filtros. "Impedir múltiplos disparos" é estado e fica no Motor. */
    fun satisfeitoPor(evento: EventoNotificacao): Boolean =
        apps.inclui(evento.pacote) &&
            !(ignorarEmCurso && evento.emCurso) &&
            filtroTitulo.aceita(evento.titulo, ignorarMaiusculas) &&
            filtroTexto.aceita(evento.texto, ignorarMaiusculas)
}
