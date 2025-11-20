package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.adapter.CartAdapter
import com.unh.pantrypalonevo.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadCartItems()
        updateUI()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = cartItems,
            onQuantityChanged = { _, _ ->
                // Quantity changed - no price to update
            },
            onItemDeleted = { item ->
                removeItemFromCart(item)
            }
        )

        binding.rvCartItems.layoutManager = LinearLayoutManager(this)
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                proceedToCheckout()
            }
        }

        binding.btnBrowsePantries.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnRecipes.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Add items to cart to generate recipes", Toast.LENGTH_SHORT).show()
            } else {
                // Pass cart items to RecipeActivity
                val intent = Intent(this, RecipeActivity::class.java)
                intent.putParcelableArrayListExtra("cart_items", ArrayList(cartItems))
                startActivity(intent)
            }
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "You're already on the cart page", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.bottomNavigation.bringToFront()
    }

    private fun loadCartItems() {
        val itemsFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("cart_items", CartItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<CartItem>("cart_items")
        }

        if (itemsFromIntent != null && itemsFromIntent.isNotEmpty()) {
            cartItems.clear()
            cartItems.addAll(itemsFromIntent)
            cartAdapter.notifyDataSetChanged()
        }
    }

    private fun removeItemFromCart(item: CartItem) {
        cartAdapter.removeItem(item)
        updateUI()
        Toast.makeText(this, "${item.name} removed", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (cartItems.isEmpty()) {
            binding.rvCartItems.visibility = View.GONE
            binding.emptyCartContainer.visibility = View.VISIBLE
        } else {
            binding.rvCartItems.visibility = View.VISIBLE
            binding.emptyCartContainer.visibility = View.GONE
        }
    }

    private fun proceedToCheckout() {
        Toast.makeText(this, "Checkout coming soon!", Toast.LENGTH_SHORT).show()
    }
}
