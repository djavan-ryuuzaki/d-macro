package br.com.djavanfernando.dmacro.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.djavanfernando.dmacro.R
import br.com.djavanfernando.dmacro.ui.configuracoes.ConfiguracoesScreen
import br.com.djavanfernando.dmacro.ui.editor.EditorScreen
import br.com.djavanfernando.dmacro.ui.macros.MacrosScreen
import br.com.djavanfernando.dmacro.ui.registros.RegistroScreen
import br.com.djavanfernando.dmacro.ui.registros.RegistrosScreen
import br.com.djavanfernando.dmacro.ui.saude.SaudeScreen

enum class Aba(val rota: String, @StringRes val rotulo: Int, val icone: ImageVector) {
    Macros("macros", R.string.aba_macros, Icons.Filled.PlayArrow),
    Registros("registros", R.string.aba_registros, Icons.AutoMirrored.Filled.List),
    Saude("saude", R.string.aba_saude, Icons.Filled.FavoriteBorder),
    Configuracoes("configuracoes", R.string.aba_configuracoes, Icons.Filled.Settings),
}

private fun NavHostController.irParaAba(aba: Aba) = navigate(aba.rota) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun DMacroNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val rotaAtual = entry?.destination?.route
    val emAba = Aba.entries.any { it.rota == rotaAtual }

    Scaffold(
        bottomBar = {
            if (emAba) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = { nav.irParaAba(aba) },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(stringResource(aba.rotulo)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Aba.Macros.rota, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Aba.Macros.rota) {
                MacrosScreen(onAbrirMacro = { nav.navigate("editor/$it") }, onAbrirSaude = { nav.irParaAba(Aba.Saude) })
            }
            composable(Aba.Registros.rota) { RegistrosScreen(onAbrir = { nav.navigate("registro/$it") }) }
            composable(Aba.Saude.rota) { SaudeScreen(onAbrirConfiguracoes = { nav.irParaAba(Aba.Configuracoes) }) }
            composable(Aba.Configuracoes.rota) { ConfiguracoesScreen() }
            composable("editor/{id}") { EditorScreen(onVoltar = { nav.popBackStack() }) }
            composable("registro/{id}") { RegistroScreen(onVoltar = { nav.popBackStack() }) }
        }
    }
}
