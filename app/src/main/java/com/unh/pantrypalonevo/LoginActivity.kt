package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.unh.pantrypalonevo.databinding.ActivityLoginBinding
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    private var fingerprintEnabled: Boolean = false
    private var fingerprintFailCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

<<<<<<< HEAD
        // Get fingerprintEnabled from Intent extras passed by SignUp (or saved prefs)
=======
        val googleButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.btnGoogleSignIn)

        // Detect light/dark theme
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK

        when (nightModeFlags) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                googleButton.setColorScheme(com.google.android.gms.common.SignInButton.COLOR_LIGHT) // white button in dark mode
            }
            else -> {
                googleButton.setColorScheme(com.google.android.gms.common.SignInButton.COLOR_DARK) // dark button in light mode
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

>>>>>>> origin/dev
        fingerprintEnabled = intent.getBooleanExtra("fingerprint_enabled", false)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(applicationContext, "Fingerprint Authentication succeeded", Toast.LENGTH_SHORT).show()
<<<<<<< HEAD
                navigateToHome()
=======

                // Save preference to SharedPreferences for offline access
                val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                prefs.edit().putBoolean("fingerprint_enabled", true).apply()

                // UPDATED: Get current user's email for fingerprint login
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    saveUserEmailAndNavigate(currentUser.email ?: "user@example.com")
                } else {
                    navigateToHome()
                }
>>>>>>> origin/dev
            }

            override fun onAuthenticationFailed() {
                fingerprintFailCount++
                Toast.makeText(applicationContext, "Fingerprint Authentication failed", Toast.LENGTH_SHORT).show()
                if (fingerprintFailCount >= 2) {
                    Toast.makeText(applicationContext, "Failed twice, login with Password now.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Login for PantryPal")
            .setSubtitle("Use your fingerprint to login quickly")
            .setNegativeButtonText("Use Password")
            .build()

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            if (fingerprintEnabled && fingerprintFailCount < 2 && BiometricManager.from(this).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

<<<<<<< HEAD
                // TODO: Validate credentials with backend or Firebase
                Toast.makeText(this, "Login successful (Password)", Toast.LENGTH_SHORT).show()
                navigateToHome()
=======
                // Authenticate with Firebase
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                            // ADD THESE 3 LINES:
                            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().putString("user_email", email).apply()

                            navigateToHome() // This line stays the same
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

>>>>>>> origin/dev
            }
        }
    }

    // UPDATED: Handle Google Sign-In result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: "user@example.com"

                Toast.makeText(this, "Google Sign-In successful", Toast.LENGTH_SHORT).show()
                saveUserEmailAndNavigate(email)

            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // NEW: Save user email and navigate to HomePageActivity
    private fun saveUserEmailAndNavigate(email: String) {
        // Save email for dynamic greeting
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_email", email)
            apply()
        }

        // Navigate to HomePageActivity (not MainActivity)
        navigateToHome()
    }

    private fun navigateToHome() {
<<<<<<< HEAD
=======
        // UPDATED: Navigate to HomePageActivity instead of MainActivity
>>>>>>> origin/dev
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }
}
