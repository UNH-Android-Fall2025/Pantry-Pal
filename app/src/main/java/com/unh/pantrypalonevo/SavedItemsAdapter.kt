package com.unh.pantrypalonevo

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemSavedProductBinding
import java.io.InputStream

class SavedItemsAdapter(
    private val items: MutableList<DetectedProduct>,
    private val imageUriMap: Map<String, String> = emptyMap(),
    private val onItemsUpdated: (List<DetectedProduct>) -> Unit
) : RecyclerView.Adapter<SavedItemsAdapter.SavedItemViewHolder>() {

    inner class SavedItemViewHolder(
        val binding: ItemSavedProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(product: DetectedProduct) {
            with(binding) {
                tvProductName.text = product.name
                tvQuantity.text = product.quantity.toString()
                tvConfidence.text = "Confidence: ${(product.confidence * 100).toInt()}%"
                
                // Set category
                val category = ProductDatabase.getCategoryForProduct(product.name)
                tvCategory.text = category
                
                // Load image if available
                imageUriMap[product.name]?.let { uriString ->
                    try {
                        val uri: Uri = uriString.toUri()
                        val context = binding.root.context
                        context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            ivProductImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        // Image load failed, keep default background
                        ivProductImage.setImageBitmap(null)
                    }
                } ?: run {
                    // No image available, clear image view
                    ivProductImage.setImageBitmap(null)
                }
                
                // Quantity controls - clear previous listeners to avoid duplicates
                btnQuantityMinus.setOnClickListener(null)
                btnQuantityPlus.setOnClickListener(null)
                
                btnQuantityMinus.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && position < items.size) {
                        val currentProduct = items[position]
                        val newQuantity = (currentProduct.quantity - 1).coerceAtLeast(1)
                        if (newQuantity != currentProduct.quantity) {
                            items[position] = currentProduct.copy(quantity = newQuantity)
                            tvQuantity.text = newQuantity.toString()
                            notifyItemChanged(position)
                            onItemsUpdated(items.toList())
                        }
                    }
                }
                
                btnQuantityPlus.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION && position < items.size) {
                        val currentProduct = items[position]
                        val newQuantity = (currentProduct.quantity + 1).coerceAtMost(99)
                        if (newQuantity != currentProduct.quantity) {
                            items[position] = currentProduct.copy(quantity = newQuantity)
                            tvQuantity.text = newQuantity.toString()
                            notifyItemChanged(position)
                            onItemsUpdated(items.toList())
                        }
                    }
                }
                
                // Remove button
                btnRemove.setOnClickListener {
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
