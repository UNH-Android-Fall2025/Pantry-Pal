package com.unh.pantrypalonevo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityReviewProductsBinding

/**
 * REVIEW ALL PRODUCTS SCREEN
 * Shows all products added in background list
 * User can edit quantities and publish to Firebase
 */
class ReviewProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewProductsBinding
    private val productList = mutableListOf<DetectedProduct>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get products from intent
        productList.addAll(
            intent.getParcelableArrayListExtra("products") ?: emptyList()
        )

        setupRecyclerView()
        setupClickListeners()

        binding.tvTotalItems.text = "${productList.size} items ready to add"
    }

    private fun setupRecyclerView() {
        val adapter = ProductReviewAdapter(productList) { position, newQuantity ->
            productList[position].quantity = newQuantity
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnPublishAll.setOnClickListener {
            publishProducts()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun publishProducts() {
        // TODO: Publish to Firebase
        Toast.makeText(
            this,
            "✓ Published ${productList.size} products!",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }
}