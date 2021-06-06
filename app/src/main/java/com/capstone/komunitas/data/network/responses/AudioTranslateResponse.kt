package com.capstone.komunitas.data.network.responses

class AudioTranslateResponse (
    val statusCode: Int? = null,
    val message: String? = null,
    val data: DataBuffer? = null,
    val error: String? = null
        )

class DataBuffer(
    val type:String? = null,
    val data: Array<Int>? = null
)