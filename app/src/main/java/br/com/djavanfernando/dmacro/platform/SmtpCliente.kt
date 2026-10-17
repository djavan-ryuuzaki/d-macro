package br.com.djavanfernando.dmacro.platform

import java.io.IOException
import java.util.Properties
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.SendFailedException
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

enum class Seguranca { Nenhuma, StartTls, Ssl }

/** Conta de e-mail do usuário (ADR 0001). */
data class ContaEmail(
    val servidor: String,
    val porta: Int,
    val seguranca: Seguranca,
    val usuario: String,
    val senha: String,
) {
    override fun toString() = "ContaEmail(servidor=$servidor, porta=$porta, seguranca=$seguranca, usuario=$usuario, senha=***)"
}

sealed interface ResultadoSmtp {
    data object Ok : ResultadoSmtp
    /** Credenciais recusadas: não adianta tentar de novo. */
    data class FalhaAutenticacao(val motivo: String) : ResultadoSmtp
    /** Rede ou servidor indisponível: vale tentar de novo. */
    data class FalhaTemporaria(val motivo: String) : ResultadoSmtp
    /** Endereço recusado ou outro erro definitivo. */
    data class FalhaPermanente(val motivo: String) : ResultadoSmtp
}

/** Envio SMTP síncrono (bloqueante): chame fora da main thread. */
class SmtpCliente(private val timeoutMs: Int = 30_000) {

    fun enviar(conta: ContaEmail, para: String, assunto: String, corpo: String): ResultadoSmtp {
        val props = Properties().apply {
            put("mail.transport.protocol", "smtp")
            put("mail.smtp.host", conta.servidor)
            put("mail.smtp.port", conta.porta.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", timeoutMs.toString())
            put("mail.smtp.timeout", timeoutMs.toString())
            put("mail.smtp.writetimeout", timeoutMs.toString())
            when (conta.seguranca) {
                Seguranca.Nenhuma -> Unit
                Seguranca.StartTls -> {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                }
                Seguranca.Ssl -> {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.ssl.checkserveridentity", "true")
                }
            }
        }
        return try {
            val session = Session.getInstance(props)
            val mensagem = MimeMessage(session).apply {
                setFrom(InternetAddress(conta.usuario))
                setRecipient(Message.RecipientType.TO, InternetAddress(para, true))
                setSubject(assunto, "UTF-8")
                setText(corpo, "UTF-8")
            }
            session.getTransport("smtp").use { transporte ->
                transporte.connect(conta.servidor, conta.porta, conta.usuario, conta.senha)
                transporte.sendMessage(mensagem, mensagem.allRecipients)
            }
            ResultadoSmtp.Ok
        } catch (e: AuthenticationFailedException) {
            ResultadoSmtp.FalhaAutenticacao(e.message ?: "autenticação recusada")
        } catch (e: SendFailedException) {
            ResultadoSmtp.FalhaPermanente(e.message ?: "endereço recusado")
        } catch (e: MessagingException) {
            if (e.temCausaDeRede()) ResultadoSmtp.FalhaTemporaria(e.message ?: "falha de conexão")
            else ResultadoSmtp.FalhaPermanente(e.message ?: "erro SMTP")
        }
    }

    private fun Throwable.temCausaDeRede(): Boolean =
        generateSequence(this) { it.cause ?: (it as? MessagingException)?.nextException }
            .take(10)
            .any { it is IOException }
}
