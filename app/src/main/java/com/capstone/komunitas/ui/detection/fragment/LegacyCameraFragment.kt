package com.capstone.komunitas.ui.detection.fragment

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import androidx.fragment.app.Fragment
import com.capstone.komunitas.R
import com.capstone.komunitas.util.Utils
import com.capstone.komunitas.ui.detection.AutoFixTexture
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LegacyCameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LegacyCameraFragment(private val imageListener: Camera.PreviewCallback, private val layout: Int, private val desiredSize: Size) : Fragment() {

    private var camera: Camera? = null
    /** An [AutoFitTextureView] for camera preview.  */
    private var textureView: AutoFixTexture? = null
    private var param1: String? = null
    private var param2: String? = null

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            texture: SurfaceTexture, width: Int, height: Int) {

            val index = cameraId
            camera = Camera.open(index)

            try {
                val parameters = camera!!.parameters
                val focusModes = parameters.supportedFocusModes
                if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                }
                val cameraSizes = parameters.supportedPreviewSizes
                val sizes = arrayOfNulls<Size>(cameraSizes.size)
                var i = 0
                for (size in cameraSizes) {
                    sizes[i++] = Size(size.width, size.height)
                }
                val previewSize = CameraConnectionFragment.chooseOptimalSize(
                    sizes, desiredSize.width, desiredSize.height
                )
                previewSize?.width?.let { parameters.setPreviewSize(it, previewSize.height) }
                camera?.setDisplayOrientation(90)
                camera?.parameters = parameters
                camera?.setPreviewTexture(texture)
            } catch (exception: IOException) {
                camera?.release()
            }

            camera?.setPreviewCallbackWithBuffer(imageListener)
            val s = camera?.parameters?.previewSize
            camera?.addCallbackBuffer(ByteArray(Utils.getYUVByteSize(s?.height!!, s?.width!!)))

            textureView?.setAspectRatio(s?.height!!, s?.width!!)

            camera?.startPreview()
        }

        override fun onSurfaceTextureSizeChanged(
            texture: SurfaceTexture, width: Int, height: Int) {
            //Not implemented
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }

    private var backgroundThread: HandlerThread? = null

    private val cameraId: Int
        get() {
            val ci = Camera.CameraInfo()
            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, ci)
                if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) return i
            }
            return -1
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_legacy_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.texture) as AutoFixTexture
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView?.isAvailable == true){
            camera?.startPreview()
        }else{
            textureView?.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        stopCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageListener")
        backgroundThread?.start()

    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null

        } catch (e: InterruptedException) {
            Log.e(TAG, "Exception!",e )
        }

    }

    protected fun stopCamera() {
        if (camera != null) {
            camera?.stopPreview()
            camera?.setPreviewCallback(null)
            camera?.release()
            camera = null
        }
    }

    companion object {
        private val ORIENTATIONS = SparseIntArray()
        private const val TAG  = "LegacyCamera"
        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
}
