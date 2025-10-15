package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemPantryBinding
import com.unh.pantrypalonevo.Pantry

class PantryAdapter(
    private val pantryList: List<Pantry>,
    private val onItemClick: (Pantry, String) -> Unit
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    inner class PantryViewHolder(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PantryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        val pantry = pantryList[position]
        holder.binding.apply {
            tvPantryName.text = pantry.name
            tvPantryDescription.text = pantry.description
            tvPantryLocation.text = pantry.location
            tvDistance.text = "${"%.1f".format(Math.random() * 2 + 0.1)} mi"

            btnMapIcon.setOnClickListener { onItemClick(pantry, "map") }
            root.setOnClickListener { onItemClick(pantry, "view") }
        }
    }

    override fun getItemCount(): Int = pantryList.size
}

// Extension function for formatting

