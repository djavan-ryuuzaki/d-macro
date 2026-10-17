package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp

/** Um nó da árvore de acessibilidade, já sem dependência do Android. [indice] identifica o nó na última leitura. */
data class NoTela(
    val indice: Int,
    val idView: String?,
    val classe: String?,
    val texto: String?,
    val descricao: String?,
    val clicavel: Boolean,
)

sealed interface EstadoTela {
    /** Conversa aberta: [campo] é a caixa de digitação e [enviar], o botão Enviar. */
    data class ConversaPronta(val enviar: NoTela, val campo: NoTela) : EstadoTela

    /** O WhatsApp recusou o número. Sai tocando em [fechar] ou, se nulo, com Voltar. Nunca toca em mais nada. */
    data class NumeroSemWhatsApp(val fechar: NoTela?) : EstadoTela

    /** Outra tela, ainda carregando ou desconhecida. */
    data object Aguardar : EstadoTela
}

/**
 * Tudo o que o D-Macro sabe sobre a tela do WhatsApp. Quando uma atualização do WhatsApp quebrar o envio,
 * o conserto começa aqui. Conferido no WhatsApp 2.26.40.76 (pt-BR).
 */
object IdentificadoresWhatsApp {
    fun idEnviar(app: AppWhatsApp) = "${app.pacote}:id/send"
    fun idCampo(app: AppWhatsApp) = "${app.pacote}:id/entry"
    /** Tela "Enviar para…", que o WhatsApp abre quando o número do link não tem conta. */
    fun idSeletorContatos(app: AppWhatsApp) = "${app.pacote}:id/contact_picker_layout"

    const val CLASSE_CAMPO = "android.widget.EditText"
    val descricoesEnviar = setOf("Enviar", "Send")
    val titulosSeletorContatos = setOf("Enviar para…", "Send to…")
    val avisosSemWhatsApp = listOf("não está no WhatsApp", "isn't on WhatsApp", "is not on WhatsApp")
    /**
     * Únicos botões que o D-Macro toca para sair do aviso de número recusado. Nunca incluir botões que
     * fazem algo (como "Enviar convite por SMS", do painel do WhatsApp 2.26).
     */
    val botoesFechar = setOf("AGORA NÃO", "NOT NOW", "CANCELAR", "CANCEL", "OK")
}

fun reconhecer(nos: List<NoTela>, app: AppWhatsApp): EstadoTela {
    val ids = IdentificadoresWhatsApp

    if (nos.any { no -> ids.avisosSemWhatsApp.any { no.texto?.contains(it, ignoreCase = true) == true } }) {
        val fechar = ids.botoesFechar.firstNotNullOfOrNull { rotulo ->
            nos.firstOrNull { it.clicavel && it.texto?.trim()?.uppercase() == rotulo }
        }
        return EstadoTela.NumeroSemWhatsApp(fechar)
    }
    if (nos.any { it.idView == ids.idSeletorContatos(app) || it.texto in ids.titulosSeletorContatos }) {
        return EstadoTela.NumeroSemWhatsApp(null)
    }

    val campo = campoDe(nos, app) ?: return EstadoTela.Aguardar
    val enviar = nos.firstOrNull { it.idView == ids.idEnviar(app) }
        ?: nos.firstOrNull { it.clicavel && it.descricao?.trim() in ids.descricoesEnviar }
        ?: return EstadoTela.Aguardar
    return EstadoTela.ConversaPronta(enviar, campo)
}

/** A caixa de digitação da conversa, se houver uma na tela. */
fun campoDe(nos: List<NoTela>, app: AppWhatsApp): NoTela? =
    nos.firstOrNull { it.idView == IdentificadoresWhatsApp.idCampo(app) }
        ?: nos.singleOrNull { it.classe == IdentificadoresWhatsApp.CLASSE_CAMPO }
