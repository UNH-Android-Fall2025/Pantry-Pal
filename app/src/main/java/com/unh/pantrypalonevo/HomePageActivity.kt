package com.unh.pantrypalonevo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding
import com.unh.pantrypalonevo.model.Pantry
import com.unh.pantrypalonevo.adapter.PantryAdapter
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pantryAdapter: PantryAdapter
    private val pantryList = mutableListOf<Pantry>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var userLocation: Location? = null
    private lateinit var placesService: PlacesService

    companion object {
        private const val NEARBY_RADIUS_METERS = 50_000.0 // 50 km
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Load logged-in user's username
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val username = prefs.getString("user_username", null)
        val displayName = prefs.getString("user_name", "User")
        
        // Use username if available, otherwise fallback to display name
        val greetingName = if (!username.isNullOrBlank()) {
            username
        } else {
            displayName
        }
        
        binding.tvGreeting.text = "Hello, $greetingName!"

        // ✅ RecyclerView setup
        pantryAdapter = PantryAdapter(pantryList) { pantry ->
            // On click → open PantryLocationActivity (details)
            val intent = Intent(this, PantryLocationActivity::class.java)
            intent.putExtra("pantry_name", pantry.name)
            intent.putExtra("pantry_address", pantry.address)
            intent.putExtra("pantry_description", pantry.description)
            startActivity(intent)
        }
        binding.rvPantryList.layoutManager = LinearLayoutManager(this)
        binding.rvPantryList.adapter = pantryAdapter

        // ✅ Fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ✅ Initialize Places Service
        placesService = PlacesService.getInstance(this)

        // ✅ Load pantries from Places API and Firestore
        loadPantries()

        // ✅ Handle location enable card
        binding.btnEnableLocation.setOnClickListener {
            requestLocationPermissionAndFetch()
        }
        // Try fetching location immediately for nearby filtering
        requestLocationPermissionAndFetch()

        // ✅ Search filter - searches both local list and Google Places API
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isEmpty()) {
                // Show all pantries when search is empty
                pantryAdapter.updateList(pantryList)
            } else {
                // Search in local list first (fast)
                val localFiltered = pantryList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
                
                // Also search Google Places API if location is available
                if (userLocation != null) {
                    searchPlacesByQuery(query, localFiltered)
                } else {
                    // Just show local filtered results
                    pantryAdapter.updateList(localFiltered)
                    if (localFiltered.isEmpty()) {
                        Toast.makeText(
                            this,
                            "No results found. Enable location to search nearby places.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // ✅ Bottom navigation wiring
        binding.btnHome.setOnClickListener {
            // already on Home — scroll to top as a UX nicety
            binding.scrollView.smoothScrollTo(0, 0)
        }
        binding.btnRecipes.setOnClickListener {
            // Navigate to Recipes page (or show toast for now)
            Toast.makeText(this, "Recipes page coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.btnAdd.setOnClickListener {
            // Navigate to PublishPantryActivity (add new pantry)
            startActivity(Intent(this, PublishPantryActivity::class.java))
        }
        binding.btnPantry.setOnClickListener {
            // Navigate to Cart
            startActivity(Intent(this, CartActivity::class.java))
        }
        binding.btnProfile.setOnClickListener {
            // open your profile screen
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // ✅ Launch Pantry Detector (for testing object detection)
        // You can add a button in the layout or use this as a quick test entry point
        // For now, you can launch it via ADB: adb shell am start -n com.unh.pantrypalonevo/.PantryDetectorActivity

        // Ensure bottom bar always receives taps (avoid overlap from list)
        binding.bottomNavigation.bringToFront()
        binding.bottomNavigation.isClickable = true
    }

    private fun loadPantries() {
        // If we have user location, fetch from Google Places API (primary source)
        if (userLocation != null) {
            // Load from Firestore in parallel, but prioritize Places API
            firestore.collection("pantries")
                .get()
                .addOnSuccessListener { snapshot ->
                    val loadedPantries = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name").orEmpty()
                        val address = doc.getString("address").orEmpty()
                        if (name.isBlank() && address.isBlank()) return@mapNotNull null

                        val start = doc.getString("startDate").orEmpty()
                        val end = doc.getString("endDate").orEmpty()
                        val lat = doc.getDouble("latitude")
                        val lon = doc.getDouble("longitude")
                        val distanceMeters = if (lat != null && lon != null && userLocation != null) {
                            calculateDistanceMeters(userLocation!!, lat, lon)
                        } else null
                        val distanceText = distanceMeters?.let { formatDistance(it) }
                            ?: doc.getString("distance").orEmpty()

                        Pantry(
                            name = name.ifBlank { "Untitled Pantry" },
                            description = listOf(start, end).filter { it.isNotBlank() }.joinToString(" - "),
                            address = address,
                            distance = distanceText,
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = distanceMeters
                        )
                    }
                    // Merge with Places API results
                    fetchPlacesPantries(loadedPantries)
                }
                .addOnFailureListener {
                    // Firestore failed, but still try Places API
                    android.util.Log.e("HomePageActivity", "Firestore load failed", it)
                    fetchPlacesPantries(emptyList())
                }
        } else {
            // No location yet - try Firestore only, or wait for location
            firestore.collection("pantries")
                .get()
                .addOnSuccessListener { snapshot ->
                    val loadedPantries = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name").orEmpty()
                        val address = doc.getString("address").orEmpty()
                        if (name.isBlank() && address.isBlank()) return@mapNotNull null

                        val start = doc.getString("startDate").orEmpty()
                        val end = doc.getString("endDate").orEmpty()
                        val lat = doc.getDouble("latitude")
                        val lon = doc.getDouble("longitude")

                        Pantry(
                            name = name.ifBlank { "Untitled Pantry" },
                            description = listOf(start, end).filter { it.isNotBlank() }.joinToString(" - "),
                            address = address,
                            distance = "",
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = null
                        )
                    }
                    displayPantries(loadedPantries)
                }
                .addOnFailureListener {
                    // No location and Firestore failed - show empty state
                    displayPantries(emptyList())
                    Toast.makeText(this, "Please enable location to find nearby pantries", Toast.LENGTH_LONG).show()
                }
        }
    }
    
    /**
     * Fetch nearby pantries from Google Places API and merge with Firestore results
     */
    private fun fetchPlacesPantries(firestorePantries: List<Pantry>) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomePageActivity", "Fetching Places pantries...")
                android.util.Log.d("HomePageActivity", "User location: ${userLocation?.latitude}, ${userLocation?.longitude}")
                android.util.Log.d("HomePageActivity", "Firestore pantries count: ${firestorePantries.size}")
                
                // Fetch from Google Places API using REST API method (more reliable)
                val placesPantries = placesService.searchNearbyPantriesRestApi(userLocation!!)
                
                android.util.Log.d("HomePageActivity", "Places API returned ${placesPantries.size} pantries")
                
                // Merge Firestore and Places results
                val allPantries = mutableListOf<Pantry>()
                allPantries.addAll(firestorePantries)
                allPantries.addAll(placesPantries)
                
                android.util.Log.d("HomePageActivity", "Total pantries before deduplication: ${allPantries.size}")
                
                // Remove duplicates (same name + address)
                val uniquePantries = allPantries.distinctBy { "${it.name}_${it.address}" }
                
                android.util.Log.d("HomePageActivity", "Unique pantries after deduplication: ${uniquePantries.size}")
                
                // Calculate distances for all pantries
                val pantriesWithDistance = uniquePantries.map { pantry ->
                    if (pantry.latitude != null && pantry.longitude != null && userLocation != null) {
                        val distanceMeters = calculateDistanceMeters(
                            userLocation!!,
                            pantry.latitude!!,
                            pantry.longitude!!
                        )
                        pantry.copy(
                            distanceMeters = distanceMeters,
                            distance = formatDistance(distanceMeters)
                        )
                    } else {
                        pantry
                    }
                }
                
                // Filter and sort by distance
                val nearbyFiltered = pantriesWithDistance
                    .filter { pantry ->
                        pantry.distanceMeters?.let { it <= NEARBY_RADIUS_METERS } ?: true
                    }
                    .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
                
                android.util.Log.d("HomePageActivity", "Final filtered pantries: ${nearbyFiltered.size}")
                
                displayPantries(nearbyFiltered)
                
                if (placesPantries.isNotEmpty()) {
                    Toast.makeText(
                        this@HomePageActivity,
                        "Found ${placesPantries.size} nearby pantry shops",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (nearbyFiltered.isEmpty()) {
                    Toast.makeText(
                        this@HomePageActivity,
                        "No nearby pantries found. Check Logcat for details.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomePageActivity", "Error fetching Places pantries", e)
                e.printStackTrace()
                // Fallback to just Firestore results
                displayPantries(firestorePantries)
                Toast.makeText(
                    this@HomePageActivity,
                    "Error fetching nearby pantries: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Display pantries in the RecyclerView
     * Shows only real data from Places API and Firestore - no hardcoded samples
     */
    private fun displayPantries(pantries: List<Pantry>) {
        pantryList.clear()
        pantryList.addAll(pantries)
        pantryAdapter.notifyDataSetChanged()
        
        // Show message if no pantries found
        if (pantries.isEmpty()) {
            if (userLocation == null) {
                Toast.makeText(
                    this, 
                    "Enable location to find nearby pantry shops", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this, 
                    "No nearby pantries found. Try a different location or check your internet connection.", 
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            android.util.Log.d("HomePageActivity", "Displaying ${pantries.size} pantries")
        }
    }

    // === LOCATION PERMISSION & FETCH ===
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) fetchUserLocation()
            else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }

    private fun requestLocationPermissionAndFetch() {
        when {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> fetchUserLocation()
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.e("HomePageActivity", "❌ Location permission not granted")
            return
        }

        android.util.Log.d("HomePageActivity", "📍 Requesting location...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLocation = location
                    android.util.Log.d("HomePageActivity", "✅ Location received:")
                    android.util.Log.d("HomePageActivity", "   - Lat: ${location.latitude}")
                    android.util.Log.d("HomePageActivity", "   - Lng: ${location.longitude}")
                    android.util.Log.d("HomePageActivity", "   - Accuracy: ${location.accuracy}m")
                    binding.locationEnableCard.alpha = 0.7f
                    Toast.makeText(this, "Location enabled successfully", Toast.LENGTH_SHORT).show()
                    loadPantries()
                } else {
                    android.util.Log.w("HomePageActivity", "⚠️ Location is null - may need to wait for GPS")
                    Toast.makeText(this, "Unable to get location. Try moving to get GPS signal.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("HomePageActivity", "❌ Location fetch failed: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this, "Error fetching location: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun calculateDistanceMeters(userLocation: Location, lat: Double, lon: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            lat,
            lon,
            results
        )
        return results[0].toDouble()
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.roundToInt()} m"
        } else {
            val miles = distanceMeters / 1609.34
            String.format(Locale.getDefault(), "%.1f mi", miles)
        }
    }
    
    /**
     * Search Google Places API using user's search query
     */
    private fun searchPlacesByQuery(query: String, localResults: List<Pantry>) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomePageActivity", "Searching Places API for: $query")
                
                // Search Places API
                val placesResults = placesService.searchPlacesByQuery(query, userLocation)
                
                // Merge local and Places results
                val allResults = mutableListOf<Pantry>()
                allResults.addAll(localResults)
                allResults.addAll(placesResults)
                
                // Remove duplicates
                val uniqueResults = allResults.distinctBy { "${it.name}_${it.address}" }
                
                // Calculate distances if location available
                val resultsWithDistance = uniqueResults.map { pantry ->
                    if (pantry.latitude != null && pantry.longitude != null && userLocation != null) {
                        val distanceMeters = calculateDistanceMeters(
                            userLocation!!,
                            pantry.latitude!!,
                            pantry.longitude!!
                        )
                        pantry.copy(
                            distanceMeters = distanceMeters,
                            distance = formatDistance(distanceMeters)
                        )
                    } else {
                        pantry
                    }
                }
                
                // Sort by distance
                val sortedResults = resultsWithDistance.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
                
                // Update UI
                pantryAdapter.updateList(sortedResults)
                
                if (sortedResults.isEmpty()) {
                    Toast.makeText(
                        this@HomePageActivity,
                        "No pantries found for '$query'",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@HomePageActivity,
                        "Found ${sortedResults.size} result(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomePageActivity", "Error searching Places API", e)
                // Fallback to local results only
                pantryAdapter.updateList(localResults)
                Toast.makeText(
                    this@HomePageActivity,
                    "Search error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
