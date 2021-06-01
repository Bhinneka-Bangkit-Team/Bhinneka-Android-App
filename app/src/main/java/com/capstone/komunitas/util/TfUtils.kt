package com.capstone.komunitas.util

import java.nio.ByteBuffer

fun MutableList<Float>.unwindToByteBuffer(inputBuffer: ByteBuffer) {
    inputBuffer.rewind()
    for (f in this) {
        inputBuffer.putFloat(f)
    }
}