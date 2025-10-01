package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemPantryBinding

data class Pantry(val name: String, val description: String, val location: String)

class PantryAdapter(private val pantryList: List<Pantry>) :
    RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    inner class PantryViewHolder(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PantryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        val pantry = pantryList[position]
        holder.binding.tvPantryName.text = pantry.name
        holder.binding.tvPantryDescription.text = pantry.description
        holder.binding.tvPantryLocation.text = pantry.location
    }

    override fun getItemCount(): Int = pantryList.size
}
