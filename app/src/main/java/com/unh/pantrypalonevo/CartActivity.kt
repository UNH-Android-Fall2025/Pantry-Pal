package com.unh.pantrypalonevo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.adapter.CartAdapter
import com.unh.pantrypalonevo.databinding.ActivityCartBinding
import com.unh.pantrypalonevo.model.CartItem

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private val cartList = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve cart data from shared preferences (temporary)
        val prefs = getSharedPreferences("PantryPal_Cart", MODE_PRIVATE)
        val cartSet = prefs.getStringSet("cart_items", emptySet()) ?: emptySet()

        cartList.clear()
        for (itemString in cartSet) {
            val parts = itemString.split("|")
            if (parts.size == 4) {
                val name = parts[0]
                val imageUrl = parts[1]
                val qty = parts[2].toInt()
                val price = parts[3].toDouble()
                cartList.add(CartItem(name, imageUrl, qty, price))
            }
        }

        cartAdapter = CartAdapter(cartList) { updateTotal() }
        binding.rvCartItems.layoutManager = LinearLayoutManager(this)
        binding.rvCartItems.adapter = cartAdapter

        updateTotal()

        binding.btnCheckout.setOnClickListener {
            Toast.makeText(this, "Order placed successfully ✅", Toast.LENGTH_SHORT).show()
            prefs.edit().clear().apply()
            cartList.clear()
            cartAdapter.notifyDataSetChanged()
            updateTotal()
        }
    }

    private fun updateTotal() {
        val total = cartAdapter.getTotal()
        binding.tvTotal.text = "Total: $%.2f".format(total)
    }
}
