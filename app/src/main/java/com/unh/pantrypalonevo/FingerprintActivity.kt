package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

class FingerprintActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBiometric()
        showBiometricPrompt()
        
        // Handle back button press - prevent going back, force authentication
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - force authentication
            }
        })
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    
                    // Check if user clicked "Use password instead" button
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || 
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        // User wants to use password - navigate to login page
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                try {
                                    goToLogin()
                                } catch (e: Exception) {
                                    // Fallback navigation if error occurs
                                    try {
                                        val intent = Intent(this@FingerprintActivity, SimpleLoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } catch (ex: Exception) {
                                        // If all else fails, just finish
                                        finish()
                                    }
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                try {
                                    Toast.makeText(applicationContext,
                                        "Authentication required", Toast.LENGTH_SHORT).show()
                                    goToLogin()
                                } catch (e: Exception) {
                                    finish()
                                }
                            }
                        }
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            try {
                                Toast.makeText(applicationContext,
                                    "Welcome back!", Toast.LENGTH_SHORT).show()
                                goToHome()
                            } catch (e: Exception) {
                                // Fallback navigation
                                try {
                                    val intent = Intent(this@FingerprintActivity, HomePageActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } catch (ex: Exception) {
                                    finish()
                                }
                            }
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            try {
                                Toast.makeText(applicationContext,
                                    "Try again", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                // Ignore toast errors
                            }
                        }
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Welcome back to PantryPal")
            .setSubtitle("Verify your identity to continue")
            .setNegativeButtonText("Use password instead")
            .build()
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Biometric not available, navigate to login
                goToLogin()
            }
        }
    }


    private fun goToHome() {
        try {
            val intent = Intent(this, HomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // If navigation fails, try without flags
            try {
                val intent = Intent(this, HomePageActivity::class.java)
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                finish()
            }
        }
    }

    private fun goToLogin() {
        // Navigate to default login page (SimpleLoginActivity)
        try {
            val intent = Intent(this, SimpleLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            // If navigation fails, try without flags
            try {
                val intent = Intent(this, SimpleLoginActivity::class.java)
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                // Last resort - just finish
                finish()
            }
        }
    }

}
