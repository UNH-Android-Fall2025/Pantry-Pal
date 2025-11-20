package com.unh.pantrypalonevo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.unh.pantrypalonevo.databinding.ActivityPantryLocationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Geocoder
import java.util.Locale

class PantryLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPantryLocationBinding
    private var googleMap: GoogleMap? = null
    private var pantryLatitude: Double? = null
    private var pantryLongitude: Double? = null
    private var pantryName: String = ""
    private var pantryAddress: String = ""
    private var pantryDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityPantryLocationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            loadPantryData()
            setupMap()
            setupDirectionsButton()
        } catch (e: Exception) {
            Log.e("PantryLocation", "Error in onCreate", e)
            Toast.makeText(this, "Error loading pantry location: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pantry Location"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadPantryData() {
        pantryName = intent.getStringExtra("pantry_name") ?: "Pantry"
        pantryAddress = intent.getStringExtra("pantry_address") ?: ""
        pantryDescription = intent.getStringExtra("pantry_description") ?: ""
        
        // Get coordinates if available
        pantryLatitude = intent.getDoubleExtra("pantry_latitude", Double.NaN).takeIf { !it.isNaN() }
        pantryLongitude = intent.getDoubleExtra("pantry_longitude", Double.NaN).takeIf { !it.isNaN() }

        binding.tvPantryName.text = pantryName
        binding.tvPantryAddress.text = pantryAddress
        binding.tvPantryDescription.text = pantryDescription

        Log.d("PantryLocation", "Loaded pantry: $pantryName at $pantryAddress")
        Log.d("PantryLocation", "Coordinates: lat=$pantryLatitude, lng=$pantryLongitude")
    }

    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            if (mapFragment == null) {
                Log.e("PantryLocation", "Map fragment not found!")
                Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show()
                return
            }
            mapFragment.getMapAsync(this)
        } catch (e: Exception) {
            Log.e("PantryLocation", "Error setting up map", e)
            Toast.makeText(this, "Error loading map: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            
            // Enable zoom controls
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.uiSettings.isCompassEnabled = true

            if (pantryLatitude != null && pantryLongitude != null) {
                // We have coordinates, show directly
                showPantryOnMap(pantryLatitude!!, pantryLongitude!!)
            } else if (pantryAddress.isNotEmpty()) {
                // No coordinates, geocode the address
                geocodeAddress(pantryAddress)
            } else {
                Toast.makeText(this, "No location information available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PantryLocation", "Error in onMapReady", e)
            Toast.makeText(this, "Error loading map: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPantryOnMap(lat: Double, lng: Double) {
        try {
            val location = LatLng(lat, lng)
            
            googleMap?.let { map ->
                // Add marker
                map.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title(pantryName)
                        .snippet(pantryAddress)
                )
                
                // Move camera to location
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                
                Log.d("PantryLocation", "Map updated with location: $lat, $lng")
            } ?: run {
                Log.w("PantryLocation", "GoogleMap is null, cannot show location")
            }
        } catch (e: Exception) {
            Log.e("PantryLocation", "Error showing pantry on map", e)
            Toast.makeText(this, "Error displaying location on map", Toast.LENGTH_SHORT).show()
        }
    }

    private fun geocodeAddress(address: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var coordinates: Pair<Double, Double>? = null
                
                // Try using PlacesService geocoding first (more accurate)
                try {
                    val placesService = PlacesService.getInstance(this@PantryLocationActivity)
                    coordinates = placesService.geocodeAddress(address)
                } catch (e: Exception) {
                    Log.w("PantryLocation", "PlacesService geocoding failed, trying Android Geocoder", e)
                }
                
                // Fallback to Android Geocoder if PlacesService fails
                if (coordinates == null && Geocoder.isPresent()) {
                    try {
                        val geocoder = Geocoder(this@PantryLocationActivity, Locale.getDefault())
                        
                        // Use deprecated API (works on all versions, just deprecated on newer)
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(address, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val location = addresses[0]
                            coordinates = Pair(location.latitude, location.longitude)
                            Log.d("PantryLocation", "Geocoded using Android Geocoder: $address -> ${coordinates.first}, ${coordinates.second}")
                        }
                    } catch (e: Exception) {
                        Log.e("PantryLocation", "Android Geocoder failed", e)
                        // Don't crash, just log the error
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (coordinates != null) {
                        pantryLatitude = coordinates.first
                        pantryLongitude = coordinates.second
                        showPantryOnMap(coordinates.first, coordinates.second)
                        Log.d("PantryLocation", "Geocoded address: $address -> ${coordinates.first}, ${coordinates.second}")
                    } else {
                        Toast.makeText(
                            this@PantryLocationActivity,
                            "Could not find location for address. You can still use the address for directions.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PantryLocation", "Error geocoding address", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PantryLocationActivity,
                        "Error finding location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupDirectionsButton() {
        binding.btnDirections.setOnClickListener {
            if (pantryLatitude != null && pantryLongitude != null) {
                // Open Google Maps with directions
                val gmmIntentUri = Uri.parse("google.navigation:q=${pantryLatitude},${pantryLongitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback to web browser
                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pantryLatitude},${pantryLongitude}")
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    startActivity(webIntent)
                }
            } else if (pantryAddress.isNotEmpty()) {
                // Use address for directions
                val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(pantryAddress)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Fallback to web browser
                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(pantryAddress)}")
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    startActivity(webIntent)
                }
            } else {
                Toast.makeText(this, "No location available for directions", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
