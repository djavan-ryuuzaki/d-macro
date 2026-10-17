package br.com.djavanfernando.dmacro.platform

import android.service.notification.NotificationListenerService
import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarWhatsApp
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.NotificacaoRecebida
import br.com.djavanfernando.dmacro.engine.Permissao
import br.com.djavanfernando.dmacro.engine.permissoesNecessarias

/** Fotografia das pré-condições para as Macros funcionarem. */
data class EstadoSaude(
    val acessoNotificacoes: Boolean,
    val listenerConectado: Boolean,
    val permissoes: Map<Permissao, Boolean>,
    val podePostarNotificacoes: Boolean,
    val bateriaIgnorada: Boolean,
    val contaEmail: Boolean,
    val whatsAppsInstalados: Set<AppWhatsApp>,
) {
    /** Problemas que impedem uma Macro específica de funcionar. */
    fun problemasDa(macro: Macro): List<ProblemaSaude> = buildList {
        if (macro.gatilhos.any { it is NotificacaoRecebida } && !acessoNotificacoes) add(ProblemaSaude.SemAcessoNotificacoes)
        permissoesNecessarias(macro).filter { permissoes[it] != true }.forEach { add(ProblemaSaude.SemPermissao(it)) }
        if (macro.acoes.any { it is EnviarEmail } && !contaEmail) add(ProblemaSaude.SemContaEmail)
        macro.acoes.filterIsInstance<EnviarWhatsApp>().map { it.app }.distinct()
            .filterNot { it in whatsAppsInstalados }.forEach { add(ProblemaSaude.SemWhatsApp(it)) }
    }

    /** Há algo crítico para o conjunto de Macros ativadas? */
    fun temProblemaCritico(ativadas: List<Macro>): Boolean =
        ativadas.any { problemasDa(it).isNotEmpty() } || (ativadas.isNotEmpty() && acessoNotificacoes && !listenerConectado)
}

sealed interface ProblemaSaude {
    data object SemAcessoNotificacoes : ProblemaSaude
    data class SemPermissao(val permissao: Permissao) : ProblemaSaude
    data object SemContaEmail : ProblemaSaude
    data class SemWhatsApp(val app: AppWhatsApp) : ProblemaSaude
}

class SaudeApp(
    private val context: android.content.Context,
    private val permissoes: PermissoesAndroid,
    private val contas: ContaEmailRepository,
) {

    fun ler(): EstadoSaude = EstadoSaude(
        acessoNotificacoes = permissoes.acessoNotificacoesConcedido(),
        listenerConectado = NotificacaoListener.conectado.value,
        permissoes = Permissao.entries.associateWith { permissoes.concedida(it) },
        podePostarNotificacoes = permissoes.podePostarNotificacoes(),
        bateriaIgnorada = permissoes.otimizacaoBateriaIgnorada(),
        contaEmail = contas.configurada.value,
        whatsAppsInstalados = whatsAppsInstalados(context),
    )

    /** Pede ao sistema para reconectar o listener quando o acesso existe mas a conexão caiu. */
    fun reconectarListenerSePreciso() {
        if (permissoes.acessoNotificacoesConcedido() && !NotificacaoListener.conectado.value) {
            NotificationListenerService.requestRebind(permissoes.componenteListener())
        }
    }
}
