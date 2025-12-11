package com.unh.pantrypalonevo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * FIXED MainActivity - Removed binding error
 * Firebase authentication check happens first, then routes to appropriate activity
 */
class MainActivity : ComponentActivity() {
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, notifications will work
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val userPrefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val hasLoggedInBefore = userPrefs.getBoolean("has_logged_in_before", false)

        // Check if user is logged in
        if (currentUser == null) {
            // Not logged in - go to WelcomeActivity for first-time users, otherwise to login
            val next = if (hasLoggedInBefore)
                SimpleLoginActivity::class.java
            else
                WelcomeActivity::class.java

            startActivity(Intent(this, next))
            finish()
        } else {
            // Logged in - save user info and proceed
            val userEmail = currentUser.email ?: "user@example.com"
            val userName = currentUser.displayName ?: userEmail.substringBefore("@")

            // Save to SharedPreferences
            userPrefs.edit()
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
                    userPrefs.edit()
                        .putString("user_username", username)
                        .putString("user_name", username)
                        .apply()

                    // Get and save FCM token
                    NotificationHelper.getAndSaveToken(currentUser.uid)

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