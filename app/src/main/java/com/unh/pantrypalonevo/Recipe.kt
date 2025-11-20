package com.unh.pantrypalonevo

data class Recipe(
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val time: String,
    val difficulty: String
)
