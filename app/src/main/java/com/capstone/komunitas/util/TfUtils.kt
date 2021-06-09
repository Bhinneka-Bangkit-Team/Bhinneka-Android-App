package com.capstone.komunitas.util

import java.nio.ByteBuffer

fun MutableList<Float>.unwindToByteBuffer(inputBuffer: ByteBuffer) {
    inputBuffer.rewind()
    for (f in this) {
        inputBuffer.putFloat(f)
    }
}

fun List<Float>.unwindListToByteBuffer(inputBuffer: ByteBuffer) {
    inputBuffer.rewind()
    for (f in this) {
        inputBuffer.putFloat(f)
    }
}



private const val UTF8_BOM = "\uFEFF"
fun String.removeUTF8BOM(): String {
    return if (this.startsWith(UTF8_BOM)) {
        this.substring(1)
    } else {
        this
    }
}