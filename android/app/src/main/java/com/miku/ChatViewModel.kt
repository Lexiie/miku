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
    
    var agentUrl by mutableStateOf("")
    
    fun connect(url: String) {
        agentUrl = url
        ApiClient.setBaseUrl(url)
        isConnected = true
        addMessage("✅ Connected to agent", false)
    }
    
    fun disconnect() {
        isConnected = false
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
                    val result = executor.execute(action)
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
