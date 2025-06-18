// EXE201/app/src/main/java/com/android/birdlens/data/AuthEventBus.kt
package com.android.birdlens.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A singleton event bus for broadcasting authentication-related events.
 * This is used to signal an authentication failure (e.g., 401 Unauthorized)
 * from the network layer (AuthInterceptor) to the UI layer (MainActivity)
 * in a decoupled way.
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    suspend fun postEvent(event: AuthEvent) {
        _events.emit(event)
    }
}

sealed class AuthEvent {
    data object TokenExpiredOrInvalid : AuthEvent()
}