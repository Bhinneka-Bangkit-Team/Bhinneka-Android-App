package com.capstone.komunitas.ui.chat

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ActivityChatWithVideoBinding
import com.capstone.komunitas.util.*
import com.google.mediapipe.components.*
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.AndroidPacketCreator
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.progress_bar_chat_novideo
import kotlinx.android.synthetic.main.activity_chat_with_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder.nativeOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


class ChatWithVideoActivity : AppCompatActivity(), ChatListener, KodeinAware {

    override val kodein by kodein()
    private val factory: ChatViewModelFactory by instance()

    companion object {
        private val TAG = "MainActivity"
        private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
        private val INPUT_VIDEO_STREAM_NAME = "input_video"
        private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
        private val INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands"
        private val NUM_HANDS = 2
        private val FLIP_FRAMES_VERTICALLY = true

        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    // TFLite things
    val tfliteInterpreter by lazy {
        Interpreter(loadModelFile())
    }
    val tfliteInputBuffer = ByteBuffer.allocateDirect(4 * 42 * 1)
        .apply { order(nativeOrder()) }
    val ASSOCIATED_AXIS_LABELS = "sign_language_v1.txt"
    var associatedAxisLabels: List<String>? = null


    // MediaPipe things
    private var lensFacing: CameraHelper.CameraFacing = CameraHelper.CameraFacing.FRONT
    private var isSurfaceAttached: Boolean = false
    private var previewFrameTexture: SurfaceTexture? = null
    private var previewDisplayView: SurfaceView? = null
    private var eglManager: EglManager? = null
    private var processor: FrameProcessor? = null
    private var converter: ExternalTextureConverter? = null
    private var cameraHelper: CameraXPreviewHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityChatWithVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_with_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.chatListener = this

        bindUI(viewModel)
//        bindCamera(viewModel.lensFacing)
        initMediaPipe()
        bindAppBar(viewModel)
    }

    private fun loadLabelFile() {
        try {
            associatedAxisLabels = FileUtil.loadLabels(this, ASSOCIATED_AXIS_LABELS)
        } catch (e: IOException) {
            Log.e("tfliteSupport", "Error reading label file", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        loadLabelFile()
        val fileDescriptor: AssetFileDescriptor = assets.openFd("sign_language_v1.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun initMediaPipe() {
        previewDisplayView = SurfaceView(this)
        setupPreviewDisplayView()
        AndroidAssetUtil.initializeNativeAssetManager(this)
        eglManager = EglManager(null)
        processor = FrameProcessor(
            this,
            eglManager!!.getNativeContext(),
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        )
        processor!!
            .getVideoSurfaceOutput()
            .setFlipY(FLIP_FRAMES_VERTICALLY)
        PermissionHelper.checkAndRequestCameraPermissions(this)
        val packetCreator: AndroidPacketCreator = processor!!.getPacketCreator()
        val inputSidePackets: MutableMap<String, Packet> = HashMap<String, Packet>()
        inputSidePackets[INPUT_NUM_HANDS_SIDE_PACKET_NAME] =
            packetCreator.createInt32(NUM_HANDS)
        processor!!.setInputSidePackets(inputSidePackets)

        processor!!.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet ->
            val multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList> =
                PacketGetter.getProtoVector(
                    packet,
                    LandmarkProto.NormalizedLandmarkList.parser()
                )
            inferenceHandLandmarks(multiHandLandmarks)
        }
    }

    private fun inferenceHandLandmarks(multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>) {

        if (multiHandLandmarks.isEmpty()) {
            return
        }

        var handIndex = 0
        for (landmarks: LandmarkProto.NormalizedLandmarkList in multiHandLandmarks) {
            var resultList: MutableList<Float> = ArrayList()

            var landmarkIndex = 0
            for (landmark: LandmarkProto.NormalizedLandmark in landmarks.getLandmarkList()) {
                resultList.add(landmark.getX())
                resultList.add(landmark.getY())
                ++landmarkIndex
            }

//            val probabilityBuffer =
//                TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)
//            resultList.unwindToByteBuffer(tfliteInputBuffer)
//            tfliteInterpreter.run(tfliteInputBuffer, probabilityBuffer.buffer)
//            mapOutputToLabels(probabilityBuffer)

            val outputArray = arrayOf(FloatArray(36))
            resultList.unwindToByteBuffer(tfliteInputBuffer)
            tfliteInterpreter.run(tfliteInputBuffer, outputArray)
            var labelResult = getTopLabel(outputArray[0])
            val (label, likelihood) = labelResult[0]
            this.runOnUiThread {
                tv_preview_isyarat.text = label
            }
            ++handIndex
        }
    }

    fun getTopLabel(output: FloatArray): List<Prediction> {
        // Get top 10 predictions, sorted
        val predictions = mutableListOf<Prediction>()

        output.forEachIndexed { index, fl ->
            val prediction = associatedAxisLabels?.get(index).toString() to fl
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

        return predictions
    }

//    fun mapOutputToLabels(probabilityBuffer: TensorBuffer) {
//        val probabilityProcessor = TensorProcessor.Builder().add(NormalizeOp((0).toFloat(), (255).toFloat())).build()
//
//        if (null != associatedAxisLabels) {
//            // Map of labels and their corresponding probability
//            val labels = TensorLabel(
//                associatedAxisLabels!!,
//                probabilityProcessor.process(probabilityBuffer)
//            )
//
//            // Create a map to access the result based on label
//            val floatMap = labels.mapWithFloatValue
//        }
//    }

//    fun getTopPrediction(output: FloatArray): List<Prediction> {
//        val predictions = mutableListOf<Prediction>()
//
//        output.forEachIndexed { index, fl ->
//            val prediction = (index + 1) to fl
//            val (currentLabel, currentLikelihood) = prediction
//
//            if (predictions.size < 10) {
//                predictions.add(prediction)
//            } else {
//                val shouldReplace = predictions.find {
//                    val (label, likelihood) = it
//                    likelihood < currentLikelihood
//                }
//
//                if (shouldReplace != null) {
//                    predictions[predictions.indexOf(shouldReplace)] = prediction
//                }
//            }
//        }
//        // log output
//        predictions.forEachIndexed { index, prediction ->
//            val (label, likelihood) = prediction
//            Log.d("TensorFlow Interpreter", "${index + 1}. Prediction: $prediction, Likelihood: $likelihood")
//        }
//
//        return predictions
//    }

    override fun onResume() {
        super.onResume()
        isSurfaceAttached = false
        converter = ExternalTextureConverter(
            eglManager?.getContext(), 2
        )
        converter!!.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter!!.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        converter?.close()

        previewDisplayView!!.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected fun onCameraStarted(surfaceTexture: SurfaceTexture?) {
        previewFrameTexture = surfaceTexture
        previewDisplayView!!.visibility = View.VISIBLE
    }

    protected fun cameraTargetResolution(): Size? {
        return null // No preference and let the camera (helper) decide.
    }

    fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper!!.setOnCameraStartedListener(
            { surfaceTexture -> onCameraStarted(surfaceTexture) })
        val cameraFacing: CameraHelper.CameraFacing = lensFacing
        cameraHelper!!.startCamera(
            this, cameraFacing,  /*unusedSurfaceTexture=*/null, cameraTargetResolution()
        )
    }

    protected fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height)
    }

    protected fun onPreviewDisplaySurfaceChanged(
        holder: SurfaceHolder?, format: Int, width: Int, height: Int
    ) {
        val viewSize = computeViewSize(width, height)
        val displaySize: Size = cameraHelper!!.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated: Boolean = cameraHelper!!.isCameraRotated()

        if (isSurfaceAttached) {
            previewFrameTexture!!.detachFromGLContext()
        }
        isSurfaceAttached = true
        converter!!.setSurfaceTextureAndAttachToGLContext(
            previewFrameTexture,
            if (isCameraRotated) displaySize.height else displaySize.width,
            if (isCameraRotated) displaySize.width else displaySize.height
        )
    }

    private fun setupPreviewDisplayView() {
        previewDisplayView!!.visibility = View.GONE
        val viewGroup = findViewById<ViewGroup>(R.id.preview_display_layout)
        viewGroup.addView(previewDisplayView)
        previewDisplayView!!
            .getHolder()
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        processor!!.getVideoSurfaceOutput().setSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        onPreviewDisplaySurfaceChanged(holder, format, width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        processor?.getVideoSurfaceOutput()!!.setSurface(null)
                    }
                })
    }

    private fun getMultiHandLandmarksDebugString(multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>): String {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks"
        }
        var multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size + "\n"
        var handIndex = 0
        for (landmarks: LandmarkProto.NormalizedLandmarkList in multiHandLandmarks) {
            multiHandLandmarksStr += "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n"
            var landmarkIndex = 0
            for (landmark: LandmarkProto.NormalizedLandmark in landmarks.getLandmarkList()) {
                multiHandLandmarksStr += ("\t\tLandmark ["
                        + landmarkIndex
                        + "]: ("
                        + landmark.getX()
                        + ", "
                        + landmark.getY()
                        + ", "
                        + landmark.getZ()
                        + ")\n")
                ++landmarkIndex
            }
            ++handIndex
        }
        return multiHandLandmarksStr
    }

    // Keyboard open/close listener
//    override fun onResume() {
//        super.onResume()
//        KeyboardEventListener(this) { isOpen ->
//            if(isOpen){
//                layout_control_withvid.hide()
//            }else{
//                layout_control_withvid.show()
//            }
//        }
//    }

    fun bindAppBar(viewModel: ChatViewModel) {
        // Back listener
        chat_with_vid_appbar.setNavigationOnClickListener {
            onBack()
        }
        // Menu listener
        chat_with_vid_appbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.flip_camera -> {
                    viewModel.changeLens()
                    false
                }
                else -> false
            }
        }
    }

//    fun bindCamera(lensFacing: Int) {
//        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener(Runnable {
//            val cameraProvider = cameraProviderFuture.get()
//            cameraProvider.unbindAll()
//            bindPreview(cameraProvider, lensFacing)
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    fun bindPreview(cameraProvider: ProcessCameraProvider, lensFacing: Int) {
//        val preview: Preview = Preview.Builder()
//            .build()
//
//        val cameraSelector: CameraSelector = CameraSelector.Builder()
//            .requireLensFacing(lensFacing)
//            .build()
//
//        preview.setSurfaceProvider(previewView.getSurfaceProvider())
//
//        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
//    }

    private fun bindUI(viewModel: ChatViewModel) = Coroutines.main {
        progress_bar_chat_novideo.show()
        viewModel.chats.await().observe(this, Observer {
            progress_bar_chat_novideo.hide()
            initRecyclerView(it)
        })
    }

    private fun initRecyclerView(chatItem: List<Chat>) {
        val groupAdapter = GroupieAdapter()

        chatItem.forEach {
            if (it.isSpeaker == 1) {
                groupAdapter.add(ChatReceivedItem(it))
            } else {
                groupAdapter.add(ChatSentItem(it))
            }
        }

        messages_rv_chat_withvid.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = groupAdapter
        }
        messages_rv_chat_withvid.scrollToPosition(groupAdapter.itemCount - 1)
    }

    override fun onGetStarted() {
        toast("Started")
    }

    override fun onGetSuccess(chats: List<Chat>) {
        toast("Success")
    }

    override fun onGetFailure(message: String) {
        toast(message)
    }

    override fun onSendStarted() {
        toast("Started")
    }

    override fun onSendSuccess(message: String) {
        toast(message)
    }

    override fun onSendFailure(message: String) {
        toast(message)
    }

    override fun onBack() {
        this.finish()
    }

    override fun onChangeLens() {
        if (CameraHelper.CameraFacing.FRONT == lensFacing) {
            lensFacing = CameraHelper.CameraFacing.BACK
            restartCamera()
        } else {
            lensFacing = CameraHelper.CameraFacing.FRONT
            restartCamera()
        }
    }

    private fun restartCamera() {
        onPause()
        onResume()
    }

    override fun onRecordPressed(isRecording: Boolean) {
        if (isRecording) {
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_stop_white)
        } else {
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_record_white)
        }
    }
}

typealias Prediction = Pair<String, Float>