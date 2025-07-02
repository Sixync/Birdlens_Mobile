// Birdlens_Mobile/app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdIdentifierViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.birdlens.R
import com.android.birdlens.data.model.ChatMessage
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance
import com.android.birdlens.data.repository.AIRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BirdPossibility(val name: String, val imageUrl: String?)

sealed interface BirdIdentifierUiState {
    object Idle : BirdIdentifierUiState
    data class Loading(val message: String) : BirdIdentifierUiState
    data class IdentificationSuccess(val possibilities: List<BirdPossibility>) : BirdIdentifierUiState
    data class ConversationReady(
        val messages: List<ChatMessage>,
        val isLoading: Boolean = false
    ) : BirdIdentifierUiState
    data class Error(val errorMessage: String) : BirdIdentifierUiState
}

class BirdIdentifierViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState: MutableStateFlow<BirdIdentifierUiState> =
        MutableStateFlow(BirdIdentifierUiState.Idle)
    val uiState: StateFlow<BirdIdentifierUiState> = _uiState.asStateFlow()

    private val aiRepository = AIRepository(application)
    private val wikiApiService = WikiRetrofitInstance.api

    private var identifiedBirdName: String?
        get() = savedStateHandle["identifiedBirdName"]
        set(value) {
            savedStateHandle["identifiedBirdName"] = value
        }

    companion object {
        private const val TAG = "BirdIdentifierVM"
        private const val CHAT_HISTORY_KEY = "chat_history"
    }

    init {
        val savedMessages: List<ChatMessage>? = savedStateHandle[CHAT_HISTORY_KEY]
        if (!savedMessages.isNullOrEmpty()) {
            _uiState.value = BirdIdentifierUiState.ConversationReady(savedMessages, false)
        }
    }

    private fun handleIdentification(
        bitmap: Bitmap?,
        apiPrompt: String,
        userVisiblePrompt: String? // The prompt to display in the UI
    ) {
        viewModelScope.launch {
            _uiState.value = BirdIdentifierUiState.Loading(
                if (bitmap != null) context.getString(R.string.bird_identifier_loading_analyzing)
                else context.getString(R.string.bird_identifier_loading_extracting_name)
            )
            resetConversation()

            try {
                val response = aiRepository.identifyBird(bitmap, apiPrompt)

                if (response.isSuccessful && response.body()?.error == false) {
                    val aiResponse = response.body()?.data
                    if (aiResponse == null) {
                        _uiState.value = BirdIdentifierUiState.Error("Received an empty success response from the server.")
                        return@launch
                    }

                    if (!aiResponse.possibilities.isNullOrEmpty()) {
                        val enrichedPossibilities = coroutineScope {
                            aiResponse.possibilities.map { birdName ->
                                async {
                                    BirdPossibility(
                                        name = birdName,
                                        imageUrl = getImageUrlForTitle(birdName)
                                    )
                                }
                            }.awaitAll()
                        }
                        _uiState.value = BirdIdentifierUiState.IdentificationSuccess(enrichedPossibilities)
                    } else if (!aiResponse.identifiedBird.isNullOrBlank() && !aiResponse.chatResponse.isNullOrBlank()) {
                        val birdName = aiResponse.identifiedBird
                        identifiedBirdName = birdName

                        val initialMessages = mutableListOf<ChatMessage>()
                        // Only add a user message to the history if there's a user-visible prompt.
                        userVisiblePrompt?.let {
                            initialMessages.add(ChatMessage(role = "user", text = it.trim()))
                        }

                        val assistantMessage = ChatMessage(
                            role = "assistant",
                            text = aiResponse.chatResponse,
                            imageUrl = aiResponse.imageUrl
                        )
                        initialMessages.add(assistantMessage)

                        _uiState.value = BirdIdentifierUiState.ConversationReady(initialMessages, isLoading = false)
                        savedStateHandle[CHAT_HISTORY_KEY] = initialMessages
                    } else {
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
        val defaultPrompt = context.getString(R.string.gemini_prompt_identify_from_image)
        val effectiveApiPrompt = if (prompt.isNotBlank()) "$defaultPrompt. The user also asked: '$prompt'" else defaultPrompt
        // Determine a clean, user-facing prompt. If the user didn't type, use a generic one.
        val userVisiblePrompt = if (prompt.isNotBlank()) prompt else "What kind of bird is in this image?"
        handleIdentification(bitmap, effectiveApiPrompt, userVisiblePrompt)
    }

    fun startChatWithText(prompt: String) {
        val basePrompt = context.getString(R.string.gemini_prompt_extract_name_from_text)
        val effectiveApiPrompt = String.format(basePrompt, prompt)
        handleIdentification(null, effectiveApiPrompt, prompt)
    }

    fun selectBirdAndStartConversation(birdName: String) {
        val newApiPrompt = context.getString(R.string.gemini_prompt_initial_question_for_selected_bird, birdName)
        // No user-visible prompt is needed here, as the conversation starts with the assistant's answer.
        handleIdentification(null, newApiPrompt, null)
    }

    fun askQuestion(question: String) {
        val birdName = identifiedBirdName
        if (birdName == null) {
            _uiState.value = BirdIdentifierUiState.Error(context.getString(R.string.bird_identifier_error_ask_first))
            return
        }

        val currentMessages = (_uiState.value as? BirdIdentifierUiState.ConversationReady)?.messages ?: emptyList()
        val apiHistory = currentMessages.map { com.android.birdlens.data.model.ChatMessage(it.role.replace("assistant", "model"), it.text) }

        val userMessage = ChatMessage(role = "user", text = question)
        val updatedMessages = currentMessages + userMessage
        _uiState.value = BirdIdentifierUiState.ConversationReady(updatedMessages, isLoading = true)
        savedStateHandle[CHAT_HISTORY_KEY] = updatedMessages

        viewModelScope.launch {
            try {
                val response = aiRepository.askQuestion(birdName, question, apiHistory)

                if (response.isSuccessful && response.body()?.error == false) {
                    val aiResponse = response.body()?.data
                    val responseText = aiResponse?.chatResponse ?: context.getString(R.string.bird_identifier_error_no_response)

                    val assistantMessage = ChatMessage(role = "assistant", text = responseText)
                    val finalMessages = updatedMessages + assistantMessage
                    _uiState.value = BirdIdentifierUiState.ConversationReady(finalMessages, isLoading = false)
                    savedStateHandle[CHAT_HISTORY_KEY] = finalMessages
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    val errorMessage = ChatMessage(role = "assistant", text = "Sorry, I couldn't process that: $message")
                    val finalMessages = updatedMessages + errorMessage
                    _uiState.value = BirdIdentifierUiState.ConversationReady(finalMessages, isLoading = false)
                    savedStateHandle[CHAT_HISTORY_KEY] = finalMessages
                }

            } catch (e: Exception) {
                val errorMessage = ChatMessage(role = "assistant", text = "An error occurred: ${e.localizedMessage}")
                val finalMessages = updatedMessages + errorMessage
                _uiState.value = BirdIdentifierUiState.ConversationReady(finalMessages, isLoading = false)
                savedStateHandle[CHAT_HISTORY_KEY] = finalMessages
            }
        }
    }

    private suspend fun getImageUrlForTitle(title: String): String? {
        return try {
            val response = wikiApiService.getPageImage(titles = title)
            if (response.isSuccessful) {
                val pageDetail = response.body()?.query?.pages?.values?.firstOrNull { it.thumbnail?.source != null }
                pageDetail?.thumbnail?.source
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for title '$title'", e)
            null
        }
    }

    private fun resetConversation() {
        identifiedBirdName = null
        savedStateHandle[CHAT_HISTORY_KEY] = emptyList<ChatMessage>()
    }

    fun resetState() {
        resetConversation()
        _uiState.value = BirdIdentifierUiState.Idle
    }
}