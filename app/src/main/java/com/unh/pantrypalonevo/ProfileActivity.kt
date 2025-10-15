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
        loadDynamicUserProfile()
    }

    private fun loadDynamicUserProfile() {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail != null) {
            val username = extractUsernameFromEmail(userEmail)
            binding.tvUserName.text = username
            binding.tvPhoneNumber.text = userEmail
        } else {
            binding.tvUserName.text = "User"
            binding.tvPhoneNumber.text = "No email available"
        }
    }

    private fun extractUsernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            val username = email.substringBefore("@")

            when {
                username.contains(".") -> {
                    username.split(".").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                username.contains("_") -> {
                    username.split("_").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                else -> {
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
        // UPDATED: Settings button WITHOUT logout option
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

        // NEW: Explicit logout button functionality
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            finish()
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

    // UPDATED: Settings options WITHOUT logout
    private fun showSettingsOptions() {
        val settingsOptions = arrayOf(
            "Account Settings",
            "Privacy Settings",
            "Notifications",
            "Help & Support"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Settings")
        builder.setItems(settingsOptions) { dialog, which ->
            when (which) {
                0 -> Toast.makeText(this, "Account Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
                1 -> Toast.makeText(this, "Privacy Settings - Coming Soon!", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(this, "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "Help & Support - Coming Soon!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    // Logout confirmation dialog
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

    // Actual logout function
    private fun performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Sign out from Google (if used)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        // Clear saved user preferences
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val pantryPrefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
        pantryPrefs.edit().clear().apply()

        // Show success message
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

        // Navigate to LoginActivity and clear all previous activities
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
