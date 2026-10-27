package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    // Primary conversational model with grounding
    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text("You are an expert, encouraging AI Financial & Savings Advisor inside the BudgetTracker app. " +
                        "Your mission is to help users manage their daily spending, cut unnecessary costs, plan realistic savings goals, " +
                        "and make smart purchasing decisions in South African Rand (R). You have access to Google Search for the latest financial rates, discounts, and prices. " +
                        "Keep answers concise, practical and actionable. Never invent transactions, balances, rates or financial facts; distinguish recorded facts from estimates, explain assumptions, avoid guaranteed returns, and give cautious educational guidance.")
            },
            tools = listOf(Tool.googleSearch())
        )
    }

    // Lightweight fallback model without heavy tools if primary model hits rate limit or tool quota
    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite",
            systemInstruction = content {
                text("You are an expert AI Financial Advisor for BudgetTracker. " +
                        "Give concise, cautious money management and savings tips in South African Rand (R). Never invent financial facts; distinguish recorded facts from estimates, explain assumptions, and avoid guaranteed returns.")
            }
        )
    }

    // A resettable lazy holder avoids initializing Firebase AI before the advisor is first used.
    private var chatSessionDelegate = lazy { chatModel.startChat() }
    private val chatSession get() = chatSessionDelegate.value

    private val _financialContext = MutableStateFlow<String?>(null)
    private val _hasFinancialContext = MutableStateFlow(false)
    val hasFinancialContext: StateFlow<Boolean> = _hasFinancialContext.asStateFlow()

    private val _includeFinancialContext = MutableStateFlow(false)
    val includeFinancialContext: StateFlow<Boolean> = _includeFinancialContext.asStateFlow()

    /** The caller must provide an aggregate-only summary scoped to the authenticated user. */
    fun setFinancialContext(summary: String?) {
        val hadContext = _financialContext.value != null
        val wasSharing = _includeFinancialContext.value
        _financialContext.value = summary?.trim()?.take(4_000)?.takeIf { it.isNotBlank() }
        _hasFinancialContext.value = _financialContext.value != null
        if (!_hasFinancialContext.value) {
            _includeFinancialContext.value = false
            if (hadContext && wasSharing) {
                generationJob?.cancel()
                generationJob = null
                resetConversation("Your local transaction summary is no longer available. Financial data sharing was turned off.")
            }
        }
    }

    /** Called before sign-out so private financial context is not retained in the Activity ViewModelStore. */
    fun clearFinancialContext() {
        generationJob?.cancel()
        generationJob = null
        _financialContext.value = null
        _hasFinancialContext.value = false
        _includeFinancialContext.value = false
        resetConversation("Signed out. Financial summary sharing has been cleared.")
    }

    /** Changing data-sharing consent starts a fresh conversation to clear the prior model context. */
    fun setIncludeFinancialContext(include: Boolean) {
        if (_isLoading.value || _includeFinancialContext.value == include) return
        _includeFinancialContext.value = include && _hasFinancialContext.value
        resetConversation(
            if (_includeFinancialContext.value) {
                "Financial summary sharing is on. Only aggregate totals and category totals are included."
            } else {
                "Financial summary sharing is off. This conversation restarted without your financial summary."
            }
        )
    }

    private fun requestPrompt(userText: String): String {
        val context = _financialContext.value
        if (!_includeFinancialContext.value || context.isNullOrBlank()) return userText
        return """
            User question:
            $userText

            Latest aggregate financial summary for the authenticated account:
            $context

            Instructions:
            - Use this latest summary for account-specific facts, preferring it over older summaries.
            - It contains aggregate totals only and may not include cloud-only records.
            - Do not infer a complete available balance from income and expenses alone.
            - State estimates and assumptions; never invent records or guarantee outcomes.
        """.trimIndent()
    }

    private fun resetConversation(advisorNotice: String? = null) {
        chatSessionDelegate = lazy { chatModel.startChat() }
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.ADVISOR,
                text = advisorNotice ?: "Hello! I'm your BudgetTracker AI Advisor. Ask me about budgeting, spending, and savings."
            )
        )
        _errorMessage.value = null
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
    private var generationJob: Job? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        val prompt = requestPrompt(trimmed)
        val userMessage = ChatMessage(sender = MessageSender.USER, text = trimmed)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true
        _errorMessage.value = null

        generationJob = viewModelScope.launch {
            try {
                val response = withTimeout(60_000L) {
                    chatSession.sendMessage(prompt)
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
                val rawMsg = e.localizedMessage.orEmpty()
                val isQuotaError = rawMsg.contains("quota", ignoreCase = true) ||
                        rawMsg.contains("429") ||
                        rawMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)

                if (isQuotaError) {
                    // Attempt fast fallback with gemini-3.1-flash-lite
                    var fallbackSuccess = false
                    try {
                        val fbResponse = withTimeout(30_000L) {
                            fallbackModel.generateContent(
                                "User query in BudgetTracker: $prompt. Give a concise, cautious financial response in South African Rand (R). Use account-specific facts only when the latest aggregate summary is supplied."
                            )
                        }
                        val fbText = fbResponse.text
                        if (!fbText.isNullOrBlank()) {
                            _messages.value = _messages.value + ChatMessage(
                                sender = MessageSender.ADVISOR,
                                text = fbText.trim()
                            )
                            fallbackSuccess = true
                        }
                    } catch (fbErr: Exception) {
                        Log.e(TAG, "Fallback model also failed", fbErr)
                    }

                    if (!fallbackSuccess) {
                        _errorMessage.value = "⚠️ Free API Quota Limit Reached: Google Gemini's free tier allows a limited number of requests per minute (RPM). Please wait 60 seconds for the rate limit to reset, then tap to retry."
                    }
                } else {
                    _errorMessage.value = rawMsg.ifBlank { "Failed to generate reply. Please check your internet connection." }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearHistory() {
        if (_isLoading.value) return
        resetConversation()
    }
}
