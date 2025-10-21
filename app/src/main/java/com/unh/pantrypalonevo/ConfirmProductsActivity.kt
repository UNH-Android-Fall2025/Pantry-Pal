package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityConfirmProductsBinding

class ConfirmProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmProductsBinding
    private lateinit var detectedProducts: ArrayList<DetectedProduct>
    private lateinit var adapter: ProductConfirmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detectedProducts = intent.getParcelableArrayListExtra("detected_products")
            ?: arrayListOf()

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = ProductConfirmAdapter(detectedProducts) { product, action ->
            when (action) {
                "approve" -> product.approved = true
                "deny" -> detectedProducts.remove(product)
                "quantity_change" -> {
                    // Handle quantity change
                }
            }
            adapter.notifyDataSetChanged()
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnAddManually.setOnClickListener {
            // TODO: Add manual product entry dialog
            Toast.makeText(this, "Manual entry coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPublish.setOnClickListener {
            val approvedProducts = detectedProducts.filter { it.approved }
            if (approvedProducts.isEmpty()) {
                Toast.makeText(this, "Please approve at least one product",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to location input
            val intent = Intent(this, PantryLocationActivity::class.java)
            intent.putParcelableArrayListExtra("approved_products",
                ArrayList(approvedProducts))
            startActivity(intent)
        }
    }
}
