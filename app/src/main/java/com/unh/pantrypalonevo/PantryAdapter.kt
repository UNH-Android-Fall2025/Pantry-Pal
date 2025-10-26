package com.unh.pantrypalonevo.adapter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
            tvPantryDescription.text = item.description
            tvPantryLocation.text = item.address   // matches your XML
            tvDistance.text = item.distance        // matches your XML

            // Tap the whole card -> (optional) open details
            root.setOnClickListener { onItemClick?.invoke(item) }

            // Tap the map icon -> open Google Maps to the pantry address
            btnMapIcon.setOnClickListener {
                val ctx = it.context
                val query = Uri.encode(item.address.ifBlank { item.name })
                val gmmIntentUri = Uri.parse("geo:0,0?q=$query")

                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    // Prefer Google Maps app if present
                    setPackage("com.google.android.apps.maps")
                }

                try {
                    ctx.startActivity(mapIntent)
                } catch (_: ActivityNotFoundException) {
                    // Fallback to browser if Maps app not installed
                    val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, web))
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Pantry>) {
        items = newList
        notifyDataSetChanged()
    }
}
