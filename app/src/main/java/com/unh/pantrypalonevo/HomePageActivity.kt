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

        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val username = prefs.getString("user_username", null)
        val displayName = prefs.getString("user_name", "User")

        val greetingName = if (!username.isNullOrBlank()) username else displayName

        binding.tvGreeting.text = "Hello $greetingName !!"
        binding.tvSubtitle.text = "Free Pantry near you"

        pantryAdapter = PantryAdapter(pantryList) { pantry ->
            val intent = Intent(this, PantryLocationActivity::class.java)
            intent.putExtra("pantry_name", pantry.name)
            intent.putExtra("pantry_address", pantry.address)
            intent.putExtra("pantry_description", pantry.description)
            startActivity(intent)
        }
        binding.rvPantryList.layoutManager = LinearLayoutManager(this)
        binding.rvPantryList.adapter = pantryAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        loadSamplePantries()

        binding.ivLocationButton.setOnClickListener {
            requestLocationPermissionAndFetch()
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isEmpty()) {
                pantryAdapter.updateList(pantryList)
            } else {
                val filtered = pantryList.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.address.contains(query, ignoreCase = true)
                }
                pantryAdapter.updateList(filtered)
            }
        }

        binding.btnHome.setOnClickListener {
            if (pantryList.isNotEmpty()) binding.rvPantryList.smoothScrollToPosition(0)
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btnRecipes.setOnClickListener {
            Toast.makeText(this, "🍳 Recipes page coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(this, "🛒 Cart feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNavigation.bringToFront()
        binding.bottomNavigation.isClickable = true
    }

    private fun loadSamplePantries() {
        pantryList.clear()
        pantryList.addAll(
            listOf(
                Pantry(
                    name = "West Haven Food Bank",
                    description = "Fresh produce, canned goods, bread",
                    address = "123 Main St, West Haven, CT",
                    distance = "1.5 mi"
                ),
                Pantry(
                    name = "Community Pantry",
                    description = "Family-friendly pantry with clothing",
                    address = "456 Oak Ave, New Haven, CT",
                    distance = "1.9 mi"
                ),
                Pantry(
                    name = "Faith Community Kitchen",
                    description = "Hot meals served Mon-Fri",
                    address = "789 Church St, Milford, CT",
                    distance = "0.8 mi"
                ),
                Pantry(
                    name = "Neighborhood Support",
                    description = "Emergency food assistance",
                    address = "321 Elm St, West Haven, CT",
                    distance = "1.7 mi"
                )
            )
        )
        pantryAdapter.notifyDataSetChanged()
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
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    binding.tvCurrentLocation.text =
                        "Lat: %.4f, Lng: %.4f".format(location.latitude, location.longitude)
                    Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show()
            }
    }
}
