package com.turismo.tchat

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String,
    val isOwn: Boolean,
    val replyPreview: String? = null
)

data class ChatThread(
    val id: String,
    val title: String,
    var mode: String,
    val endpoint: String? = null,
    val messages: MutableList<ChatMessage> = mutableListOf()
)
