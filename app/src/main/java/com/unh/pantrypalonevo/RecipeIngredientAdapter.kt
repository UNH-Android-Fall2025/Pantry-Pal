package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemRecipeIngredientBinding

class RecipeIngredientAdapter(
    private val ingredients: List<String>
) : RecyclerView.Adapter<RecipeIngredientAdapter.IngredientViewHolder>() {

    inner class IngredientViewHolder(val binding: ItemRecipeIngredientBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemRecipeIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.binding.tvIngredient.text = ingredients[position]
    }

    override fun getItemCount(): Int = ingredients.size
}

