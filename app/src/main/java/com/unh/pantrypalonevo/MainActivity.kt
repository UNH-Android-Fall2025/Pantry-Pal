package com.unh.pantrypalonevo

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
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            val userEmail = currentUser.email ?: "user@example.com"
            val userName = currentUser.displayName ?: userEmail.substringBefore("@")

            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
            sharedPref.edit()
                .putString("user_email", userEmail)
                .putString("user_name", userName)
                .apply()

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val fingerprintEnabled = document.getBoolean("fingerprintEnabled") ?: false
                    val username = document.getString("username") ?: "User${System.currentTimeMillis().toString().takeLast(6)}"
                    
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("fingerprint_enabled", fingerprintEnabled).apply()
                    
                    // Save username to SharedPreferences and update user_name to use username
                    val userPrefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                    userPrefs.edit()
                        .putString("user_username", username)
                        .putString("user_name", username) // Update user_name to use username
                        .apply()

                    val next = if (fingerprintEnabled)
                        FingerprintActivity::class.java else HomePageActivity::class.java
                    startActivity(Intent(this, next))
                    finish()
                }
                .addOnFailureListener {
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    val fingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
                    val next = if (fingerprintEnabled)
                        FingerprintActivity::class.java else HomePageActivity::class.java
                    startActivity(Intent(this, next))
                    finish()
                }
        }
    }
}
