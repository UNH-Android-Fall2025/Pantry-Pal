package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FIXED MainActivity - Removed binding error
 * Firebase authentication check happens first, then routes to appropriate activity
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Check if user is logged in
        if (currentUser == null) {
            // Not logged in - go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Logged in - save user info and proceed
            val userEmail = currentUser.email ?: "user@example.com"
            val userName = currentUser.displayName ?: userEmail.substringBefore("@")

            // Save to SharedPreferences
            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
            sharedPref.edit()
                .putString("user_email", userEmail)
                .putString("user_name", userName)
                .apply()

            // Fetch additional user data from Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val fingerprintEnabled = document.getBoolean("fingerprintEnabled") ?: false
                    val username = document.getString("username")
                        ?: "User${System.currentTimeMillis().toString().takeLast(6)}"

                    // Save fingerprint preference
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("fingerprint_enabled", fingerprintEnabled).apply()

                    // Save username
                    val userPrefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                    userPrefs.edit()
                        .putString("user_username", username)
                        .putString("user_name", username)
                        .apply()

                    // Navigate to appropriate screen
                    val next = if (fingerprintEnabled)
                        FingerprintActivity::class.java
                    else
                        HomePageActivity::class.java

                    startActivity(Intent(this, next))
                    finish()
                }
                .addOnFailureListener {
                    // Firestore fetch failed - use local prefs
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    val fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)

                    val next = if (fingerprintEnabled)
                        FingerprintActivity::class.java
                    else
                        HomePageActivity::class.java

                    startActivity(Intent(this, next))
                    finish()
                }
        }
    }
}