package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.R
import com.unh.pantrypalonevo.databinding.ItemPantryBinding
import com.unh.pantrypalonevo.model.Pantry

class PantryAdapter(
    private var items: List<Pantry>,
    private val onItemClick: ((Pantry) -> Unit)? = null
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    inner class PantryViewHolder(val binding: ItemPantryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PantryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvPantryName.text = item.name
            tvPantryLocation.text = item.address
            tvDistance.text = item.distance

            // Set icon based on pantry name or use default
            val iconRes = when {
                item.name.contains("Bread", ignoreCase = true) -> R.drawable.ic_bakery_dining
                item.name.contains("Food Bank", ignoreCase = true) -> R.drawable.ic_food_bank
                item.name.contains("Store", ignoreCase = true) -> R.drawable.ic_local_convenience_store
                else -> R.drawable.ic_bakery_dining // default icon
            }
            ivPantryIcon.setImageResource(iconRes)

            // Tap the whole card -> open details
            root.setOnClickListener { onItemClick?.invoke(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Pantry>) {
        items = newList
        notifyDataSetChanged()
    }
}
