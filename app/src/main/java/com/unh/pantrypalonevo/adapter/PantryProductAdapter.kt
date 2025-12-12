package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unh.pantrypalonevo.R
import com.unh.pantrypalonevo.databinding.ItemPantryProductBinding
import com.unh.pantrypalonevo.model.PantryProduct

class PantryProductAdapter(
    private val products: List<PantryProduct>,
    private val onSelectionChanged: (PantryProduct, Boolean) -> Unit
) : RecyclerView.Adapter<PantryProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemPantryProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        android.util.Log.d("PantryProductAdapter", "onCreateViewHolder called")
        val binding = ItemPantryProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        android.util.Log.d("PantryProductAdapter", "Binding product at position $position: ${product.name}")
        with(holder.binding) {
            tvProductName.text = product.name
            tvProductDescription.text = product.description.ifBlank { "Quantity: ${product.quantity}" }
            
            // Load image if available
            if (!product.imageUrl.isNullOrBlank()) {
                try {
                    Glide.with(root.context)
                        .load(product.imageUrl)
                        .placeholder(R.drawable.ic_image_not_supported)
                        .error(R.drawable.ic_image_not_supported)
                        .into(ivProductImage)
                } catch (e: Exception) {
                    android.util.Log.e("PantryProductAdapter", "Error loading image: ${e.message}")
                    ivProductImage.setImageResource(R.drawable.ic_image_not_supported)
                }
            } else {
                ivProductImage.setImageResource(R.drawable.ic_image_not_supported)
            }

            // Set checkbox state
            cbSelect.isChecked = product.selected

            // Handle checkbox click
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                product.selected = isChecked
                onSelectionChanged(product, isChecked)
            }

            // Also allow clicking the whole item to toggle
            root.setOnClickListener {
                cbSelect.isChecked = !cbSelect.isChecked
            }
        }
    }

    override fun getItemCount(): Int {
        val count = products.size
        android.util.Log.d("PantryProductAdapter", "getItemCount() = $count")
        return count
    }
}

