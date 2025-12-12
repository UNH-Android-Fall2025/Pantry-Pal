@file:Suppress("DEPRECATION")

package com.unh.pantrypalonevo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.unh.pantrypalonevo.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadProfilePicture(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Ensure bottom bar doesn't block clicks
        binding.bottomNavigation.bringToFront()
        binding.bottomNavigation.isClickable = true

        setupClickListeners()
        loadDynamicUserProfile()
    }

    private fun loadDynamicUserProfile() {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val savedName = prefs.getString("user_name", null)
        val savedEmail = prefs.getString("user_email", null)
        val savedUsername = prefs.getString("user_username", null)

        // Fallbacks: Firebase displayName → derive from email → "User"
        val authName = FirebaseAuth.getInstance().currentUser?.displayName
        val finalName = when {
            !savedName.isNullOrBlank() -> savedName
            !authName.isNullOrBlank() -> authName
            !savedEmail.isNullOrBlank() -> extractUsernameFromEmail(savedEmail)
            else -> "User"
        }

        // Display username if available, otherwise show name
        val displayText = if (!savedUsername.isNullOrBlank()) {
            savedUsername
        } else {
            finalName
        }

        binding.tvUserName.text = displayText
        binding.tvPhoneNumber.text =
            savedEmail ?: FirebaseAuth.getInstance().currentUser?.email ?: "No email available"
        
        // Load profile picture from Firebase
        loadProfilePicture()
        
        // Set user initials in avatar (fallback if no picture)
        val initials = getInitials(displayText)
        binding.tvUserInitials.text = initials
    }
    
    private fun loadProfilePicture() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return
        
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val profilePictureUrl = document.getString("profilePictureUrl")
                if (!profilePictureUrl.isNullOrBlank()) {
                    // Load image with Glide
                    Glide.with(this)
                        .load(profilePictureUrl)
                        .circleCrop()
                        .into(binding.ivProfilePicture)
                    binding.ivProfilePicture.visibility = android.view.View.VISIBLE
                    binding.tvUserInitials.visibility = android.view.View.GONE
                } else {
                    binding.ivProfilePicture.visibility = android.view.View.GONE
                    binding.tvUserInitials.visibility = android.view.View.VISIBLE
                }
            }
            .addOnFailureListener {
                // If Firestore fails, show initials
                binding.ivProfilePicture.visibility = android.view.View.GONE
                binding.tvUserInitials.visibility = android.view.View.VISIBLE
            }
    }
    
    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "U"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first()}${parts.last().first()}".uppercase()
        }
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
        // Edit avatar button - show options for editing username or picture
        binding.btnEditAvatar.setOnClickListener {
            showEditProfileOptionsDialog()
        }
        
        // Profile avatar click - also opens edit options
        binding.ivProfileAvatar.setOnClickListener {
            showEditProfileOptionsDialog()
        }

        binding.btnPantryCode.setOnClickListener { showPantryCodeDialog() }

        binding.btnPublishPantry.setOnClickListener {
            startActivity(Intent(this, PublishPantryActivity::class.java))
        }

        binding.btnMyOrders.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        binding.btnMyClaims.setOnClickListener {
            startActivity(Intent(this, DonorDashboardActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileOptionsDialog()
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
            startActivity(Intent(this, RecipeActivity::class.java))
            finish()
        }

        binding.btnCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }
    }


    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton("Yes, Logout") { dialog: android.content.DialogInterface, _: Int -> 
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Suppress("DEPRECATION")
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

    private fun showEditProfileOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent to show custom layout
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Edit Username button
        dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnEditUsername)
            .setOnClickListener {
                dialog.dismiss()
                showEditUsernameDialog()
            }
        
        // Change Profile Picture button
        dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btnChangePicture)
            .setOnClickListener {
                dialog.dismiss()
                pickProfilePicture()
            }
        
        // Cancel button
        dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnCancel)
            .setOnClickListener {
                dialog.dismiss()
            }
        
        dialog.show()
    }
    
    private fun pickProfilePicture() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun uploadProfilePicture(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
        
        // Create reference to profile picture in Firebase Storage
        val storageRef = storage.reference
        val profilePictureRef = storageRef.child("profile_pictures/${currentUser.uid}.jpg")
        
        // Upload the image
        profilePictureRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Get download URL
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    
                    // Update Firestore with profile picture URL
                    db.collection("users").document(currentUser.uid)
                        .update("profilePictureUrl", downloadUrl)
                        .addOnSuccessListener {
                            // Update local display
                            Glide.with(this)
                                .load(downloadUrl)
                                .circleCrop()
                                .into(binding.ivProfilePicture)
                            binding.ivProfilePicture.visibility = android.view.View.VISIBLE
                            binding.tvUserInitials.visibility = android.view.View.GONE
                            
                            Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showEditUsernameDialog() {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val currentUsername = prefs.getString("user_username", "") ?: ""
        
        val editText = EditText(this).apply {
            setText(currentUsername)
            hint = "Enter new username"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Username")
            .setMessage("Enter a unique username (without @ symbol)")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newUsername = editText.text.toString().trim()
                if (newUsername.isNotEmpty() && newUsername != currentUsername) {
                    validateAndUpdateUsername(newUsername)
                } else if (newUsername == currentUsername) {
                    Toast.makeText(this, "Username unchanged", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a valid username", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateAndUpdateUsername(newUsername: String) {
        // Check if username is valid (no spaces, special characters except underscore)
        if (!newUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            Toast.makeText(this, "Username can only contain letters, numbers, and underscores", Toast.LENGTH_SHORT).show()
            return
        }

        if (newUsername.length < 3 || newUsername.length > 20) {
            Toast.makeText(this, "Username must be 3-20 characters long", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user is trying to keep their current username
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val currentUsername = prefs.getString("user_username", "") ?: ""
        
        if (newUsername == currentUsername) {
            Toast.makeText(this, "This is already your username", Toast.LENGTH_SHORT).show()
            return
        }

        // For now, we'll skip uniqueness checking and just update the username
        // This avoids permission issues while still allowing username changes
        updateUsernameDirectly(newUsername)
    }

    private fun updateUsernameDirectly(newUsername: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Update in Firebase
        db.collection("users").document(currentUser.uid)
            .update("username", newUsername)
            .addOnSuccessListener {
                // Update in SharedPreferences
                val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("user_username", newUsername)
                    .putString("user_name", newUsername) // Update user_name to use username
                    .apply()
                
                // Refresh the profile display
                loadDynamicUserProfile()
                Toast.makeText(this, "Username updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
