package com.unh.pantrypalonevo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetectedProduct(
    val name: String,
    val confidence: Float,
    var quantity: Int = 1,
    var approved: Boolean = false
) : Parcelable