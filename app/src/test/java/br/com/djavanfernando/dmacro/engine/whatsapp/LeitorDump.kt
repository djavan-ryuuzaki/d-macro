package br.com.djavanfernando.dmacro.engine.whatsapp

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** Lê um `uiautomator dump` (fixture em `resources/whatsapp/`) na mesma ordem em que o serviço percorre a árvore. */
object LeitorDump {
    fun ler(fixture: String): List<NoTela> {
        val entrada = requireNotNull(javaClass.getResourceAsStream("/whatsapp/$fixture")) { "fixture ausente: $fixture" }
        val doc = entrada.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val nos = doc.getElementsByTagName("node")
        return (0 until nos.length).map { i ->
            val e = nos.item(i) as Element
            NoTela(
                indice = i,
                idView = e.getAttribute("resource-id").ifEmpty { null },
                classe = e.getAttribute("class").ifEmpty { null },
                texto = e.getAttribute("text").ifEmpty { null },
                descricao = e.getAttribute("content-desc").ifEmpty { null },
                clicavel = e.getAttribute("clickable") == "true",
            )
        }
    }
}
