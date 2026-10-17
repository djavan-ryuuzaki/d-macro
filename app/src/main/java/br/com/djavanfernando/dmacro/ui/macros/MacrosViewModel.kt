package br.com.djavanfernando.dmacro.ui.macros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.djavanfernando.dmacro.data.ConfiguracoesRepository
import br.com.djavanfernando.dmacro.data.MacroRepository
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.platform.EstadoSaude
import br.com.djavanfernando.dmacro.platform.SaudeApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MacrosViewModel @Inject constructor(
    private val repo: MacroRepository,
    private val configuracoes: ConfiguracoesRepository,
    private val saudeApp: SaudeApp,
) : ViewModel() {

    val macros: StateFlow<List<Macro>> = repo.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val chaveGeral: StateFlow<Boolean> = configuracoes.chaveGeral.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _saude = MutableStateFlow<EstadoSaude?>(null)
    val saude: StateFlow<EstadoSaude?> = _saude.asStateFlow()

    fun atualizarSaude() {
        saudeApp.reconectarListenerSePreciso()
        _saude.value = saudeApp.ler()
    }

    fun definirChaveGeral(ligada: Boolean) {
        viewModelScope.launch { configuracoes.definirChaveGeral(ligada) }
    }

    fun definirAtivada(macro: Macro, ativada: Boolean) {
        viewModelScope.launch { repo.salvar(macro.copy(ativada = ativada)) }
    }

    fun criar(nome: String, abrir: (String) -> Unit) {
        viewModelScope.launch { abrir(repo.criar(nome).id) }
    }
}
