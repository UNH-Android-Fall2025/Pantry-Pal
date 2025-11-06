package com.unh.pantrypalonevo

import android.graphics.RectF

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)
