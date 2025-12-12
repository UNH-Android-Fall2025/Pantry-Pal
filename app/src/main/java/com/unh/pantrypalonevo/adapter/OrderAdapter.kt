package com.unh.pantrypalonevo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.unh.pantrypalonevo.R
import com.unh.pantrypalonevo.databinding.ItemOrderBinding
import com.unh.pantrypalonevo.model.Order
import com.unh.pantrypalonevo.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        with(holder.binding) {
            tvPantryName.text = order.pantryName
            tvPantryAddress.text = order.pantryAddress
            tvItemCount.text = "${order.items.size} item(s)"
            tvStatus.text = order.status.name
            tvStatus.setTextColor(getStatusColor(holder.itemView.context, order.status))
            
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            tvOrderDate.text = dateFormat.format(Date(order.createdAt))

            root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }

    override fun getItemCount() = orders.size

    private fun getStatusColor(context: android.content.Context, status: OrderStatus): Int {
        return when (status) {
            OrderStatus.PENDING -> Color.parseColor("#F59E0B") // Orange
            OrderStatus.CONFIRMED -> Color.parseColor("#10B981") // Green
            OrderStatus.PICKED_UP -> Color.parseColor("#6366F1") // Indigo
            OrderStatus.CANCELLED -> Color.parseColor("#EF4444") // Red
        }
    }
}

