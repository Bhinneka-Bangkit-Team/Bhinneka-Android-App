package com.capstone.komunitas.data.network.responses

import com.capstone.komunitas.data.db.entities.Chat

data class AudioResponse(
    val statusCode: Int? = null,
    val message: String? = null,
    val data: String? = null,
    val error: String? = null
)
