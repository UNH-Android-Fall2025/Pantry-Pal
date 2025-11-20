package com.unh.pantrypalonevo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityRecipeBinding

class RecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productName = intent.getStringExtra("product_name") ?: "Item"

        val recipes = RecipeDatabase.getRecipesForItem(productName)

        binding.tvRecipeFor.text = "Recipes for $productName"

        binding.recyclerRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecipes.adapter = RecipeAdapter(recipes)
    }
}
