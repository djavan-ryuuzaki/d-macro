package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.json.Json

/** Formato de armazenamento (e da futura exportação) das Macros. */
object MacroJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "tipo"
    }

    fun codificar(macro: Macro): String = json.encodeToString(Macro.serializer(), macro)

    fun decodificar(texto: String): Macro = json.decodeFromString(Macro.serializer(), texto)
}
