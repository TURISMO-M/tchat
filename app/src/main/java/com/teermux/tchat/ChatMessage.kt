package com.teermux.tchat

data class ChatMessage(
    val messageId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: String = "",
    val replyToSender: String? = null,
    val replyToText: String? = null,
    val isLocalOnly: Boolean = false
)
