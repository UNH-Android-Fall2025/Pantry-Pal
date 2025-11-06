package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemSavedProductBinding

class SavedItemsAdapter(
    private val items: MutableList<DetectedProduct>,
    private val onItemsUpdated: (List<DetectedProduct>) -> Unit
) : RecyclerView.Adapter<SavedItemsAdapter.SavedItemViewHolder>() {

    inner class SavedItemViewHolder(
        val binding: ItemSavedProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnRemove.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, items.size - position)
                    onItemsUpdated(items.toList())
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedItemViewHolder {
        val binding = ItemSavedProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedItemViewHolder, position: Int) {
        val product = items[position]
        with(holder.binding) {
            tvProductName.text = product.name
            tvQuantity.text = "Quantity: ${product.quantity}"
            tvConfidence.text = "Confidence: ${(product.confidence * 100).toInt()}%"
        }
    }

    override fun getItemCount(): Int = items.size
}

