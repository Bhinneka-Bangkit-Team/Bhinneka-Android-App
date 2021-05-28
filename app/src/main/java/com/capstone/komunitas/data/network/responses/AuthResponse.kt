package com.capstone.komunitas.data.network.responses

import com.capstone.komunitas.data.db.entities.User

data class AuthResponse(
    val statusCode: Int? = null,
    val message: String? = null,
    val data: User? = null,
    val error: String? = null,
    val accessToken: String? = null,
    val isSuccessful: Boolean? = null
)