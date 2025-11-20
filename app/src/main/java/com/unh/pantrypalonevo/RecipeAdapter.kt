package com.unh.pantrypalonevo

import android.view.LayoutInflater
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
            binding.tvTime.text = "⏱ ${recipe.time}"
            binding.tvDifficulty.text = "📊 ${recipe.difficulty}"
            
            // Show first 3-4 ingredients as preview
            val ingredientsPreview = if (recipe.ingredients.size > 3) {
                recipe.ingredients.take(3).joinToString(", ") + " +${recipe.ingredients.size - 3} more"
            } else {
                recipe.ingredients.joinToString(", ")
            }
            binding.tvIngredients.text = "Ingredients: $ingredientsPreview"
            
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
