package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.Serializable

@Serializable
data class Macro(
    val id: String,
    val nome: String,
    val ativada: Boolean = false,
    val gatilhos: List<Gatilho> = emptyList(),
    val acoes: List<Acao> = emptyList(),
    val restricoes: List<Restricao> = emptyList(),
)
