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

        // No UI layout - just show fingerprint prompt immediately
        setupBiometric()
        showBiometricPrompt()
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or error - go to login screen
                    Toast.makeText(applicationContext, "Authentication required", Toast.LENGTH_SHORT).show()
                    goToLogin()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Fingerprint success - go to home
                    Toast.makeText(applicationContext, "Welcome back!", Toast.LENGTH_SHORT).show()
                    goToHome()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Try again", Toast.LENGTH_SHORT).show()
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
                // Biometric not available - go to login
                Toast.makeText(this, "Biometric not available, please login", Toast.LENGTH_SHORT).show()
                goToLogin()
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        // Log out user and go to login
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // Prevent back button
    override fun onBackPressed() {
        // Do nothing - force authentication
    }
}
