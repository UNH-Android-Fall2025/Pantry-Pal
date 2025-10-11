package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show the splash for a brief moment then navigate to login
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
