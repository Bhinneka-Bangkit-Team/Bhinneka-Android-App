package com.capstone.komunitas.views.camera_detection

import android.graphics.*
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import com.capstone.komunitas.R
import com.capstone.komunitas.tflite.Classifier
import com.capstone.komunitas.tflite.TFLiteModel
import com.capstone.komunitas.tracking.BorderedText
import com.capstone.komunitas.tracking.MultiBoxTracker
import com.capstone.komunitas.utils.Utils
import com.capstone.komunitas.views.OverlayView
import java.io.IOException
import java.util.*

class DetectionObjectActivity : CameraActivity(), ImageReader.OnImageAvailableListener {



    internal lateinit var trackingOverlay: OverlayView
    private var sensorOrientation: Int? = null

    private var detector: Classifier? = null

    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null

    private var computingDetection = false

    private var timestamp: Long = 0

    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null

    private var tracker: MultiBoxTracker? = null
    private var borderedText: BorderedText? = null


    override val desiredPreviewFrameSize: Size
        get() =DESIRED_PREVIEW_SIZE

    override val layoutId: Int
        get() = R.layout.fragment_camera_connection

    override fun onPreviewSizeChosen(size: Size, cameraRotation: Int) {
        tracker = MultiBoxTracker(this)
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
        borderedText = BorderedText(textSizePx)
        borderedText?.setTypeface(Typeface.MONOSPACE)

        tracker = MultiBoxTracker(this)
        var cropSize =TF_OD_API_INPUT_SIZE
        try {
            detector = TFLiteModel.create(
                assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
            cropSize = TF_OD_API_INPUT_SIZE
        }catch (e: IOException){
            e.printStackTrace()
            Log.e(TAG, "onPreviewSizeChosen: $e")
            Toast.makeText(this,"Classifier not found", Toast.LENGTH_LONG).show()
            finish()
        }

        preHeight = size.height
        preWidth = size.width
        sensorOrientation =cameraRotation  - screenOrientation
        rgbFrameBitmap = Bitmap.createBitmap(preWidth, preHeight, Bitmap.Config.ARGB_8888)
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)

        frameToCropTransform = Utils.getTransformationMatrix(preWidth,preHeight,cropSize,cropSize,
            sensorOrientation!!, MAINTAIN_ASPECT
        )
        cropToFrameTransform = Matrix()
        frameToCropTransform?.invert(cropToFrameTransform)
        trackingOverlay = findViewById(R.id.tracking_overlay) as OverlayView
        trackingOverlay.addCallback(object :OverlayView.DrawCallback{
            override fun drawCallback(canvas: Canvas) {
                tracker?.draw(canvas)
                if (isDebug)
                    tracker?.drawDebug(canvas)
            }

        })
        tracker?.setFrameConfiguration(preWidth,preHeight,sensorOrientation!!)
    }

    override fun processImage(textDetection: TextView) {
        timestamp = timestamp+1
        val currentTime = timestamp
        trackingOverlay.postInvalidate()

        if (computingDetection){
            readyForNextImage()
            return
        }

        computingDetection = true
        rgbFrameBitmap?.setPixels(getRgbBytes(),0,preWidth,0,0,preWidth,preHeight)
        readyForNextImage()

        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!,frameToCropTransform!!,null)
        if (SAVE_PREVIEW_BITMAP){
            Utils.saveBitmap(croppedBitmap!!)
        }

        val runable = Runnable {
            Log.i(TAG, "processImage: $currentTime")
            val results = detector?.recognizeImage(croppedBitmap!!)
            val startTime = SystemClock.uptimeMillis()
            lastProcessingTimeMs = SystemClock.uptimeMillis()-startTime
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
            val canvas= Canvas(cropCopyBitmap!!)
            val paint = Paint()
            paint.color  = Color.WHITE
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 1.0f

            var minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API
            when (MODE) {
                DetectorMode.TF_OD_API -> minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API
            }

            val mappedRecognitions = LinkedList<Classifier.Recognition>()

            if (results != null) {
                for (result in results) {
                    val location = result.location
                    if (location != null && result.confidence >= minimumConfidence) {
                        canvas.drawRect(location, paint)
                        cropToFrameTransform?.mapRect(location)
                        result.location = location
                        mappedRecognitions.add(result)
                        textDetection.setText(result.title)
                    }
                }
            }

            tracker?.trackResults(mappedRecognitions,currentTime)
            trackingOverlay.postInvalidate()
            computingDetection = false
        }

        runInBackground(runable)
    }



    override fun setNumThreads(numThreads: Int) {
        runInBackground(Runnable { detector?.setNumThreads(numThreads) })
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        runInBackground(Runnable { detector?.setUseNNAPI(isChecked) })
    }

    private enum class DetectorMode {
        TF_OD_API
    }

    companion object{
        private const val TAG ="DetectionObjectActivity"
        private val TF_OD_API_INPUT_SIZE = 300
        private val TF_OD_API_IS_QUANTIZED = true
        private val TF_OD_API_MODEL_FILE = "detect.tflite"
        private val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"
        private val MODE = DetectorMode.TF_OD_API
        private val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
        private val MAINTAIN_ASPECT = false
        private val DESIRED_PREVIEW_SIZE = Size(640, 480)
        private val SAVE_PREVIEW_BITMAP = false
        private val TEXT_SIZE_DIP = 8.0f
    }
}
