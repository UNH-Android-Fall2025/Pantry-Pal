package com.unh.pantrypalonevo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var pantryAdapter: PantryAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // DYNAMIC: Setup user greeting first
        setupDynamicGreeting()

        setupRecyclerView()
        setupClickListeners()
    }

    // DYNAMIC: Setup greeting based on stored user email
    private fun setupDynamicGreeting() {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail != null) {
            // Extract username from email dynamically
            val username = extractUsernameFromEmail(userEmail)
            binding.tvGreeting.text = "Hello $username !!"
        } else {
            // First time - set a test email (remove this when you add login)
            setTestUserEmail()
            val testEmail = sharedPref.getString("user_email", "user@example.com")!!
            val username = extractUsernameFromEmail(testEmail)
            binding.tvGreeting.text = "Hello $username !!"
        }
    }

    // TEMPORARY: Set test email (remove when you add login screen)
    private fun setTestUserEmail() {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            // Change this email to test different usernames
            putString("user_email", "rajul@gmail.com") // Try: john.doe@yahoo.com, mary_smith@hotmail.com
            apply()
        }
    }

    // DYNAMIC: Extract username from any email format
    private fun extractUsernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            val username = email.substringBefore("@")

            // Handle different formats dynamically
            when {
                username.contains(".") -> {
                    // john.doe@gmail.com → "John Doe"
                    username.split(".").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                username.contains("_") -> {
                    // john_doe@gmail.com → "John Doe"
                    username.split("_").joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                }
                else -> {
                    // rajul@gmail.com → "Rajul"
                    username.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }
            }
        } else {
            "User"
        }
    }

    // PUBLIC: Method to update user email (call this from login screen later)
    fun updateUserEmail(email: String) {
        val sharedPref = getSharedPreferences("PantryPal_UserPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_email", email)
            apply()
        }
        // Refresh greeting
        setupDynamicGreeting()
    }

    private fun setupRecyclerView() {
        val pantryList = listOf(
            Pantry("West Haven Food Bank", "Fresh produce, canned goods, bread", "123 Main St, West Haven, CT", 41.2706, -72.9389),
            Pantry("Community Pantry", "Family-friendly pantry with clothing", "456 Oak Ave, New Haven, CT", 41.3083, -72.9279),
            Pantry("Faith Community Kitchen", "Hot meals served Mon-Fri", "789 Church St, Milford, CT", 41.2225, -73.0640),
            Pantry("Neighborhood Support", "Emergency food assistance", "321 Elm St, West Haven, CT", 41.2650, -72.9470),
            Pantry("Hope Center", "Comprehensive family services", "654 Park Ave, New Haven, CT", 41.3111, -72.9267)
        )

        pantryAdapter = PantryAdapter(pantryList) { pantry, action ->
            when (action) {
                "view" -> {
                    Toast.makeText(this, "Selected: ${pantry.name}", Toast.LENGTH_SHORT).show()
                }
                "map" -> {
                    openPantryInMaps(pantry)
                }
            }
        }

        binding.rvPantryList.layoutManager = LinearLayoutManager(this)
        binding.rvPantryList.adapter = pantryAdapter
    }

    private fun setupClickListeners() {
        // Location functionality in top-right location button
        binding.ivLocationButton.setOnClickListener {
            requestLocationPermission()
        }

        binding.btnSearch.setOnClickListener {
            val searchQuery = binding.etSearch.text.toString().trim()
            if (searchQuery.isNotEmpty()) {
                Toast.makeText(this, "Searching for: $searchQuery", Toast.LENGTH_SHORT).show()
            }
        }

        // Bottom navigation clicks
        binding.btnHome.setOnClickListener {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "Recipes page coming soon! 🍳", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "Cart page coming soon! 🛒", Toast.LENGTH_SHORT).show()
        }

        // Profile navigation (navigate to ProfileActivity)
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        binding.tvCurrentLocation.text = "Location: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                        Toast.makeText(this, "Location updated! Finding nearby pantries...", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openPantryInMaps(pantry: Pantry) {
        val uri = Uri.parse("geo:${pantry.latitude},${pantry.longitude}?q=${Uri.encode(pantry.name + " " + pantry.location)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=${Uri.encode(pantry.name + " " + pantry.location)}"))
            startActivity(browserIntent)
        }
    }

    private fun openAllPantriesInMaps() {
        val uri = Uri.parse("geo:0,0?q=food+pantries+near+me")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=food+pantries+near+me"))
            startActivity(browserIntent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
