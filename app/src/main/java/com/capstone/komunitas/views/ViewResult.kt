package com.capstone.komunitas.views

import com.capstone.komunitas.tflite.Classifier

interface ViewResult {
    fun setViewResult(result: Result<Classifier.Recognition>)
}