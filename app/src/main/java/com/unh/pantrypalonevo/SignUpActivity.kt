package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.unh.pantrypalonevo.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var fingerprintEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔐 Fingerprint toggle listener
        binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            fingerprintEnabled = isChecked
        }

        // 📩 Sign Up button logic
        binding.btnSignUp.setOnClickListener {
            val email = binding.etNewEmail.text.toString().trim()
            val password = binding.etNewPassword.text.toString().trim()

            // ✅ Validate inputs
            if (!isValidEmail(email)) {
                showToast("Please enter a valid email address")
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                showToast("Password must be at least 6 characters")
                return@setOnClickListener
            }

            // 🔄 Here you would normally save the new user info to Firebase or Room DB
            // TODO: Implement actual signup logic (e.g., Firebase Auth or API call)
            saveUserData(email, password, fingerprintEnabled)

            val fingerprintMsg = if (fingerprintEnabled) {
                "Fingerprint Login Enabled"
            } else {
                "Fingerprint Login Disabled"
            }

            showToast("Sign Up Successful. $fingerprintMsg")

            // 🔁 Go back to Login screen with fingerprint status
            val loginIntent = Intent(this, LoginActivity::class.java).apply {
                putExtra("fingerprint_enabled", fingerprintEnabled)
                putExtra("new_user_email", email)
            }
            startActivity(loginIntent)
            finish()
        }
    }

    // 📧 Email validation
    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // 🔑 Password validation (basic example)
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    // 💾 Save user data (placeholder function)
    private fun saveUserData(email: String, password: String, fingerprint: Boolean) {
        // TODO: Replace with real database or Firebase logic
        // This is just a placeholder for now.
    }

    // 🪄 Helper: Show toast messages
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
