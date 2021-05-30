package com.capstone.komunitas.ui.chat

import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstone.komunitas.R
import com.capstone.komunitas.data.db.entities.Chat
import com.capstone.komunitas.databinding.ActivityChatWithVideoBinding
import com.capstone.komunitas.util.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.components.*
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.AndroidPacketCreator
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import com.xwray.groupie.GroupieAdapter
import kotlinx.android.synthetic.main.activity_chat_no_video.*
import kotlinx.android.synthetic.main.activity_chat_no_video.messagesRecyclerView
import kotlinx.android.synthetic.main.activity_chat_no_video.progress_bar_chat_novideo
import kotlinx.android.synthetic.main.activity_chat_with_video.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.HashMap

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
        private val CAMERA_FACING: CameraHelper.CameraFacing = CameraHelper.CameraFacing.FRONT
        private val FLIP_FRAMES_VERTICALLY = true

        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

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
        viewModel.lensFacing = CameraSelector.LENS_FACING_BACK

        bindUI(viewModel)
//        bindCamera(viewModel.lensFacing)
        initMediaPipe()
        bindAppBar(viewModel)
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
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            processor!!.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME
            ) { packet ->
                Log.v(TAG, "Received multi-hand landmarks packet.")
                val multiHandLandmarks: List<LandmarkProto.NormalizedLandmarkList> =
                    PacketGetter.getProtoVector(
                        packet,
                        LandmarkProto.NormalizedLandmarkList.parser()
                    )
                Log.v(
                    TAG,
                    ("[TS:"
                            + packet.getTimestamp()
                            ) + "] " + getMultiHandLandmarksDebugString(multiHandLandmarks)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        val cameraFacing: CameraHelper.CameraFacing = CameraHelper.CameraFacing.FRONT
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

        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = groupAdapter
        }
        messagesRecyclerView.scrollToPosition(groupAdapter.itemCount - 1)
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

    override fun onChangeLens(lensFacing: Int) {
//        bindCamera(lensFacing)
    }

    override fun onRecordPressed(isRecording: Boolean) {
        if (isRecording) {
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_stop_white)
        } else {
            btnrecord_chat_withvid.setImageResource(R.drawable.ic_record_white)
        }
    }
}