package com.unh.pantrypalonevo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * SMART Product Searcher with English-only filtering
 * Filters out foreign language results and non-food items
 */
class SimpleProductSearcher {

    private val client = OkHttpClient()

    companion object {
        private const val TAG = "ProductSearcher"

        // Common grocery categories - ONLY search for these
        private val VALID_FOOD_ITEMS = setOf(
            "onion", "tomato", "potato", "carrot", "lettuce", "cabbage",
            "broccoli", "cauliflower", "pepper", "cucumber", "celery",
            "spinach", "kale", "mushroom", "garlic", "ginger",
            "apple", "banana", "orange", "grape", "strawberry",
            "milk", "cheese", "yogurt", "butter", "egg",
            "bread", "rice", "pasta", "flour", "sugar",
            "chicken", "beef", "pork", "fish", "salmon",
            "vegetable", "fruit", "dairy", "meat", "grain"
        )
    }

    /**
     * Search for products - ONLY if label is a valid food item
     */
    suspend fun searchProduct(label: String): List<String> = withContext(Dispatchers.IO) {
        try {
            // ✅ FILTER 1: Only search for actual food items
            val normalizedLabel = label.lowercase().trim()
            val isValidFood = VALID_FOOD_ITEMS.any { normalizedLabel.contains(it) }

            if (!isValidFood) {
                Log.d(TAG, "⚠️ Skipping non-food label: $label")
                return@withContext emptyList()
            }

            Log.d(TAG, "✅ Searching for valid food: $label")

            // Search Open Food Facts with English language filter
            val encodedLabel = URLEncoder.encode(label, "UTF-8")
            val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encodedLabel&search_simple=1&json=1&page_size=5&lc=en"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val jsonResponse = response.body?.string() ?: return@withContext emptyList()

            val json = JSONObject(jsonResponse)
            val products = mutableListOf<String>()

            if (json.has("products")) {
                val productArray = json.getJSONArray("products")

                for (i in 0 until minOf(3, productArray.length())) {
                    val product = productArray.getJSONObject(i)

                    // Get product name
                    val productName = when {
                        product.has("product_name") && product.getString("product_name").isNotEmpty() ->
                            product.getString("product_name")
                        product.has("generic_name") && product.getString("generic_name").isNotEmpty() ->
                            product.getString("generic_name")
                        else -> continue // Skip if no name
                    }

                    // ✅ FILTER 2: Only English names (no Arabic, French, German)
                    if (isEnglishOnly(productName)) {
                        products.add(cleanProductName(productName))
                        Log.d(TAG, "✅ Found English product: $productName")
                    } else {
                        Log.d(TAG, "⚠️ Skipping non-English: $productName")
                    }
                }
            }

            // ✅ FILTER 3: If no results, return simple category name
            if (products.isEmpty()) {
                // Return the original label (simplified)
                val simpleName = simplifyLabel(label)
                products.add(simpleName)
                Log.d(TAG, "ℹ️ No results, using: $simpleName")
            }

            return@withContext products

        } catch (e: Exception) {
            Log.e(TAG, "Search error for '$label'", e)
            return@withContext listOf(simplifyLabel(label))
        }
    }

    /**
     * Check if text is mostly English (no special characters)
     */
    private fun isEnglishOnly(text: String): Boolean {
        // Allow only English letters, numbers, spaces, and basic punctuation
        val englishPattern = Regex("^[a-zA-Z0-9 .,'&%-]+$")
        return englishPattern.matches(text)
    }

    /**
     * Simplify label to basic food name
     */
    private fun simplifyLabel(label: String): String {
        val lower = label.lowercase()
        return when {
            lower.contains("onion") -> "Onion"
            lower.contains("tomato") -> "Tomato"
            lower.contains("potato") -> "Potato"
            lower.contains("carrot") -> "Carrot"
            lower.contains("vegetable") || lower.contains("plant") -> "Vegetable"
            lower.contains("fruit") -> "Fruit"
            lower.contains("milk") || lower.contains("dairy") -> "Milk"
            else -> label.split(" ").firstOrNull()?.capitalize() ?: label
        }
    }

    /**
     * Clean up product name
     */
    private fun cleanProductName(name: String): String {
        return name.trim()
            .split(" ")
            .take(4) // Max 4 words
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() }
    }
}
