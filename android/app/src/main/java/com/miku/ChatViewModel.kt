package com.miku.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.SocketTimeoutException
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * UI state holder for connection lifecycle, message history, and action execution flow.
 */
class ChatViewModel : ViewModel() {
    var messages by mutableStateOf(listOf<Message>())
        private set
    
    var isConnected by mutableStateOf(false)
        private set

    var isConnecting by mutableStateOf(false)
        private set
    
    var agentUrl by mutableStateOf("")
    
    /** Attempts backend connection and runs health checks before enabling chat. */
    fun connect(url: String) {
        val normalizedUrl = ApiClient.normalizeBaseUrl(url)
        agentUrl = normalizedUrl
        ApiClient.setBaseUrl(normalizedUrl)
        isConnecting = true

        viewModelScope.launch {
            try {
                val api = ApiClient.getApi() ?: error("Agent client unavailable")
                val healthStatus = checkHealth(api)

                if (healthStatus == null) {
                    isConnected = true
                    addMessage("✅ Connected to agent", false)
                } else {
                    isConnected = false
                    addMessage("❌ Agent health check failed ($healthStatus)", false)
                }
            } catch (e: Exception) {
                isConnected = false
                addMessage("❌ Connection error: ${e.message}", false)
            } finally {
                isConnecting = false
            }
        }
    }
    
    /** Resets connection state without clearing chat history. */
    fun disconnect() {
        isConnected = false
        isConnecting = false
        addMessage("❌ Disconnected", false)
    }
    
    /** Sends user message, renders assistant text, and executes returned Android actions. */
    fun sendMessage(text: String, executor: AutomationExecutor) {
        addMessage(text, true)
        
        viewModelScope.launch {
            try {
                val api = ApiClient.getApi() ?: run {
                    addMessage("❌ Not connected", false)
                    return@launch
                }
                
                val response = api.sendMessage(AgentRequest(text))
                addMessage(response.text, false)
                
                response.actions.forEach { action ->
                    val result = executor.execute(action) { update ->
                        addMessage("⚡ ${action.type}: $update", false)
                    }
                    addMessage("⚡ ${action.type}: $result", false)
                }
            } catch (e: Exception) {
                if (e is SocketTimeoutException) {
                    addMessage(
                        "⏱️ Request timeout. Server agent terlalu lama merespons. Coba ulangi atau cek endpoint sedang sehat.",
                        false
                    )
                } else {
                    addMessage("❌ Error: ${e.message}", false)
                }
            }
        }
    }
    
    /** Appends message to immutable list so Compose recomposes predictably. */
    private fun addMessage(text: String, isUser: Boolean) {
        messages = messages + Message(text, isUser)
    }

    /**
     * Tries both health endpoints and returns:
     * - null for healthy response
     * - HTTP status code for non-2xx responses
     * - throws transport error when no endpoint returned HTTP.
     */
    private suspend fun checkHealth(api: AgentApi): Int? {
        val endpoints: List<suspend () -> Response<ResponseBody>> = listOf(
            { api.health() },
            { api.apiHealth() },
        )

        var lastHttpCode: Int? = null
        var lastError: Throwable? = null
        var sawHttpResponse = false

        for (endpoint in endpoints) {
            try {
                val response = endpoint()
                sawHttpResponse = true
                lastError = null
                response.useBody {
                    if (response.isSuccessful) {
                        return null
                    }
                    lastHttpCode = response.code()
                }
            } catch (error: Exception) {
                lastError = error
            }
        }

        if (sawHttpResponse) {
            return lastHttpCode ?: 0
        }

        lastError?.let { throw it }
        return 0
    }

    /** Ensures Retrofit response/error bodies are always closed. */
    private inline fun <T> Response<ResponseBody>.useBody(block: () -> T): T {
        return try {
            block()
        } finally {
            errorBody()?.close()
            body()?.close()
        }
    }
}
