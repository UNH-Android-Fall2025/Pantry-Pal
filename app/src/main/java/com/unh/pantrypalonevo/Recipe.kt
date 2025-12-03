package com.unh.pantrypalonevo

// Data class representing a recipe item in the app
data class Recipe(
    // Name of the recipe (e.g., "Pasta", "Paneer Curry")
    val title: String,

    // List of ingredients required for the recipe
    val ingredients: List<String>,

    // Step-by-step cooking instructions
    val steps: List<String>,

    // Total time needed to prepare/cook (e.g., "20 mins")
    val time: String,

    // Difficulty level (e.g., "Easy", "Medium", "Hard")
    val difficulty: String
)
