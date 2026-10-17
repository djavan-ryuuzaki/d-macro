package br.com.djavanfernando.dmacro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.djavanfernando.dmacro.engine.Desfecho
import br.com.djavanfernando.dmacro.engine.FaixaHorario
import br.com.djavanfernando.dmacro.engine.ItemRegistro
import br.com.djavanfernando.dmacro.engine.OrigemDisparo
import br.com.djavanfernando.dmacro.engine.Permissao
import br.com.djavanfernando.dmacro.engine.RegistroExecucao
import br.com.djavanfernando.dmacro.engine.ResultadoAcao
import br.com.djavanfernando.dmacro.engine.TipoAcao
import br.com.djavanfernando.dmacro.engine.TipoTeto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class RegistroRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var agora = Instant.parse("2026-10-12T12:00:00Z")
    private var db = Room.inMemoryDatabaseBuilder(context, DMacroDatabase::class.java).allowMainThreadQueries().build()
    private val log get() = RegistroRepository(db.execucaoDao()) { agora }

    @After fun fechar() = db.close()

    private fun registro(
        id: String,
        instante: Instant = agora,
        teste: Boolean = false,
        desfecho: Desfecho = Desfecho.Executada,
        itens: List<ItemRegistro> = emptyList(),
    ) = RegistroExecucao(
        id = id, macroId = "m1", macroNome = "Alarme", instante = instante, teste = teste,
        origem = if (teste) null else OrigemDisparo("com.alarme", "Alarme Casa", "Alarme", "Sensor"),
        desfecho = desfecho, itens = itens,
    )

    @Test
    fun `registro com itens e lido de volta igual e itens podem ser atualizados`() = runTest {
        val r = registro(
            "r1",
            itens = listOf(
                ItemRegistro("i1", 0, TipoAcao.Sms, "111", "Ana", ResultadoAcao.Ok),
                ItemRegistro("i2", 1, TipoAcao.Email, "a@b.com", null, ResultadoAcao.Pendente),
            ),
        )
        log.registrar(r)
        assertEquals(listOf(r), log.observar().first())

        log.atualizarItem("i2", ResultadoAcao.Erro("timeout"))
        assertEquals(ResultadoAcao.Erro("timeout"), log.observar().first().single().itens[1].resultado)
    }

    @Test
    fun `falhar se pendente so altera itens que ainda estao pendentes`() = runTest {
        log.registrar(
            registro(
                "r1",
                itens = listOf(
                    ItemRegistro("pendente", 0, TipoAcao.Email, "a@b.com", null, ResultadoAcao.Pendente),
                    ItemRegistro("enviado", 0, TipoAcao.Email, "c@d.com", null, ResultadoAcao.Ok),
                ),
            ),
        )

        log.falharSePendente("pendente", "prazo esgotado")
        log.falharSePendente("enviado", "prazo esgotado")

        assertEquals(
            listOf(ResultadoAcao.Erro("prazo esgotado"), ResultadoAcao.Ok),
            log.observar().first().single().itens.map { it.resultado },
        )
    }

    @Test
    fun `desfechos barrado e bloqueado sobrevivem a ida e volta`() = runTest {
        val barrado = registro("r1", desfecho = Desfecho.BarradaPorRestricao(FaixaHorario(LocalTime.of(8, 0), LocalTime.of(9, 0))))
        val bloqueado = registro("r2", instante = agora.plusSeconds(1), desfecho = Desfecho.BloqueadaPorPermissao(setOf(Permissao.Sms)))
        log.registrar(barrado)
        log.registrar(bloqueado)
        assertEquals(listOf(bloqueado, barrado), log.observar().first())
    }

    @Test
    fun `ultima execucao ignora testes e disparos que nao executaram`() = runTest {
        assertNull(log.ultimaExecucao("m1"))
        log.registrar(registro("r1", instante = agora))
        log.registrar(registro("r2", instante = agora.plusSeconds(60), teste = true))
        log.registrar(registro("r3", instante = agora.plusSeconds(120), desfecho = Desfecho.BloqueadaPorPermissao(setOf(Permissao.Sms))))
        assertEquals(agora, log.ultimaExecucao("m1"))
    }

    @Test
    fun `retencao mantem as ultimas 500 entradas`() = runTest {
        repeat(501) { i -> log.registrar(registro("r$i", instante = agora.plusSeconds(i.toLong()))) }
        val ids = log.observar().first().map { it.id }
        assertEquals(500, ids.size)
        assertEquals(false, "r0" in ids)
    }

    @Test
    fun `retencao remove entradas com mais de 7 dias`() = runTest {
        log.registrar(registro("antigo", instante = agora))
        agora = agora.plus(Duration.ofDays(7)).plusSeconds(1)
        log.registrar(registro("novo", instante = agora))
        assertEquals(listOf("novo"), log.observar().first().map { it.id })
    }

    @Test
    fun `limpar apaga todos os registros`() = runTest {
        log.registrar(registro("r1"))
        log.limpar()
        assertEquals(emptyList<RegistroExecucao>(), log.observar().first())
    }

    @Test
    fun `teto global e limite de frequencia continuam valendo depois de reabrir o banco`() = runTest {
        db.close()
        val arquivo = context.getDatabasePath("teste-reabrir.db").also { it.delete() }
        fun abrir() = Room.databaseBuilder(context, DMacroDatabase::class.java, arquivo.absolutePath).allowMainThreadQueries().build()

        db = abrir()
        TetoRepository(db.tetoDao()) { agora }.registrarUso(TipoTeto.Sms, agora)
        log.registrar(registro("r1"))
        db.close()

        db = abrir()
        assertEquals(1, TetoRepository(db.tetoDao()) { agora }.usosDesde(TipoTeto.Sms, agora.minusSeconds(3600)))
        assertEquals(0, TetoRepository(db.tetoDao()) { agora }.usosDesde(TipoTeto.Chamada, agora.minusSeconds(3600)))
        assertEquals(agora, log.ultimaExecucao("m1"))
    }
}
