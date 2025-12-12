package com.unh.pantrypalonevo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val orderId: String = "",
    val pantryId: String = "",
    val pantryName: String = "",
    val pantryAddress: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientEmail: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) : Parcelable

@Parcelize
data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val category: String = "",
    val imageUrl: String? = null
) : Parcelable

enum class OrderStatus {
    PENDING,      // Order created, waiting for donor confirmation
    CONFIRMED,    // Donor confirmed the order
    PICKED_UP,    // Items have been picked up
    CANCELLED     // Order was cancelled
}

