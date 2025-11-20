package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemRecipeBinding

class RecipeAdapter(
    private val recipes: List<Recipe>
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.binding.tvTitle.text = recipe.title
        holder.binding.tvTime.text = "Time: ${recipe.time}"
        holder.binding.tvDifficulty.text = "Difficulty: ${recipe.difficulty}"
        holder.binding.tvIngredients.text =
            "Ingredients: ${recipe.ingredients.joinToString(", ")}"
        holder.binding.tvSteps.text =
            "Steps:\n${recipe.steps.joinToString("\n")}"
    }

    override fun getItemCount(): Int = recipes.size
}
