package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.CartItem
import com.unh.pantrypalonevo.R

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem, Int) -> Unit,
    private val onItemDeleted: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        // Set product info
        holder.tvProductName.text = item.name
        holder.tvCategory.text = item.category
        holder.tvPrice.text = String.format("$%.2f", item.price)
        holder.tvQuantity.text = item.quantity.toString()

        // Quantity controls
        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                holder.tvQuantity.text = item.quantity.toString()
                onQuantityChanged(item, item.quantity)
            }
        }

        holder.btnPlus.setOnClickListener {
            item.quantity++
            holder.tvQuantity.text = item.quantity.toString()
            onQuantityChanged(item, item.quantity)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            onItemDeleted(item)
        }

        // Product image placeholder (you can load actual images here)
        // For now, we'll just use the background from the layout
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateList(newList: List<CartItem>) {
        cartItems.clear()
        cartItems.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(item: CartItem) {
        val position = cartItems.indexOf(item)
        if (position != -1) {
            cartItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, cartItems.size)
        }
    }
}

