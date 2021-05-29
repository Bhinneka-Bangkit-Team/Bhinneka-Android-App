package com.capstone.komunitas.ui.detection.camera_detection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.komunitas.R
import com.capstone.komunitas.databinding.ActivityCameraBinding
import com.capstone.komunitas.util.Utils
import com.capstone.komunitas.ui.detection.fragment.CameraConnectionFragment
import com.capstone.komunitas.ui.detection.fragment.LegacyCameraFragment

abstract class CameraActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener, Camera.PreviewCallback, CompoundButton.OnCheckedChangeListener, View.OnClickListener, CameraConnectionFragment.ConnectionCallback {

    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    protected var luminanceStride: Int = 0
        private set
    private var postInterfeCallback: Runnable? = null
    private var imgConverter: Runnable? = null
    protected var preWidth = 0
    protected var preHeight = 0
    val isDebug = false
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var useCamera2API: Boolean = false
    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var textDetection: TextView


    protected val screenOrientation: Int
        get() {
            when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_270 -> return 270
                Surface.ROTATION_180 -> return 180
                Surface.ROTATION_90 -> return 90
                else -> return 0
            }
        }

    protected abstract val desiredPreviewFrameSize: Size

    protected abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(viewBinding.root)
        textDetection = findViewById(R.id.detectionText)
        var isRequestPermission = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isRequestPermission = checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                PERMISSION_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        } else {
            isRequestPermission = true
        }

        if (isRequestPermission) {
            setFragment()
        } else {
            requestPermission()
        }

    }
    protected fun getRgbBytes(): IntArray? {
        imgConverter?.run()
        return rgbBytes
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setFragment()
            } else {
                requestPermission()
            }
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG)
                    .show()
            }
            requestPermissions(arrayOf(PERMISSION_CAMERA, PERMISSION_STORAGE), PERMISSIONS_REQUEST)
        }
    }

    private fun isHardwareLevelSupported(
        characteristics: CameraCharacteristics, requiredLevel: Int): Boolean {
        val deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
        return if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            requiredLevel == deviceLevel
        } else requiredLevel <= deviceLevel

    }

    private fun chooseCamera():String?{
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList){
                val characteristic = manager.getCameraCharacteristics(cameraId)
                val facing = characteristic.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue

                if(facing == CameraCharacteristics.LENS_FACING_EXTERNAL || isHardwareLevelSupported(characteristic, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)){
                    useCamera2API = true
                }else{
                    useCamera2API = false
                }
                Log.i(TAG, "Camera API lv2?: ${useCamera2API}" )
                return cameraId
            }
        }catch (e: CameraAccessException){
            Log.e(TAG, "Not allowed to access camera" )
        }
        return null
    }

    protected fun setFragment(){
        val cameraId = chooseCamera()
        val fragment: Fragment
        if (useCamera2API){
            val camera2Fragment = CameraConnectionFragment.newInstance(
                object: CameraConnectionFragment.ConnectionCallback{
                    override fun onPreviewSizeChosen(size: Size, cameraRotation: Int) {
                        preHeight = size.height
                        preWidth = size.width
                        this.onPreviewSizeChosen(size,cameraRotation)
                    }

                },this,layoutId,desiredPreviewFrameSize
            )
            if (cameraId != null) {
                camera2Fragment.setCamera(cameraId)
            }
            fragment = camera2Fragment
        }else{
            fragment = LegacyCameraFragment(this,layoutId,desiredPreviewFrameSize)
        }

        supportFragmentManager.beginTransaction().replace(R.id.container,fragment).commit()
    }

    protected fun readyForNextImage() {
        if (postInterfeCallback != null) {
            postInterfeCallback?.run()
        }
    }
    protected fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {

        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                Log.d(TAG, "Initializing buffer ${i} at size ${buffer.capacity()}")
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer.get(yuvBytes[i])
        }
    }



    override fun onImageAvailable(reader: ImageReader?) {
        if (preWidth == 0 || preHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(preWidth * preHeight)
        }
        try {
            val image = reader?.acquireLatestImage() ?: return

            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            Trace.beginSection("imageAvailable")
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            luminanceStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride

            imgConverter = Runnable {
                Utils.convertYUV420ToARGB8888(
                    yuvBytes[0]!!,
                    yuvBytes[1]!!,
                    yuvBytes[2]!!,
                    preWidth,
                    preHeight,
                    luminanceStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!)
            }

            postInterfeCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }

            processImage(textDetection)
        } catch (e: Exception) {
            Log.e(TAG, "onImageAvailable: ${e}" )
            Trace.endSection()
            return
        }

        Trace.endSection()
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (isProcessingFrame) {
            Log.w(TAG, "onPreviewFrame: $isProcessingFrame" )
            return
        }

        try {
            if (rgbBytes == null) {
                val previewSize = camera?.parameters?.previewSize
                preWidth = previewSize?.width!!
                preHeight = previewSize?.height!!

                rgbBytes = IntArray(preWidth * preHeight)
                onPreviewSizeChosen(Size(previewSize.width, previewSize.height), 90)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onPreviewFrame :$e " )
            return
        }

        isProcessingFrame = true
        yuvBytes[0] = data
        luminanceStride = preWidth

        imgConverter = Runnable { data?.let { Utils.convertYUVoARGB(it, preWidth, preHeight, rgbBytes!!) } }

        postInterfeCallback = Runnable {
            camera?.addCallbackBuffer(data)
            isProcessingFrame = false
        }
        processImage(textDetection)
    }
    @Synchronized
    override fun onResume() {
        super.onResume()
        handlerThread = HandlerThread("inference")
        handlerThread?.start()
        handler = Handler(handlerThread!!.looper)
    }


    @Synchronized
    override fun onPause() {
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handlerThread = null
            handler=null
        }catch (e:InterruptedException){
            Log.e(TAG, "onPause: ${e}" )
        }
        super.onPause()
    }
    @Synchronized
    override fun onStart() {
        super.onStart()
    }

    @Synchronized
    override fun onStop() {
        Log.d(TAG, "onStop: CameraActivity")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: CameraActivity")
        super.onDestroy()
    }

    @Synchronized
    protected fun runInBackground(r: Runnable) {
        if (handler != null) {
            handler!!.post(r)
        }
    }

    protected abstract fun processImage(textDetection: TextView)
    abstract override fun onPreviewSizeChosen(size: Size, cameraRotation: Int)
    protected abstract fun setNumThreads(numThreads: Int)
    protected abstract fun setUseNNAPI(isChecked: Boolean)


    companion object {
        private const val TAG = "CameraActivity"
        private val PERMISSIONS_REQUEST = 1
        private val PERMISSION_CAMERA = Manifest.permission.CAMERA
        private val PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}
