// app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdIdentifierViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.BuildConfig
import com.android.birdlens.R
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BirdIdentifierUiState {
    object Idle : BirdIdentifierUiState
    data class Loading(val message: String) : BirdIdentifierUiState
    data class Success(
        val identifiedBird: String,
        val chatResponse: String,
        val imageUrl: String? // Added for the bird image
    ) : BirdIdentifierUiState
    data class Error(val errorMessage: String) : BirdIdentifierUiState
}

class BirdIdentifierViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState: MutableStateFlow<BirdIdentifierUiState> =
        MutableStateFlow(BirdIdentifierUiState.Idle)
    val uiState: StateFlow<BirdIdentifierUiState> = _uiState.asStateFlow()

    private val _conversationHistory = mutableListOf<Content>()

    private var identifiedBirdName: String? = null

    companion object {
        private const val TAG = "BirdIdentifierVM"
    }

    private val generativeVisionModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-pro",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private val generativeChatModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private val wikiApiService = WikiRetrofitInstance.api
    private val ebirdApiService = EbirdRetrofitInstance.api

    fun startChatWithImage(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_analyzing))
            resetConversation()

            try {
                // Step 1: Identify the bird to get its name
                val identificationPrompt = context.getString(R.string.gemini_prompt_identify_from_image)
                val identificationContent = content("user") {
                    image(bitmap)
                    text(identificationPrompt)
                }
                val identificationResponse = generativeVisionModel.generateContent(identificationContent)
                val birdName = identificationResponse.text?.trim()

                if (birdName.isNullOrBlank()) {
                    _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_no_identify))
                    return@launch
                }
                if (birdName.contains("no bird", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_no_bird_found))
                    return@launch
                }

                identifiedBirdName = birdName
                _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_fetching_details, birdName))

                // Step 2 & 3: Fetch image and answer question concurrently
                val effectivePrompt = if (prompt.isNotBlank()) prompt else context.getString(R.string.gemini_prompt_initial_question_no_image)
                val imageDeferred = async { fetchBestWikipediaImage(birdName) }
                val initialAnswerDeferred = async { getInitialAnswer(birdName, effectivePrompt) }

                val imageUrl = imageDeferred.await()
                val initialAnswer = initialAnswerDeferred.await()

                if (initialAnswer != null) {
                    // Update conversation history
                    val userContent = content("user") { text(prompt) }
                    val modelContent = content("model") { text(initialAnswer) }
                    _conversationHistory.addAll(listOf(userContent, modelContent))

                    _uiState.value = BirdIdentifierUiState.Success(
                        identifiedBird = birdName,
                        chatResponse = initialAnswer,
                        imageUrl = imageUrl
                    )
                } else {
                    _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_failed_details, birdName))
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_generic, e.localizedMessage))
                Log.e(TAG, "Error in identifyBird", e)
            }
        }
    }

    fun startChatWithText(prompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_extracting_name))
            resetConversation()

            try {
                // Step 1: Extract bird name from the prompt
                val extractionPrompt = context.getString(R.string.gemini_prompt_extract_name_from_text, prompt)
                val nameExtractionResponse = generativeChatModel.generateContent(extractionPrompt)
                val birdName = nameExtractionResponse.text?.trim()

                if (birdName.isNullOrBlank() || birdName.contains("Error:", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error(birdName ?: context.getString(R.string.bird_identifier_error_no_name_from_question))
                    return@launch
                }

                identifiedBirdName = birdName
                _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_fetching_details, birdName))

                // Step 2 & 3: Fetch image and answer question concurrently
                val imageDeferred = async { fetchBestWikipediaImage(birdName) }
                val initialAnswerDeferred = async { getInitialAnswer(birdName, prompt) }

                val imageUrl = imageDeferred.await()
                val initialAnswer = initialAnswerDeferred.await()

                if (initialAnswer != null) {
                    // Update conversation history
                    val userContent = content("user") { text(prompt) }
                    val modelContent = content("model") { text(initialAnswer) }
                    _conversationHistory.addAll(listOf(userContent, modelContent))

                    _uiState.value = BirdIdentifierUiState.Success(
                        identifiedBird = birdName,
                        chatResponse = initialAnswer,
                        imageUrl = imageUrl
                    )
                } else {
                    _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_failed_details, birdName))
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_generic, e.localizedMessage))
                Log.e(TAG, "Error in startChatWithText", e)
            }
        }
    }


    private suspend fun getInitialAnswer(birdName: String, prompt: String): String? {
        try {
            val chat = generativeChatModel.startChat(history = emptyList())
            val languageSuffix = context.getString(R.string.gemini_language_suffix)
            val contextualPrompt = "In the context of the bird '$birdName', answer this question: $prompt.$languageSuffix"
            val response = chat.sendMessage(contextualPrompt)
            return response.text
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial answer", e)
            return null
        }
    }

    private suspend fun fetchBestWikipediaImage(birdName: String): String? {
        // 1. Primary attempt using the common name
        val primaryImageUrl = getImageUrlForTitle(birdName)
        if (primaryImageUrl != null) {
            Log.d(TAG, "Primary Wikipedia image found for $birdName.")
            return primaryImageUrl
        }

        // 2. Fallback: Get eBird taxonomy to find the family name
        Log.w(TAG, "No direct image for '$birdName'. Trying fallback via eBird taxonomy.")
        try {
            // eBird API needs a species code, but we can try with the common name.
            // A better approach might be needed if common names are ambiguous.
            // For now, let's assume the common name is specific enough for a taxonomy lookup.
            val ebirdResponse = ebirdApiService.getSpeciesTaxonomy(speciesCodes = birdName)
            if (!ebirdResponse.isSuccessful || ebirdResponse.body().isNullOrEmpty()) {
                Log.e(TAG, "Fallback failed: Could not get eBird taxonomy for '$birdName'.")
                return null
            }

            val birdData = ebirdResponse.body()!!.first()
            val familySciName = birdData.familyScientificName
            if (familySciName.isNullOrBlank()) {
                Log.w(TAG, "Fallback failed: No family scientific name available for '$birdName'.")
                return null
            }

            // 3. Fallback: Get representative species from Wikipedia category
            val representativeSpeciesTitle = getRepresentativeSpeciesFromFamily(familySciName)
            if (representativeSpeciesTitle != null) {
                Log.d(TAG, "Found representative species '$representativeSpeciesTitle' for family '$familySciName'. Fetching its image.")
                return getImageUrlForTitle(representativeSpeciesTitle)
            } else {
                Log.w(TAG, "Fallback failed: Could not find any representative species for family '$familySciName'.")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during image fallback for '$birdName'", e)
            return null
        }
    }


    private suspend fun getImageUrlForTitle(title: String): String? {
        return try {
            val wikiResponse = wikiApiService.getPageImage(titles = title)
            if (wikiResponse.isSuccessful && wikiResponse.body() != null) {
                val pageDetail = wikiResponse.body()!!.query?.pages?.values?.firstOrNull { it.thumbnail?.source != null }
                pageDetail?.thumbnail?.source
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for title '$title'", e)
            null
        }
    }

    private suspend fun getRepresentativeSpeciesFromFamily(familySciName: String): String? {
        return try {
            val categoryTitle = "Category:$familySciName"
            Log.d(TAG, "Querying Wikipedia for members of '$categoryTitle'")
            val response = wikiApiService.getCategoryMembers(cmTitle = categoryTitle)

            if (response.isSuccessful) {
                val members = response.body()?.query?.categoryMembers
                // Find the first member that isn't a sub-category or template page
                members?.firstOrNull { member ->
                    !member.title.contains("list of", ignoreCase = true) &&
                            !member.title.startsWith("Category:", ignoreCase = true) &&
                            !member.title.contains("template", ignoreCase = true)
                }?.title
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting representative species for '$familySciName'", e)
            null
        }
    }

    fun askQuestion(question: String) {
        val birdName = identifiedBirdName
        if (birdName == null) {
            _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_ask_first))
            return
        }

        viewModelScope.launch {
            // Keep previous success state while loading new answer
            val previousState = _uiState.value
            _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_thinking))

            try {
                val chat = generativeChatModel.startChat(
                    history = _conversationHistory
                )
                val languageSuffix = context.getString(R.string.gemini_language_suffix)
                val questionWithLanguage = "$question$languageSuffix"
                val response = chat.sendMessage(questionWithLanguage)

                _conversationHistory.add(content("user") { text(question) })
                val responseText = response.text
                    ?: context.getString(R.string.bird_identifier_error_no_response)
                _conversationHistory.add(content("model") { text(responseText) })

                _uiState.value = BirdIdentifierUiState.Success(
                    identifiedBird = birdName,
                    chatResponse = responseText,
                    imageUrl = if (previousState is BirdIdentifierUiState.Success) previousState.imageUrl else null
                )

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_generic, e.localizedMessage))
            }
        }
    }

    private fun resetConversation() {
        _conversationHistory.clear()
        identifiedBirdName = null
    }

    fun resetState() {
        resetConversation()
        _uiState.value = BirdIdentifierUiState.Idle
    }
}