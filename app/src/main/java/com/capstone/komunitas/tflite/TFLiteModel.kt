package com.capstone.komunitas.tflite

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Trace
import android.util.ArrayMap
import org.tensorflow.lite.Interpreter
import java.io.*
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList

class TFLiteModel private constructor():Classifier{
    override val statString: String
        get() = TODO("Not yet implemented")

    private var isQuantized:Boolean= false
    private var inputSize:Int = 0
    private val labels = Vector<String>()
    private var intValues: IntArray? = null
    private var outputLocations: Array<Array<FloatArray>>? = null
    private var outputClasses: Array<FloatArray>? = null
    private var outputScores: Array<FloatArray>? = null
    private var numDetections: FloatArray? = null
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null

    override fun recognizeImage(bitmap: Bitmap): List<Classifier.Recognition> {
        Trace.beginSection("recognizeImage")

        Trace.beginSection("preprocessBitmap")
        bitmap.getPixels(intValues,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        imgData?.rewind()
        for (i in 0 until inputSize){
            for (j in 0 until inputSize){
                val pixelValue = intValues?.get(i * inputSize + j)
                if(isQuantized){
                    if (pixelValue != null) {
                        imgData?.put((pixelValue shr 16 and 0xFF).toByte())
                        imgData?.put((pixelValue shr 8 and 0xFF).toByte())
                        imgData?.put((pixelValue and 0xFF).toByte())
                    }
                }else{
                    if (pixelValue != null) {
                        imgData?.putFloat(((pixelValue shr 16 and 0xFF)- IMAGE_MEAN)/ IMAGE_STD)
                        imgData?.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN)/ IMAGE_STD)
                        imgData?.putFloat(((pixelValue and 0xFF)-IMAGE_MEAN)/IMAGE_STD)
                    }
                }
            }
        }
        Trace.endSection()
        Trace.beginSection("feed")

        outputLocations = Array(1) { Array(NUM_DETECTION) { FloatArray(4) } }
        outputClasses = Array(1) { FloatArray(NUM_DETECTION) }
        outputScores = Array(1) { FloatArray(NUM_DETECTION) }
        numDetections = FloatArray(1)

        val inputArray = arrayOf<Any>(imgData!!)
        val outputMap = ArrayMap<Int, Any>()
        outputMap.put(0,outputLocations!!)
        outputMap.put(1,outputClasses!!)
        outputMap.put(2,outputScores!!)
        outputMap.put(3,numDetections!!)
        Trace.endSection()

        Trace.beginSection("run")
        tfLite?.runForMultipleInputsOutputs(inputArray,outputMap)

        val recognition = ArrayList<Classifier.Recognition>(NUM_DETECTION)
        for (i in 0 until NUM_DETECTION){
            val left = outputLocations?.get(0)?.get(i)?.get(1)?.times(inputSize)
            val top = outputLocations?.get(0)?.get(i)?.get(0)?.times(inputSize)
            val right = outputLocations?.get(0)?.get(i)?.get(3)?.times(inputSize)
            val bottom = outputLocations?.get(0)?.get(i)?.get(2)?.times(inputSize)
            val detection = RectF(left!!,top!!,right!!,bottom!!)

            val labelOffset = 1
            recognition.add(
                Classifier.Recognition(
                    "" + i,
                    labels[outputClasses!![0][i].toInt() + labelOffset],
                    outputScores!![0][i],
                    detection))
        }
        Trace.endSection()
        return recognition
    }

    override fun enableStatLogging(debug: Boolean) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun setNumThreads(numThreads: Int) {
        if (tfLite != null) tfLite!!.setNumThreads(numThreads)
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        if (tfLite != null) tfLite!!.setUseNNAPI(isChecked)
    }

    companion object{
        private val NUM_DETECTION = 10
        private val IMAGE_MEAN = 128.0f
        private val IMAGE_STD = 128.0f
        private val NUMBER_THREAD = 4
        private val USE_NNAPI = false

        @Throws(IOException::class)
        private fun loadModelFile(asset: AssetManager, modelFilename:String): MappedByteBuffer {
            val fileDesc = asset.openFd(modelFilename)
            val inputStream = FileInputStream(fileDesc.fileDescriptor)
            val fileChan = inputStream.channel
            val start = fileDesc.startOffset
            val declaration = fileDesc.declaredLength
            return fileChan.map(FileChannel.MapMode.READ_ONLY,start,declaration)
        }

        @Throws(IOException::class)
        fun create(asset: AssetManager, modelFilename: String, lableFilename:String, inputSize: Int, isQuantized:Boolean):Classifier{
            val tfLiteModel = TFLiteModel()
            var labelInp: InputStream? = null
            val actual = lableFilename.split("file:///android_asset/".toRegex()).
            dropLastWhile {
                it.isEmpty()
            }.toTypedArray()[1]
            labelInp = asset.open(actual)
            val buffered: BufferedReader? = BufferedReader(InputStreamReader(labelInp))
            while (buffered?.readLine()?.let { tfLiteModel.labels.add(it) } != null);
            buffered?.close()

            tfLiteModel.inputSize = inputSize

            try {
                val option = Interpreter.Options()
                option.setNumThreads(NUMBER_THREAD)
                option.setUseNNAPI(USE_NNAPI)
                tfLiteModel.tfLite= Interpreter(loadModelFile(asset,modelFilename),option)
            }catch (e:Exception){
                throw RuntimeException(e)
            }

            tfLiteModel.isQuantized = isQuantized
            val numBytesPerChannel: Int
            if (isQuantized) {
                numBytesPerChannel = 1 // Quantized
            } else {
                numBytesPerChannel = 4 // Floating point
            }

            tfLiteModel.imgData = ByteBuffer.allocateDirect(1 * tfLiteModel.inputSize * tfLiteModel.inputSize * 3 * numBytesPerChannel)
            tfLiteModel.imgData!!.order(ByteOrder.nativeOrder())
            tfLiteModel.intValues = IntArray(tfLiteModel.inputSize * tfLiteModel.inputSize)

            tfLiteModel.outputLocations = Array(1) { Array(NUM_DETECTION) { FloatArray(4) } }
            tfLiteModel.outputClasses = Array(1) { FloatArray(NUM_DETECTION) }
            tfLiteModel.outputScores = Array(1) { FloatArray(NUM_DETECTION) }
            tfLiteModel.numDetections = FloatArray(1)
            return tfLiteModel
        }
    }
}