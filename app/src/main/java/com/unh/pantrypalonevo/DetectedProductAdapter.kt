package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.databinding.ItemDetectedProductBinding

class DetectedProductAdapter(
    private val onProductConfirm: (DetectedProduct) -> Unit,
    private val onProductReject: (DetectedProduct) -> Unit
) : RecyclerView.Adapter<DetectedProductAdapter.DetectedProductViewHolder>() {

    private val products = mutableListOf<DetectedProduct>()
    private var expandedPosition = -1

    inner class DetectedProductViewHolder(
        val binding: ItemDetectedProductBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectedProductViewHolder {
        val binding = ItemDetectedProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetectedProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetectedProductViewHolder, position: Int) {
        val product = products[position]
        val isExpanded = expandedPosition == position

        with(holder.binding) {
            tvProductName.text = product.name
            tvConfidence.text = "Confidence: ${(product.confidence * 100).toInt()}%"

            // Update tick mark state
            btnTickMark.isEnabled = !product.approved
            btnTickMark.alpha = if (product.approved) 0.4f else 1f

            // Show/hide action buttons based on expanded state
            layoutActionButtons.visibility = if (isExpanded && !product.approved) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Tick mark click - toggle expansion
            btnTickMark.setOnClickListener {
                if (!product.approved) {
                    val previousExpanded = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position
                    
                    if (previousExpanded != -1 && previousExpanded != position) {
                        notifyItemChanged(previousExpanded)
                    }
                    notifyItemChanged(position)
                }
            }

            // Confirm button click
            btnConfirm.setOnClickListener {
                if (!product.approved) {
                    onProductConfirm(product)
                    expandedPosition = -1
                    notifyItemChanged(position)
                }
            }

            // Reject button click
            btnReject.setOnClickListener {
                onProductReject(product)
                expandedPosition = -1
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = products.size

    fun submitList(newProducts: List<DetectedProduct>) {
        products.clear()
        products.addAll(newProducts)
        expandedPosition = -1
        notifyDataSetChanged()
    }

    fun removeProduct(product: DetectedProduct) {
        val index = products.indexOf(product)
        if (index != -1) {
            if (expandedPosition == index) {
                expandedPosition = -1
            } else if (expandedPosition > index) {
                expandedPosition--
            }
            products.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addProduct(product: DetectedProduct) {
        products.add(product)
        notifyItemInserted(products.lastIndex)
    }

    fun markProductAsApproved(product: DetectedProduct) {
        val index = products.indexOf(product)
        if (index != -1) {
            products[index] = product.copy(approved = true)
            if (expandedPosition == index) {
                expandedPosition = -1
            }
            notifyItemChanged(index)
        }
    }
}
