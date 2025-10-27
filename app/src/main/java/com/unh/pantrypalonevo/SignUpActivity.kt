package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivitySignUpBinding
import java.util.UUID

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private var fingerprintEnabled = false
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private fun generateUniqueUsername(): String {
        val uniqueId = UUID.randomUUID().toString().substring(0, 8)
        return "User$uniqueId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.switchFingerprint.setOnCheckedChangeListener { _, isChecked ->
            fingerprintEnabled = isChecked
        }

        binding.btnSignUp.setOnClickListener {
            val email = binding.etNewEmail.text.toString().trim()
            val password = binding.etNewPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: ""
                        val username = generateUniqueUsername()
                        val userMap = hashMapOf(
                            "email" to email,
                            "username" to username,
                            "fingerprintEnabled" to fingerprintEnabled,
                            "createdAt" to System.currentTimeMillis(),
                            "first" to "",
                            "last" to "",
                            "role" to "recipient", // or let user choose
                            "dietaryRestrictions" to arrayListOf<String>(),
                            "nutritionGoals" to arrayListOf<String>(),
                            "favorites" to arrayListOf<String>(),
                            "notificationToken" to ""
                        )

                        db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener {
                                // Save username to SharedPreferences
                                val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                                sharedPref.edit()
                                    .putString("user_email", email)
                                    .putString("user_name", username) // Use username as primary display name
                                    .putString("user_username", username)
                                    .apply()
                                
                                Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()

                                val loginIntent = Intent(this, SimpleLoginActivity::class.java)
                                loginIntent.putExtra("fingerprint_enabled", fingerprintEnabled)
                                startActivity(loginIntent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Add back to login navigation
        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, SimpleLoginActivity::class.java))
            finish()
        }
    }
}
