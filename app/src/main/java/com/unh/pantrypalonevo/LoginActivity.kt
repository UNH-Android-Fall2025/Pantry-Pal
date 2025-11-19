package com.unh.pantrypalonevo

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.unh.pantrypalonevo.databinding.ActivityLoginBinding
import java.util.concurrent.Executor
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor
    private var fingerprintEnabled = false
    private var fingerprintFailCount = 0
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val googleButton = binding.btnGoogleSignIn

            // Adjust button for light/dark mode
            val nightModeFlags = resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            when (nightModeFlags) {
                Configuration.UI_MODE_NIGHT_YES ->
                    googleButton.setColorScheme(SignInButton.COLOR_LIGHT)
                else ->
                    googleButton.setColorScheme(SignInButton.COLOR_DARK)
            }

            // Google sign-in setup
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
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
                    val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                    prefs.edit().putBoolean("fingerprint_enabled", true).apply()
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        saveUserEmailAndNavigate(currentUser.email ?: "user@example.com")
                    } else navigateToHome()
                }

                override fun onAuthenticationFailed() {
                    fingerprintFailCount++
                    Toast.makeText(applicationContext, "Fingerprint Authentication failed", Toast.LENGTH_SHORT).show()
                    if (fingerprintFailCount >= 2)
                        Toast.makeText(applicationContext, "Failed twice, login with Password now.", Toast.LENGTH_SHORT).show()
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
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                            sharedPref.edit().putString("user_email", email)
                                .putString("user_name", email.substringBefore("@")) // default name
                                .apply()
                            navigateToHome()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Login screen error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign in failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val email = user?.email ?: "user@example.com"
                    val displayName = user?.displayName ?: "User"

                    // ✅ Save both email & name
                    val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("user_email", email)
                        putString("user_name", displayName) // Will be updated to username later
                        apply()
                    }

                    Toast.makeText(this, "Welcome $displayName!", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                } else {
                    Log.w("LoginActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserEmailAndNavigate(email: String) {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_email", email)
            putString("user_name", email.substringBefore("@"))
            apply()
        }
        navigateToHome()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomePageActivity::class.java))
        finish()
    }
}
