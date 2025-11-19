package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
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
            onQuantityChanged = { item, newQuantity ->
                updateTotals()
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

        // Checkout button
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                proceedToCheckout()
            }
        }

        // Browse Pantries button (empty state)
        binding.btnBrowsePantries.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "Recipes page coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            // Already on cart page
            Toast.makeText(this, "You're already on the cart page", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        // Ensure bottom bar always receives taps
        binding.bottomNavigation.bringToFront()
        binding.bottomNavigation.isClickable = true
    }

    private fun loadCartItems() {
        // TODO: Load cart items from SharedPreferences, Firestore, or local database
        // For now, using sample data or items passed from intent
        
        // Check if items were passed from intent
        val itemsFromIntent = intent.getParcelableArrayListExtra<CartItem>("cart_items")
        if (itemsFromIntent != null && itemsFromIntent.isNotEmpty()) {
            cartItems.clear()
            cartItems.addAll(itemsFromIntent)
            cartAdapter.notifyDataSetChanged()
        } else {
            // Load from SharedPreferences or use sample data for testing
            loadCartFromSharedPreferences()
        }
    }

    private fun loadCartFromSharedPreferences() {
        // TODO: Implement loading from SharedPreferences
        // For now, you can add sample items for testing:
        // addSampleItems()
    }

    private fun addSampleItems() {
        // Sample items for testing
        cartItems.addAll(
            listOf(
                CartItem(
                    name = "Organic Avocado",
                    category = "Produce",
                    price = 5.99,
                    quantity = 2
                ),
                CartItem(
                    name = "Whole Milk",
                    category = "Dairy",
                    price = 3.49,
                    quantity = 1
                ),
                CartItem(
                    name = "Sourdough Bread",
                    category = "Bakery",
                    price = 4.25,
                    quantity = 1
                )
            )
        )
        cartAdapter.notifyDataSetChanged()
    }

    private fun removeItemFromCart(item: CartItem) {
        cartItems.remove(item)
        cartAdapter.removeItem(item)
        updateUI()
        Toast.makeText(this, "${item.name} removed from cart", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        updateTotals()
        updateEmptyState()
    }

    private fun updateTotals() {
        val subtotal = cartItems.sumOf { it.getTotalPrice() }
        val total = subtotal // Add tax/shipping if needed

        binding.tvSubtotalAmount.text = String.format("$%.2f", subtotal)
        binding.tvTotalAmount.text = String.format("$%.2f", total)

        // Enable/disable checkout button based on cart state
        binding.btnCheckout.isEnabled = cartItems.isNotEmpty()
        binding.btnCheckout.alpha = if (cartItems.isNotEmpty()) 1.0f else 0.5f
    }

    private fun updateEmptyState() {
        if (cartItems.isEmpty()) {
            binding.rvCartItems.visibility = android.view.View.GONE
            binding.emptyCartContainer.visibility = android.view.View.VISIBLE
        } else {
            binding.rvCartItems.visibility = android.view.View.VISIBLE
            binding.emptyCartContainer.visibility = android.view.View.GONE
        }
    }

    private fun proceedToCheckout() {
        // TODO: Implement checkout flow
        // This could navigate to a checkout activity or show a confirmation dialog
        Toast.makeText(this, "Checkout functionality coming soon!", Toast.LENGTH_SHORT).show()
        
        // Example: Navigate to checkout activity
        // val intent = Intent(this, CheckoutActivity::class.java)
        // intent.putParcelableArrayListExtra("cart_items", ArrayList(cartItems))
        // startActivity(intent)
    }

    // Public method to add items to cart (can be called from other activities)
    fun addItemToCart(item: CartItem) {
        // Check if item already exists in cart
        val existingItem = cartItems.find { it.name == item.name }
        if (existingItem != null) {
            existingItem.quantity += item.quantity
            cartAdapter.notifyDataSetChanged()
        } else {
            cartItems.add(item)
            cartAdapter.notifyItemInserted(cartItems.size - 1)
        }
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        // Save cart to SharedPreferences when leaving the activity
        saveCartToSharedPreferences()
    }

    private fun saveCartToSharedPreferences() {
        // TODO: Implement saving cart to SharedPreferences
        // You can use Gson or similar to serialize the cart items
    }
}

