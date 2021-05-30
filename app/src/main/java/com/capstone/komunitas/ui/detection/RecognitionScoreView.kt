package com.capstone.komunitas.ui.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.capstone.komunitas.tflite.Classifier

class RecognitionScoreView(attr: AttributeSet, context: Context): View(context,attr), ViewResult {

    private val textSize: Float
    private val fg: Paint
    private val bg: Paint
    private var results: List<Classifier.Recognition>? = null

    init {
        bg = Paint()
        bg.color = Color.BLACK

        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics)
        fg = Paint()
        fg.textSize = textSize


    }

    override fun setViewResult(result: Result<Classifier.Recognition>) {
        this.results = results
    }

    public override fun onDraw(canvas: Canvas?) {
        val x = 10
        var y = (fg.textSize * 1.5f).toInt()

        canvas?.drawPaint(bg)

        if (results != null) {
            for (recog in results!!) {
                canvas?.drawText(recog.title + ": " + recog.confidence, x.toFloat(), y.toFloat(), fg)
                y += (fg.textSize * 1.5f).toInt()
            }
        }
    }

    companion object {
        private val TEXT_SIZE_DIP = 13f
    }
}