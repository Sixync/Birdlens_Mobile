// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/EventDetailViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.repository.EventRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventDetailViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val eventRepository = EventRepository(application.applicationContext)
    private val eventId: Long = savedStateHandle.get<Long>("eventId") ?: -1L

    private val _eventDetailState = MutableStateFlow<EventUIState<Event>>(EventUIState.Idle)
    val eventDetailState: StateFlow<EventUIState<Event>> = _eventDetailState.asStateFlow()

    companion object {
        private const val TAG = "EventDetailVM"
    }

    init {
        if (eventId != -1L) {
            fetchEventDetails(eventId)
        } else {
            _eventDetailState.value = EventUIState.Error("Event ID not provided.")
            Log.e(TAG, "Event ID not provided in init.")
        }
    }

    fun fetchEventDetails(id: Long = eventId) {
        if (id == -1L) {
            _eventDetailState.value = EventUIState.Error("Invalid Event ID.")
            Log.e(TAG, "Invalid Event ID for fetching details: $id")
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
                        Log.e(TAG, "API error fetching event details: ${genericResponse.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _eventDetailState.value = EventUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching event details: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _eventDetailState.value = EventUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching event details", e)
            }
        }
    }
}