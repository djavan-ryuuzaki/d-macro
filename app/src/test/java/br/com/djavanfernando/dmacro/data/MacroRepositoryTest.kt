package br.com.djavanfernando.dmacro.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.djavanfernando.dmacro.engine.DiasSemana
import br.com.djavanfernando.dmacro.engine.EfetuarChamada
import br.com.djavanfernando.dmacro.engine.Email
import br.com.djavanfernando.dmacro.engine.EnviarEmail
import br.com.djavanfernando.dmacro.engine.EnviarSms
import br.com.djavanfernando.dmacro.engine.FaixaHorario
import br.com.djavanfernando.dmacro.engine.FiltroTexto
import br.com.djavanfernando.dmacro.engine.LimiteFrequencia
import br.com.djavanfernando.dmacro.engine.Macro
import br.com.djavanfernando.dmacro.engine.ModoFiltro
import br.com.djavanfernando.dmacro.engine.NotificacaoRecebida
import br.com.djavanfernando.dmacro.engine.SelecaoApps
import br.com.djavanfernando.dmacro.engine.Telefone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class MacroRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DMacroDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repo = MacroRepository(db.macroDao())

    @After fun fechar() = db.close()

    @Test
    fun `macro com todos os tipos e salva e lida sem perda`() = runTest {
        val macro = Macro(
            id = "m1", nome = "Alarme", ativada = true,
            gatilhos = listOf(
                NotificacaoRecebida(
                    apps = SelecaoApps.Somente(setOf("com.alarme")),
                    filtroTitulo = FiltroTexto(ModoFiltro.Contem, listOf("alarme")),
                ),
            ),
            acoes = listOf(
                EfetuarChamada(Telefone("999", "Ana")),
                EnviarSms(listOf(Telefone("111")), "{texto}"),
                EnviarEmail(listOf(Email("a@b.com")), "Assunto", "Corpo"),
            ),
            restricoes = listOf(
                FaixaHorario(LocalTime.of(22, 0), LocalTime.of(6, 0)),
                DiasSemana(setOf(DayOfWeek.MONDAY)),
                LimiteFrequencia(5),
            ),
        )

        repo.salvar(macro)

        assertEquals(macro, repo.obter("m1"))
        assertEquals(listOf(macro), repo.observar().first())
    }

    @Test
    fun `nova macro ja vem com limite de frequencia de 5 minutos e desativada`() = runTest {
        val macro = repo.criar("Minha macro")

        assertEquals(listOf(LimiteFrequencia(5)), macro.restricoes)
        assertEquals(false, macro.ativada)
        assertEquals(macro, repo.obter(macro.id))
    }

    @Test
    fun `somente macros ativadas sao fornecidas ao motor`() = runTest {
        repo.salvar(Macro("a", "A", ativada = true))
        repo.salvar(Macro("b", "B", ativada = false))

        assertEquals(listOf("a"), repo.macrosAtivadas().map { it.id })
    }

    @Test
    fun `excluir remove a macro`() = runTest {
        repo.salvar(Macro("a", "A"))
        repo.excluir("a")
        assertEquals(null, repo.obter("a"))
    }
}
