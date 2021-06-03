package com.capstone.komunitas.ui.chat

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
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
import com.google.mediapipe.framework.*
import com.google.mediapipe.glutil.EglManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.progress_bar_chat_novideo
import kotlinx.android.synthetic.main.activity_chat_with_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

    // MediaPipe things
    companion object {
        private val TAG = "MainActivity"
        private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
        private val INPUT_VIDEO_STREAM_NAME = "input_video"
        private val OUTPUT_VIDEO_STREAM_NAME = "throttled_input_video"
        private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
        private val INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands"
        private val NUM_HANDS = 2
        private val FLIP_FRAMES_VERTICALLY = true

        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }
    private var lensFacing: CameraHelper.CameraFacing = CameraHelper.CameraFacing.FRONT
    private var isSurfaceAttached: Boolean = false
    private var previewFrameTexture: SurfaceTexture? = null
    private var previewDisplayView: SurfaceView? = null
    private var eglManager: EglManager? = null
    private var processor: FrameProcessor? = null
    private var converter: ExternalTextureConverter? = null
    private var cameraHelper: CameraXPreviewHelper? = null
    private var mpImageBitmap: Bitmap? = null
    private var viewSize: Size? = null

    // TFLite things
    val tfliteInterpreter by lazy {
        Interpreter(loadModelFile())
    }
    val tfliteInputBuffer = ByteBuffer.allocateDirect(4 * 42 * 1)
        .apply { order(nativeOrder()) }
    val ASSOCIATED_AXIS_LABELS = "sign_language_v1.txt"
    var associatedAxisLabels: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityChatWithVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_with_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.chatListener = this

        bindUI(viewModel)
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
        val fileDescriptor: AssetFileDescriptor = assets.openFd("Final_model_default.tflite")
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
        PermissionHelper.checkAndRequestAudioPermissions(this)
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
//            inferenceHandLandmarks(multiHandLandmarks)
            if(mpImageBitmap!=null){
                inferenceImageLandmarks(mpImageBitmap!!, multiHandLandmarks)
            }
        }
        processor!!.addPacketCallback(
            "throttled_input_video_cpu"
        ) { packet: Packet ->
            Log.v(
                TAG,
                "Received input_image_cpu packet."
            )
            mpImageBitmap = AndroidPacketGetter.getBitmapFromRgba(packet)
        }
    }

    private fun inferenceImageLandmarks(imageInput: Bitmap, multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>) {
        if (multiHandLandmarks.isEmpty()) {
            return
        }
        if(viewSize == null){
            return
        }

        var handIndex = 0
        for (landmarks: LandmarkProto.NormalizedLandmarkList in multiHandLandmarks) {
            var resultList: MutableList<Float> = ArrayList()
            var centerValues = landmarks.getLandmarkCenterImage(imageInput)
            Log.d("imageInput.width", imageInput.width.toString())
            Log.d("imageInput.height", imageInput.height.toString())
            Log.d("centerValues", centerValues.toString())
            var resizedImage = Bitmap.createBitmap(imageInput, centerValues[6].toInt(), centerValues[7].toInt(), centerValues[2].toInt(), centerValues[3].toInt())
            // FOR DEBUGGING BITMAP
            Log.d("resizedImage.width", resizedImage.width.toString())
            Log.d("resizedImage.height", resizedImage.height.toString())
            this.runOnUiThread {
                bitmap_preview.setImageBitmap(resizedImage)
            }
//            inferenceImageHand(resizedImage)
            ++handIndex
        }
    }

    private fun inferenceImageHand(imageInput: Bitmap) {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(150, 150, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage(DataType.UINT8)

        tensorImage.load(imageInput)
        tensorImage = imageProcessor.process(tensorImage)

        val probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)
        tfliteInterpreter.run(tensorImage.buffer, probabilityBuffer)

        val probabilityProcessor = TensorProcessor.Builder().add(NormalizeOp(0F, 255F)).build()

        val labels = TensorLabel(
            associatedAxisLabels!!,
            probabilityProcessor.process(probabilityBuffer)
        )
        Log.d("labels", labels.toString())

        val floatMap = labels.mapWithFloatValue
    }

    private fun inferenceHandLandmarks(multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>) {

        if (multiHandLandmarks.isEmpty()) {
            return
        }

        var handIndex = 0
        for (landmarks: LandmarkProto.NormalizedLandmarkList in multiHandLandmarks) {
            var resultList: MutableList<Float> = ArrayList()
            var centerValues = landmarks.getLandmarkCenter()

            var landmarkIndex = 0
            for (landmark: LandmarkProto.NormalizedLandmark in landmarks.getLandmarkList()) {
                resultList.add(landmark.getX()-centerValues[0])
                resultList.add(landmark.getY()-centerValues[1])
                ++landmarkIndex
            }
            Log.d("resultList", resultList.toString())
            Log.d("resultList size", resultList.size.toString())

//            val probabilityBuffer =
//                TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)
//            resultList.unwindToByteBuffer(tfliteInputBuffer)
//            tfliteInterpreter.run(tfliteInputBuffer, probabilityBuffer.buffer)
//            mapOutputToLabels(probabilityBuffer)

            val outputArray = arrayOf(FloatArray(36))
            resultList.unwindToByteBuffer(tfliteInputBuffer)
            tfliteInterpreter.run(tfliteInputBuffer, outputArray)
            var labelResult = outputArray[0].getTopLabel(associatedAxisLabels!!)
            Log.d("labelResult", labelResult.toString())

            val (label, likelihood) = labelResult[0]
            this.runOnUiThread {
                if(label.length==1){
                    tv_preview_isyarat.text = tv_preview_isyarat.text.toString().plus(label)
                }else{
                    tv_preview_isyarat.text = tv_preview_isyarat.text.toString().plus(" ").plus(label)
                }
                if(tv_preview_isyarat.text.length > 100){
                    tv_preview_isyarat.text = label
                }
            }
            ++handIndex
        }
    }

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
        viewSize = computeViewSize(width, height)
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