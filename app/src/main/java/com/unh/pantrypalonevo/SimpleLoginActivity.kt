package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executor

class SimpleLoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: LinearLayout
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var tvUseDifferentEmail: TextView
    private var isPasswordVisible = false
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isFingerprintEnabled = false
    private var rememberedEmail = ""
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null && account.idToken != null) {
                        Toast.makeText(this, "Google Sign-In successful, authenticating...", Toast.LENGTH_SHORT).show()
                        // Save Google account info before Firebase auth
                        saveGoogleAccountInfo(account)
                        firebaseAuthWithGoogle(account.idToken)
                    } else {
                        Toast.makeText(this, "Google Sign-In failed: No account data or ID token", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        12501 -> Toast.makeText(this, "Google Sign-In cancelled by user", Toast.LENGTH_SHORT).show()
                        7 -> Toast.makeText(this, "Network error. Please check your internet connection", Toast.LENGTH_LONG).show()
                        8 -> Toast.makeText(this, "Internal error. Please try again", Toast.LENGTH_LONG).show()
                        10 -> Toast.makeText(this, "Developer error. Please contact support", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(this, "Google Sign-In failed: ${e.message} (Code: ${e.statusCode})", Toast.LENGTH_LONG).show()
                    }
                }
            }
            RESULT_CANCELED -> {
                Toast.makeText(this, "Google Sign-In was cancelled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Google Sign-In failed with result code: ${result.resultCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_login_simple)
            
            // Initialize views with null checks
            etEmail = findViewById(R.id.etEmail)
            etPassword = findViewById(R.id.etPassword)
            btnLogin = findViewById(R.id.btnLogin)
            btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
            btnTogglePassword = findViewById(R.id.btnTogglePassword)
            tvForgotPassword = findViewById(R.id.tvForgotPassword)
            tvSignUp = findViewById(R.id.tvSignUp)
            tvUseDifferentEmail = findViewById(R.id.tvUseDifferentEmail)
            
            // Check if all views are found
            if (etEmail == null || etPassword == null || btnLogin == null || 
                btnGoogleSignIn == null || btnTogglePassword == null ||
                tvForgotPassword == null || tvSignUp == null || tvUseDifferentEmail == null) {
                Toast.makeText(this, "Login screen setup failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Setup Google Sign-In
            setupGoogleSignIn()
            
            // Setup password visibility toggle
            setupPasswordToggle()
            
            // Setup click listeners
            setupClickListeners()
            
            // Load remembered email and check for fingerprint
            loadRememberedEmail()
            // Check fingerprint after a short delay to ensure email is loaded
            etEmail.post {
                checkFingerprintAvailability()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Login screen error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh fingerprint check when returning to this activity
        // This ensures fingerprint prompt shows if user enabled it in Account Settings
        etEmail.post {
            checkFingerprintAvailability()
        }
    }
    
    private fun setupGoogleSignIn() {
        try {
            val webClientId = getString(R.string.default_web_client_id)
            
            // Validate client ID
            if (webClientId.isNullOrEmpty() || !webClientId.contains("apps.googleusercontent.com")) {
                Toast.makeText(this, "Invalid Google client ID configuration", Toast.LENGTH_LONG).show()
                return
            }
            
            Toast.makeText(this, "Setting up Google Sign-In with client ID: ${webClientId.take(30)}...", Toast.LENGTH_SHORT).show()
            
            // For Google Sign-In, we need to use the web client ID for Firebase authentication
            // This is the correct approach for Firebase + Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)  // Use web client ID for Firebase
                .requestEmail()
                .requestProfile()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            
            // Check if Google Play Services is available
            try {
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
                
                if (resultCode != ConnectionResult.SUCCESS) {
                    Toast.makeText(this, "Google Play Services not available", Toast.LENGTH_LONG).show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Google Play Services check failed: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }
            
            btnGoogleSignIn.setOnClickListener {
                try {
                    Toast.makeText(this, "Starting Google Sign-In...", Toast.LENGTH_SHORT).show()
                    
                    // Check if user is already signed in
                    val account = GoogleSignIn.getLastSignedInAccount(this)
                    if (account != null) {
                        Toast.makeText(this, "Already signed in, signing out first...", Toast.LENGTH_SHORT).show()
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    } else {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
                    
                } catch (e: Exception) {
                    Toast.makeText(this, "Google Sign-In setup error: ${e.message}", Toast.LENGTH_LONG).show()
                    
                    // Fallback: try to sign out and retry
                    try {
                        googleSignInClient.signOut().addOnCompleteListener {
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        }
                    } catch (fallbackException: Exception) {
                        Toast.makeText(this, "Google Sign-In fallback failed: ${fallbackException.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In configuration error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupPasswordToggle() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            
            val selection = etPassword.selectionEnd
            
            if (isPasswordVisible) {
                // Show password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                // Hide password
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            
            // Restore cursor position
            etPassword.setSelection(selection)
        }
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = if (rememberedEmail.isNotEmpty()) rememberedEmail else etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            performEmailPasswordLogin(email, password)
        }
        
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        
        tvUseDifferentEmail.setOnClickListener {
            // Reset to allow email editing
            etEmail.isEnabled = true
            etEmail.alpha = 1.0f
            etEmail.setText("")
            etEmail.hint = "Enter your email"
            etEmail.requestFocus()
            tvUseDifferentEmail.visibility = View.GONE
            rememberedEmail = ""
        }
    }
    
    
    private fun saveGoogleAccountInfo(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        
        // Get username from Google account (displayName or email)
        val googleUsername = account.displayName ?: account.email?.substringBefore("@") ?: ""
        val googlePhotoUrl = account.photoUrl?.toString() ?: ""
        
        // Save Google account info
        if (googleUsername.isNotEmpty()) {
            prefs.edit()
                .putString("google_username", googleUsername)
                .putString("google_photo_url", googlePhotoUrl)
                .apply()
        }
    }
    
    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, "Google Sign-In failed: No ID token received", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Authenticating with Firebase...", Toast.LENGTH_SHORT).show()
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val email = user?.email ?: "user@example.com"
                    Toast.makeText(this, "Firebase authentication successful!", Toast.LENGTH_SHORT).show()
                    saveUserEmailAndNavigate(email)
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Firebase authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Firebase authentication error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    private fun saveUserEmailAndNavigate(email: String) {
        try {
            val sharedPref = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
            sharedPref.edit()
                .putString("user_email", email)
                .putBoolean("has_logged_in_before", true)
                .apply()
            
            // Check if user has a username, if not use Google username or generate one
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val db = FirebaseFirestore.getInstance()
                val googleUsername = sharedPref.getString("google_username", "") ?: ""
                val googlePhotoUrl = sharedPref.getString("google_photo_url", "") ?: ""
                
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { document ->
                        val existingUsername = document.getString("username")
                        val existingPhotoUrl = document.getString("profilePictureUrl")
                        
                        // Use Google username if available and no existing username
                        val usernameToUse = when {
                            !existingUsername.isNullOrBlank() -> existingUsername
                            googleUsername.isNotEmpty() -> {
                                // Clean Google username (remove spaces, make it valid)
                                googleUsername.replace(" ", "_").take(20)
                            }
                            else -> "User${System.currentTimeMillis().toString().takeLast(6)}"
                        }
                        
                        // Use Google photo URL if available and no existing photo
                        val photoUrlToUse = existingPhotoUrl ?: googlePhotoUrl
                        
                        // Update Firebase with username and photo
                        val updates = mutableMapOf<String, Any>()
                        if (existingUsername.isNullOrBlank()) {
                            updates["username"] = usernameToUse
                        }
                        if (photoUrlToUse.isNotEmpty() && existingPhotoUrl.isNullOrBlank()) {
                            updates["profilePictureUrl"] = photoUrlToUse
                        }
                        
                        if (updates.isNotEmpty()) {
                            db.collection("users").document(currentUser.uid)
                                .update(updates)
                                .addOnSuccessListener {
                                    sharedPref.edit()
                                        .putString("user_username", usernameToUse)
                                        .putString("user_name", usernameToUse)
                                        .apply()
                                }
                        } else {
                            // Just load existing data
                            sharedPref.edit()
                                .putString("user_username", existingUsername ?: usernameToUse)
                                .putString("user_name", existingUsername ?: usernameToUse)
                                .apply()
                        }
                    }
                    .addOnFailureListener {
                        // If Firestore fails, use Google username or generate
                        val usernameToUse = if (googleUsername.isNotEmpty()) {
                            googleUsername.replace(" ", "_").take(20)
                        } else {
                            "User${System.currentTimeMillis().toString().takeLast(6)}"
                        }
                        sharedPref.edit()
                            .putString("user_username", usernameToUse)
                            .putString("user_name", usernameToUse)
                            .apply()
                    }
            }
            
            Toast.makeText(this, "User data saved: $email", Toast.LENGTH_SHORT).show()
            navigateToHome()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    private fun navigateToHome() {
        try {
            Toast.makeText(this, "Navigating to home screen...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, HomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRememberedEmail() {
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        rememberedEmail = prefs.getString("user_email", "") ?: ""
        
        if (rememberedEmail.isNotEmpty()) {
            etEmail.setText(rememberedEmail)
            etEmail.isEnabled = false // Disable email editing
            etEmail.alpha = 0.7f // Make it look disabled
            etPassword.requestFocus() // Auto-focus on password
            
            // Show a subtle indicator that email is remembered
            etEmail.hint = "Remembered email"
            
            // Show "Use Different Email" option
            tvUseDifferentEmail.visibility = View.VISIBLE
        } else {
            etEmail.hint = "Enter your email"
        }
    }

    private fun checkFingerprintAvailability() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Check if user has enabled fingerprint in settings
                val prefs = getSharedPreferences("PantryPrefs", MODE_PRIVATE)
                isFingerprintEnabled = prefs.getBoolean("fingerprint_enabled", false)
                
                
                if (isFingerprintEnabled && rememberedEmail.isNotEmpty()) {
                    // Show fingerprint option
                    showFingerprintOption()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No fingerprint hardware
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Fingerprint hardware unavailable
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No fingerprints enrolled
            }
        }
    }

    private fun showFingerprintOption() {
        // Add a fingerprint button or show prompt
        setupBiometricPrompt()
        showBiometricPrompt()
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or error - continue with password login
                    etPassword.requestFocus()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Fingerprint success - proceed with login
                    performEmailPasswordLogin(rememberedEmail, "")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@SimpleLoginActivity, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Login")
            .setSubtitle("Use your fingerprint to sign in to $rememberedEmail")
            .setNegativeButtonText("Use Password")
            .build()
    }

    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo)
    }

    private fun performEmailPasswordLogin(email: String, password: String) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty() && !isFingerprintEnabled) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return
        }

        // If password is empty but fingerprint is enabled, it means user used fingerprint
        if (password.isEmpty() && isFingerprintEnabled) {
            // Proceed with saved credentials (fingerprint authentication)
            saveUserEmailAndNavigate(email)
            return
        }

        // Regular email/password login
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    saveUserEmailAndNavigate(email)
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
