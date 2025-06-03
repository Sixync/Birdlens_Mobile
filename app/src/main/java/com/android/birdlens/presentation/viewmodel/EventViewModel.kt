// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/EventViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.Event // Assuming this is the correct model
import com.android.birdlens.data.model.PaginatedEventData
import com.android.birdlens.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val eventRepository = EventRepository(application.applicationContext)

    // For a list of events (paginated)
    private val _eventsState = MutableStateFlow<EventUIState<PaginatedEventData>>(EventUIState.Idle)
    val eventsState: StateFlow<EventUIState<PaginatedEventData>> = _eventsState.asStateFlow()

    // For a single event detail (if you implement an event detail screen later)
    // private val _eventDetailState = MutableStateFlow<EventUIState<Event>>(EventUIState.Idle)
    // val eventDetailState: StateFlow<EventUIState<Event>> = _eventDetailState.asStateFlow()

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
                    }
                } else {
                    _eventsState.value = EventUIState.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _eventsState.value = EventUIState.Error(e.localizedMessage ?: "An unexpected error occurred while fetching events")
            }
        }
    }

    // Example for fetching a single event by ID (if needed later)
    // fun fetchEventById(eventId: Long) {
    //     viewModelScope.launch {
    //         _eventDetailState.value = EventUIState.Loading
    //         // ... implementation similar to fetchTourById in TourViewModel ...
    //         // For now, this is a placeholder.
    //     }
    // }
}