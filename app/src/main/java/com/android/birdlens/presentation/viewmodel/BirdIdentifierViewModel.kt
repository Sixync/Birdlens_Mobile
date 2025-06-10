// app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdIdentifierViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.BuildConfig
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.type.Content

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

class BirdIdentifierViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<BirdIdentifierUiState> =
        MutableStateFlow(BirdIdentifierUiState.Idle)
    val uiState: StateFlow<BirdIdentifierUiState> = _uiState.asStateFlow()

    private val _conversationHistory = mutableListOf<Content>()
    val conversationHistory: List<Content>
        get() = _conversationHistory

    private var identifiedBirdName: String? = null

    private val generativeVisionModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro-vision",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private val generativeChatModel: GenerativeModel by lazy {
        GenerativeModel(
            // Use the versioned model name to fix the error
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    private val wikiApiService = WikiRetrofitInstance.api

    fun startChatWithImage(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading("Analyzing image...")
            resetConversation()

            try {
                // Step 1: Identify the bird to get its name
                val identificationContent = content("user") {
                    image(bitmap)
                    text("What is the common name of the bird in this image? If it's not a bird, say so. Respond with only the name.")
                }
                val identificationResponse = generativeVisionModel.generateContent(identificationContent)
                val birdName = identificationResponse.text?.trim()

                if (birdName.isNullOrBlank()) {
                    _uiState.value = BirdIdentifierUiState.Error("Could not identify the bird. Please try another image.")
                    return@launch
                }
                if (birdName.contains("not a bird", ignoreCase = true) || birdName.contains("no bird", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error("No bird was found in the image.")
                    return@launch
                }

                identifiedBirdName = birdName
                _uiState.value = BirdIdentifierUiState.Loading("Fetching details for $birdName...")

                // Step 2 & 3: Fetch image and answer question concurrently
                val wikiImageDeferred = async { fetchWikipediaImage(birdName) }
                val initialAnswerDeferred = async { getInitialAnswer(birdName, prompt) }

                val imageUrl = wikiImageDeferred.await()
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
                    _uiState.value = BirdIdentifierUiState.Error("Failed to get details for $birdName.")
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error("Error during identification: ${e.localizedMessage}")
                Log.e("BirdIdentifierVM", "Error in identifyBird", e)
            }
        }
    }

    fun startChatWithText(prompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading("Figuring out which bird you mean...")
            resetConversation()

            try {
                // Step 1: Extract bird name from the prompt
                val extractionPrompt = "Extract the common name of the bird from the following text. Respond with only the name. If no specific bird is mentioned, respond with 'Error: No bird name found.'. Text: \"$prompt\""
                val nameExtractionResponse = generativeChatModel.generateContent(extractionPrompt)
                val birdName = nameExtractionResponse.text?.trim()

                if (birdName.isNullOrBlank() || birdName.contains("Error:", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error(birdName ?: "Could not determine the bird from your question. Please be more specific, e.g., 'Tell me about the Blue Jay'.")
                    return@launch
                }

                identifiedBirdName = birdName
                _uiState.value = BirdIdentifierUiState.Loading("Fetching details for $birdName...")

                // Step 2 & 3: Fetch image and answer question concurrently
                val wikiImageDeferred = async { fetchWikipediaImage(birdName) }
                // Use the user's original, full prompt for the initial answer
                val initialAnswerDeferred = async { getInitialAnswer(birdName, prompt) }

                val imageUrl = wikiImageDeferred.await()
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
                    _uiState.value = BirdIdentifierUiState.Error("Failed to get details for $birdName.")
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error("An error occurred: ${e.localizedMessage}")
                Log.e("BirdIdentifierVM", "Error in startChatWithText", e)
            }
        }
    }


    private suspend fun getInitialAnswer(birdName: String, prompt: String): String? {
        try {
            val chat = generativeChatModel.startChat(history = emptyList())
            val contextualPrompt = "In the context of the bird '$birdName', answer this question: $prompt"
            val response = chat.sendMessage(contextualPrompt)
            return response.text
        } catch (e: Exception) {
            Log.e("BirdIdentifierVM", "Error getting initial answer", e)
            return null
        }
    }

    private suspend fun fetchWikipediaImage(birdName: String): String? {
        return try {
            Log.d("BirdIdentifierVM", "Fetching Wikipedia image for: $birdName")
            val wikiResponse = wikiApiService.getPageImage(titles = birdName)
            if (wikiResponse.isSuccessful && wikiResponse.body() != null) {
                val wikiQueryResponse = wikiResponse.body()!!
                val pageDetail = wikiQueryResponse.query?.pages?.values?.firstOrNull { it.pageId != null && it.pageId > 0 }
                pageDetail?.thumbnail?.source
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("BirdIdentifierVM", "Exception fetching Wikipedia image for $birdName", e)
            null
        }
    }

    fun askQuestion(question: String) {
        val birdName = identifiedBirdName
        if (birdName == null) {
            _uiState.value = BirdIdentifierUiState.Error("Please identify a bird first before asking questions.")
            return
        }

        viewModelScope.launch {
            // Keep previous success state while loading new answer
            val previousState = _uiState.value
            _uiState.value = BirdIdentifierUiState.Loading("Thinking...")

            try {
                val chat = generativeChatModel.startChat(
                    history = _conversationHistory
                )

                val response = chat.sendMessage(question)

                _conversationHistory.add(content("user") { text(question) })
                val responseText = response.text
                    ?: "I'm sorry, I couldn't generate a response."
                _conversationHistory.add(content("model") { text(responseText) })

                _uiState.value = BirdIdentifierUiState.Success(
                    identifiedBird = birdName,
                    chatResponse = responseText,
                    imageUrl = if (previousState is BirdIdentifierUiState.Success) previousState.imageUrl else null
                )

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error("Error getting answer: ${e.localizedMessage}")
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