package com.unh.pantrypalonevo.model

data class CartItem(
    val name: String,
    val imageUrl: String,
    var quantity: Int,
    val price: Double
)
