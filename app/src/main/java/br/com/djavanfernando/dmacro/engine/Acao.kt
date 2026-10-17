package br.com.djavanfernando.dmacro.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cópia do número ou e-mail gravada no momento da escolha, não um vínculo com a agenda. */
@Serializable
sealed interface Destinatario {
    val valor: String
    val rotulo: String?
}

@Serializable
@SerialName("telefone")
data class Telefone(val numero: String, override val rotulo: String? = null) : Destinatario {
    override val valor get() = numero
}

@Serializable
@SerialName("email")
data class Email(val endereco: String, override val rotulo: String? = null) : Destinatario {
    override val valor get() = endereco
}

@Serializable
sealed interface Acao

@Serializable
@SerialName("efetuar_chamada")
data class EfetuarChamada(val destinatario: Telefone) : Acao

@Serializable
@SerialName("enviar_sms")
data class EnviarSms(
    val destinatarios: List<Telefone>,
    val mensagem: String,
    val limiteCaracteres: Int = LIMITE_SMS_PADRAO,
) : Acao {
    companion object {
        const val LIMITE_SMS_PADRAO = 300
    }
}

@Serializable
@SerialName("enviar_email")
data class EnviarEmail(
    val destinatarios: List<Email>,
    val assunto: String,
    val corpo: String,
) : Acao

/** Qual WhatsApp instalado envia a mensagem. */
@Serializable
enum class AppWhatsApp(val pacote: String) {
    @SerialName("pessoal") Pessoal("com.whatsapp"),
    @SerialName("business") Business("com.whatsapp.w4b"),
}

@Serializable
@SerialName("enviar_whatsapp")
data class EnviarWhatsApp(
    val destinatarios: List<Telefone>,
    val mensagem: String,
    val app: AppWhatsApp = AppWhatsApp.Pessoal,
) : Acao
