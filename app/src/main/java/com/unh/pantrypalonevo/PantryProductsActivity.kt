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
            Log.w("PantryProducts", "⚠️ Missing pantryId, trying to find pantry by name/address")
            findPantryAndLoadProducts()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d("PantryProducts", "📂 Fetching pantry document: $pantryId")
                val pantryRef = firestore.collection("pantries").document(pantryId!!)
                val pantryDoc = pantryRef.get().await()
                
                if (!pantryDoc.exists()) {
                    Log.w("PantryProducts", "❌ Pantry document not found: $pantryId")
                    Toast.makeText(this@PantryProductsActivity, "Pantry not found. Trying to search by name...", Toast.LENGTH_SHORT).show()
                    findPantryAndLoadProducts()
                    return@launch
                }
                
                Log.d("PantryProducts", "✅ Pantry document exists")
                Log.d("PantryProducts", "Pantry data: ${pantryDoc.data}")
                
                products.clear()
                
                // Try new structure first: pantries/{pantryId}/donors/{donorId}/items/{itemId}
                Log.d("PantryProducts", "🔍 Checking for donors subcollection...")
                val donorsSnapshot = pantryRef.collection("donors").get().await()
                
                Log.d("PantryProducts", "Found ${donorsSnapshot.size()} donors")
                
                if (!donorsSnapshot.isEmpty) {
                    // New structure: Read from donors/items subcollections
                    Log.d("PantryProducts", "✅ Using new structure: donors/items")
                    
                    for (donorDoc in donorsSnapshot.documents) {
                        Log.d("PantryProducts", "  📦 Processing donor: ${donorDoc.id}")
                        val itemsSnapshot = donorDoc.reference.collection("items").get().await()
                        Log.d("PantryProducts", "  Found ${itemsSnapshot.size()} items for donor ${donorDoc.id}")
                        
                        itemsSnapshot.documents.forEach { itemDoc ->
                            val itemData = itemDoc.data ?: emptyMap()
                            Log.d("PantryProducts", "    Item: ${itemData["name"]}, Quantity: ${itemData["quantity"]}")
                            products.add(
                                PantryProduct(
                                    productId = itemDoc.id,
                                    name = itemData["name"] as? String ?: "",
                                    description = itemData["pickupLocation"] as? String ?: "",
                                    imageUrl = itemData["imageUri"] as? String,
                                    category = itemData["category"] as? String ?: "",
                                    selected = false,
                                    quantity = (itemData["quantity"] as? Long ?: 1).toInt()
                                )
                            )
                        }
                    }
                    
                    Log.d("PantryProducts", "✅ Loaded ${products.size} items from ${donorsSnapshot.size()} donors (new structure)")
                } else {
                    // Fallback to old structure: products array in pantry document
                    Log.d("PantryProducts", "🔍 No donors found, checking for products array...")
                    @Suppress("UNCHECKED_CAST")
                    val productsList = pantryDoc.get("products") as? List<Map<String, Any>>?
                    
                    if (productsList != null && productsList.isNotEmpty()) {
                        Log.d("PantryProducts", "✅ Using old structure: products array")
                        Log.d("PantryProducts", "Found ${productsList.size} products in array")
                        
                        products.addAll(productsList.mapIndexed { index, productMap ->
                            Log.d("PantryProducts", "  Product $index: ${productMap["name"]}")
                            PantryProduct(
                                productId = index.toString(),
                                name = productMap["name"] as? String ?: "",
                                description = productMap["imageUri"] as? String ?: "",
                                imageUrl = productMap["imageUri"] as? String,
                                category = "",
                                selected = false,
                                quantity = (productMap["quantity"] as? Long ?: 1).toInt()
                            )
                        })
                        
                        Log.d("PantryProducts", "✅ Loaded ${products.size} items from products array (old structure)")
                    } else {
                        Log.w("PantryProducts", "❌ No products found in either structure")
                        Log.w("PantryProducts", "Pantry document fields: ${pantryDoc.data?.keys}")
                        Toast.makeText(this@PantryProductsActivity, "No products found in this pantry", Toast.LENGTH_LONG).show()
                    }
                }
                
                Log.d("PantryProducts", "=== FINAL RESULT: ${products.size} products ===")
                productAdapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressBar.visibility = View.GONE

            } catch (e: Exception) {
                Log.e("PantryProducts", "❌ Error loading products", e)
                e.printStackTrace()
                binding.progressBar.visibility = View.GONE
                
                // Check if it's a permission error
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e("PantryProducts", "⚠️ PERMISSION DENIED - Firestore rules need to be updated!")
                    Toast.makeText(
                        this@PantryProductsActivity, 
                        "Permission denied. Please deploy Firestore rules. Check FIX_PERMISSION_DENIED.md", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
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
                // Try new schema first: pantries/{zipCode}/posts/{pantryId}
                val zip = zipCode ?: "UNKNOWN"
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
                    loadProducts()
                    return@launch
                }

                // Fallback: Try to find pantry by name/address
                val oldSnapshot = firestore
                    .collection("pantries")
                    .whereEqualTo("name", pantryName)
                    .whereEqualTo("address", pantryAddress)
                    .limit(1)
                    .get()
                    .await()

                if (!oldSnapshot.isEmpty) {
                    val doc = oldSnapshot.documents.first()
                    pantryId = doc.id
                    // Load products (will handle both old and new structure)
                    loadProducts()
                } else {
                    Log.w("PantryProducts", "Pantry not found by name/address")
                    binding.progressBar.visibility = View.GONE
                    updateEmptyState()
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("PantryProducts", "Error finding pantry", e)
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
                    imageUrl = product.imageUrl
                )
            }

        val intent = Intent(this, CartActivity::class.java).apply {
            putParcelableArrayListExtra("cart_items", ArrayList(selectedItems))
        }
        startActivity(intent)
    }
}

