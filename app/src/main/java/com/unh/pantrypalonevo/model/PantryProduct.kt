package com.unh.pantrypalonevo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PantryProduct(
    val productId: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val category: String = "",
    var selected: Boolean = false,
    val quantity: Int = 1,
    val pantryId: String? = null,
    val donorId: String? = null
) : Parcelable

