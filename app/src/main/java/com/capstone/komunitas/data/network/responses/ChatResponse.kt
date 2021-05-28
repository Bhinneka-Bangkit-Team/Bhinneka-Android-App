package com.capstone.komunitas.data.network.responses

import com.capstone.komunitas.data.db.entities.Chat

data class ChatResponse(
    val statusCode: Int? = null,
    val message: String? = null,
    val data: List<Chat>? = null,
    val error: String? = null,
    val isSuccessful: Boolean? = null
)