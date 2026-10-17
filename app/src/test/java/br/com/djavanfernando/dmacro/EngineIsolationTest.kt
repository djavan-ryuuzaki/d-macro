package br.com.djavanfernando.dmacro

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EngineIsolationTest {

    @Test
    fun `motor nao depende do Android`() {
        val engineDir = File("src/main/java/br/com/djavanfernando/dmacro/engine")
        assertTrue("pasta do motor não encontrada: ${engineDir.absolutePath}", engineDir.isDirectory)

        val violations = engineDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> line.trim().matches(Regex("""import\s+(android|androidx)\..*""")) }
                    .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
            }
            .toList()

        assertTrue("O motor não pode importar Android:\n" + violations.joinToString("\n"), violations.isEmpty())
    }
}
