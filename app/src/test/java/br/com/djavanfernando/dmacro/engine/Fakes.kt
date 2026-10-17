package br.com.djavanfernando.dmacro.engine

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class RelogioDeTeste(var agora: Instant, private val zona: ZoneId = ZoneId.of("America/Sao_Paulo")) : Clock() {
    override fun getZone(): ZoneId = zona
    override fun withZone(zone: ZoneId): Clock = RelogioDeTeste(agora, zone)
    override fun instant(): Instant = agora
    fun avancar(duracao: Duration) { agora = agora.plus(duracao) }
}

class SmsFake : SmsSender {
    data class Enviado(val numero: String, val texto: String)
    val enviados = mutableListOf<Enviado>()
    /** Números que falham; o valor é quantas vezes ainda vão falhar. */
    val falhas = mutableMapOf<String, Int>()
    /** Simula o tempo de espera do sentIntent. */
    var atrasoMs = 0L

    override suspend fun enviar(numero: String, texto: String): ResultadoAcao {
        if (atrasoMs > 0) kotlinx.coroutines.delay(atrasoMs)
        val restantes = falhas[numero] ?: 0
        if (restantes > 0) {
            falhas[numero] = restantes - 1
            return ResultadoAcao.Erro("sem sinal")
        }
        enviados += Enviado(numero, texto)
        return ResultadoAcao.Ok
    }
}

class ChamadaFake : CallPlacer {
    val chamados = mutableListOf<String>()
    override suspend fun ligar(numero: String): ResultadoAcao {
        chamados += numero
        return ResultadoAcao.Ok
    }
}

class EmailFake : EmailSender {
    data class Enfileirado(val idItem: String, val endereco: String, val assunto: String, val corpo: String)
    val enfileirados = mutableListOf<Enfileirado>()
    override suspend fun enfileirar(idItem: String, endereco: String, assunto: String, corpo: String, prazo: Instant): ResultadoAcao {
        enfileirados += Enfileirado(idItem, endereco, assunto, corpo)
        return ResultadoAcao.Pendente
    }
}

class WhatsAppFake : WhatsAppSender {
    data class Enfileirado(val idItem: String, val numero: String, val texto: String, val app: AppWhatsApp, val prazo: Instant)
    val enfileirados = mutableListOf<Enfileirado>()
    override suspend fun enfileirar(idItem: String, numero: String, texto: String, app: AppWhatsApp, prazo: Instant): ResultadoAcao {
        enfileirados += Enfileirado(idItem, numero, texto, app, prazo)
        return ResultadoAcao.Pendente
    }
}

class LogFake : ExecutionLog {
    val registros = mutableListOf<RegistroExecucao>()
    override suspend fun registrar(registro: RegistroExecucao) { registros += registro }
    override suspend fun atualizarItem(idItem: String, resultado: ResultadoAcao) {
        val i = registros.indexOfFirst { r -> r.itens.any { it.id == idItem } }
        val r = registros[i]
        registros[i] = r.copy(itens = r.itens.map { if (it.id == idItem) it.copy(resultado = resultado) else it })
    }
    override suspend fun ultimaExecucao(macroId: String): Instant? =
        registros.filter { it.macroId == macroId && !it.teste && it.desfecho == Desfecho.Executada }.maxOfOrNull { it.instante }
}

class TetoFake : ContadorTeto {
    val usos = mutableListOf<Pair<TipoTeto, Instant>>()
    override suspend fun usosDesde(tipo: TipoTeto, desde: Instant) = usos.count { it.first == tipo && it.second > desde }
    override suspend fun registrarUso(tipo: TipoTeto, instante: Instant) { usos += tipo to instante }
}

class MacrosFake(var macros: List<Macro> = emptyList()) : FonteMacros {
    override suspend fun macrosAtivadas() = macros.filter { it.ativada }
}

class ConfiguracoesFake(var chaveGeral: Boolean = true) : ConfiguracoesMotor {
    override suspend fun chaveGeralLigada() = chaveGeral
}

class PermissoesFake(val negadas: MutableSet<Permissao> = mutableSetOf()) : VerificadorPermissoes {
    override fun concedida(permissao: Permissao) = permissao !in negadas
}

class AvisosFake : Avisos {
    val tetosAtingidos = mutableListOf<TipoTeto>()
    override fun tetoAtingido(tipo: TipoTeto) { tetosAtingidos += tipo }
}
