package com.unh.pantrypalonevo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityRecipeDetailBinding

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        supportActionBar?.title = "Recipe Details"

        // Get recipe data from intent
        val title = intent.getStringExtra("recipe_title") ?: "Recipe"
        val ingredients = intent.getStringArrayExtra("recipe_ingredients")?.toList() ?: emptyList()
        val steps = intent.getStringArrayExtra("recipe_steps")?.toList() ?: emptyList()
        val time = intent.getStringExtra("recipe_time") ?: "N/A"
        val difficulty = intent.getStringExtra("recipe_difficulty") ?: "N/A"

        // Set recipe details
        binding.tvRecipeTitle.text = title
        binding.tvTime.text = "⏱ $time"
        binding.tvDifficulty.text = "📊 $difficulty"

        // Setup ingredients RecyclerView
        val ingredientsAdapter = RecipeIngredientAdapter(ingredients)
        binding.recyclerIngredients.layoutManager = LinearLayoutManager(this)
        binding.recyclerIngredients.adapter = ingredientsAdapter

        // Setup steps RecyclerView
        val stepsAdapter = RecipeStepAdapter(steps)
        binding.recyclerSteps.layoutManager = LinearLayoutManager(this)
        binding.recyclerSteps.adapter = stepsAdapter
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

