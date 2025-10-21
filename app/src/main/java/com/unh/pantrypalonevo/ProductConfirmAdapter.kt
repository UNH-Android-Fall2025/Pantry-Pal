package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductConfirmAdapter(
    private val products: MutableList<DetectedProduct>,
    private val onProductAction: (DetectedProduct, String) -> Unit
) : RecyclerView.Adapter<ProductConfirmAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        val btnDeny: Button = itemView.findViewById(R.id.btnDeny)
        val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_confirm, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvConfidence.text = "Confidence: ${(product.confidence * 100).toInt()}%"
        holder.tvQuantity.text = product.quantity.toString()

        // Quantity controls
        holder.btnMinus.setOnClickListener {
            if (product.quantity > 1) {
                product.quantity--
                holder.tvQuantity.text = product.quantity.toString()
            }
        }

        holder.btnPlus.setOnClickListener {
            product.quantity++
            holder.tvQuantity.text = product.quantity.toString()
        }

        // Approve button
        holder.btnApprove.setOnClickListener {
            product.approved = true
            holder.btnApprove.isEnabled = false
            holder.btnApprove.text = "✓ Approved"
            holder.btnApprove.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            )
            onProductAction(product, "approve")
        }

        // Deny button
        holder.btnDeny.setOnClickListener {
            onProductAction(product, "deny")
            products.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, products.size)
        }

        // Update UI if already approved
        if (product.approved) {
            holder.btnApprove.isEnabled = false
            holder.btnApprove.text = "✓ Approved"
            holder.btnApprove.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            )
        }
    }

    override fun getItemCount(): Int = products.size
}
