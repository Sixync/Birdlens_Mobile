// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/TourViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.PaginatedToursResponse
import com.android.birdlens.data.model.Tour
import com.android.birdlens.data.model.TourCreateRequest
import com.android.birdlens.data.repository.TourRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class TourUIState<out T> {
    data object Idle : TourUIState<Nothing>()
    data object Loading : TourUIState<Nothing>()
    data class Success<T>(val data: T) : TourUIState<T>()
    data class Error(val message: String) : TourUIState<Nothing>()
}

class TourViewModel(application: Application) : AndroidViewModel(application) {

    private val tourRepository = TourRepository(application.applicationContext)

    private val _toursState = MutableStateFlow<TourUIState<PaginatedToursResponse>>(TourUIState.Idle)
    val toursState: StateFlow<TourUIState<PaginatedToursResponse>> = _toursState.asStateFlow()

    val _tourDetailState = MutableStateFlow<TourUIState<Tour>>(TourUIState.Idle)
    val tourDetailState: StateFlow<TourUIState<Tour>> = _tourDetailState.asStateFlow()

    private val _createTourState = MutableStateFlow<TourUIState<Tour>>(TourUIState.Idle)
    val createTourState: StateFlow<TourUIState<Tour>> = _createTourState.asStateFlow()

    private val _uploadImagesState = MutableStateFlow<TourUIState<List<String>>>(TourUIState.Idle)
    val uploadImagesState: StateFlow<TourUIState<List<String>>> = _uploadImagesState.asStateFlow()

    private val _uploadThumbnailState = MutableStateFlow<TourUIState<String>>(TourUIState.Idle)
    val uploadThumbnailState: StateFlow<TourUIState<String>> = _uploadThumbnailState.asStateFlow()

    private val _popularTourState = MutableStateFlow<TourUIState<Tour?>>(TourUIState.Idle)
    val popularTourState: StateFlow<TourUIState<Tour?>> = _popularTourState.asStateFlow()

    private val _horizontalToursState = MutableStateFlow<TourUIState<PaginatedToursResponse>>(TourUIState.Idle)
    val horizontalToursState: StateFlow<TourUIState<PaginatedToursResponse>> = _horizontalToursState.asStateFlow()

    companion object {
        private const val TAG = "TourVM"
    }

    fun fetchTours(limit: Int = 10, offset: Int = 0) {
        viewModelScope.launch {
            _toursState.value = TourUIState.Loading
            try {
                val response = tourRepository.getTours(limit, offset)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _toursState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _toursState.value = TourUIState.Error(genericResponse.message ?: "Failed to load tours")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _toursState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching tours: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _toursState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching tours", e)
            }
        }
    }

    fun fetchTourById(tourId: Long) {
        viewModelScope.launch {
            _tourDetailState.value = TourUIState.Loading
            try {
                val response = tourRepository.getTourById(tourId)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _tourDetailState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _tourDetailState.value = TourUIState.Error(genericResponse.message ?: "Failed to load tour details")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _tourDetailState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching tour detail: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _tourDetailState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching tour detail", e)
            }
        }
    }

    fun fetchPopularTour() {
        viewModelScope.launch {
            _popularTourState.value = TourUIState.Loading
            try {
                val response = tourRepository.getTours(limit = 1, offset = 0)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _popularTourState.value = TourUIState.Success(genericResponse.data.items?.firstOrNull())
                    } else {
                        _popularTourState.value = TourUIState.Error(genericResponse.message ?: "Failed to load popular tour")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _popularTourState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching popular tour: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _popularTourState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching popular tour", e)
            }
        }
    }

    fun fetchHorizontalTours(limit: Int = 5) {
        viewModelScope.launch {
            _horizontalToursState.value = TourUIState.Loading
            try {
                val response = tourRepository.getTours(limit = limit, offset = 0)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _horizontalToursState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _horizontalToursState.value = TourUIState.Error(genericResponse.message ?: "Failed to load horizontal tours")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _horizontalToursState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching horizontal tours: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _horizontalToursState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching horizontal tours", e)
            }
        }
    }

    fun createTour(tourCreateRequest: TourCreateRequest) {
        viewModelScope.launch {
            _createTourState.value = TourUIState.Loading
            try {
                val response = tourRepository.createTour(tourCreateRequest)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _createTourState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _createTourState.value = TourUIState.Error(genericResponse.message ?: "Failed to create tour")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _createTourState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error creating tour: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _createTourState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception creating tour", e)
            }
        }
    }

    fun addTourImages(tourId: Long, imageFiles: List<File>) {
        viewModelScope.launch {
            _uploadImagesState.value = TourUIState.Loading
            try {
                val response = tourRepository.addTourImages(tourId, imageFiles)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _uploadImagesState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _uploadImagesState.value = TourUIState.Error(genericResponse.message ?: "Failed to upload images")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _uploadImagesState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error uploading images: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _uploadImagesState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "addTourImages Exception", e)
            }
        }
    }

    fun addTourThumbnail(tourId: Long, thumbnailFile: File) {
        viewModelScope.launch {
            _uploadThumbnailState.value = TourUIState.Loading
            try {
                val response = tourRepository.addTourThumbnail(tourId, thumbnailFile)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _uploadThumbnailState.value = TourUIState.Success(genericResponse.data)
                    } else {
                        _uploadThumbnailState.value = TourUIState.Error(genericResponse.message ?: "Failed to upload thumbnail")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _uploadThumbnailState.value = TourUIState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error uploading thumbnail: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _uploadThumbnailState.value = TourUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "addTourThumbnail Exception", e)
            }
        }
    }

    fun resetCreateTourState() {
        _createTourState.value = TourUIState.Idle
    }

    fun resetUploadImagesState() {
        _uploadImagesState.value = TourUIState.Idle
    }

    fun resetUploadThumbnailState() {
        _uploadThumbnailState.value = TourUIState.Idle
    }
}