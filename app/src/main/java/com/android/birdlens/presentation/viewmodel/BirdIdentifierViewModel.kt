package com.android.birdlens.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.type.Content

sealed interface BirdIdentifierUiState {
    object Idle : BirdIdentifierUiState
    data class Loading(val message: String) : BirdIdentifierUiState
    data class Success(val identifiedBird: String, val chatResponse: String) : BirdIdentifierUiState
    data class Error(val errorMessage: String) : BirdIdentifierUiState
}

class BirdIdentifierViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<BirdIdentifierUiState> = MutableStateFlow(BirdIdentifierUiState.Idle)
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
            modelName = "gemini-pro",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    fun identifyBirdFromImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading("Identifying bird...")
            resetConversation()

            try {
                val inputContent = content("user") {
                    image(bitmap)
                    text("What is the common name of the bird in this image? If it's not a bird, say so. Respond with only the name.")
                }

                val response = generativeVisionModel.generateContent(inputContent)
                val birdName = response.text?.trim()

                if (birdName.isNullOrBlank()) {
                    _uiState.value = BirdIdentifierUiState.Error("Could not identify the bird. Please try another image.")
                } else if (birdName.contains("not a bird", ignoreCase = true) || birdName.contains("no bird", ignoreCase = true)) {
                    _uiState.value = BirdIdentifierUiState.Error("No bird was found in the image.")
                } else {
                    identifiedBirdName = birdName
                    // Start the conversation with a default question
                    askQuestion("Give me a brief, one-paragraph summary about the $birdName.")
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error("Error identifying bird: ${e.localizedMessage}")
            }
        }
    }

    fun askQuestion(question: String) {
        val birdName = identifiedBirdName
        if (birdName == null) {
            _uiState.value = BirdIdentifierUiState.Error("Please identify a bird first before asking questions.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading("Thinking...")

            try {
                val chat = generativeChatModel.startChat(
                    history = _conversationHistory
                )

                val contextualQuestion = "In the context of the bird '$birdName', answer this question: $question"

                val response = chat.sendMessage(contextualQuestion)

                _conversationHistory.add(content("user") { text(contextualQuestion) })
                response.text?.let {
                    _conversationHistory.add(content("model") { text(it) })
                }

                _uiState.value = BirdIdentifierUiState.Success(
                    identifiedBird = birdName,
                    chatResponse = response.text ?: "I'm sorry, I couldn't generate a response."
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