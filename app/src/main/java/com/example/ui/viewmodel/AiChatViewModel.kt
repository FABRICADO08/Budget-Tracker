package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiChatViewModel"

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ADVISOR
}

class AiChatViewModel : ViewModel() {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text("You are an expert, encouraging AI Financial & Savings Advisor inside the BudgetTracker app. " +
                        "Your mission is to help users manage their daily spending, cut unnecessary costs, plan realistic savings goals, " +
                        "and make smart purchasing decisions. You have access to Google Search for the latest financial rates, discounts, and prices, " +
                        "and Google Maps to recommend budget-friendly local stores, banks, or markets. " +
                        "Keep your answers concise, practical, actionable, and formatted with bullet points where helpful.")
            },
            tools = listOf(Tool.googleSearch(), Tool.googleMaps())
        )
    }

    private val chatSession by lazy {
        chatModel.startChat()
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ADVISOR,
                text = "Hello! I'm your BudgetTracker AI Advisor. Ask me anything about budgeting tips, cutting expenses, investing for savings goals, or finding budget-friendly places nearby!"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(sender = MessageSender.USER, text = trimmed)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = withTimeout(60_000L) {
                    chatSession.sendMessage(trimmed)
                }
                val replyText = response.text
                if (!replyText.isNullOrBlank()) {
                    _messages.value = _messages.value + ChatMessage(
                        sender = MessageSender.ADVISOR,
                        text = replyText.trim()
                    )
                } else {
                    _errorMessage.value = "Advisor returned an empty response. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI message generation error", e)
                _errorMessage.value = e.localizedMessage ?: "Failed to generate reply. Please check your connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.ADVISOR,
                text = "Hello! I'm your BudgetTracker AI Advisor. Ask me anything about budgeting tips, cutting expenses, investing for savings goals, or finding budget-friendly places nearby!"
            )
        )
        _errorMessage.value = null
    }
}
