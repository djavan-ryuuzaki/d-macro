package br.com.djavanfernando.dmacro.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.djavanfernando.dmacro.data.MacroRepository
import br.com.djavanfernando.dmacro.di.EscopoApp
import br.com.djavanfernando.dmacro.engine.Acao
import br.com.djavanfernando.dmacro.engine.Gatilho
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.Motor
import br.com.djavanfernando.dmacro.engine.Restricao
import br.com.djavanfernando.dmacro.engine.validar
import br.com.djavanfernando.dmacro.platform.CatalogoApps
import br.com.djavanfernando.dmacro.platform.ContaEmailRepository
import br.com.djavanfernando.dmacro.platform.PermissoesAndroid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Editor com gravação automática: toda alteração é salva na hora. */
@HiltViewModel
class EditorViewModel @Inject constructor(
    estado: SavedStateHandle,
    private val repo: MacroRepository,
    private val motor: Motor,
    private val catalogo: CatalogoApps,
    val permissoes: PermissoesAndroid,
    contas: ContaEmailRepository,
    @EscopoApp private val escopoApp: CoroutineScope,
) : ViewModel() {

    private val id: String = checkNotNull(estado["id"])
    private val _macro = MutableStateFlow<Macro?>(null)
    val macro: StateFlow<Macro?> = _macro.asStateFlow()
    val contaConfigurada = contas.configurada

    init {
        viewModelScope.launch { _macro.value = repo.obter(id) }
    }

    fun apps() = catalogo.listar()

    private fun alterar(transformar: (Macro) -> Macro) {
        _macro.update { atual ->
            atual?.let(transformar)?.let { nova ->
                // Uma Macro que deixou de ser válida é desativada para não falhar em silêncio.
                if (nova.ativada && validar(nova).isNotEmpty()) nova.copy(ativada = false) else nova
            }
        }
        // Cada gravação salva o estado mais recente, em fila, para que edições rápidas não se sobreponham.
        escopoApp.launch { travaGravacao.withLock { _macro.value?.let { repo.salvar(it) } } }
    }

    private val travaGravacao = Mutex()

    fun renomear(nome: String) = alterar { it.copy(nome = nome) }

    /** Ativa somente se não houver problemas; devolve false se não pôde ativar. */
    fun definirAtivada(ativada: Boolean): Boolean {
        val m = _macro.value ?: return false
        if (ativada && validar(m).isNotEmpty()) return false
        alterar { it.copy(ativada = ativada) }
        return true
    }

    fun salvarGatilho(indice: Int?, gatilho: Gatilho) = alterar { m -> m.copy(gatilhos = m.gatilhos.substituir(indice, gatilho)) }
    fun removerGatilho(indice: Int) = alterar { m -> m.copy(gatilhos = m.gatilhos.removerEm(indice)) }

    fun salvarAcao(indice: Int?, acao: Acao) = alterar { m -> m.copy(acoes = m.acoes.substituir(indice, acao)) }
    fun removerAcao(indice: Int) = alterar { m -> m.copy(acoes = m.acoes.removerEm(indice)) }
    fun moverAcao(indice: Int, delta: Int) = alterar { m ->
        val destino = indice + delta
        if (destino !in m.acoes.indices) m
        else m.copy(acoes = m.acoes.toMutableList().apply { add(destino, removeAt(indice)) })
    }

    fun salvarRestricao(indice: Int?, restricao: Restricao) =
        alterar { m -> m.copy(restricoes = m.restricoes.substituir(indice, restricao)) }
    fun removerRestricao(indice: Int) = alterar { m -> m.copy(restricoes = m.restricoes.removerEm(indice)) }

    fun testar() {
        val m = _macro.value ?: return
        escopoApp.launch { motor.testar(m) }
    }

    fun excluir(depois: () -> Unit) {
        escopoApp.launch { repo.excluir(id) }
        depois()
    }
}

private fun <T> List<T>.substituir(indice: Int?, item: T): List<T> =
    if (indice == null) this + item else toMutableList().apply { set(indice, item) }

private fun <T> List<T>.removerEm(indice: Int): List<T> = filterIndexed { i, _ -> i != indice }
