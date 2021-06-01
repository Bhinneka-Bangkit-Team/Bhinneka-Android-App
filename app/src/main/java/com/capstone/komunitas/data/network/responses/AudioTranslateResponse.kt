package com.capstone.komunitas.data.network.responses

import java.nio.Buffer

class AudioTranslateResponse (
    val statusCode: Int? = null,
    val message: String? = null,
    val data: dataBuffer? = null,
    val error: String? = null
        )

class dataBuffer(
    val type:String? = null,
    val data: Buffer? = null
)