package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.R
import com.unh.pantrypalonevo.databinding.ItemPantryBinding
import com.unh.pantrypalonevo.model.Pantry
import kotlin.random.Random

class PantryAdapter(
    private var items: List<Pantry>,
    private val onItemClick: ((Pantry) -> Unit)? = null
) : RecyclerView.Adapter<PantryAdapter.PantryViewHolder>() {

    // List of food icon drawables
    private val foodIcons = listOf(
        R.drawable.bakery_dining_24,
        R.drawable.storefront_24,
        R.drawable.local_convenience_store_24,
        R.drawable.shopping_bag_24,
        R.drawable.soup_kitchen_24
    )

    // Map to store assigned icons for each pantry (by position or stable ID)
    private val pantryIconMap = mutableMapOf<Int, Int>()

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

            // Assign a random icon to each pantry (consistent per pantry)
            // Use pantry name + address as a stable key to ensure same icon for same pantry
            val pantryKey = (item.name + item.address).hashCode()
            val iconRes = pantryIconMap.getOrPut(pantryKey) {
                foodIcons[Random.nextInt(foodIcons.size)]
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
