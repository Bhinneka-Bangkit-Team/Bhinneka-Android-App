package com.capstone.komunitas.util

import android.graphics.Bitmap
import android.util.Size
import com.google.mediapipe.formats.proto.LandmarkProto
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


// Check confidence by threshold
// Ambil label yang paling banyak muncul (by timing or by frames)
//
fun LandmarkProto.NormalizedLandmarkList.getLandmarkCenter(): List<Float> {
    var xmax: Float = 0F
    var ymax: Float = 0F
    var xmin: Float = 999F
    var ymin: Float = 999F

    var landmarkIndex = 0
    var xcur: Float
    var ycur: Float
    for (landmark: LandmarkProto.NormalizedLandmark in this.getLandmarkList()) {
        xcur = landmark.getX()
        ycur = landmark.getY()

        if (xcur > xmax)
            xmax = xcur
        if (ycur > ymax)
            ymax = ycur
        if (xcur < xmin)
            xmin = xcur
        if (ycur < ymin)
            ymin = ycur
        ++landmarkIndex
    }

    xcur = xmin + (xmax - xmin) / 2
    ycur = ymin + (ymax - ymin) / 2
    val result: List<Float> = listOf(xcur, ycur, xmax-xmin, ymax-ymin, xmax, ymax, xmin, ymin)

    return result
}

fun LandmarkProto.NormalizedLandmarkList.getLandmarkCenterImage(size: Bitmap): List<Float> {
    var xmax: Float = 0F
    var ymax: Float = 0F
    var xmin: Float = 99999F
    var ymin: Float = 99999F

    var landmarkIndex = 0
    var xcur: Float
    var ycur: Float
    for (landmark: LandmarkProto.NormalizedLandmark in this.getLandmarkList()) {
        xcur = landmark.getX()*size.width
        ycur = landmark.getY()*size.height
        if(xcur > size.width || ycur > size.height){
            continue
        }

        if (xcur > xmax)
            xmax = xcur
        if (ycur > ymax)
            ymax = ycur
        if (xcur < xmin)
            if(xcur < 0){
                xmin = 0F
            }else{
                xmin = xcur
            }
        if (ycur < ymin)
            if(ycur < 0){
                ymin = 0F
            }else{
                ymin = ycur
            }
        ++landmarkIndex
    }

    xcur = xmin + (xmax - xmin) / 2
    ycur = ymin + (ymax - ymin) / 2
    val result: List<Float> = listOf(xcur, ycur, xmax-xmin, ymax-ymin, xmax, ymax, xmin, ymin)

    return result
}

typealias Prediction = Pair<String, Float>

fun FloatArray.getTopLabel(associatedAxisLabels: List<String>): List<Prediction> {
    // Get top 10 predictions, sorted
    val predictions = mutableListOf<Prediction>()

    this.forEachIndexed { index, fl ->
        val prediction = associatedAxisLabels?.get(index).toString() to fl
        val (currentLabel, currentLikelihood) = prediction
//            Log.d("currentLabel", currentLabel.toString())
//            Log.d("prediction", prediction.toString())

        if (predictions.size < 10) {
            predictions.add(prediction)
        } else {
            val shouldReplace = predictions.find {
                val (label, likelihood) = it
                likelihood < currentLikelihood
            }

            if (shouldReplace != null) {
                predictions[predictions.indexOf(shouldReplace)] = prediction
            }
        }
    }

    return predictions
}


typealias PredictionInt = Pair<Int, Float>

fun FloatArray.getTopPrediction(): List<PredictionInt> {
    val predictions = mutableListOf<PredictionInt>()

    this.forEachIndexed { index, fl ->
        val prediction = (index + 1) to fl
        val (currentLabel, currentLikelihood) = prediction

        if (predictions.size < 10) {
            predictions.add(prediction)
        } else {
            val shouldReplace = predictions.find {
                val (label, likelihood) = it
                likelihood < currentLikelihood
            }

            if (shouldReplace != null) {
                predictions[predictions.indexOf(shouldReplace)] = prediction
            }
        }
    }
    // log output
    predictions.forEachIndexed { index, prediction ->
        val (label, likelihood) = prediction
    }

    return predictions
}

fun TensorBuffer.mapOutputToLabels(associatedAxisLabels: List<String>) {
    val probabilityProcessor =
        TensorProcessor.Builder().add(NormalizeOp((0).toFloat(), (255).toFloat())).build()

    if (null != associatedAxisLabels) {
        // Map of labels and their corresponding probability
        val labels = TensorLabel(
            associatedAxisLabels!!,
            probabilityProcessor.process(this)
        )

        // Create a map to access the result based on label
        val floatMap = labels.mapWithFloatValue
    }
}