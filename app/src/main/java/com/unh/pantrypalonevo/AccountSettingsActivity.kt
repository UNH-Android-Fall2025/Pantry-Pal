package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivityAccountSettingsBinding

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountSettingsBinding
    private lateinit var db: FirebaseFirestore
    private var isFingerprintEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        db = FirebaseFirestore.getInstance()
        
        setupClickListeners()
        loadAccountInfo()
        loadFingerprintStatus()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkFingerprintAvailabilityAndEnable()
            } else {
                disableFingerprint()
            }
        }

        binding.layoutChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "Recipes - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "Cart - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun loadAccountInfo() {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val email = prefs.getString("user_email", "No email")
        val username = prefs.getString("user_username", "No username")

        binding.tvEmail.text = email
        binding.tvUsername.text = username
    }

    private fun loadFingerprintStatus() {
        val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
        isFingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
        binding.switchFingerprint.isChecked = isFingerprintEnabled
    }

    private fun checkFingerprintAvailabilityAndEnable() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Fingerprint is available, enable it
                enableFingerprint()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.switchFingerprint.isChecked = false
                Toast.makeText(this, "Fingerprint hardware not available on this device", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                binding.switchFingerprint.isChecked = false
                Toast.makeText(this, "Fingerprint hardware is currently unavailable", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.switchFingerprint.isChecked = false
                Toast.makeText(this, "No fingerprints enrolled. Please add a fingerprint in device settings first.", Toast.LENGTH_LONG).show()
            }
            else -> {
                binding.switchFingerprint.isChecked = false
                Toast.makeText(this, "Fingerprint authentication is not available", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableFingerprint() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            binding.switchFingerprint.isChecked = false
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Update local preferences
        val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("fingerprint_enabled", true).apply()

        // Update Firebase
        db.collection("users").document(currentUser.uid)
            .update("fingerprintEnabled", true)
            .addOnSuccessListener {
                isFingerprintEnabled = true
                Toast.makeText(this, "Fingerprint authentication enabled successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Revert local change if Firebase update fails
                prefs.edit().putBoolean("fingerprint_enabled", false).apply()
                binding.switchFingerprint.isChecked = false
                Toast.makeText(this, "Failed to enable fingerprint: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun disableFingerprint() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            binding.switchFingerprint.isChecked = true
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Update local preferences
        val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("fingerprint_enabled", false).apply()

        // Update Firebase
        db.collection("users").document(currentUser.uid)
            .update("fingerprintEnabled", false)
            .addOnSuccessListener {
                isFingerprintEnabled = false
                Toast.makeText(this, "Fingerprint authentication disabled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Revert local change if Firebase update fails
                prefs.edit().putBoolean("fingerprint_enabled", true).apply()
                binding.switchFingerprint.isChecked = true
                Toast.makeText(this, "Failed to disable fingerprint: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("Password change feature will be available soon. For now, please use the 'Forgot Password' option on the login screen.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Forgot Password") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            }
            .show()
    }
}
