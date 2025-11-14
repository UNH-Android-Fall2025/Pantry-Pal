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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding
import com.unh.pantrypalonevo.model.Pantry
import com.unh.pantrypalonevo.adapter.PantryAdapter

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pantryAdapter: PantryAdapter
    private val pantryList = mutableListOf<Pantry>()

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

        // ✅ Load sample pantries (replace with Firestore later)
        loadSamplePantries()

        // ✅ Handle location enable card
        binding.btnEnableLocation.setOnClickListener {
            requestLocationPermissionAndFetch()
        }

        // ✅ Search filter
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isEmpty()) {
                pantryAdapter.updateList(pantryList)
            } else {
                val filtered = pantryList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
                pantryAdapter.updateList(filtered)
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
            // Navigate to Cart (repointed from Pantry)
            Toast.makeText(this, "Cart page coming soon!", Toast.LENGTH_SHORT).show()
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

    // === SAMPLE PANTRIES (you can replace with real Firestore later) ===
    private fun loadSamplePantries() {
        pantryList.clear()
        pantryList.addAll(
            listOf(
                Pantry(
                    name = "Community Bread Basket",
                    description = "Fresh produce, canned goods, bread",
                    address = "123 Main Street, Anytown",
                    distance = "0.5 mi"
                ),
                Pantry(
                    name = "Hopewell Food Bank",
                    description = "Family-friendly pantry with clothing",
                    address = "456 Oak Avenue, Anytown",
                    distance = "1.2 mi"
                ),
                Pantry(
                    name = "Neighbor's Fridge",
                    description = "Hot meals served Mon-Fri",
                    address = "789 Pine Lane, Anytown",
                    distance = "2.1 mi"
                )
            )
        )
        pantryAdapter.notifyDataSetChanged()
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
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Update location enable card to show success
                    binding.locationEnableCard.alpha = 0.7f
                    Toast.makeText(this, "Location enabled successfully", Toast.LENGTH_SHORT).show()
                    // You can update the card text here if needed
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show()
            }
    }
}
