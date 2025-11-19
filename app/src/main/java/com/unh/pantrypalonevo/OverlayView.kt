package com.unh.pantrypalonevo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var detections: List<Detection> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    fun setDetections(detections: List<Detection>) {
        this.detections = detections
        invalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (detection in detections) {
            val box = detection.boundingBox

            // Draw bounding box
            canvas.drawRect(box, boxPaint)

            // Draw label with background
            val label = "${detection.label} ${(detection.confidence * 100).toInt()}%"
            val textBounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)

            val textX = box.left
            val textY = box.top - 10f

            // Draw text background
            canvas.drawRect(
                textX,
                textY - textBounds.height() - 10f,
                textX + textBounds.width() + 20f,
                textY + 10f,
                textBackgroundPaint
            )

            // Draw text
            canvas.drawText(label, textX + 10f, textY, textPaint)
        }
    }
}
