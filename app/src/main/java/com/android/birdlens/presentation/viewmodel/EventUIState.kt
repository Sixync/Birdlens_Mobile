// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/EventUIState.kt
package com.android.birdlens.presentation.viewmodel

// This sealed class will represent the different states for event-related UI.
sealed class EventUIState<out T> {
    data object Idle : EventUIState<Nothing>()
    data object Loading : EventUIState<Nothing>()
    data class Success<T>(val data: T) : EventUIState<T>()
    data class Error(val message: String) : EventUIState<Nothing>()
}