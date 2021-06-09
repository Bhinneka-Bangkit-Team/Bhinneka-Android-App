package com.capstone.komunitas.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar


fun Context.toast(message: String){
//    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun RecyclerView.show(){
    visibility = View.VISIBLE
}
fun RecyclerView.hide(){
    visibility = View.GONE
}
fun TextView.show(){
    visibility = View.VISIBLE
}
fun TextView.hide(){
    visibility = View.GONE
}
fun LinearLayout.show(){
    visibility = View.VISIBLE
}
fun LinearLayout.hide(){
    visibility = View.GONE
}
fun ProgressBar.show(){
    visibility = View.VISIBLE
}
fun ProgressBar.hide(){
    visibility = View.GONE
}

fun View.snackbar(message: String){
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).also {
        snackbar -> snackbar.setAction("Ok"){
            snackbar.dismiss()
        }
    }.show()
}

fun Activity.getRootView(): View {
    return findViewById<View>(android.R.id.content)
}

fun Context.convertDpToPx(dp: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        this.resources.displayMetrics
    )
}

fun Activity.isKeyboardOpen(): Boolean {
    val visibleBounds = Rect()
    this.getRootView().getWindowVisibleDisplayFrame(visibleBounds)
    val heightDiff = getRootView().height - visibleBounds.height()
    val marginOfError = Math.round(this.convertDpToPx(10F))
    return heightDiff > marginOfError
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