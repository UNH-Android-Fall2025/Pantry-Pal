package com.unh.pantrypalonevo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PantryLocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Create proper layout later
        Toast.makeText(this, "Location Activity - Coming Soon!", Toast.LENGTH_SHORT).show()

        val products = intent.getParcelableArrayListExtra<DetectedProduct>("approved_products")
        // For now, just log the products
        products?.forEach { product ->
            println("Product: ${product.name}, Quantity: ${product.quantity}")
        }
    }
}
