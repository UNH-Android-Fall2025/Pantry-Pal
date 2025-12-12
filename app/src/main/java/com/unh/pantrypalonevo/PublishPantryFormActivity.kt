package com.unh.pantrypalonevo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivityPublishPantryFormBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PublishPantryFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishPantryFormBinding
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val pantryItems = mutableListOf<DetectedProduct>()
    private val imageUriMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishPantryFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPantryItems()
        setupToolbar()
        setupTextFields()
        setupDatePickers()
        setupButtons()
    }
    
    private fun loadPantryItems() {
        // Load items from intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_PANTRY_ITEMS, DetectedProduct::class.java)?.let {
                pantryItems.clear()
                pantryItems.addAll(it)
            }
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<DetectedProduct>(EXTRA_PANTRY_ITEMS)?.let {
                pantryItems.clear()
                pantryItems.addAll(it)
            }
        }
        
        // Load image URI map
        intent.getBundleExtra(EXTRA_IMAGE_URI_MAP)?.let { bundle ->
            bundle.keySet().forEach { key ->
                bundle.getString(key)?.let { uri ->
                    imageUriMap[key] = uri
                }
            }
        }
        
        Log.d("PublishPantryForm", "Loaded ${pantryItems.size} items for pantry")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_publish_form)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener { showDatePicker { date -> binding.etStartDate.setText(date) } }
        binding.etEndDate.setOnClickListener { showDatePicker { date -> binding.etEndDate.setText(date) } }
    }

    private fun setupTextFields() {
        binding.etPantryName.doAfterTextChanged {
            binding.etPantryName.error = null
            binding.btnClearPantryName.isVisible = !it.isNullOrEmpty()
        }
        binding.btnClearPantryName.setOnClickListener {
            binding.etPantryName.setText("")
        }

        binding.etAddress.doAfterTextChanged {
            binding.etAddress.error = null
            binding.btnClearAddress.isVisible = !it.isNullOrEmpty()
        }
        binding.btnClearAddress.setOnClickListener {
            binding.etAddress.setText("")
        }
    }

    private fun setupButtons() {
        binding.btnReject.setOnClickListener { finish() }

        binding.btnConfirm.setOnClickListener {
            val name = binding.etPantryName.text?.toString()?.trim().orEmpty()
            val address = binding.etAddress.text?.toString()?.trim().orEmpty()
            val startDate = binding.etStartDate.text?.toString()?.trim().orEmpty()
            val endDate = binding.etEndDate.text?.toString()?.trim().orEmpty()

            if (name.isEmpty()) {
                binding.etPantryName.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                binding.etAddress.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            if (startDate.isEmpty()) {
                binding.etStartDate.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            if (endDate.isEmpty()) {
                binding.etEndDate.error = getString(R.string.error_required)
                return@setOnClickListener
            }
            
            // Check if items were added
            if (pantryItems.isEmpty()) {
                Toast.makeText(this, "Please add at least one item to your pantry before publishing.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            publishPantry(name, address, startDate, endDate)
        }
    }

    private fun publishPantry(name: String, address: String, startDate: String, endDate: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to publish a pantry.", Toast.LENGTH_SHORT).show()
            binding.btnConfirm.isEnabled = true
            return
        }
        
        binding.btnConfirm.isEnabled = false
        
        // Get user's display name for donor name
        val userPrefs = getSharedPreferences("PantryPal_UserPrefs", MODE_PRIVATE)
        val donorName = userPrefs.getString("user_name", null) 
            ?: currentUser.displayName 
            ?: currentUser.email?.substringBefore("@") 
            ?: "Anonymous Donor"
        
        // Create pantry document data
        val pantryData = hashMapOf<String, Any>(
            "name" to name,
            "address" to address,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        Log.d("PublishPantryForm", "📝 Publishing pantry to Firestore:")
        Log.d("PublishPantryForm", "   - Name: $name")
        Log.d("PublishPantryForm", "   - Address: $address")
        Log.d("PublishPantryForm", "   - Owner ID: ${currentUser.uid}")
        Log.d("PublishPantryForm", "   - Donor Name: $donorName")
        Log.d("PublishPantryForm", "   - Items: ${pantryItems.size} items")
        Log.d("PublishPantryForm", "   - Structure: pantries/{pantryId}/donors/{donorId}/items/{itemId}")

        // First, create or get the pantry document
        firestore.collection("pantries")
            .whereEqualTo("name", name)
            .whereEqualTo("address", address)
            .limit(1)
            .get()
            .addOnSuccessListener { pantryQuery ->
                val pantryRef = if (!pantryQuery.isEmpty) {
                    // Pantry exists, use it
                    pantryQuery.documents.first().reference
                } else {
                    // Create new pantry with auto-generated ID
                    firestore.collection("pantries").document()
                }
                
                // Set/update pantry document
                pantryRef.set(pantryData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        // Create donor document under pantry
                        val donorRef = pantryRef.collection("donors").document(currentUser.uid)
                        val donorData = hashMapOf<String, Any>(
                            "name" to donorName,
                            "contact" to (currentUser.email ?: "")
                        )
                        
                        donorRef.set(donorData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                // Save each item as a document in the items subcollection
                                val batch = firestore.batch()
                                
                                pantryItems.forEachIndexed { index, product ->
                                    val itemRef = donorRef.collection("items").document("item_${currentUser.uid}_$index")
                                    val itemData = hashMapOf<String, Any>(
                                        "name" to product.name,
                                        "category" to "", // You can add category detection later
                                        "tags" to listOf<String>(), // You can add tags later
                                        "quantity" to product.quantity,
                                        "expiration" to "", // You can add expiration date later
                                        "pickupLocation" to address,
                                        "addedAt" to FieldValue.serverTimestamp()
                                    )
                                    
                                    // Add imageUri if available
                                    imageUriMap[product.name]?.let { uri ->
                                        itemData["imageUri"] = uri
                                    }
                                    
                                    batch.set(itemRef, itemData)
                                }
                                
                                // Commit all items in batch
                                batch.commit()
                                    .addOnSuccessListener {
                                        Log.d("PublishPantryForm", "✅ Pantry published successfully!")
                                        Log.d("PublishPantryForm", "   - Pantry ID: ${pantryRef.id}")
                                        Log.d("PublishPantryForm", "   - Donor ID: ${currentUser.uid}")
                                        Log.d("PublishPantryForm", "   - Items saved: ${pantryItems.size}")
                                        
                                        Toast.makeText(this, getString(R.string.toast_pantry_published), Toast.LENGTH_SHORT).show()
                                        navigateHome(name, address, startDate, endDate)
                                    }
                                    .addOnFailureListener { error ->
                                        binding.btnConfirm.isEnabled = true
                                        Log.e("PublishPantryForm", "❌ Error saving items: ${error.message}")
                                        Toast.makeText(this, "Error saving items: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { error ->
                                binding.btnConfirm.isEnabled = true
                                Log.e("PublishPantryForm", "❌ Error creating donor: ${error.message}")
                                Toast.makeText(this, "Error creating donor: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { error ->
                        binding.btnConfirm.isEnabled = true
                        Log.e("PublishPantryForm", "❌ Error creating pantry: ${error.message}")
                        Toast.makeText(this, "Error creating pantry: ${error.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { error ->
                binding.btnConfirm.isEnabled = true
                Log.e("PublishPantryForm", "❌ Error checking pantry: ${error.message}")
                Toast.makeText(this, "Error checking pantry: ${error.message}", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { error ->
                binding.btnConfirm.isEnabled = true
                Log.e("PublishPantryForm", "❌ Error publishing pantry to Firestore")
                Log.e("PublishPantryForm", "   - Error type: ${error.javaClass.simpleName}")
                Log.e("PublishPantryForm", "   - Error message: ${error.message}")
                Log.e("PublishPantryForm", "   - Collection: pantries")
                Log.e("PublishPantryForm", "   - Owner ID: ${currentUser.uid}")
                error.printStackTrace()
                Toast.makeText(this, "Unable to publish pantry: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateHome(name: String, address: String, startDate: String, endDate: String) {
        val toHome = Intent(this, HomePageActivity::class.java).apply {
            putExtra(EXTRA_PANTRY_NAME, name)
            putExtra(EXTRA_PANTRY_ADDRESS, address)
            putExtra(EXTRA_PANTRY_START_DATE, startDate)
            putExtra(EXTRA_PANTRY_END_DATE, endDate)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(toHome)
        finish()
    }

    private fun showDatePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                onPicked(dateFormat.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    companion object {
        const val EXTRA_PANTRY_NAME = "extra_pantry_name"
        const val EXTRA_PANTRY_ADDRESS = "extra_pantry_address"
        const val EXTRA_PANTRY_START_DATE = "extra_pantry_start"
        const val EXTRA_PANTRY_END_DATE = "extra_pantry_end"
        const val EXTRA_PANTRY_ITEMS = "extra_pantry_items"
        const val EXTRA_IMAGE_URI_MAP = "extra_image_uri_map"
    }
}
