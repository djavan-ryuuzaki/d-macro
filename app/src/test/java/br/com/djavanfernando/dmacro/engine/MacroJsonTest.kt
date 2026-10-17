package br.com.djavanfernando.dmacro.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class MacroJsonTest {

    @Test
    fun `macro com todos os tipos sobrevive a ida e volta em JSON`() {
        val macro = Macro(
            id = "a1b2",
            nome = "Alarme da casa",
            ativada = true,
            gatilhos = listOf(
                NotificacaoRecebida(
                    apps = SelecaoApps.Somente(setOf("com.alarme")),
                    filtroTitulo = FiltroTexto(ModoFiltro.Igual, listOf("Alarme")),
                    filtroTexto = FiltroTexto(ModoFiltro.NaoContem, listOf("teste", "simulação")),
                    ignorarMaiusculas = false,
                    ignorarEmCurso = false,
                    impedirMultiplosDisparos = false,
                ),
                NotificacaoRecebida(),
            ),
            acoes = listOf(
                EfetuarChamada(Telefone("+5511999990000", "Ana")),
                EnviarSms(listOf(Telefone("11988887777"), Telefone("11977776666", "Bia")), "{titulo}: {texto}", 160),
                EnviarEmail(listOf(Email("ana@exemplo.com", "Ana")), "Alerta {app}", "Recebido às {hora}"),
                EnviarWhatsApp(listOf(Telefone("11966665555", "Caio")), "{titulo}", AppWhatsApp.Business),
                EnviarWhatsApp(listOf(Telefone("+5511955554444")), "Oi"),
            ),
            restricoes = listOf(
                FaixaHorario(LocalTime.of(22, 0), LocalTime.of(6, 30)),
                DiasSemana(setOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)),
                LimiteFrequencia(10),
            ),
        )

        assertEquals(macro, MacroJson.decodificar(MacroJson.codificar(macro)))
    }
}
