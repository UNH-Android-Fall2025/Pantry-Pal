package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemRecipeBinding

class RecipeAdapter(
    private val recipes: MutableList<Recipe>
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {
    
    var onRecipeClick: ((Recipe) -> Unit)? = null
    
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(recipe: Recipe) {
            binding.tvTitle.text = recipe.title
            // Format time without emoji (e.g., "10 min" or "30 mins")
            binding.tvTime.text = recipe.time.replace("⏱", "").trim()
            // Format difficulty without emoji
            binding.tvDifficulty.text = recipe.difficulty.replace("📊", "").trim()
            
            // Show ingredients as one line (no "Ingredients:" prefix)
            val ingredientsPreview = recipe.ingredients.joinToString(", ")
            binding.tvIngredients.text = ingredientsPreview
            
            // Kcal placeholder (since Recipe model doesn't have kcal, we'll hide it or use a default)
            // For now, hide it since we don't have the data
            binding.tvKcal.visibility = View.GONE
            
            // Set click listener
            binding.cardRecipe.setOnClickListener {
                onRecipeClick?.invoke(recipe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size
}
