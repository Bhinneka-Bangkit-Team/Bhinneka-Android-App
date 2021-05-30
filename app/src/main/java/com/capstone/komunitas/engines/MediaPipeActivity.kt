package com.capstone.komunitas.engines

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.capstone.komunitas.R
import com.google.mediapipe.components.CameraHelper
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.AndroidPacketCreator
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import java.util.*

class MediaPipeActivity() : AppCompatActivity() {
    companion object {
        private val TAG = "MainActivity"
        private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
        private val INPUT_VIDEO_STREAM_NAME = "input_video"
        private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
        private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
        private val INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands"
        private val NUM_HANDS = 2
        private val CAMERA_FACING: CameraHelper.CameraFacing = CameraHelper.CameraFacing.FRONT

        // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
        // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
        // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
        // corner, whereas MediaPipe in general assumes the image origin is at top-left.
        private val FLIP_FRAMES_VERTICALLY = true

        init {
            // Load all native libraries needed by the app.
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private var previewFrameTexture: SurfaceTexture? = null

    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private var previewDisplayView: SurfaceView? = null

    // Creates and manages an {@link EGLContext}.
    private var eglManager: EglManager? = null

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    private var processor: FrameProcessor? = null

    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private var converter: ExternalTextureConverter? = null

    // Handles camera access via the {@link CameraX} Jetpack support library.
    private var cameraHelper: CameraXPreviewHelper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewLayoutResId)

        previewDisplayView = SurfaceView(this)
        setupPreviewDisplayView()

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
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

        // To show verbose logging, run:
        // adb shell setprop log.tag.MainActivity VERBOSE
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            processor!!.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME
            ) { packet ->
                Log.v(TAG, "Received multi-hand landmarks packet.")
                val multiHandLandmarks: List<NormalizedLandmarkList> =
                    PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser())
                Log.v(
                    TAG,
                    ("[TS:"
                            + packet.getTimestamp()
                            ) + "] " + getMultiHandLandmarksDebugString(multiHandLandmarks)
                )
            }
        }
    }

    // Used to obtain the content view for this application. If you are extending this class, and
    // have a custom layout, override this method and return the custom layout.
    protected val contentViewLayoutResId: Int
        protected get() = R.layout.activity_mediapipe

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

        // Hide preview display until we re-open the camera again.
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
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
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
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        val viewSize = computeViewSize(width, height)
        val displaySize: Size = cameraHelper!!.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated: Boolean = cameraHelper!!.isCameraRotated()

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
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

    private fun getMultiHandLandmarksDebugString(multiHandLandmarks: List<NormalizedLandmarkList>): String {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks"
        }
        var multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size + "\n"
        var handIndex = 0
        for (landmarks: NormalizedLandmarkList in multiHandLandmarks) {
            multiHandLandmarksStr += "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n"
            var landmarkIndex = 0
            for (landmark: NormalizedLandmark in landmarks.getLandmarkList()) {
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
}