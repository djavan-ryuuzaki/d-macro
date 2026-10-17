package br.com.djavanfernando.dmacro.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Predefinições de servidor; o usuário só informa usuário e senha de app. */
enum class PresetEmail(val rotulo: String, val servidor: String, val porta: Int, val seguranca: Seguranca) {
    Gmail("Gmail", "smtp.gmail.com", 587, Seguranca.StartTls),
    Outlook("Outlook", "smtp-mail.outlook.com", 587, Seguranca.StartTls),
    Personalizado("Personalizado", "", 587, Seguranca.StartTls),
}

/**
 * Conta de e-mail guardada em armazenamento criptografado (chave no Android Keystore).
 * A senha nunca é exposta à UI depois de salva.
 */
class ContaEmailRepository(context: Context) {

    @Suppress("DEPRECATION")
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "conta_email",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _configurada = MutableStateFlow(obter() != null)
    val configurada: StateFlow<Boolean> = _configurada.asStateFlow()

    fun obter(): ContaEmail? {
        val servidor = prefs.getString(SERVIDOR, null) ?: return null
        return ContaEmail(
            servidor = servidor,
            porta = prefs.getInt(PORTA, 587),
            seguranca = Seguranca.valueOf(prefs.getString(SEGURANCA, Seguranca.StartTls.name)!!),
            usuario = prefs.getString(USUARIO, "")!!,
            senha = prefs.getString(SENHA, "")!!,
        )
    }

    /** Dados exibíveis da conta (sem senha). */
    fun obterSemSenha(): ContaEmail? = obter()?.copy(senha = "")

    /** Salva a conta. Senha vazia mantém a senha já salva. */
    fun salvar(conta: ContaEmail) {
        prefs.edit {
            putString(SERVIDOR, conta.servidor.trim())
            putInt(PORTA, conta.porta)
            putString(SEGURANCA, conta.seguranca.name)
            putString(USUARIO, conta.usuario.trim())
            if (conta.senha.isNotEmpty()) putString(SENHA, conta.senha)
        }
        _configurada.value = true
    }

    fun remover() {
        prefs.edit { clear() }
        _configurada.value = false
    }

    private companion object {
        const val SERVIDOR = "servidor"
        const val PORTA = "porta"
        const val SEGURANCA = "seguranca"
        const val USUARIO = "usuario"
        const val SENHA = "senha"
    }
}
