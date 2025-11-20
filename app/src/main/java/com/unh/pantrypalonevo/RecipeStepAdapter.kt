package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemRecipeStepBinding

class RecipeStepAdapter(
    private val steps: List<String>
) : RecyclerView.Adapter<RecipeStepAdapter.StepViewHolder>() {

    inner class StepViewHolder(val binding: ItemRecipeStepBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemRecipeStepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val stepNumber = position + 1
        holder.binding.tvStepNumber.text = stepNumber.toString()
        holder.binding.tvStepText.text = steps[position]
    }

    override fun getItemCount(): Int = steps.size
}

