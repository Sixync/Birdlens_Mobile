package com.android.birdlens.presentation.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CreatePostUiState(
    val postType: String = "general",
    val content: String = "",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val privacyLevel: String = "public",
    val isFeatured: Boolean = false,
    val selectedImageUris: List<Uri> = emptyList(),
    // Sighting-specific state
    val sightingDate: LocalDate? = LocalDate.now(),
    val taggedSpecies: BirdSpecies? = null, // Holds the selected bird object
    val speciesSearchQuery: String = "",
    val speciesSearchResults: List<BirdSpecies> = emptyList(),
    val isSearchingSpecies: Boolean = false
)

class CreatePostViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val birdSpeciesRepository = BirdSpeciesRepository(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    private var speciesSearchJob: Job? = null

    companion object {
        private const val TAG = "CreatePostVM"
    }

    init {
        // Pre-fill form if data is passed from another screen (e.g., AI Identifier)
        val speciesCode = savedStateHandle.get<String>("speciesCode")
        val speciesName = savedStateHandle.get<String>("speciesName")
        val imageUriString = savedStateHandle.get<String>("imageUri")

        if (speciesCode != null && speciesName != null) {
            _uiState.update {
                it.copy(
                    postType = "sighting",
                    taggedSpecies = BirdSpecies(speciesCode = speciesCode, commonName = speciesName, scientificName = ""), // SciName can be fetched if needed
                    speciesSearchQuery = speciesName
                )
            }
        }
        imageUriString?.let { uriStr ->
            _uiState.update { it.copy(selectedImageUris = listOf(Uri.parse(uriStr))) }
        }
    }

    fun onPostTypeChange(type: String) {
        _uiState.update { it.copy(postType = type) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun onSightingDateChange(date: LocalDate) {
        _uiState.update { it.copy(sightingDate = date) }
    }

    fun onPrivacyLevelChange(level: String) {
        _uiState.update { it.copy(privacyLevel = level) }
    }

    fun onImageUrisChange(uris: List<Uri>) {
        _uiState.update { it.copy(selectedImageUris = uris) }
    }

    fun onSpeciesSearchQueryChange(query: String) {
        _uiState.update { it.copy(speciesSearchQuery = query, taggedSpecies = null) }
        speciesSearchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(speciesSearchResults = emptyList()) }
            return
        }
        _uiState.update { it.copy(isSearchingSpecies = true) }
        speciesSearchJob = viewModelScope.launch {
            delay(300) // Debounce
            try {
                val results = birdSpeciesRepository.searchBirds(query)
                _uiState.update { it.copy(speciesSearchResults = results, isSearchingSpecies = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Species search failed", e)
                _uiState.update { it.copy(speciesSearchResults = emptyList(), isSearchingSpecies = false) }
            }
        }
    }

    fun onSpeciesSelected(bird: BirdSpecies) {
        _uiState.update {
            it.copy(
                taggedSpecies = bird,
                speciesSearchQuery = bird.commonName,
                speciesSearchResults = emptyList() // Clear results after selection
            )
        }
    }

    @SuppressLint("MissingPermission") // Permissions are checked in the Composable
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
                location?.let {
                    _uiState.update { state ->
                        state.copy(latitude = it.latitude, longitude = it.longitude)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current location", e)
            }
        }
    }

    fun setLocation(latLng: LatLng) {
        _uiState.update { it.copy(latitude = latLng.latitude, longitude = latLng.longitude) }
    }

    fun getSightingDateAsString(): String? {
        return uiState.value.sightingDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toString()
    }
}