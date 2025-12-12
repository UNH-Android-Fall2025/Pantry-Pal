package com.unh.pantrypalonevo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val category: String,
    var quantity: Int = 1,
    val imageUrl: String? = null,
    val pantryId: String? = null,
    val pantryName: String? = null,
    val donorId: String? = null,
    val itemId: String? = null // Firestore item ID
) : Parcelable

