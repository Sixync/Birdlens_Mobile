// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/EventDetailViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// You can reuse EventUIState if its structure (Idle, Loading, Success<T>, Error) is suitable
// Or create a new one like EventDetailUiState if the data structure for success differs significantly.
// For now, let's assume EventUIState<Event> is fine.

class EventDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val eventRepository = EventRepository(application.applicationContext)
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: -1L

    private val _eventDetailState = MutableStateFlow<EventUIState<Event>>(EventUIState.Idle)
    val eventDetailState: StateFlow<EventUIState<Event>> = _eventDetailState.asStateFlow()

    init {
        if (eventId != -1L) {
            fetchEventDetails(eventId)
        } else {
            _eventDetailState.value = EventUIState.Error("Event ID not provided.")
        }
    }

    fun fetchEventDetails(id: Long = eventId) {
        if (id == -1L) {
            _eventDetailState.value = EventUIState.Error("Invalid Event ID.")
            return
        }
        viewModelScope.launch {
            _eventDetailState.value = EventUIState.Loading
            try {
                val response = eventRepository.getEventById(id)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _eventDetailState.value = EventUIState.Success(genericResponse.data)
                    } else {
                        _eventDetailState.value = EventUIState.Error(genericResponse.message ?: "Failed to load event details")
                    }
                } else {
                    _eventDetailState.value = EventUIState.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _eventDetailState.value = EventUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }
}