package br.com.djavanfernando.dmacro.engine

import java.time.Instant

/** Envia um SMS e suspende até saber o resultado do envio. */
interface SmsSender {
    suspend fun enviar(numero: String, texto: String): ResultadoAcao
}

/** Inicia uma chamada; não espera ela terminar. */
interface CallPlacer {
    suspend fun ligar(numero: String): ResultadoAcao
}

/**
 * Enfileira um e-mail. Normalmente devolve [ResultadoAcao.Pendente] e depois informa o resultado
 * por [ExecutionLog.atualizarItem] usando [idItem]. Desiste se não conseguir enviar até [prazo].
 */
interface EmailSender {
    suspend fun enfileirar(idItem: String, endereco: String, assunto: String, corpo: String, prazo: Instant): ResultadoAcao
}

/**
 * Põe uma mensagem na Fila do WhatsApp. Normalmente devolve [ResultadoAcao.Pendente] e depois informa o
 * resultado por [ExecutionLog.atualizarItem] usando [idItem]. [numero] já vem no formato internacional.
 */
interface WhatsAppSender {
    suspend fun enfileirar(idItem: String, numero: String, texto: String, app: AppWhatsApp, prazo: Instant): ResultadoAcao
}

interface ExecutionLog {
    suspend fun registrar(registro: RegistroExecucao)
    suspend fun atualizarItem(idItem: String, resultado: ResultadoAcao)
    /** Instante da última Execução que não foi Teste, para o Limite de frequência. */
    suspend fun ultimaExecucao(macroId: String): Instant?
}

enum class TipoTeto(val maximoPorHora: Int) { Sms(20), Chamada(5), WhatsApp(20) }

/** Uso persistido do Teto global. */
interface ContadorTeto {
    suspend fun usosDesde(tipo: TipoTeto, desde: Instant): Int
    suspend fun registrarUso(tipo: TipoTeto, instante: Instant)
}

interface FonteMacros {
    suspend fun macrosAtivadas(): List<Macro>
}

interface ConfiguracoesMotor {
    suspend fun chaveGeralLigada(): Boolean
}

/** Pré-condições de plataforma; [Acessibilidade] é o serviço de acessibilidade do D-Macro ligado. */
enum class Permissao { Sms, Telefone, Acessibilidade }

interface VerificadorPermissoes {
    fun concedida(permissao: Permissao): Boolean
}

/** Avisos ao usuário fora do fluxo de Execução. */
interface Avisos {
    fun tetoAtingido(tipo: TipoTeto)
}
