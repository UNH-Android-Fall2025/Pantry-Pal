package com.unh.pantrypalonevo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.unh.pantrypalonevo.ui.theme.PantryPaloneVoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
<<<<<<< HEAD
        enableEdgeToEdge()
        setContent {
            PantryPaloneVoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
=======

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // ADDED: Save user email for dynamic greeting
            val userEmail = currentUser.email ?: "user@example.com"
            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
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
>>>>>>> origin/dev
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PantryPaloneVoTheme {
        Greeting("Android")
    }
}