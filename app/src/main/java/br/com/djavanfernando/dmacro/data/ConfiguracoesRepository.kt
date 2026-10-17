package br.com.djavanfernando.dmacro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import br.com.djavanfernando.dmacro.engine.ConfiguracoesMotor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configuracoes")

class ConfiguracoesRepository(private val store: DataStore<Preferences>) : ConfiguracoesMotor {

    val chaveGeral: Flow<Boolean> = store.data.map { it[CHAVE_GERAL] ?: true }
    val servicoPrimeiroPlano: Flow<Boolean> = store.data.map { it[SERVICO_PRIMEIRO_PLANO] ?: false }

    override suspend fun chaveGeralLigada(): Boolean = chaveGeral.first()

    suspend fun definirChaveGeral(ligada: Boolean) {
        store.edit { it[CHAVE_GERAL] = ligada }
    }

    suspend fun definirServicoPrimeiroPlano(ligado: Boolean) {
        store.edit { it[SERVICO_PRIMEIRO_PLANO] = ligado }
    }

    private companion object {
        val CHAVE_GERAL = booleanPreferencesKey("chave_geral")
        val SERVICO_PRIMEIRO_PLANO = booleanPreferencesKey("servico_primeiro_plano")
    }
}
