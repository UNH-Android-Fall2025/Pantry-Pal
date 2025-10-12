package com.unh.pantrypalonevo

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
        loadUserProfile()
    }

    private fun loadUserProfile() {
        // TODO: Load from database/preferences
        binding.tvUserName.text = "Abhinav"
        binding.tvPhoneNumber.text = "203565224"
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
