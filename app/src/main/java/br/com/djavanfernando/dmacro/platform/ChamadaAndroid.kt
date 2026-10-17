package br.com.djavanfernando.dmacro.platform

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import br.com.djavanfernando.dmacro.engine.CallPlacer
import br.com.djavanfernando.dmacro.engine.ResultadoAcao

/** Inicia a chamada pelo chip padrão via Telecom; não espera a chamada terminar. */
class ChamadaAndroid(private val context: Context) : CallPlacer {
    override suspend fun ligar(numero: String): ResultadoAcao = try {
        context.getSystemService(TelecomManager::class.java)
            .placeCall(Uri.fromParts("tel", numero, null), Bundle())
        ResultadoAcao.Ok
    } catch (e: SecurityException) {
        ResultadoAcao.Erro("sem permissão de chamada")
    } catch (e: Exception) {
        ResultadoAcao.Erro(e.message ?: e.javaClass.simpleName)
    }
}
