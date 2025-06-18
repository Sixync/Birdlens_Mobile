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
    data class IdentificationSuccess(val possibilities: List<String>, val pendingPrompt: String) : BirdIdentifierUiState
    data class ConversationReady(
        val identifiedBird: String,
        val chatResponse: String,
        val imageUrl: String?
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
            modelName = "gemini-1.5-flash", // Updated model
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
                val identificationPrompt = context.getString(R.string.gemini_prompt_identify_from_image)
                val identificationContent = content("user") {
                    image(bitmap)
                    text(identificationPrompt)
                }
                val identificationResponse = generativeVisionModel.generateContent(identificationContent)
                val responseText = identificationResponse.text?.trim()

                if (responseText.isNullOrBlank() || responseText.contains("no bird", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_no_bird_found))
                    return@launch
                }

                val possibilities = responseText.split(',').map { it.trim() }.filter { it.isNotEmpty() }

                if (possibilities.size == 1) {
                    selectBirdAndStartConversation(possibilities.first(), prompt)
                } else {
                    _uiState.value = BirdIdentifierUiState.IdentificationSuccess(possibilities, prompt)
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
                val extractionPrompt = context.getString(R.string.gemini_prompt_extract_name_from_text, prompt)
                val nameExtractionResponse = generativeChatModel.generateContent(extractionPrompt)
                val responseText = nameExtractionResponse.text?.trim()

                if (responseText.isNullOrBlank() || responseText.contains("Error:", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error(responseText ?: context.getString(R.string.bird_identifier_error_no_name_from_question))
                    return@launch
                }

                val possibilities = responseText.split(',').map { it.trim() }.filter { it.isNotEmpty() }

                if (possibilities.size == 1) {
                    selectBirdAndStartConversation(possibilities.first(), prompt)
                } else {
                    _uiState.value = BirdIdentifierUiState.IdentificationSuccess(possibilities, prompt)
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_generic, e.localizedMessage))
                Log.e(TAG, "Error in startChatWithText", e)
            }
        }
    }

    fun selectBirdAndStartConversation(birdName: String, userPrompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading(context.getString(R.string.bird_identifier_loading_fetching_details, birdName))
            identifiedBirdName = birdName

            val effectivePrompt = if (userPrompt.isNotBlank()) userPrompt else context.getString(R.string.gemini_prompt_initial_question_no_image)
            val imageDeferred = async { fetchBestWikipediaImage(birdName) }
            val initialAnswerDeferred = async { getInitialAnswer(birdName, effectivePrompt) }

            val imageUrl = imageDeferred.await()
            val initialAnswer = initialAnswerDeferred.await()

            if (initialAnswer != null) {
                val userContent = content("user") { text(effectivePrompt) }
                val modelContent = content("model") { text(initialAnswer) }
                _conversationHistory.addAll(listOf(userContent, modelContent))

                _uiState.value = BirdIdentifierUiState.ConversationReady(
                    identifiedBird = birdName,
                    chatResponse = initialAnswer,
                    imageUrl = imageUrl
                )
            } else {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_failed_details, birdName))
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
        val primaryImageUrl = getImageUrlForTitle(birdName)
        if (primaryImageUrl != null) {
            Log.d(TAG, "Primary Wikipedia image found for $birdName.")
            return primaryImageUrl
        }

        Log.w(TAG, "No direct image for '$birdName'. Trying fallback via eBird taxonomy.")
        try {
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

                _uiState.value = BirdIdentifierUiState.ConversationReady(
                    identifiedBird = birdName,
                    chatResponse = responseText,
                    imageUrl = if (previousState is BirdIdentifierUiState.ConversationReady) previousState.imageUrl else null
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