package com.unh.pantrypalonevo

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
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
import java.util.regex.Pattern
import kotlin.math.roundToInt

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pantryAdapter: PantryAdapter
    private val pantryList = mutableListOf<Pantry>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var userLocation: Location? = null
    private lateinit var placesService: PlacesService
    private lateinit var prefs: SharedPreferences
    
    companion object {
        private const val NEARBY_RADIUS_METERS = 50_000.0 // 50 km
        private const val PREFS_NAME = "PantryPal_UserPrefs"
        private const val KEY_LOCATION_ENABLED = "has_location_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val username = prefs.getString("user_username", null)
        val displayName = prefs.getString("user_name", "User")
        
        val greetingName = if (!username.isNullOrBlank()) {
            username
        } else {
            displayName
        }
        
        binding.tvGreeting.text = "Hello, $greetingName!"

        pantryAdapter = PantryAdapter(pantryList) { pantry ->
            val intent = Intent(this, PantryProductsActivity::class.java)
            intent.putExtra("pantry_name", pantry.name)
            intent.putExtra("pantry_address", pantry.address)
            intent.putExtra("pantry_description", pantry.description)
            // Pass coordinates if available
            pantry.latitude?.let { intent.putExtra("pantry_latitude", it) }
            pantry.longitude?.let { intent.putExtra("pantry_longitude", it) }
            // Pass zipCode and pantryId if available
            pantry.zipCode?.let { intent.putExtra("pantry_zip_code", it) }
            pantry.pantryId?.let { intent.putExtra("pantry_id", it) }
            startActivity(intent)
        }

        binding.rvPantryList.layoutManager = LinearLayoutManager(this)
        binding.rvPantryList.adapter = pantryAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places Service
        placesService = PlacesService.getInstance(this)

        // Check if location was already enabled previously
        if (isLocationEnabled() && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.locationEnableCard.visibility = View.GONE
            // Try to get location if we already have permission
            fetchUserLocation()
        }

        // Load pantries from Places API and Firestore
        loadPantries()

        binding.btnEnableLocation.setOnClickListener {
            requestLocationPermissionAndFetch()
        }
        // Try fetching location immediately for nearby filtering (only if not already enabled)
        if (!isLocationEnabled()) {
            requestLocationPermissionAndFetch()
        }

        // Search filter - searches both local list and Google Places API
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

        binding.btnHome.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
        }

        binding.btnRecipes.setOnClickListener {
            // Get items from cart or allow user to select items
            openRecipeActivity()
        }

        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, PublishPantryActivity::class.java))
        }

        binding.btnPantry.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

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
                        
                        // Extract zipCode from address
                        val zipCode = extractZipCodeFromAddress(address)

                            Pantry(
                                name = name.ifBlank { "Untitled Pantry" },
                                description = listOf(start, end).filter { it.isNotBlank() }.joinToString(" - "),
                                address = address,
                            distance = distanceText,
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = distanceMeters,
                            zipCode = zipCode,
                            pantryId = doc.id
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
                        
                        // Extract zipCode from address
                        val zipCode = extractZipCodeFromAddress(address)

                        Pantry(
                            name = name.ifBlank { "Untitled Pantry" },
                            description = listOf(start, end).filter { it.isNotBlank() }.joinToString(" - "),
                            address = address,
                            distance = "",
                            latitude = lat,
                            longitude = lon,
                            distanceMeters = null,
                            zipCode = zipCode,
                            pantryId = doc.id
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
                
                // Calculate distances for all pantries and extract zipCode
                val pantriesWithDistance = uniquePantries.map { pantry ->
                    val zipCode = pantry.zipCode ?: extractZipCodeFromAddress(pantry.address)
                    if (pantry.latitude != null && pantry.longitude != null && userLocation != null) {
                        val distanceMeters = calculateDistanceMeters(
                            userLocation!!,
                            pantry.latitude!!,
                            pantry.longitude!!
                        )
                        pantry.copy(
                            distanceMeters = distanceMeters,
                            distance = formatDistance(distanceMeters),
                            zipCode = zipCode
                        )
                    } else {
                        pantry.copy(zipCode = zipCode)
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
            android.util.Log.e("HomePageActivity", "Location permission not granted")
            return
        }

        android.util.Log.d("HomePageActivity", "📍 Requesting location...")
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLocation = location
                    android.util.Log.d("HomePageActivity", "Location received:")
                    android.util.Log.d("HomePageActivity", "   - Lat: ${location.latitude}")
                    android.util.Log.d("HomePageActivity", "   - Lng: ${location.longitude}")
                    android.util.Log.d("HomePageActivity", "   - Accuracy: ${location.accuracy}m")
                    
                    // Mark location as enabled and animate card out
                    setLocationEnabled(true)
                    animateCardOut()
                    
                    Toast.makeText(this, "Location enabled successfully", Toast.LENGTH_SHORT).show()
                    loadPantries()
                } else {
                    android.util.Log.w("HomePageActivity", "Location is null - may need to wait for GPS")
                    Toast.makeText(this, "Unable to get location. Try moving to get GPS signal.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("HomePageActivity", "Location fetch failed: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this, "Error fetching location: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    /**
     * Check if location was previously enabled
     */
    private fun isLocationEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCATION_ENABLED, false)
    }
    
    /**
     * Save location enabled state
     */
    private fun setLocationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }
    
    /**
     * Animate the location card upward and fade out, then hide it
     */
    private fun animateCardOut() {
        val card = binding.locationEnableCard
        if (card.visibility != View.VISIBLE) return
        
        // Ensure the view is measured before getting height
        card.post {
            val cardHeight = if (card.height > 0) card.height else {
                // Fallback: estimate height based on typical card size (padding + content)
                // Approximate: 16dp padding top + 16dp padding bottom + ~80dp content = ~112dp
                // Convert dp to pixels (roughly 3.5dp per pixel on most devices)
                (112 * resources.displayMetrics.density).toInt()
            }
            
            // Slide up animation - move card up by its height plus some margin
            val slideUp = ObjectAnimator.ofFloat(card, "translationY", 0f, -cardHeight.toFloat() - 24f)
            slideUp.duration = 400
            slideUp.interpolator = AccelerateInterpolator()
            
            // Fade out animation
            val fadeOut = ObjectAnimator.ofFloat(card, "alpha", 1f, 0f)
            fadeOut.duration = 400
            fadeOut.interpolator = DecelerateInterpolator()
            
            // Combine animations
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(slideUp, fadeOut)
            animatorSet.start()
            
            // Hide the card after animation completes
            animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    card.visibility = View.GONE
                }
            })
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
    
    private fun extractZipCodeFromAddress(address: String): String? {
        // Try to extract 5-digit zip code from address
        val pattern = Pattern.compile("\\b\\d{5}(?:-\\d{4})?\\b")
        val matcher = pattern.matcher(address)
        return if (matcher.find()) {
            matcher.group().substring(0, 5) // Take first 5 digits
        } else {
            null
        }
    }
    
    /**
     * Open RecipeActivity with items from cart or prompt user to select items
     */
    private fun openRecipeActivity() {
        // Try to get items from SharedPreferences (cart items might be stored there)
        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val cartItemsJson = prefs.getString("cart_items_json", null)
        
        val intent = Intent(this, RecipeActivity::class.java)
        
        // If we have cart items stored, pass them
        if (!cartItemsJson.isNullOrBlank()) {
            try {
                // For now, just pass a message - user can select items in RecipeActivity
                intent.putExtra("items_string", cartItemsJson)
            } catch (e: Exception) {
                android.util.Log.e("HomePageActivity", "Error parsing cart items", e)
            }
        }
        
        // If no items, RecipeActivity will prompt user or use default
        startActivity(intent)
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
