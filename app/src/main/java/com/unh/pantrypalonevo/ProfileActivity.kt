package com.unh.pantrypalonevo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.unh.pantrypalonevo.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        // UPDATED: Load dynamic user profile instead of static
        loadDynamicUserProfile()
    }

    // UPDATED: Load dynamic profile from saved email
    private fun loadDynamicUserProfile() {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail != null) {
            // Extract username from email dynamically
            val username = extractUsernameFromEmail(userEmail)
            binding.tvUserName.text = username

            // Show full email instead of phone number
            binding.tvPhoneNumber.text = userEmail
        } else {
            // Fallback if no email stored
            binding.tvUserName.text = "User"
            binding.tvPhoneNumber.text = "No email available"
        }
    }

    // SAME function as HomePageActivity for consistency
    private fun extractUsernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            val username = email.substringBefore("@")

            // Handle different formats dynamically
            when {
                username.contains(".") -> {
                    // john.doe@gmail.com → "John Doe"
                    username.split(".").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                username.contains("_") -> {
                    // john_doe@gmail.com → "John Doe"
                    username.split("_").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                else -> {
                    // rajul@gmail.com → "Rajul"
                    username.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }
            }
        } else {
            "User"
        }
    }

    private fun setupClickListeners() {
        // Settings button
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Profile action buttons
        binding.btnPantryCode.setOnClickListener {
            showPantryCodeDialog()
        }

        binding.btnPublishPantry.setOnClickListener {
            Toast.makeText(this, "Publish Pantry - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareProfile.setOnClickListener {
            shareProfile()
        }

        binding.btnRecipeSaved.setOnClickListener {
            Toast.makeText(this, "Recipe Saved - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            finish() // Go back to home
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "Recipes - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "Cart - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPantryCodeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pantry Code")
        builder.setMessage("Your Pantry Code: PNT-${(Math.random() * 10000).toInt()}")
        builder.setPositiveButton("Copy") { dialog, which ->
            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Close") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun shareProfile() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my PantryPal profile!")
        startActivity(Intent.createChooser(shareIntent, "Share Profile"))
    }
}
