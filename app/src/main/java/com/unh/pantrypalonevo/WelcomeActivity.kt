package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.unh.pantrypalonevo.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Sign In button - navigate to login
        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(this, SimpleLoginActivity::class.java))
            finish()
        }

        // Continue as Guest button - navigate to home without authentication
        binding.btnContinueAsGuest.setOnClickListener {
            // Mark as guest user in SharedPreferences
            val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_guest", true)
                .putString("user_name", "Guest")
                .apply()

            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }
    }
}

