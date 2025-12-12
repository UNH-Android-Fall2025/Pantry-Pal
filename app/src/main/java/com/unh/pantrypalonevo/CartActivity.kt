package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.adapter.CartAdapter
import com.unh.pantrypalonevo.databinding.ActivityCartBinding
import com.unh.pantrypalonevo.model.Order
import com.unh.pantrypalonevo.model.OrderItem
import com.unh.pantrypalonevo.model.OrderStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var pantryId: String? = null
    private var pantryName: String? = null
    private var pantryAddress: String? = null

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
                // Update the item quantity in the list
                val index = cartItems.indexOf(item)
                if (index != -1) {
                    cartItems[index].quantity = newQuantity
                    cartAdapter.notifyItemChanged(index)
                }
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
            // Always allow navigation to recipes - if cart is empty, RecipeActivity will show dialog
            val intent = Intent(this, RecipeActivity::class.java)
            if (cartItems.isNotEmpty()) {
                intent.putParcelableArrayListExtra("cart_items", ArrayList(cartItems))
            }
            startActivity(intent)
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
        
        // Load pantry info from intent
        pantryId = intent.getStringExtra("pantry_id")
        pantryName = intent.getStringExtra("pantry_name")
        pantryAddress = intent.getStringExtra("pantry_address")
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
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to proceed with checkout", Toast.LENGTH_LONG).show()
            return
        }
        
        // Get pantry info from first item or intent
        val firstItem = cartItems.firstOrNull()
        val finalPantryId = firstItem?.pantryId ?: pantryId
        val finalPantryName = firstItem?.pantryName ?: pantryName ?: "Unknown Pantry"
        val finalPantryAddress = pantryAddress ?: "Unknown Address"
        
        if (finalPantryId.isNullOrBlank()) {
            Toast.makeText(this, "Unable to identify pantry. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Show confirmation dialog
        showCheckoutConfirmationDialog(finalPantryId, finalPantryName, finalPantryAddress)
    }
    
    private fun showCheckoutConfirmationDialog(pantryId: String, pantryName: String, pantryAddress: String) {
        val currentUser = auth.currentUser ?: return
        
        val message = buildString {
            append("Confirm order for:\n\n")
            append("Pantry: $pantryName\n")
            append("Address: $pantryAddress\n\n")
            append("Items (${cartItems.size}):\n")
            cartItems.forEach { item ->
                append("  • ${item.name} (Qty: ${item.quantity})\n")
            }
            append("\nThe donor will be notified of your order.")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Order")
            .setMessage(message)
            .setPositiveButton("Confirm Order") { _, _ ->
                createOrder(pantryId, pantryName, pantryAddress)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createOrder(pantryId: String, pantryName: String, pantryAddress: String) {
        val currentUser = auth.currentUser ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Get user info
                val userPrefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                val recipientName = userPrefs.getString("user_name", null) 
                    ?: currentUser.displayName 
                    ?: currentUser.email?.substringBefore("@") 
                    ?: "Anonymous User"
                
                // Get donor info from pantry
                val pantryRef = firestore.collection("pantries").document(pantryId)
                val pantryDoc = pantryRef.get().await()
                
                // Get first donor (assuming single donor per pantry for now)
                val donorsSnapshot = pantryRef.collection("donors").get().await()
                if (donorsSnapshot.isEmpty) {
                    Toast.makeText(this@CartActivity, "Error: No donor found for this pantry", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    return@launch
                }
                
                val donorDoc = donorsSnapshot.documents.first()
                val donorId = donorDoc.id
                val donorName = donorDoc.getString("name") ?: "Unknown Donor"
                
                // Create order items
                val orderItems = cartItems.map { item ->
                    OrderItem(
                        itemId = item.itemId ?: item.id,
                        name = item.name,
                        quantity = item.quantity,
                        category = item.category,
                        imageUrl = item.imageUrl
                    )
                }
                
                // Create order document
                val orderId = firestore.collection("orders").document().id
                val order = Order(
                    orderId = orderId,
                    pantryId = pantryId,
                    pantryName = pantryName,
                    pantryAddress = pantryAddress,
                    donorId = donorId,
                    donorName = donorName,
                    recipientId = currentUser.uid,
                    recipientName = recipientName,
                    recipientEmail = currentUser.email ?: "",
                    items = orderItems,
                    status = OrderStatus.PENDING,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Save order to Firestore
                val orderData = hashMapOf(
                    "orderId" to order.orderId,
                    "pantryId" to order.pantryId,
                    "pantryName" to order.pantryName,
                    "pantryAddress" to order.pantryAddress,
                    "donorId" to order.donorId,
                    "donorName" to order.donorName,
                    "recipientId" to order.recipientId,
                    "recipientName" to order.recipientName,
                    "recipientEmail" to order.recipientEmail,
                    "items" to order.items.map { item ->
                        hashMapOf(
                            "itemId" to item.itemId,
                            "name" to item.name,
                            "quantity" to item.quantity,
                            "category" to item.category,
                            "imageUrl" to (item.imageUrl ?: "")
                        )
                    },
                    "status" to order.status.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "notes" to order.notes
                )
                
                // Save to orders collection
                firestore.collection("orders").document(orderId).set(orderData).await()
                
                // Save to user's order history
                firestore.collection("users").document(currentUser.uid)
                    .collection("orders").document(orderId).set(orderData).await()
                
                // Save to donor's claims
                firestore.collection("users").document(donorId)
                    .collection("claims").document(orderId).set(orderData).await()
                
                // Save to pantry's claims
                pantryRef.collection("claims").document(orderId).set(orderData).await()
                
                // Send notification to donor
                sendDonorNotification(donorId, recipientName, orderItems.size, pantryName)
                
                Log.d("CartActivity", "✅ Order created successfully: $orderId")
                
                binding.progressBar.visibility = View.GONE
                
                // Show success message and navigate
                AlertDialog.Builder(this@CartActivity)
                    .setTitle("Order Confirmed!")
                    .setMessage("Your order has been placed. The donor will be notified.\n\nOrder ID: $orderId")
                    .setPositiveButton("View Orders") { _, _ ->
                        val intent = Intent(this@CartActivity, OrderHistoryActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("OK") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
                
            } catch (e: Exception) {
                Log.e("CartActivity", "❌ Error creating order", e)
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CartActivity, "Error creating order: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun sendDonorNotification(donorId: String, recipientName: String, itemCount: Int, pantryName: String) {
        lifecycleScope.launch {
            try {
                // Get donor's notification token
                val donorDoc = firestore.collection("users").document(donorId).get().await()
                val notificationToken = donorDoc.getString("notificationToken") ?: ""
                
                if (notificationToken.isNotEmpty()) {
                    // Send notification via FCM
                    val title = "New Order Received!"
                    val body = "$recipientName has placed an order for $itemCount item(s) from $pantryName"
                    
                    // Queue notification for sending (actual FCM sending should be via Cloud Functions)
                    lifecycleScope.launch {
                        com.unh.pantrypalonevo.NotificationHelper.sendNotification(
                            token = notificationToken,
                            title = title,
                            body = body
                        )
                    }
                    
                    Log.d("CartActivity", "✅ Notification sent to donor: $donorId")
                } else {
                    Log.w("CartActivity", "⚠️ Donor has no notification token")
                }
            } catch (e: Exception) {
                Log.e("CartActivity", "❌ Error sending notification", e)
            }
        }
    }
}
