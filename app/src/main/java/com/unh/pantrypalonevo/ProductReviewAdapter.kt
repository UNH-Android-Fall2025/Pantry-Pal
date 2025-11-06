package com.unh.pantrypalonevo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductReviewAdapter(
    private val products: List<DetectedProduct>,
    private val onQuantityChanged: (position: Int, newQuantity: Int) -> Unit
) : RecyclerView.Adapter<ProductReviewAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnMinus: Button = view.findViewById(R.id.btnMinus)
        val btnPlus: Button = view.findViewById(R.id.btnPlus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_review, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvCategory.text = ProductDatabase.getCategoryForProduct(product.name)
        holder.tvQuantity.text = "${product.quantity}"

        holder.btnMinus.setOnClickListener {
            if (product.quantity > 1) {
                product.quantity--
                holder.tvQuantity.text = "${product.quantity}"
                onQuantityChanged(position, product.quantity)
            }
        }

        holder.btnPlus.setOnClickListener {
            product.quantity++
            holder.tvQuantity.text = "${product.quantity}"
            onQuantityChanged(position, product.quantity)
        }
    }

    override fun getItemCount() = products.size
}