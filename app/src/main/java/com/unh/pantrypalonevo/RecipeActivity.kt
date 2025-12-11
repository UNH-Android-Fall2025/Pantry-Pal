package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.unh.pantrypalonevo.databinding.ActivityRecipeBinding
import com.unh.pantrypalonevo.databinding.BottomSheetIngredientInputBinding
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
        setupCancelButton()
        setupBottomNavigation()
    }
    
    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Recipes button is already active, no action needed
        
        binding.btnCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
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
        // Show loading state
        binding.loadingContainer.visibility = View.VISIBLE
        binding.recyclerRecipes.visibility = View.GONE
        binding.tvRecipeFor.visibility = View.GONE
        binding.btnCancel.visibility = View.VISIBLE
        
        // Initialize progress (start at 0%)
        updateProgress(0)

        lifecycleScope.launch {
            try {
                // Simulate progress updates (since API doesn't provide real progress)
                updateProgress(25)
                kotlinx.coroutines.delay(300)
                updateProgress(50)
                kotlinx.coroutines.delay(300)
                updateProgress(75)
                
                val generatedRecipes = deepSeekService.generateRecipes(items)
                
                // Complete progress
                updateProgress(100)
                kotlinx.coroutines.delay(200)
                
                if (generatedRecipes.isNotEmpty()) {
                    recipes.clear()
                    // Limit to top 10 recipes
                    val top10Recipes = generatedRecipes.take(10)
                    recipes.addAll(top10Recipes)
                    recipeAdapter.notifyDataSetChanged()
                    
                    // Hide loading, show results
                    binding.loadingContainer.visibility = View.GONE
                    binding.recyclerRecipes.visibility = View.VISIBLE
                    binding.tvRecipeFor.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.GONE
                    
                    // Update header text (already set in XML, but ensure it's visible)
                    binding.tvRecipeFor.text = "BASED ON YOUR PANTRY"
                    
                    Toast.makeText(
                        this@RecipeActivity,
                        "Generated ${top10Recipes.size} recipes! Tap any recipe to view details.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Fallback: Show message or use default recipes
                    binding.loadingContainer.visibility = View.GONE
                    binding.tvLoading.text = "Unable to generate recipes.\n\nPlease check:\n• Internet connection\n• DeepSeek API key is valid\n• Try again in a moment\n\nCheck Logcat for details."
                    binding.tvLoading.visibility = View.VISIBLE
                    binding.btnCancel.visibility = View.VISIBLE
                    
                    android.util.Log.e("RecipeActivity", "❌ No recipes generated. Check Logcat for DeepSeekService logs.")
                    
                    Toast.makeText(
                        this@RecipeActivity,
                        "No recipes generated. Check Logcat (DeepSeekService) for details.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeActivity", "Error generating recipes", e)
                binding.loadingContainer.visibility = View.GONE
                
                val errorMessage = when (e) {
                    is java.net.SocketTimeoutException -> "Request timed out.\n\nThe API is taking too long to respond.\n\nPlease:\n• Check your internet connection\n• Try again in a moment\n• The request may need more time"
                    is java.net.UnknownHostException -> "Network error.\n\nCannot connect to the API.\n\nPlease check your internet connection."
                    else -> "Error generating recipes: ${e.message}\n\nPlease try again."
                }
                
                binding.tvLoading.text = errorMessage
                binding.tvLoading.visibility = View.VISIBLE
                binding.btnCancel.visibility = View.VISIBLE
                
                Toast.makeText(
                    this@RecipeActivity,
                    "Error: ${e.javaClass.simpleName}. Check Logcat for details.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Update progress bar and percentage
     */
    private fun updateProgress(percentage: Int) {
        val clampedPercentage = percentage.coerceIn(0, 100)
        binding.tvProgressPercentage.text = "$clampedPercentage%"
        
        // Update progress fill width
        binding.progressTrackContainer.post {
            val maxWidth = binding.progressTrackContainer.width
            if (maxWidth > 0) {
                val fillWidth = (maxWidth * clampedPercentage / 100).toInt()
                val layoutParams = binding.progressFill.layoutParams
                layoutParams.width = fillWidth
                binding.progressFill.layoutParams = layoutParams
            }
        }
    }
    
    /**
     * Show bottom sheet dialog to let user enter items manually
     */
    private fun showItemSelectionDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetIngredientInputBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        
        // Set up unit dropdown (optional - for future use)
        val units = listOf("Grams (g)", "Kilograms (kg)", "Cups", "Pieces", "Tablespoons")
        // Note: Unit field is for future enhancement, currently not used in logic
        
        // Generate Recipes button
        bottomSheetBinding.btnGenerateRecipes.setOnClickListener {
            val itemsText = bottomSheetBinding.etIngredientName.text?.toString()?.trim() ?: ""
            if (itemsText.isNotBlank()) {
                val items = itemsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                
                if (items.isNotEmpty()) {
                    bottomSheetDialog.dismiss()
                    binding.tvRecipeFor.text = "Recipes for: ${items.joinToString(", ")}"
                    generateRecipes(items)
                } else {
                    Toast.makeText(this, "Please enter at least one item", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter items", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Cancel button
        bottomSheetBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
            finish()
        }
        
        // Prevent dismissing by tapping outside
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setCanceledOnTouchOutside(false)
        
        bottomSheetDialog.show()
    }
}
