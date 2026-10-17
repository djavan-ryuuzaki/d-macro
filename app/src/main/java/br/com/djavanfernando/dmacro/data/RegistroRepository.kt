package br.com.djavanfernando.dmacro.data

import br.com.djavanfernando.dmacro.engine.ContadorTeto
import br.com.djavanfernando.dmacro.engine.Desfecho
import br.com.djavanfernando.dmacro.engine.ExecutionLog
import br.com.djavanfernando.dmacro.engine.ItemRegistro
import br.com.djavanfernando.dmacro.engine.OrigemDisparo
import br.com.djavanfernando.dmacro.engine.RegistroExecucao
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.TipoAcao
import br.com.djavanfernando.dmacro.engine.TipoTeto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "tipo"
}

/** Registros de Execução persistidos, com retenção de 7 dias / 500 entradas aplicada a cada inserção. */
class RegistroRepository(
    private val dao: ExecucaoDao,
    private val agora: () -> Instant = Instant::now,
) : ExecutionLog {

    override suspend fun registrar(registro: RegistroExecucao) {
        dao.inserir(
            registro.paraEntidade(),
            registro.itens.mapIndexed { posicao, item -> item.paraEntidade(registro.id, posicao) },
            limiteInstante = agora().minus(RETENCAO).toEpochMilli(),
            maximo = MAXIMO_REGISTROS,
        )
    }

    override suspend fun atualizarItem(idItem: String, resultado: ResultadoAcao) =
        dao.atualizarItem(idItem, json.encodeToString(ResultadoAcao.serializer(), resultado))

    /** Marca o item como erro somente se ele ainda estiver pendente (ex.: prazo do e-mail esgotado). */
    suspend fun falharSePendente(idItem: String, motivo: String) {
        dao.substituirResultado(
            idItem,
            de = json.encodeToString(ResultadoAcao.serializer(), ResultadoAcao.Pendente),
            para = json.encodeToString(ResultadoAcao.serializer(), ResultadoAcao.Erro(motivo)),
        )
    }

    override suspend fun ultimaExecucao(macroId: String): Instant? =
        dao.ultimaExecucao(macroId)?.let(Instant::ofEpochMilli)

    fun observar(): Flow<List<RegistroExecucao>> = dao.observar().map { lista -> lista.map { it.paraDominio() } }

    fun observar(id: String): Flow<RegistroExecucao?> = dao.observar(id).map { it?.paraDominio() }

    suspend fun limpar() = dao.limpar()

    companion object {
        val RETENCAO: Duration = Duration.ofDays(7)
        const val MAXIMO_REGISTROS = 500
    }
}

/** Usos do Teto global; usos com mais de um dia são descartados. */
class TetoRepository(
    private val dao: TetoDao,
    private val agora: () -> Instant = Instant::now,
) : ContadorTeto {
    override suspend fun usosDesde(tipo: TipoTeto, desde: Instant): Int = dao.contar(tipo.name, desde.toEpochMilli())

    override suspend fun registrarUso(tipo: TipoTeto, instante: Instant) {
        dao.inserir(UsoTetoEntity(tipo = tipo.name, instante = instante.toEpochMilli()))
        dao.apagarAntesDe(agora().minus(Duration.ofDays(1)).toEpochMilli())
    }
}

private fun Desfecho.discriminador() = when (this) {
    Desfecho.Executada -> "executada"
    is Desfecho.BarradaPorRestricao -> "barrada"
    is Desfecho.BloqueadaPorPermissao -> "bloqueada_permissao"
}

private fun RegistroExecucao.paraEntidade() = ExecucaoEntity(
    id = id,
    macroId = macroId,
    macroNome = macroNome,
    instante = instante.toEpochMilli(),
    teste = teste,
    origemPacote = origem?.pacote,
    origemApp = origem?.nomeApp,
    origemTitulo = origem?.titulo,
    origemTexto = origem?.texto,
    tipoDesfecho = desfecho.discriminador(),
    desfechoJson = json.encodeToString(Desfecho.serializer(), desfecho),
)

private fun ItemRegistro.paraEntidade(execucaoId: String, posicao: Int) = ItemEntity(
    id = id,
    execucaoId = execucaoId,
    posicao = posicao,
    indiceAcao = indiceAcao,
    tipo = tipo.name,
    destinatario = destinatario,
    rotulo = rotulo,
    resultadoJson = json.encodeToString(ResultadoAcao.serializer(), resultado),
)

private fun ExecucaoComItens.paraDominio() = RegistroExecucao(
    id = execucao.id,
    macroId = execucao.macroId,
    macroNome = execucao.macroNome,
    instante = Instant.ofEpochMilli(execucao.instante),
    teste = execucao.teste,
    origem = execucao.origemPacote?.let {
        OrigemDisparo(it, execucao.origemApp.orEmpty(), execucao.origemTitulo.orEmpty(), execucao.origemTexto.orEmpty())
    },
    desfecho = json.decodeFromString(Desfecho.serializer(), execucao.desfechoJson),
    itens = itens.sortedBy { it.posicao }.map {
        ItemRegistro(
            id = it.id,
            indiceAcao = it.indiceAcao,
            tipo = TipoAcao.valueOf(it.tipo),
            destinatario = it.destinatario,
            rotulo = it.rotulo,
            resultado = json.decodeFromString(ResultadoAcao.serializer(), it.resultadoJson),
        )
    },
)
