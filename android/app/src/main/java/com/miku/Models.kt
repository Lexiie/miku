package com.miku.agent

data class AgentRequest(
    val text: String,
    val userId: String = "android_user"
)

data class AgentResponse(
    val text: String,
    val actions: List<AndroidAction> = emptyList()
)

data class AndroidAction(
    val type: String,
    val params: Map<String, Any> = emptyMap()
)

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
