package com.unh.pantrypalonevo

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivityRecipeDetailBinding

class SharedRecipeViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        supportActionBar?.title = "Shared Recipe"

        // Hide share button for public view
        binding.btnShareRecipe.visibility = android.view.View.GONE

        // Get recipe ID from intent
        val recipeId = intent.getStringExtra("recipe_id")
            ?: intent.data?.getQueryParameter("recipe_id")
            ?: intent.data?.lastPathSegment?.takeIf { it.isNotBlank() }
            ?: intent.data?.pathSegments?.lastOrNull()?.takeIf { it.isNotBlank() }

        if (recipeId == null) {
            Toast.makeText(this, "Invalid recipe link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load recipe from Firestore
        loadRecipe(recipeId)
    }

    private fun loadRecipe(recipeId: String) {
        binding.tvRecipeTitle.text = "Loading recipe..."
        
        db.collection("shared_recipes")
            .document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val title = document.getString("title") ?: "Recipe"
                    val ingredients = document.get("ingredients") as? List<*> 
                        ?: emptyList<Any>()
                    val steps = document.get("steps") as? List<*> 
                        ?: emptyList<Any>()
                    val time = document.getString("time") ?: "N/A"
                    val difficulty = document.getString("difficulty") ?: "N/A"

                    // Set recipe details
                    binding.tvRecipeTitle.text = title
                    binding.tvTime.text = "⏱ $time"
                    binding.tvDifficulty.text = "📊 $difficulty"

                    // Setup ingredients RecyclerView
                    val ingredientsList = ingredients.mapNotNull { it.toString() }
                    val ingredientsAdapter = RecipeIngredientAdapter(ingredientsList)
                    binding.recyclerIngredients.layoutManager = LinearLayoutManager(this)
                    binding.recyclerIngredients.adapter = ingredientsAdapter

                    // Setup steps RecyclerView
                    val stepsList = steps.mapNotNull { it.toString() }
                    val stepsAdapter = RecipeStepAdapter(stepsList)
                    binding.recyclerSteps.layoutManager = LinearLayoutManager(this)
                    binding.recyclerSteps.adapter = stepsAdapter
                } else {
                    Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load recipe: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

