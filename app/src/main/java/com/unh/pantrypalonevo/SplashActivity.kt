package com.unh.pantrypalonevo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY = 3000L // 3 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            
            // Hide system bars for immersive experience
            hideSystemBars()
            
            // Start animations
            startAnimations()
            
            // Navigate to login after delay
            navigateToLogin()
        } catch (e: Exception) {
            // If splash fails, go directly to login
            navigateToLogin()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back button during splash
        // Do nothing
    }
    
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    private fun startAnimations() {
        val logoContainer = findViewById<View>(R.id.logoContainer)
        val versionText = findViewById<View>(R.id.tvVersion)
        // Add null checks to prevent crashes
        if (logoContainer == null || versionText == null) {
            return
        }
        
        // Logo container animation - scale in with bounce
        val logoScaleX = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.3f, 1.0f)
        val logoScaleY = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.3f, 1.0f)
        val logoAlpha = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f)
        
        logoScaleX.duration = 800
        logoScaleY.duration = 800
        logoAlpha.duration = 800
        
        logoScaleX.interpolator = OvershootInterpolator(1.2f)
        logoScaleY.interpolator = OvershootInterpolator(1.2f)
        logoAlpha.interpolator = AccelerateInterpolator()
        
        val logoAnimatorSet = AnimatorSet()
        logoAnimatorSet.playTogether(logoScaleX, logoScaleY, logoAlpha)
        
        
        
        // Version text animation - fade in
        val versionAlpha = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 1f)
        versionAlpha.duration = 800
        versionAlpha.startDelay = 1000
        versionAlpha.interpolator = DecelerateInterpolator()
        
        // Start animations with delays
        logoAnimatorSet.start()
        versionAlpha.start()
        
        // Add subtle floating animation to logo
        startFloatingAnimation(logoContainer)
        
        // Start loading dots wave animation
        
        // Add pulse animation to logo
        startPulseAnimation(logoContainer)
        
        
        // Add circular loader animation
        startCircularLoaderAnimation()
    }
    
    private fun startFloatingAnimation(view: View) {
        val floatAnimation = ObjectAnimator.ofFloat(view, "translationY", 0f, -10f, 0f)
        floatAnimation.duration = 3000
        floatAnimation.repeatCount = ObjectAnimator.INFINITE
        floatAnimation.repeatMode = ObjectAnimator.RESTART
        floatAnimation.interpolator = DecelerateInterpolator()
        floatAnimation.startDelay = 1000
        floatAnimation.start()
    }
    
    
    private fun startPulseAnimation(view: View) {
        val pulseScaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.05f, 1.0f)
        val pulseScaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.05f, 1.0f)
        val pulseAlpha = ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0.8f, 1.0f)
        
        pulseScaleX.duration = 2000
        pulseScaleY.duration = 2000
        pulseAlpha.duration = 2000
        
        pulseScaleX.repeatCount = ObjectAnimator.INFINITE
        pulseScaleY.repeatCount = ObjectAnimator.INFINITE
        pulseAlpha.repeatCount = ObjectAnimator.INFINITE
        
        pulseScaleX.repeatMode = ObjectAnimator.RESTART
        pulseScaleY.repeatMode = ObjectAnimator.RESTART
        pulseAlpha.repeatMode = ObjectAnimator.RESTART
        
        pulseScaleX.interpolator = AccelerateInterpolator()
        pulseScaleY.interpolator = AccelerateInterpolator()
        pulseAlpha.interpolator = AccelerateInterpolator()
        
        pulseScaleX.startDelay = 1500
        pulseScaleY.startDelay = 1500
        pulseAlpha.startDelay = 1500
        
        pulseScaleX.start()
        pulseScaleY.start()
        pulseAlpha.start()
    }
    
    
    private fun startCircularLoaderAnimation() {
        val circularLoader = findViewById<ProgressBar>(R.id.circularLoader)
        
        // Add null check to prevent crashes
        if (circularLoader == null) {
            return
        }
        
        // Create rotation animation for the circular loader
        val rotationAnimation = ObjectAnimator.ofFloat(circularLoader, "rotation", 0f, 360f)
        rotationAnimation.duration = 1500
        rotationAnimation.repeatCount = ObjectAnimator.INFINITE
        rotationAnimation.repeatMode = ObjectAnimator.RESTART
        rotationAnimation.interpolator = AccelerateInterpolator()
        rotationAnimation.startDelay = 800
        rotationAnimation.start()
        
        // Add scale animation for emphasis
        val scaleAnimation = ObjectAnimator.ofFloat(circularLoader, "scaleX", 0.8f, 1.1f, 0.8f)
        val scaleYAnimation = ObjectAnimator.ofFloat(circularLoader, "scaleY", 0.8f, 1.1f, 0.8f)
        
        scaleAnimation.duration = 2000
        scaleYAnimation.duration = 2000
        scaleAnimation.repeatCount = ObjectAnimator.INFINITE
        scaleYAnimation.repeatCount = ObjectAnimator.INFINITE
        scaleAnimation.repeatMode = ObjectAnimator.RESTART
        scaleYAnimation.repeatMode = ObjectAnimator.RESTART
        
        scaleAnimation.interpolator = DecelerateInterpolator()
        scaleYAnimation.interpolator = DecelerateInterpolator()
        
        scaleAnimation.startDelay = 1000
        scaleYAnimation.startDelay = 1000
        
        scaleAnimation.start()
        scaleYAnimation.start()
        
        // Add alpha animation for breathing effect
        val alphaAnimation = ObjectAnimator.ofFloat(circularLoader, "alpha", 0.7f, 1.0f, 0.7f)
        alphaAnimation.duration = 1500
        alphaAnimation.repeatCount = ObjectAnimator.INFINITE
        alphaAnimation.repeatMode = ObjectAnimator.RESTART
        alphaAnimation.interpolator = AccelerateInterpolator()
        alphaAnimation.startDelay = 1200
        alphaAnimation.start()
    }
    
    private fun navigateToLogin() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SimpleLoginActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)
    }
}
