package com.unh.pantrypalonevo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.unh.pantrypalonevo.CartItem
import com.unh.pantrypalonevo.databinding.ItemCartBinding

class CartAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onQuantityChanged: (item: CartItem, newQuantity: Int) -> Unit,
    private val onItemDeleted: (item: CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
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
                onQuantityChanged(item, item.quantity)
            }

            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    tvQuantity.text = item.quantity.toString()
                    onQuantityChanged(item, item.quantity)
                }
            }

            btnRemove.setOnClickListener {
                onItemDeleted(item)
            }
        }
    }

    override fun getItemCount() = cartItems.size

    fun removeItem(item: CartItem) {
        val index = cartItems.indexOf(item)
        if (index != -1) {
            cartItems.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
