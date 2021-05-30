package com.capstone.komunitas.ui.detection

import com.capstone.komunitas.tflite.Classifier

interface ViewResult {
    fun setViewResult(result: Result<Classifier.Recognition>)
}