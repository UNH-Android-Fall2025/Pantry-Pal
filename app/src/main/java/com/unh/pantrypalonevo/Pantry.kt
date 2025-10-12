package com.unh.pantrypalonevo

data class Pantry(
    val name: String,
    val description: String,
    val location: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
