package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.unh.pantrypalonevo.databinding.ActivityLoginBinding
import java.util.concurrent.Executor
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    private var fingerprintEnabled: Boolean = false
    private var fingerprintFailCount = 0

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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




        fingerprintEnabled = intent.getBooleanExtra("fingerprint_enabled", false)

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(applicationContext, "Fingerprint Authentication succeeded", Toast.LENGTH_SHORT).show()

                // Save preference to SharedPreferences for offline access
                val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                prefs.edit().putBoolean("fingerprint_enabled", true).apply()

                navigateToHome()
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
            if (fingerprintEnabled && fingerprintFailCount < 2 &&
                BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Authenticate with Firebase
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
