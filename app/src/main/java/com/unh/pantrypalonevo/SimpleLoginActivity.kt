package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SimpleLoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: android.widget.EditText
    private lateinit var etPassword: android.widget.EditText
    private lateinit var btnLogin: android.widget.Button
    private lateinit var btnGoogleSignIn: android.widget.LinearLayout
    private lateinit var tvForgotPassword: android.widget.TextView
    private lateinit var tvSignUp: android.widget.TextView
    
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    
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
            tvForgotPassword = findViewById(R.id.tvForgotPassword)
            tvSignUp = findViewById(R.id.tvSignUp)
            
            // Check if all views are found
            if (etEmail == null || etPassword == null || btnLogin == null || 
                btnGoogleSignIn == null || tvForgotPassword == null || tvSignUp == null) {
                Toast.makeText(this, "Login screen setup failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            // Setup Google Sign-In
            setupGoogleSignIn()
            
            // Setup click listeners
            setupClickListeners()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Login screen error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
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
                val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
                
                if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
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
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
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
        
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
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
            sharedPref.edit().putString("user_email", email).apply()
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
}
