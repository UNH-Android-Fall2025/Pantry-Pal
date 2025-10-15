package com.unh.pantrypalonevo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // User not logged in - go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // CRITICAL: Save user email for global account-wide greeting
            val userEmail = currentUser.email ?: "user@example.com"
            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("user_email", userEmail).apply()

            // Fetch fingerprint preference from Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val fingerprintEnabled = document.getBoolean("fingerprintEnabled") ?: false

                    // Save to SharedPreferences for offline access
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("fingerprint_enabled", fingerprintEnabled).apply()

                    if (fingerprintEnabled) {
                        startActivity(Intent(this, FingerprintActivity::class.java))
                    } else {
                        startActivity(Intent(this, HomePageActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    // Fallback to SharedPreferences if Firestore fails
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    val fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)

                    if (fingerprintEnabled) {
                        startActivity(Intent(this, FingerprintActivity::class.java))
                    } else {
                        startActivity(Intent(this, HomePageActivity::class.java))
                    }
                    finish()
                }
        }
    }
}
