package com.unh.pantrypalonevo

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PantryLocationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Create proper layout later
        Toast.makeText(this, "Location Activity - Coming Soon!", Toast.LENGTH_SHORT).show()

        val products = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("approved_products", DetectedProduct::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<DetectedProduct>("approved_products")
        }
        // log the products
        products?.forEach { product ->
            println("Product: ${product.name}, Quantity: ${product.quantity}")
        }
    }
}
