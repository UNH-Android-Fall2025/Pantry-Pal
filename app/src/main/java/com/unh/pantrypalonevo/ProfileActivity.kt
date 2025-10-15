package com.unh.pantrypalonevo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.unh.pantrypalonevo.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

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
        // UPDATED: Settings button with logout option
        binding.btnSettings.setOnClickListener {
            showSettingsOptions()
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

    // NEW: Settings options with logout
    private fun showSettingsOptions() {
        val settingsOptions = arrayOf(
            "Account Settings",
            "Privacy Settings",
            "Notifications",
            "Help & Support",
            "Logout"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Settings")
        builder.setItems(settingsOptions) { dialog, which ->
            when (which) {
                0 -> Toast.makeText(this, "Account Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
                1 -> Toast.makeText(this, "Privacy Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "Help & Support - Coming Soon!", Toast.LENGTH_SHORT).show()
                4 -> showLogoutDialog() // Logout
            }
        }
        builder.show()
    }

    // NEW: Logout confirmation dialog
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes, Logout") { dialog, which ->
            performLogout()
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        builder.show()
    }

    // NEW: Actual logout function
    private fun performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // 2. Sign out from Google (if used)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        // 3. Clear saved user preferences
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val pantryPrefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
        pantryPrefs.edit().clear().apply()

        // 4. Show success message
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

        // 5. Navigate to LoginActivity and clear all previous activities
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
