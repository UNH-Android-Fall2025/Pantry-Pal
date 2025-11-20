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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding
import com.unh.pantrypalonevo.model.Pantry
import com.unh.pantrypalonevo.adapter.PantryAdapter

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var pantryAdapter: PantryAdapter
    private val pantryList = mutableListOf<Pantry>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val username = prefs.getString("user_username", null)
        val displayName = prefs.getString("user_name", "User")

        val greetingName = if (!username.isNullOrBlank()) {
            username
        } else {
            displayName
        }

        binding.tvGreeting.text = "Hello, $greetingName!"

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

        loadPantries()

        binding.btnEnableLocation.setOnClickListener {
            requestLocationPermissionAndFetch()
        }

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

        binding.btnHome.setOnClickListener {
            binding.scrollView.smoothScrollTo(0, 0)
        }

        binding.btnRecipes.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            intent.putExtra("product_name", "Potato")
            startActivity(intent)
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
        firestore.collection("pantries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                pantryList.clear()
                if (!snapshot.isEmpty) {
                    snapshot.documents.forEach { doc ->
                        val name = doc.getString("name").orEmpty()
                        val address = doc.getString("address").orEmpty()
                        val start = doc.getString("startDate").orEmpty()
                        val end = doc.getString("endDate").orEmpty()
                        pantryList.add(
                            Pantry(
                                name = name.ifBlank { "Untitled Pantry" },
                                description = listOf(start, end).filter { it.isNotBlank() }.joinToString(" - "),
                                address = address,
                                distance = doc.getString("distance").orEmpty()
                            )
                        )
                    }
                    pantryAdapter.notifyDataSetChanged()
                } else {
                    loadSamplePantries()
                }
            }
            .addOnFailureListener {
                loadSamplePantries()
                Toast.makeText(this, "Unable to load pantries. Showing samples.", Toast.LENGTH_SHORT).show()
            }
    }

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
                    binding.locationEnableCard.alpha = 0.7f
                    Toast.makeText(this, "Location enabled successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show()
            }
    }
}
