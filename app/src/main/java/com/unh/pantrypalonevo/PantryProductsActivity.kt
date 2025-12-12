package com.unh.pantrypalonevo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.adapter.PantryProductAdapter
import com.unh.pantrypalonevo.databinding.ActivityPantryProductsBinding
import com.unh.pantrypalonevo.model.PantryProduct
import com.unh.pantrypalonevo.CartItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

class PantryProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPantryProductsBinding
    private lateinit var productAdapter: PantryProductAdapter
    private val products = mutableListOf<PantryProduct>()
    private val selectedProducts = mutableSetOf<String>() // Track selected product IDs
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    
    private var pantryName: String = ""
    private var pantryAddress: String = ""
    private var pantryDescription: String = ""
    private var pantryLatitude: Double? = null
    private var pantryLongitude: Double? = null
    private var zipCode: String? = null
    private var pantryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantryProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPantryData()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        loadProducts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pantry Products"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Refresh button
        binding.btnRefresh.setOnClickListener {
            Log.d("PantryProducts", "🔄 Manual refresh triggered")
            products.clear()
            productAdapter.notifyDataSetChanged()
            // Force reload by clearing pantryId to trigger search
            val savedPantryId = pantryId
            pantryId = null
            loadProducts()
            // Restore pantryId if search fails
            if (pantryId == null) {
                pantryId = savedPantryId
            }
        }
    }

    private fun loadPantryData() {
        pantryName = intent.getStringExtra("pantry_name") ?: "Pantry"
        pantryAddress = intent.getStringExtra("pantry_address") ?: ""
        pantryDescription = intent.getStringExtra("pantry_description") ?: ""
        pantryLatitude = intent.getDoubleExtra("pantry_latitude", Double.NaN).takeIf { !it.isNaN() }
        pantryLongitude = intent.getDoubleExtra("pantry_longitude", Double.NaN).takeIf { !it.isNaN() }
        zipCode = intent.getStringExtra("pantry_zip_code")
        pantryId = intent.getStringExtra("pantry_id")
        
        // Extract zipCode from address if not provided
        if (zipCode.isNullOrBlank() && pantryAddress.isNotEmpty()) {
            zipCode = extractZipCode(pantryAddress)
        }

        binding.tvPantryName.text = pantryName
        binding.tvPantryAddress.text = pantryAddress
        binding.tvPantryDescription.text = pantryDescription

        Log.d("PantryProducts", "=== LOADING PANTRY DATA ===")
        Log.d("PantryProducts", "Pantry Name: $pantryName")
        Log.d("PantryProducts", "Pantry Address: $pantryAddress")
        Log.d("PantryProducts", "Pantry ID: $pantryId")
        Log.d("PantryProducts", "ZipCode: $zipCode")
    }

    private fun extractZipCode(address: String): String? {
        // Try to extract 5-digit zip code from address
        val pattern = Pattern.compile("\\b\\d{5}(?:-\\d{4})?\\b")
        val matcher = pattern.matcher(address)
        return if (matcher.find()) {
            matcher.group().substring(0, 5) // Take first 5 digits
        } else {
            "UNKNOWN"
        }
    }

    private fun setupRecyclerView() {
        productAdapter = PantryProductAdapter(products) { product, isSelected ->
            if (isSelected) {
                selectedProducts.add(product.productId)
            } else {
                selectedProducts.remove(product.productId)
            }
            updateCartButton()
        }
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = productAdapter
    }

    private fun setupButtons() {
        binding.btnDirections.setOnClickListener {
            openDirections()
        }

        binding.btnReviewSelection.setOnClickListener {
            if (selectedProducts.isEmpty()) {
                Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
            } else {
                goToCart()
            }
        }
    }

    private fun loadProducts() {
        Log.d("PantryProducts", "=== STARTING loadProducts() ===")
        Log.d("PantryProducts", "pantryId: $pantryId")
        Log.d("PantryProducts", "pantryName: $pantryName")
        Log.d("PantryProducts", "pantryAddress: $pantryAddress")
        
        if (pantryId.isNullOrBlank()) {
            Log.w("PantryProducts", "Missing pantryId, trying to find pantry by name/address")
            findPantryAndLoadProducts()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d("PantryProducts", "Fetching pantry document: $pantryId")
                val pantryRef = firestore.collection("pantries").document(pantryId!!)
                val pantryDoc = pantryRef.get().await()
                
                if (!pantryDoc.exists()) {
                    Log.w("PantryProducts", "Pantry document not found: $pantryId")
                    Log.w("PantryProducts", "   Trying to find by name: $pantryName, address: $pantryAddress")
                    Toast.makeText(this@PantryProductsActivity, "Pantry not found. Trying to search by name...", Toast.LENGTH_SHORT).show()
                    findPantryAndLoadProducts()
                    return@launch
                }
                
                Log.d("PantryProducts", "Pantry document exists")
                Log.d("PantryProducts", "Pantry data: ${pantryDoc.data}")
                
                products.clear()
                
                // Store pantryId in local variable to avoid smart cast issues
                val currentPantryId = pantryId ?: run {
                    Log.e("PantryProducts", "pantryId is null after document check")
                    binding.progressBar.visibility = View.GONE
                    updateEmptyState()
                    return@launch
                }
                
                // Try new structure first: pantries/{pantryId}/donors/{donorId}/items/{itemId}
                Log.d("PantryProducts", "Checking for donors subcollection at: pantries/$currentPantryId/donors")
                var donorsSnapshot = try {
                    pantryRef.collection("donors").get().await()
                } catch (e: Exception) {
                    Log.e("PantryProducts", "Error fetching donors: ${e.message}")
                    firestore.collection("pantries").document(currentPantryId).collection("donors").get().await()
                }
                
                Log.d("PantryProducts", "Found ${donorsSnapshot.size()} donors")
                
                if (!donorsSnapshot.isEmpty) {
                    // New structure: Read from donors/items subcollections
                    Log.d("PantryProducts", "Using new structure: donors/items")
                    Log.d("PantryProducts", "   Found ${donorsSnapshot.size()} donor(s)")
                    
                    var totalItems = 0
                    for (donorDoc in donorsSnapshot.documents) {
                        val donorId = donorDoc.id
                        val donorName = donorDoc.getString("name") ?: "Unknown"
                        Log.d("PantryProducts", "Processing donor: $donorId (name: $donorName)")
                        Log.d("PantryProducts", "     Path: pantries/$currentPantryId/donors/$donorId/items")
                        
                        try {
                            val itemsSnapshot = donorDoc.reference.collection("items").get().await()
                            Log.d("PantryProducts", "     Found ${itemsSnapshot.size()} items for donor $donorId")
                            totalItems += itemsSnapshot.size()
                            
                            if (itemsSnapshot.isEmpty) {
                                Log.w("PantryProducts", "No items in subcollection for donor $donorId")
                            }
                            
                            itemsSnapshot.documents.forEach { itemDoc ->
                                val itemData = itemDoc.data ?: emptyMap()
                                val itemName = itemData["name"] as? String ?: ""
                                val itemQuantity = (itemData["quantity"] as? Long ?: 1).toInt()
                                Log.d("PantryProducts", "Item ID: ${itemDoc.id}")
                                Log.d("PantryProducts", "          Name: $itemName")
                                Log.d("PantryProducts", "          Quantity: $itemQuantity")
                                Log.d("PantryProducts", "          Data: $itemData")
                                
                                products.add(
                                    PantryProduct(
                                        productId = itemDoc.id,
                                        name = itemName,
                                        description = itemData["pickupLocation"] as? String ?: itemData["description"] as? String ?: "",
                                        imageUrl = itemData["imageUri"] as? String,
                                        category = itemData["category"] as? String ?: "",
                                        selected = false,
                                        quantity = itemQuantity,
                                        pantryId = currentPantryId,
                                        donorId = donorDoc.id
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("PantryProducts", "Error loading items for donor $donorId", e)
                            e.printStackTrace()
                        }
                    }
                    
                    Log.d("PantryProducts", "Loaded ${products.size} items from ${donorsSnapshot.size()} donors (new structure)")
                    
                    if (products.isEmpty()) {
                        Log.w("PantryProducts", "No items found in any donor subcollection")
                        Log.w("PantryProducts", "   Total donors checked: ${donorsSnapshot.size()}")
                        Log.w("PantryProducts", "   This pantry may have been published without items")
                        Log.w("PantryProducts", "   OR items were not saved correctly during publishing")
                        
                        // Try to diagnose: Check all donors and their items
                        Log.d("PantryProducts", "DIAGNOSTIC: Checking all donors and items...")
                        for (donorDoc in donorsSnapshot.documents) {
                            Log.d("PantryProducts", "     Donor ID: ${donorDoc.id}")
                            Log.d("PantryProducts", "     Donor data: ${donorDoc.data}")
                            try {
                                val itemsCheck = donorDoc.reference.collection("items").get().await()
                                Log.d("PantryProducts", "     Items in subcollection: ${itemsCheck.size()}")
                                itemsCheck.documents.forEach { item ->
                                    Log.d("PantryProducts", "       - Item ID: ${item.id}, Name: ${item.getString("name")}")
                                }
                            } catch (e: Exception) {
                                Log.e("PantryProducts", "     Error checking items: ${e.message}")
                            }
                        }
                        
                        Toast.makeText(
                            this@PantryProductsActivity, 
                            "No products found. Check Logcat for details. Try refreshing.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Fallback to old structure: products array in pantry document
                    Log.d("PantryProducts", "No donors found, checking for products array...")
                    @Suppress("UNCHECKED_CAST")
                    val productsList = pantryDoc.get("products") as? List<Map<String, Any>>?
                    
                    if (productsList != null && productsList.isNotEmpty()) {
                        Log.d("PantryProducts", "Using old structure: products array")
                        Log.d("PantryProducts", "Found ${productsList.size} products in array")
                        
                        // Try to get donor ID from pantry (for old structure, use ownerId if available)
                        val ownerId = pantryDoc.getString("ownerId")
                        
                        products.addAll(productsList.mapIndexed { index, productMap ->
                            Log.d("PantryProducts", "  Product $index: ${productMap["name"]}")
                            PantryProduct(
                                productId = index.toString(),
                                name = productMap["name"] as? String ?: "",
                                description = productMap["imageUri"] as? String ?: "",
                                imageUrl = productMap["imageUri"] as? String,
                                category = "",
                                selected = false,
                                quantity = (productMap["quantity"] as? Long ?: 1).toInt(),
                                pantryId = currentPantryId,
                                donorId = ownerId
                            )
                        })
                        
                        Log.d("PantryProducts", "Loaded ${products.size} items from products array (old structure)")
                    } else {
                        Log.w("PantryProducts", "No products found in either structure")
                        Log.w("PantryProducts", "Pantry document fields: ${pantryDoc.data?.keys}")
                        Log.w("PantryProducts", "This pantry was published but has no items")
                        Toast.makeText(
                            this@PantryProductsActivity, 
                            "No products found. Make sure you added items when publishing this pantry.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                Log.d("PantryProducts", "=== FINAL RESULT: ${products.size} products ===")
                Log.d("PantryProducts", "   Product names: ${products.map { it.name }}")
                
                // Force adapter update
                productAdapter.notifyDataSetChanged()
                
                // Ensure RecyclerView is visible and request layout
                binding.rvProducts.visibility = View.VISIBLE
                binding.rvProducts.requestLayout()
                
                updateEmptyState()
                binding.progressBar.visibility = View.GONE
                
                // Log adapter state
                Log.d("PantryProducts", "   Adapter itemCount: ${productAdapter.itemCount}")
                Log.d("PantryProducts", "   RecyclerView visibility: ${if (binding.rvProducts.visibility == View.VISIBLE) "VISIBLE" else "GONE"}")
                Log.d("PantryProducts", "   RecyclerView height: ${binding.rvProducts.height}")

            } catch (e: Exception) {
                Log.e("PantryProducts", "Error loading products", e)
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                
                // Check if it's a permission error
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("PantryProducts", "PERMISSION DENIED - Firestore rules need to be updated")
                    Toast.makeText(
                        this@PantryProductsActivity, 
                        "Permission denied. Please check Firestore security rules.", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("PantryProducts", "Error details: ${e.javaClass.simpleName} - ${e.message}")
                    Toast.makeText(this@PantryProductsActivity, "Error loading products: ${e.message}", Toast.LENGTH_LONG).show()
                }
                updateEmptyState()
            }
        }
    }

    private fun findPantryAndLoadProducts() {
        // Try to find pantry in Firestore by name and address
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                Log.d("PantryProducts", "Searching for pantry: name='$pantryName', address='$pantryAddress'")
                
                // Try new schema first: pantries/{zipCode}/posts/{pantryId}
                val zip = zipCode ?: "UNKNOWN"
                Log.d("PantryProducts", "   Trying zip-based structure: pantries/$zip/posts")
                try {
                    val postsSnapshot = firestore
                        .collection("pantries")
                        .document(zip)
                        .collection("posts")
                        .whereEqualTo("name", pantryName)
                        .whereEqualTo("address", pantryAddress)
                        .limit(1)
                        .get()
                        .await()

                    if (!postsSnapshot.isEmpty) {
                        val postDoc = postsSnapshot.documents.first()
                        pantryId = postDoc.id
                        zipCode = zip
                        Log.d("PantryProducts", "Found pantry in zip-based structure: $pantryId")
                        loadProducts()
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.d("PantryProducts", "   Zip-based structure not found or error: ${e.message}")
                }

                // Fallback: Try to find pantry by name/address in root collection
                Log.d("PantryProducts", "   Trying root collection: pantries (where name='$pantryName' AND address='$pantryAddress')")
                val oldSnapshot = firestore
                    .collection("pantries")
                    .whereEqualTo("name", pantryName)
                    .whereEqualTo("address", pantryAddress)
                    .limit(1)
                    .get()
                    .await()

                if (!oldSnapshot.isEmpty) {
                    val doc = oldSnapshot.documents.first()
                    val foundPantryId = doc.id
                    val foundName = doc.getString("name") ?: ""
                    val foundAddress = doc.getString("address") ?: ""
                    
                    Log.d("PantryProducts", "Found pantry in root collection")
                    Log.d("PantryProducts", "   Pantry ID: $foundPantryId")
                    Log.d("PantryProducts", "   Name: $foundName (searching for: $pantryName)")
                    Log.d("PantryProducts", "   Address: $foundAddress (searching for: $pantryAddress)")
                    Log.d("PantryProducts", "   Full document data: ${doc.data}")
                    
                    // Update pantryId
                    pantryId = foundPantryId
                    
                    // Verify items exist before loading
                    Log.d("PantryProducts", "Verifying items exist at: pantries/$foundPantryId/donors")
                    try {
                        val verifyDonors = firestore.collection("pantries")
                            .document(foundPantryId)
                            .collection("donors")
                            .get()
                            .await()
                        
                        Log.d("PantryProducts", "Found ${verifyDonors.size()} donor(s) in pantry")
                        if (!verifyDonors.isEmpty) {
                            var totalItemsCount = 0
                            for (donor in verifyDonors.documents) {
                                val donorId = donor.id
                                val donorName = donor.getString("name") ?: "Unknown"
                                Log.d("PantryProducts", "Donor: $donorId (name: $donorName)")
                                
                                val items = donor.reference.collection("items").get().await()
                                totalItemsCount += items.size()
                                Log.d("PantryProducts", "        Items: ${items.size()}")
                                
                                // Log each item
                                items.documents.forEach { item ->
                                    Log.d("PantryProducts", "          - ${item.id}: ${item.getString("name")} (qty: ${item.getLong("quantity")})")
                                }
                            }
                            Log.d("PantryProducts", "Total items found: $totalItemsCount")
                            
                            if (totalItemsCount == 0) {
                                Log.w("PantryProducts", "WARNING: Donors exist but no items found")
                            }
                        } else {
                            Log.w("PantryProducts", "No donors found in pantry")
                        }
                    } catch (e: Exception) {
                        Log.e("PantryProducts", "Error verifying items: ${e.message}")
                        e.printStackTrace()
                    }
                    
                    // Load products (will handle both old and new structure)
                    loadProducts()
                } else {
                    Log.w("PantryProducts", "Pantry not found by name/address")
                    Log.w("PantryProducts", "   Searched for: name='$pantryName', address='$pantryAddress'")
                    
                    // Try a broader search - just by name
                    Log.d("PantryProducts", "   Trying broader search: just by name='$pantryName'")
                    val nameOnlySnapshot = firestore
                        .collection("pantries")
                        .whereEqualTo("name", pantryName)
                        .limit(5)
                        .get()
                        .await()
                    
                    if (!nameOnlySnapshot.isEmpty) {
                        Log.d("PantryProducts", "   Found ${nameOnlySnapshot.size()} pantries with name '$pantryName':")
                        nameOnlySnapshot.documents.forEach { doc ->
                            Log.d("PantryProducts", "     - ID: ${doc.id}, Address: ${doc.getString("address")}")
                        }
                    }
                    
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PantryProductsActivity, 
                        "Pantry not found. Make sure the pantry was published successfully.", 
                        Toast.LENGTH_LONG
                    ).show()
                    updateEmptyState()
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("PantryProducts", "Error finding pantry", e)
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@PantryProductsActivity, "Error finding pantry: ${e.message}", Toast.LENGTH_LONG).show()
                updateEmptyState()
            }
        }
    }

    private fun updateEmptyState() {
        Log.d("PantryProducts", "updateEmptyState: products.size = ${products.size}")
        if (products.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.GONE
            Log.d("PantryProducts", "Showing empty state message")
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
            Log.d("PantryProducts", "Showing ${products.size} products in RecyclerView")
        }
    }

    private fun updateCartButton() {
        val count = selectedProducts.size
        if (count > 0) {
            binding.btnReviewSelection.text = "Review Selection ($count)"
            binding.btnReviewSelection.visibility = View.VISIBLE
        } else {
            binding.btnReviewSelection.visibility = View.GONE
        }
    }

    private fun openDirections() {
        if (pantryLatitude != null && pantryLongitude != null) {
            val gmmIntentUri = Uri.parse("google.navigation:q=${pantryLatitude},${pantryLongitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${pantryLatitude},${pantryLongitude}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } else if (pantryAddress.isNotEmpty()) {
            val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(pantryAddress)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(pantryAddress)}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                startActivity(webIntent)
            }
        } else {
            Toast.makeText(this, "No location available for directions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToCart() {
        val selectedItems = products.filter { selectedProducts.contains(it.productId) }
            .map { product ->
                CartItem(
                    id = product.productId,
                    name = product.name,
                    category = product.category,
                    quantity = product.quantity,
                    imageUrl = product.imageUrl,
                    pantryId = product.pantryId ?: pantryId,
                    pantryName = pantryName,
                    donorId = product.donorId,
                    itemId = product.productId
                )
            }

        val intent = Intent(this, CartActivity::class.java).apply {
            putParcelableArrayListExtra("cart_items", ArrayList(selectedItems))
            putExtra("pantry_id", pantryId)
            putExtra("pantry_name", pantryName)
            putExtra("pantry_address", pantryAddress)
        }
        startActivity(intent)
    }
}

