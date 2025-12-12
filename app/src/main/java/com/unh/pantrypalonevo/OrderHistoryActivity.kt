package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.adapter.OrderAdapter
import com.unh.pantrypalonevo.databinding.ActivityOrderHistoryBinding
import com.unh.pantrypalonevo.model.Order
import com.unh.pantrypalonevo.model.OrderItem
import com.unh.pantrypalonevo.model.OrderStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private val orders = mutableListOf<Order>()
    private lateinit var orderAdapter: OrderAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        loadOrders()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Orders"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(orders) { order ->
            // Show order details
            showOrderDetails(order)
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = orderAdapter
    }

    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnRecipes.setOnClickListener {
            startActivity(Intent(this, RecipeActivity::class.java))
            finish()
        }

        binding.btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun loadOrders() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val ordersSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("orders")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                orders.clear()

                ordersSnapshot.documents.forEach { doc ->
                    try {
                        val orderData = doc.data ?: return@forEach
                        val itemsList = orderData["items"] as? List<Map<String, Any>> ?: emptyList()
                        
                        val orderItems = itemsList.map { itemMap ->
                            OrderItem(
                                itemId = itemMap["itemId"] as? String ?: "",
                                name = itemMap["name"] as? String ?: "",
                                quantity = (itemMap["quantity"] as? Long ?: 1).toInt(),
                                category = itemMap["category"] as? String ?: "",
                                imageUrl = itemMap["imageUrl"] as? String
                            )
                        }

                        val statusStr = orderData["status"] as? String ?: "PENDING"
                        val status = try {
                            OrderStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            OrderStatus.PENDING
                        }

                        val createdAt = (orderData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                            ?: System.currentTimeMillis()

                        val order = Order(
                            orderId = orderData["orderId"] as? String ?: doc.id,
                            pantryId = orderData["pantryId"] as? String ?: "",
                            pantryName = orderData["pantryName"] as? String ?: "",
                            pantryAddress = orderData["pantryAddress"] as? String ?: "",
                            donorId = orderData["donorId"] as? String ?: "",
                            donorName = orderData["donorName"] as? String ?: "",
                            recipientId = orderData["recipientId"] as? String ?: "",
                            recipientName = orderData["recipientName"] as? String ?: "",
                            recipientEmail = orderData["recipientEmail"] as? String ?: "",
                            items = orderItems,
                            status = status,
                            createdAt = createdAt,
                            updatedAt = createdAt,
                            notes = orderData["notes"] as? String ?: ""
                        )

                        orders.add(order)
                    } catch (e: Exception) {
                        Log.e("OrderHistory", "Error parsing order ${doc.id}", e)
                    }
                }

                orderAdapter.notifyDataSetChanged()
                updateEmptyState()

                Log.d("OrderHistory", "Loaded ${orders.size} orders")

            } catch (e: Exception) {
                Log.e("OrderHistory", "Error loading orders", e)
                e.printStackTrace()
                Toast.makeText(this@OrderHistoryActivity, "Error loading orders: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateEmptyState() {
        if (orders.isEmpty()) {
            binding.rvOrders.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvOrders.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun showOrderDetails(order: Order) {
        val itemsText = order.items.joinToString("\n") { "  • ${it.name} (Qty: ${it.quantity})" }
        val message = """
            Order ID: ${order.orderId}
            
            Pantry: ${order.pantryName}
            Address: ${order.pantryAddress}
            
            Donor: ${order.donorName}
            
            Status: ${order.status.name}
            
            Items:
            $itemsText
            
            Created: ${java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(order.createdAt))}
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Order Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}

