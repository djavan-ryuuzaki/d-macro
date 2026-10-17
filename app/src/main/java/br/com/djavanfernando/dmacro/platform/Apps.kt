package br.com.djavanfernando.dmacro.platform

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.edit

/** Pacotes que já publicaram notificações desde que o listener foi ligado. */
class PacotesVistos(context: Context) {
    private val prefs = context.getSharedPreferences("pacotes_vistos", Context.MODE_PRIVATE)

    fun registrar(pacote: String) {
        val atuais = todos()
        if (pacote !in atuais) prefs.edit { putStringSet(CHAVE, atuais + pacote) }
    }

    fun todos(): Set<String> = prefs.getStringSet(CHAVE, emptySet()).orEmpty()

    private companion object { const val CHAVE = "pacotes" }
}

data class AppInstalado(val pacote: String, val nome: String, val icone: Drawable?)

/**
 * Apps oferecidos no Filtro de apps: os que têm ícone na tela inicial (via <queries>, sem QUERY_ALL_PACKAGES)
 * mais os que já notificaram (ADR 0002).
 */
class CatalogoApps(private val context: Context, private val vistos: PacotesVistos) {

    fun listar(): List<AppInstalado> {
        val pm = context.packageManager
        val launcher = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
        return (launcher + vistos.todos())
            .distinct()
            .filter { it != context.packageName }
            .map { pacote ->
                AppInstalado(
                    pacote = pacote,
                    nome = NotificacaoListener.rotuloDoApp(context, pacote),
                    icone = runCatching { pm.getApplicationIcon(pacote) }.getOrNull(),
                )
            }
            .sortedBy { it.nome.lowercase() }
    }
}
