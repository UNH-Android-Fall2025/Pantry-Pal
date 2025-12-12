package com.unh.pantrypalonevo

import android.content.Intent
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
import com.unh.pantrypalonevo.adapter.OrderAdapter
import com.unh.pantrypalonevo.databinding.ActivityDonorDashboardBinding
import com.unh.pantrypalonevo.model.Order
import com.unh.pantrypalonevo.model.OrderItem
import com.unh.pantrypalonevo.model.OrderStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DonorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDonorDashboardBinding
    private val claims = mutableListOf<Order>()
    private lateinit var claimAdapter: OrderAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        loadClaims()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Claims"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        claimAdapter = OrderAdapter(claims) { order ->
            // Show claim details with action buttons
            showClaimDetails(order)
        }
        binding.rvClaims.layoutManager = LinearLayoutManager(this)
        binding.rvClaims.adapter = claimAdapter
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

    private fun loadClaims() {
        val currentUser = auth.currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val claimsSnapshot = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("claims")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                claims.clear()

                claimsSnapshot.documents.forEach { doc ->
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

                        claims.add(order)
                    } catch (e: Exception) {
                        Log.e("DonorDashboard", "Error parsing claim ${doc.id}", e)
                    }
                }

                claimAdapter.notifyDataSetChanged()
                updateEmptyState()

                Log.d("DonorDashboard", "Loaded ${claims.size} claims")

            } catch (e: Exception) {
                Log.e("DonorDashboard", "Error loading claims", e)
                e.printStackTrace()
                Toast.makeText(this@DonorDashboardActivity, "Error loading claims: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateEmptyState() {
        if (claims.isEmpty()) {
            binding.rvClaims.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvClaims.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun showClaimDetails(order: Order) {
        val itemsText = order.items.joinToString("\n") { "  • ${it.name} (Qty: ${it.quantity})" }
        val message = """
            Order ID: ${order.orderId}
            
            Pantry: ${order.pantryName}
            Address: ${order.pantryAddress}
            
            Recipient: ${order.recipientName}
            Email: ${order.recipientEmail}
            
            Status: ${order.status.name}
            
            Items:
            $itemsText
            
            Created: ${java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()).format(java.util.Date(order.createdAt))}
        """.trimIndent()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Claim Details")
            .setMessage(message)
            .setPositiveButton("OK", null)

        // Add action buttons based on status
        when (order.status) {
            OrderStatus.PENDING -> {
                dialog.setNeutralButton("Confirm") { _, _ ->
                    updateOrderStatus(order.orderId, OrderStatus.CONFIRMED)
                }
                dialog.setNegativeButton("Cancel Order") { _, _ ->
                    updateOrderStatus(order.orderId, OrderStatus.CANCELLED)
                }
            }
            OrderStatus.CONFIRMED -> {
                dialog.setNeutralButton("Mark as Picked Up") { _, _ ->
                    updateOrderStatus(order.orderId, OrderStatus.PICKED_UP)
                }
            }
            else -> {
                // No additional actions for PICKED_UP or CANCELLED
            }
        }

        dialog.show()
    }

    private fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val updateData = hashMapOf<String, Any>(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                // Update in orders collection
                firestore.collection("orders").document(orderId).update(updateData).await()

                // Update in user's order history
                val currentUser = auth.currentUser ?: return@launch
                firestore.collection("users").document(currentUser.uid)
                    .collection("claims").document(orderId).update(updateData).await()

                // Update in recipient's order history
                val orderDoc = firestore.collection("orders").document(orderId).get().await()
                val recipientId = orderDoc.getString("recipientId") ?: ""
                if (recipientId.isNotEmpty()) {
                    firestore.collection("users").document(recipientId)
                        .collection("orders").document(orderId).update(updateData).await()
                }

                // Update in pantry's claims
                val pantryId = orderDoc.getString("pantryId") ?: ""
                if (pantryId.isNotEmpty()) {
                    firestore.collection("pantries").document(pantryId)
                        .collection("claims").document(orderId).update(updateData).await()
                }

                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@DonorDashboardActivity, "Order status updated to ${newStatus.name}", Toast.LENGTH_SHORT).show()
                
                // Reload claims
                loadClaims()

            } catch (e: Exception) {
                Log.e("DonorDashboard", "Error updating order status", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@DonorDashboardActivity, "Error updating status: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

