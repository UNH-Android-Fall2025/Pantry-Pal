package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
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

            // Store fingerprint preference for use in login (demo with Toast)
            val fingerprintMsg = if (fingerprintEnabled) "Fingerprint Login Enabled" else "Fingerprint Login Disabled"
            Toast.makeText(this, "Sign Up Successful. $fingerprintMsg", Toast.LENGTH_SHORT).show()

            // TODO: Save new user signup data, including fingerprint preference.

            // Navigate back to login screen after sign up
            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.putExtra("fingerprint_enabled", fingerprintEnabled)
            startActivity(loginIntent)
            finish()
        }
    }
}
