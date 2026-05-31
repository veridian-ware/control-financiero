package com.controlfinanciero.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Holder en memoria del token JWT para que el interceptor de OkHttp lo lea de forma
 * sincrónica (DataStore es asíncrono). Se mantiene en sync con [SessionManager].
 */
object AuthTokenProvider {
    @Volatile
    var token: String? = null

    /** Emite cuando el backend responde 401 (token ausente/ inválido/ vencido). */
    val unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyUnauthorized() {
        token = null
        unauthorizedEvents.tryEmit(Unit)
    }
}
