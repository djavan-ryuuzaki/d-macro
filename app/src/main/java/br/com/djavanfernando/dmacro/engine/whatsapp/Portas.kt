package br.com.djavanfernando.dmacro.engine.whatsapp

import br.com.djavanfernando.dmacro.engine.AppWhatsApp
import java.time.Instant

interface EstadoAparelho {
    fun telaBloqueada(): Boolean
    fun chamadaEmAndamento(): Boolean
    /** Suspende até a tela estar desbloqueada e sem chamada em andamento. */
    suspend fun aguardarLiberado()
}

/** A tela do WhatsApp vista pelo serviço de acessibilidade. */
interface TelaWhatsApp {
    fun instalado(app: AppWhatsApp): Boolean
    /** O serviço de acessibilidade está ligado e conectado. */
    fun disponivel(): Boolean
    fun abrirConversa(app: AppWhatsApp, numero: String, texto: String)
    /** Árvore da janela ativa, vazia se não for do WhatsApp ou não puder ser lida. */
    fun ler(): List<NoTela>
    /** Toca num nó da última leitura. */
    fun tocar(no: NoTela)
    fun voltar()
    fun irParaInicio()
}

/** Garante que o item vire falha no fim do prazo, mesmo se o processo morrer antes. */
interface PrazoItem {
    fun agendar(idItem: String, prazo: Instant)
}
