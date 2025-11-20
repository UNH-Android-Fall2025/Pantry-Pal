package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityRecipeBinding
import kotlinx.coroutines.launch

class RecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeBinding
    private lateinit var deepSeekService: DeepSeekService
    private val recipes = mutableListOf<Recipe>()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize DeepSeek service
        deepSeekService = DeepSeekService.getInstance(this)

        // Get items from intent
        val items = getItemsFromIntent()
        
        if (items.isEmpty()) {
            // Show dialog to let user enter items manually
            showItemSelectionDialog()
        } else {
            val itemsText = items.joinToString(", ")
            binding.tvRecipeFor.text = "Recipes for: $itemsText"
            generateRecipes(items)
        }

        setupRecyclerView()
    }

    private fun getItemsFromIntent(): List<String> {
        // Try to get list of items
        val itemsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("items", DetectedProduct::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<DetectedProduct>("items")
        }

        if (itemsList != null && itemsList.isNotEmpty()) {
            return itemsList.map { it.name }
        }

        // Try to get cart items
        val cartItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("cart_items", CartItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<CartItem>("cart_items")
        }

        if (cartItems != null && cartItems.isNotEmpty()) {
            return cartItems.map { it.name }
        }

        // Fallback: try comma-separated string
        val itemsString = intent.getStringExtra("items_string")
        if (!itemsString.isNullOrBlank()) {
            return itemsString.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

        return emptyList()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(recipes)
        recipeAdapter.onRecipeClick = { recipe ->
            // Navigate to recipe detail page
            val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                putExtra("recipe_title", recipe.title)
                putExtra("recipe_ingredients", recipe.ingredients.toTypedArray())
                putExtra("recipe_steps", recipe.steps.toTypedArray())
                putExtra("recipe_time", recipe.time)
                putExtra("recipe_difficulty", recipe.difficulty)
            }
            startActivity(intent)
        }
        binding.recyclerRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecipes.adapter = recipeAdapter
    }

    private fun generateRecipes(items: List<String>) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerRecipes.visibility = View.GONE
        binding.tvLoading.text = "Generating recipes with AI..."
        binding.tvLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val generatedRecipes = deepSeekService.generateRecipes(items)
                
                if (generatedRecipes.isNotEmpty()) {
                    recipes.clear()
                    // Limit to top 10 recipes
                    val top10Recipes = generatedRecipes.take(10)
                    recipes.addAll(top10Recipes)
                    recipeAdapter.notifyDataSetChanged()
                    
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerRecipes.visibility = View.VISIBLE
                    binding.tvLoading.visibility = View.GONE
                    
                    // Update header to show count
                    val itemsText = items.joinToString(", ")
                    binding.tvRecipeFor.text = "Top ${top10Recipes.size} Recipes for: $itemsText"
                    
                    Toast.makeText(
                        this@RecipeActivity,
                        "Generated ${top10Recipes.size} recipes! Tap any recipe to view details.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Fallback: Show message or use default recipes
                    binding.progressBar.visibility = View.GONE
                    binding.tvLoading.text = "Unable to generate recipes.\n\nPlease check:\n• Internet connection\n• DeepSeek API key is valid\n• Try again in a moment\n\nCheck Logcat for details."
                    binding.tvLoading.visibility = View.VISIBLE
                    
                    android.util.Log.e("RecipeActivity", "❌ No recipes generated. Check Logcat for DeepSeekService logs.")
                    
                    Toast.makeText(
                        this@RecipeActivity,
                        "No recipes generated. Check Logcat (DeepSeekService) for details.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeActivity", "Error generating recipes", e)
                binding.progressBar.visibility = View.GONE
                
                val errorMessage = when (e) {
                    is java.net.SocketTimeoutException -> "Request timed out.\n\nThe API is taking too long to respond.\n\nPlease:\n• Check your internet connection\n• Try again in a moment\n• The request may need more time"
                    is java.net.UnknownHostException -> "Network error.\n\nCannot connect to the API.\n\nPlease check your internet connection."
                    else -> "Error generating recipes: ${e.message}\n\nPlease try again."
                }
                
                binding.tvLoading.text = errorMessage
                binding.tvLoading.visibility = View.VISIBLE
                
                Toast.makeText(
                    this@RecipeActivity,
                    "Error: ${e.javaClass.simpleName}. Check Logcat for details.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Show dialog to let user enter items manually
     */
    private fun showItemSelectionDialog() {
        val input = EditText(this)
        input.hint = "Enter items separated by commas (e.g., Potato, Tomato, Onion)"
        
        AlertDialog.Builder(this)
            .setTitle("Enter Ingredients")
            .setMessage("What ingredients do you have?")
            .setView(input)
            .setPositiveButton("Generate Recipes") { _, _ ->
                val itemsText = input.text.toString().trim()
                if (itemsText.isNotBlank()) {
                    val items = itemsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    
                    if (items.isNotEmpty()) {
                        binding.tvRecipeFor.text = "Recipes for: ${items.joinToString(", ")}"
                        generateRecipes(items)
                    } else {
                        Toast.makeText(this, "Please enter at least one item", Toast.LENGTH_SHORT).show()
                        showItemSelectionDialog() // Show again
                    }
                } else {
                    Toast.makeText(this, "Please enter items", Toast.LENGTH_SHORT).show()
                    showItemSelectionDialog() // Show again
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
