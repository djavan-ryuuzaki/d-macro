package br.com.djavanfernando.dmacro.data

import br.com.djavanfernando.dmacro.engine.FonteMacros
import br.com.djavanfernando.dmacro.engine.LimiteFrequencia
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.MacroJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MacroRepository(
    private val dao: MacroDao,
    private val agora: () -> Long = System::currentTimeMillis,
) : FonteMacros {

    fun observar(): Flow<List<Macro>> = dao.observar().map { lista -> lista.map { MacroJson.decodificar(it.json) } }

    suspend fun obter(id: String): Macro? = dao.obter(id)?.let { MacroJson.decodificar(it.json) }

    override suspend fun macrosAtivadas(): List<Macro> = dao.ativadas().map { MacroJson.decodificar(it.json) }

    /** Cria e grava uma Macro nova, já com o Limite de frequência padrão. */
    suspend fun criar(nome: String): Macro {
        val macro = Macro(id = UUID.randomUUID().toString(), nome = nome, restricoes = listOf(LimiteFrequencia()))
        salvar(macro)
        return macro
    }

    suspend fun salvar(macro: Macro) {
        val criadaEm = dao.obter(macro.id)?.criadaEm ?: agora()
        dao.salvar(MacroEntity(macro.id, macro.nome, macro.ativada, MacroJson.codificar(macro), criadaEm))
    }

    suspend fun excluir(id: String) = dao.excluir(id)
}
