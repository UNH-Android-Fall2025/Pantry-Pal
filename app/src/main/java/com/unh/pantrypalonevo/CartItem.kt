package com.unh.pantrypalonevo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val category: String,
    val price: Double = 0.0,
    var quantity: Int = 1,
    val imageUrl: String? = null
) : Parcelable {
    fun getTotalPrice(): Double = price * quantity
}

