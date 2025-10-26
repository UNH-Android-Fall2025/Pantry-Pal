package com.unh.pantrypalonevo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.unh.pantrypalonevo.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Fix: ensure bottom bar doesn’t block clicks
        binding.bottomNavigation.bringToFront()
        binding.bottomNavigation.isClickable = true

        // Add safe padding so bottom buttons remain clickable
        binding.root.post {
            val navHeight = binding.bottomNavigation.height.coerceAtLeast(100)
            binding.root.setPadding(
                binding.root.paddingLeft,
                binding.root.paddingTop,
                binding.root.paddingRight,
                navHeight
            )
        }

        setupClickListeners()
        loadDynamicUserProfile()
    }

    private fun loadDynamicUserProfile() {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("user_name", null)
        val savedEmail = prefs.getString("user_email", null)

        // Fallbacks: Firebase displayName → derive from email → "User"
        val authName = FirebaseAuth.getInstance().currentUser?.displayName
        val finalName = when {
            !savedName.isNullOrBlank() -> savedName
            !authName.isNullOrBlank() -> authName
            !savedEmail.isNullOrBlank() -> extractUsernameFromEmail(savedEmail)
            else -> "User"
        }

        binding.tvUserName.text = finalName
        binding.tvPhoneNumber.text =
            savedEmail ?: FirebaseAuth.getInstance().currentUser?.email ?: "No email available"
    }

    private fun extractUsernameFromEmail(email: String): String {
        val base = email.substringBefore("@")
        return base
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") {
                it.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
            }
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener { showSettingsOptions() }

        binding.btnPantryCode.setOnClickListener { showPantryCodeDialog() }

        binding.btnPublishPantry.setOnClickListener {
            startActivity(Intent(this, PublishPantryActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareProfile.setOnClickListener { shareProfile() }

        binding.btnRecipeSaved.setOnClickListener {
            Toast.makeText(this, "Recipe Saved - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        // ✅ Bottom navigation buttons
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnProfile.setOnClickListener {
            Toast.makeText(this, "Already on Profile", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "Recipes - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "Cart - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsOptions() {
        val options =
            arrayOf("Account Settings", "Privacy Settings", "Notifications", "Help & Support")
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                val msg = when (which) {
                    0 -> "Account Settings - Coming Soon!"
                    1 -> "Privacy Settings - Coming Soon!"
                    2 -> "Notifications - Coming Soon!"
                    else -> "Help & Support - Coming Soon!"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes, Logout") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()

        // Clear saved prefs
        getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("PantryPrefs", MODE_PRIVATE).edit().clear().apply()

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SimpleLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showPantryCodeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Pantry Code")
            .setMessage("Your Pantry Code: PNT-${(Math.random() * 10000).toInt()}")
            .setPositiveButton("Copy") { d, _ ->
                Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun shareProfile() {
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out my PantryPal profile!")
            }
        )
    }
}
