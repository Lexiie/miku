package com.miku.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    var messages by mutableStateOf(listOf<Message>())
        private set
    
    var isConnected by mutableStateOf(false)
        private set

    var isConnecting by mutableStateOf(false)
        private set
    
    var agentUrl by mutableStateOf("")
    
    fun connect(url: String) {
        val normalizedUrl = ApiClient.normalizeBaseUrl(url)
        agentUrl = normalizedUrl
        ApiClient.setBaseUrl(normalizedUrl)
        isConnecting = true

        viewModelScope.launch {
            try {
                val api = ApiClient.getApi() ?: error("Agent client unavailable")
                val health = api.health()

                if (health.status == "ok") {
                    isConnected = true
                    addMessage("✅ Connected to agent", false)
                } else {
                    isConnected = false
                    addMessage("❌ Agent health check failed", false)
                }
            } catch (e: Exception) {
                isConnected = false
                addMessage("❌ Connection error: ${e.message}", false)
            } finally {
                isConnecting = false
            }
        }
    }
    
    fun disconnect() {
        isConnected = false
        isConnecting = false
        addMessage("❌ Disconnected", false)
    }
    
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
                addMessage("❌ Error: ${e.message}", false)
            }
        }
    }
    
    private fun addMessage(text: String, isUser: Boolean) {
        messages = messages + Message(text, isUser)
    }
}
