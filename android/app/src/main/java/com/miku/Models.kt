package com.miku.agent

/** Request payload posted by Android app to `/api/chat`. */
data class AgentRequest(
    val text: String,
    val userId: String = "android_user"
)

/** Structured response from backend parser/conversation orchestrator. */
data class AgentResponse(
    val text: String,
    val actions: List<AndroidAction> = emptyList()
)

/** Device action envelope consumed by [AutomationExecutor]. */
data class AndroidAction(
    val type: String,
    val params: Map<String, Any> = emptyMap()
)

/** Chat timeline item rendered by Compose UI. */
data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
