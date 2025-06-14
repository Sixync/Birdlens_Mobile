// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/EventViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.PaginatedEventData
import com.android.birdlens.data.repository.EventRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository = EventRepository(application.applicationContext)

    private val _eventsState = MutableStateFlow<EventUIState<PaginatedEventData>>(EventUIState.Idle)
    val eventsState: StateFlow<EventUIState<PaginatedEventData>> = _eventsState.asStateFlow()

    companion object {
        private const val TAG = "EventVM"
    }

    fun fetchEvents(limit: Int = 10, offset: Int = 0) {
        viewModelScope.launch {
            _eventsState.value = EventUIState.Loading
            try {
                val response = eventRepository.getEvents(limit, offset)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _eventsState.value = EventUIState.Success(genericResponse.data)
                    } else {
                        _eventsState.value = EventUIState.Error(genericResponse.message ?: "Failed to load events")
                        Log.e(TAG, "API error fetching events: ${genericResponse.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _eventsState.value = EventUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching events: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _eventsState.value = EventUIState.Error(e.localizedMessage ?: "An unexpected error occurred while fetching events")
                Log.e(TAG, "Exception fetching events", e)
            }
        }
    }
}