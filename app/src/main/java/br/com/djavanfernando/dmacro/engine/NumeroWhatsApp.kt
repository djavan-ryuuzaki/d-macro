package br.com.djavanfernando.dmacro.engine

/** DDI acrescentado a números digitados só com DDD. */
const val DDI_PADRAO = "55"

/**
 * Converte um número para o formato que o WhatsApp exige: só dígitos, com DDI.
 * Devolve `null` quando não dá para saber o DDI (por exemplo, número sem DDD).
 */
fun numeroWhatsApp(numero: String): String? {
    val limpo = numero.trim()
    val digitos = limpo.filter(Char::isDigit)
    if (limpo.startsWith("+")) return digitos.takeIf { it.length in 8..15 }
    val semZeros = digitos.trimStart('0')
    return when {
        semZeros.length in 10..11 -> DDI_PADRAO + semZeros
        semZeros.length in 12..13 && semZeros.startsWith(DDI_PADRAO) -> semZeros
        else -> null
    }
}
