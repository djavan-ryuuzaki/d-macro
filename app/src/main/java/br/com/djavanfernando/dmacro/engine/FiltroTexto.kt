package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.Serializable

@Serializable
enum class ModoFiltro { Qualquer, Igual, Contem, NaoContem }

/** Filtro de um campo de texto da notificação (título ou texto). */
@Serializable
data class FiltroTexto(
    val modo: ModoFiltro = ModoFiltro.Qualquer,
    val termos: List<String> = emptyList(),
) {
    fun aceita(valor: String, ignorarMaiusculas: Boolean): Boolean = when (modo) {
        ModoFiltro.Qualquer -> true
        ModoFiltro.Igual -> termos.any { valor.equals(it, ignoreCase = ignorarMaiusculas) }
        ModoFiltro.Contem -> termos.any { valor.contains(it, ignoreCase = ignorarMaiusculas) }
        ModoFiltro.NaoContem -> termos.none { valor.contains(it, ignoreCase = ignorarMaiusculas) }
    }
}
