// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdIdentifierViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.R
import com.android.birdlens.data.model.ChatMessage
import com.android.birdlens.data.repository.AIRepository
import com.android.birdlens.utils.ErrorUtils
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

    // Logic: Use our new AIRepository instead of the Gemini SDK.
    private val aiRepository = AIRepository(application)

    // Logic: The conversation history is now a list of our custom ChatMessage data class.
    private val _conversationHistory = mutableListOf<ChatMessage>()

    private var identifiedBirdName: String? = null

    companion object {
        private const val TAG = "BirdIdentifierVM"
    }

    // Logic: The Gemini SDK models are no longer needed.
    // private val generativeVisionModel: GenerativeModel by lazy { ... }
    // private val generativeChatModel: GenerativeModel by lazy { ... }
    // private val wikiApiService = WikiRetrofitInstance.api
    // private val ebirdApiService = EbirdRetrofitInstance.api

    private fun handleIdentification(bitmap: Bitmap?, prompt: String) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading(
                if (bitmap != null) context.getString(R.string.bird_identifier_loading_analyzing)
                else context.getString(R.string.bird_identifier_loading_extracting_name)
            )
            resetConversation()

            try {
                // Logic: Call our new repository method which communicates with our backend.
                val response = aiRepository.identifyBird(bitmap, prompt)

                if (response.isSuccessful && response.body()?.error == false) {
                    val aiResponse = response.body()?.data
                    if (aiResponse == null) {
                        _uiState.value = BirdIdentifierUiState.Error("Received an empty success response from the server.")
                        return@launch
                    }

                    // Backend will provide either a final identified bird or a list of possibilities.
                    if (!aiResponse.identifiedBird.isNullOrBlank() && !aiResponse.chatResponse.isNullOrBlank()) {
                        // The backend identified one bird and gave an initial response.
                        val birdName = aiResponse.identifiedBird
                        identifiedBirdName = birdName
                        val initialAnswer = aiResponse.chatResponse

                        // Store history
                        val userContent = ChatMessage(role = "user", text = prompt)
                        val modelContent = ChatMessage(role = "model", text = initialAnswer)
                        _conversationHistory.addAll(listOf(userContent, modelContent))

                        _uiState.value = BirdIdentifierUiState.ConversationReady(
                            identifiedBird = birdName,
                            chatResponse = initialAnswer,
                            imageUrl = aiResponse.imageUrl
                        )
                    } else if (!aiResponse.possibilities.isNullOrEmpty()) {
                        // The backend returned multiple possibilities for the user to choose from.
                        _uiState.value = BirdIdentifierUiState.IdentificationSuccess(aiResponse.possibilities, prompt)
                    } else {
                        // The backend couldn't identify anything.
                        _uiState.value = BirdIdentifierUiState.Error(aiResponse.chatResponse ?: context.getString(R.string.bird_identifier_error_no_bird_found))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _uiState.value = BirdIdentifierUiState.Error(message)
                }

            } catch (e: Exception) {
                _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_generic, e.localizedMessage))
                Log.e(TAG, "Error in handleIdentification", e)
            }
        }
    }


    fun startChatWithImage(bitmap: Bitmap, prompt: String) {
        val effectivePrompt = if (prompt.isNotBlank()) prompt else "Identify this bird and tell me about it."
        handleIdentification(bitmap, effectivePrompt)
    }

    fun startChatWithText(prompt: String) {
        handleIdentification(null, prompt)
    }

    fun selectBirdAndStartConversation(birdName: String, userPrompt: String) {
        // With the new backend flow, the backend gives us the chat response directly.
        // We can either re-call the identify endpoint with the selected bird, or have a separate one.
        // For simplicity, we can treat this as a new text-based identification.
        val newPrompt = if (userPrompt.isNotBlank()) {
            "Tell me more about the $birdName, specifically focusing on this question: $userPrompt"
        } else {
            "Tell me about the $birdName"
        }
        handleIdentification(null, newPrompt)
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
                // Logic: Call the new repository method for asking follow-up questions.
                val response = aiRepository.askQuestion(birdName, question, _conversationHistory)

                if (response.isSuccessful && response.body()?.error == false) {
                    val aiResponse = response.body()?.data
                    val responseText = aiResponse?.chatResponse ?: context.getString(R.string.bird_identifier_error_no_response)

                    // Add to history
                    _conversationHistory.add(ChatMessage("user", question))
                    _conversationHistory.add(ChatMessage("model", responseText))

                    _uiState.value = BirdIdentifierUiState.ConversationReady(
                        identifiedBird = birdName,
                        chatResponse = responseText,
                        imageUrl = if (previousState is BirdIdentifierUiState.ConversationReady) previousState.imageUrl else null
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _uiState.value = BirdIdentifierUiState.Error(message)
                }

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