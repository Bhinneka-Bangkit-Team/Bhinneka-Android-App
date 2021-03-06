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
import com.google.mediapipe.formats.proto.DetectionProto
import com.google.mediapipe.formats.proto.DetectionProto.Detection
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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
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

    // MediaPipe things
    companion object {
        private val TAG = "MainActivity"
        private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
        private val INPUT_VIDEO_STREAM_NAME = "input_video"

        private val OUTPUT_VIDEO_STREAM_NAME = "throttled_input_video"

        //        private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
        private val OUTPUT_HANDEDNESS_STREAM_NAME = "handedness"
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
    private var handedness: List<DetectionProto.Detection>? = null
    private var viewSize: Size? = null

    // TFLite things
    private val tfliteInterpreter by lazy {
        Interpreter(loadModelFile())
    }
    private val tfliteInputBuffer = ByteBuffer.allocateDirect(4 * 84 * 1)
        .apply { order(nativeOrder()) }
    private val ASSOCIATED_AXIS_LABELS = "sign_language_v1.txt"
    private val TFLITE_MODEL = "sign_language_v1.tflite"
    private lateinit var associatedAxisLabels: MutableList<String>

    // Inferencing logic
    private var tobeSubmitted = ""
    private var lastLabel = "-"
    private var submittedLabel = "-"
    private var time = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel binding
        val binding: ActivityChatWithVideoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_chat_with_video)
        val viewModel = ViewModelProviders.of(this, factory).get(ChatViewModel::class.java)
        binding.viewmodel = viewModel

        viewModel.chatListener = this

        bindUI(viewModel)
        initMediaPipe(viewModel)
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
        val fileDescriptor: AssetFileDescriptor = assets.openFd(TFLITE_MODEL)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun initMediaPipe(viewModel: ChatViewModel) {
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
            OUTPUT_HANDEDNESS_STREAM_NAME
        ) { packet: Packet ->
            Log.d(TAG, "Recieved handeness")
            handedness = PacketGetter.getProtoVector(packet, Detection.parser())
        }
        processor!!.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet ->
            val multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList> =
                PacketGetter.getProtoVector(
                    packet,
                    LandmarkProto.NormalizedLandmarkList.parser()
                )
            if (handedness != null) {
                inferenceHandLandmarks(multiHandLandmarks, viewModel, handedness!!)
            }
//            if(mpImageBitmap!=null){
//                inferenceImageLandmarks(mpImageBitmap!!, multiHandLandmarks, viewModel)
//            }
        }
        processor!!.addPacketCallback(
            "throttled_input_video_cpu"
        ) { packet: Packet ->
//            mpImageBitmap = AndroidPacketGetter.getBitmapFromRgba(packet)
            if (System.currentTimeMillis() - time >= 1000) {
                time = System.currentTimeMillis()
                viewModel.sendMessage(tobeSubmitted, 0)
                if (tobeSubmitted.length != 0) {
                    viewModel.newMessageText.set("")
                }
                tobeSubmitted = ""
                lastLabel = ""
                submittedLabel = ""
            }
        }
    }

    private fun inferenceImageLandmarks(
        imageInput: Bitmap,
        multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>,
        viewModel: ChatViewModel
    ) {
        if (multiHandLandmarks.isEmpty()) {
            return
        }
        if (viewSize == null) {
            return
        }

        var resultList: MutableList<Float> = ArrayList()
        val centerValues = multiHandLandmarks.getLandmarkCenterImage(imageInput)

        Log.d("imageInput.width", imageInput.width.toString())
        Log.d("imageInput.height", imageInput.height.toString())
        Log.d("centerValues", centerValues.toString())
        val resizedImage = Bitmap.createBitmap(
            imageInput,
            centerValues[6].toInt(),
            centerValues[7].toInt(),
            centerValues[2].toInt(),
            centerValues[3].toInt()
        )
        // FOR DEBUGGING BITMAP
        Log.d("resizedImage.width", resizedImage.width.toString())
        Log.d("resizedImage.height", resizedImage.height.toString())
        this.runOnUiThread {
            bitmap_preview.setImageBitmap(resizedImage)
        }
        inferenceImageHand(resizedImage, viewModel)
    }

    private fun inferenceImageHand(imageInput: Bitmap, viewModel: ChatViewModel) {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage(DataType.UINT8)

        tensorImage.load(imageInput)
        tensorImage = imageProcessor.process(tensorImage)

        val probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.FLOAT32)
        val outputArray = arrayOf(FloatArray(19))

        tfliteInterpreter.run(tensorImage.buffer, outputArray)
        val labelResult = outputArray[0].getTopLabel(associatedAxisLabels!!)
        Log.d("labelResult", labelResult.toString())
        val (label, likelihood) = labelResult[0]

        this.runOnUiThread {
            if (label.length == 1) {
                tv_preview_isyarat.text = tv_preview_isyarat.text.toString().plus(label)
            } else {
                tv_preview_isyarat.text = tv_preview_isyarat.text.toString().plus(" ").plus(label)
            }
            if (tv_preview_isyarat.text.length > 100) {
                tv_preview_isyarat.text = label
            }
            viewModel.newMessageText.set(label)
        }
    }

    private fun decisionAlgorithm(currentLabel: String) {
        // If the label is the same as the last one
        if (currentLabel.equals(lastLabel)) {

        }
    }

    private fun inferenceHandLandmarks(
        multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList>,
        viewModel: ChatViewModel,
        handedness: List<Detection>
    ) {

        if (multiHandLandmarks.isEmpty()) {
            return
        }
        var fullLandmarksData: List<Float> = ArrayList()
        var rightHandLandmark: MutableList<Float> = MutableList(42) { 0F }
        var leftHandLandmark: MutableList<Float> = MutableList(42) { 0F }

        var isLeftFound: Boolean = false
        var isRightFound: Boolean = false

        var handIndex = 0
        // If the number of hand and handedness is not the same
        if (multiHandLandmarks.size != handedness.size) {
            return
        }

        for (landmarks: LandmarkProto.NormalizedLandmarkList in multiHandLandmarks) {
            val resultList: MutableList<Float> = ArrayList()
            val centerValues = landmarks.getLandmarkCenter()

            var landmarkIndex = 0
            for (landmark: LandmarkProto.NormalizedLandmark in landmarks.getLandmarkList()) {
                resultList.add(landmark.getX() - centerValues[0])
                resultList.add(landmark.getY() - centerValues[1])
                ++landmarkIndex
            }
            Log.d("resultList", resultList.toString())
            Log.d("resultList size", resultList.size.toString())

            // Get handedness and clean all non alphanumeric characters
            var landmarkHand = handedness[handIndex].getLabel(0).takeLast(5)
            landmarkHand = Regex("[^A-Za-z]").replace(landmarkHand, "")

            if (landmarkHand == "Left") {
                Log.d("IS LEFTs", landmarkHand)
                // Check if double left hand found
                if (isLeftFound) {
                    return
                }
                isLeftFound = true
                leftHandLandmark = resultList
            } else {
                Log.d("IS RIGHTs", landmarkHand)
                // Check if double right hand found
                if (isRightFound) {
                    return
                }
                isRightFound = true
                rightHandLandmark = resultList
            }
            Log.d("landmarkHand label", landmarkHand)

            ++handIndex
        }
        fullLandmarksData = leftHandLandmark + rightHandLandmark

        val outputArray = arrayOf(FloatArray(92))
        fullLandmarksData.unwindListToByteBuffer(tfliteInputBuffer)
        tfliteInterpreter.run(tfliteInputBuffer, outputArray)
        var labelResult = outputArray[0].getTopLabel(associatedAxisLabels)

        val (label, likelihood) = labelResult[0]
        Log.d("labelResult", labelResult.toString())
        Log.d("lastLabel != label", (lastLabel != label).toString())
        this.runOnUiThread {
//            if (label.length == 1) {
//                tv_preview_isyarat.text = tv_preview_isyarat.text.toString().plus(label)
//            } else {
//                tv_preview_isyarat.text =
//                    tv_preview_isyarat.text.toString().plus(" ").plus(label)
//            }
//            if (tv_preview_isyarat.text.length > 100) {
//                tv_preview_isyarat.text = label
//            }
            if (lastLabel != label) {
                time = System.currentTimeMillis()
            }
            if (submittedLabel == label) {
                time = System.currentTimeMillis()
            }
            if (lastLabel == label || submittedLabel != label) {
                viewModel.newMessageText.set(tobeSubmitted + label)
            }
            lastLabel = label
            Log.d("System.currentTimeMillis", (System.currentTimeMillis() - time).toString())
            if (System.currentTimeMillis() - time >= 500) {
                // Some tweaks that j appear too much, so it take longer time
                if (label == "j") {
                    if (System.currentTimeMillis() - time >= 1000) {
                        if (label.length == 1 && submittedLabel.length == 1) {
                            tobeSubmitted += label
                        } else {
                            tobeSubmitted += " $label"
                        }
                        submittedLabel = label
                        time = System.currentTimeMillis()
                    }
                } else {
                    if (label.length == 1 && submittedLabel.length == 1) {
                        tobeSubmitted += label
                    } else {
                        tobeSubmitted += " $label"
                    }
                    submittedLabel = label
                    time = System.currentTimeMillis()
                }
            }
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

    private fun onCameraStarted(surfaceTexture: SurfaceTexture?) {
        previewFrameTexture = surfaceTexture
        previewDisplayView!!.visibility = View.VISIBLE
    }

    private fun cameraTargetResolution(): Size? {
        return null // No preference and let the camera (helper) decide.
    }

    private fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper!!.setOnCameraStartedListener { surfaceTexture -> onCameraStarted(surfaceTexture) }
        val cameraFacing: CameraHelper.CameraFacing = lensFacing
        cameraHelper!!.startCamera(
            this, cameraFacing,  /*unusedSurfaceTexture=*/null, cameraTargetResolution()
        )
    }

    private fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height)
    }

    private fun onPreviewDisplaySurfaceChanged(
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

    private fun bindAppBar(viewModel: ChatViewModel) {
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
            initRecyclerView(it, viewModel)
        })
    }

    private fun initRecyclerView(chatItem: List<Chat>, viewModel: ChatViewModel) {
        val groupAdapter = GroupieAdapter()

        chatItem.forEach {
            if (it.isSpeaker == 1) {
                groupAdapter.add(ChatReceivedItem(it))
            } else {
                groupAdapter.add(ChatSentItem(it, viewModel))
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