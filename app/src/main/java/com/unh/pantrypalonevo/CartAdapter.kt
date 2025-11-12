package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unh.pantrypalonevo.databinding.ItemCartBinding
import com.unh.pantrypalonevo.model.CartItem

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (Double) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        with(holder.binding) {
            tvItemName.text = item.name
            tvItemPrice.text = "$${item.price}"
            tvQuantity.text = item.quantity.toString()

            Glide.with(ivItemImage.context)
                .load(item.imageUrl)
                .into(ivItemImage)

            btnIncrease.setOnClickListener {
                item.quantity++
                tvQuantity.text = item.quantity.toString()
                onQuantityChanged(getTotal())
            }

            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    tvQuantity.text = item.quantity.toString()
                    onQuantityChanged(getTotal())
                }
            }

            btnRemove.setOnClickListener {
                val removed = cartItems.removeAt(position)
                notifyItemRemoved(position)
                onQuantityChanged(getTotal())
            }
        }
    }

    override fun getItemCount() = cartItems.size

    fun getTotal(): Double = cartItems.sumOf { it.price * it.quantity }
}
