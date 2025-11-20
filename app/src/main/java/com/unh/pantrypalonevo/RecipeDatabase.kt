package com.unh.pantrypalonevo

object RecipeDatabase {

    private val recipeMap = mapOf(

        "Potato" to listOf(
            Recipe(
                "Crispy Roasted Potatoes",
                listOf("Potato", "Oil", "Salt", "Pepper"),
                listOf(
                    "Wash and chop potatoes",
                    "Mix with oil and seasoning",
                    "Bake at 200°C for 30 minutes"
                ),
                "30 mins",
                "Easy"
            ),
            Recipe(
                "Mashed Potatoes",
                listOf("Potato", "Milk", "Butter"),
                listOf(
                    "Boil potatoes",
                    "Mash them",
                    "Add milk and butter"
                ),
                "25 mins",
                "Easy"
            )
        ),

        "Tomato" to listOf(
            Recipe(
                "Tomato Soup",
                listOf("Tomato", "Garlic", "Cream"),
                listOf(
                    "Cook tomatoes with garlic",
                    "Blend into soup",
                    "Add cream and serve"
                ),
                "20 mins",
                "Easy"
            )
        ),

        "Banana" to listOf(
            Recipe(
                "Banana Pancakes",
                listOf("Banana", "Flour", "Egg", "Milk"),
                listOf(
                    "Mash banana",
                    "Mix into batter",
                    "Cook on pan"
                ),
                "15 mins",
                "Easy"
            )
        ),

        "Apple" to listOf(
            Recipe(
                "Apple Pie",
                listOf("Apple", "Sugar", "Flour"),
                listOf(
                    "Slice apples",
                    "Prepare dough",
                    "Bake in oven"
                ),
                "1 hour",
                "Medium"
            )
        )
    )

    private fun normalizeName(name: String): String {
        val lower = name.trim().lowercase()

        val singular = when {
            lower.endsWith("es") -> lower.dropLast(2)
            lower.endsWith("s") -> lower.dropLast(1)
            else -> lower
        }

        return singular.replaceFirstChar { it.uppercase() }
    }

    fun getRecipesForItem(itemName: String): List<Recipe> {
        val normalized = normalizeName(itemName)
        return recipeMap[normalized] ?: emptyList()
    }
}
