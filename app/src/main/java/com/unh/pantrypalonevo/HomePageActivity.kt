package com.unh.pantrypalonevo

import android.Manifest
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

        setupRecyclerView()
        setupClickListeners()
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

        // UPDATED: Profile navigation (navigate to ProfileActivity)
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // REMOVED: showProfileOptions() method
    // REMOVED: showLogoutDialog() method

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
