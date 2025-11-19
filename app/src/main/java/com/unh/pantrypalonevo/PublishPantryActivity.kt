package com.unh.pantrypalonevo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.unh.pantrypalonevo.databinding.ActivityPublishPantryBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * ULTRA SIMPLE VERSION - Accept ALL Food-Related Labels
 * No strict filtering!
 */
class PublishPantryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishPantryBinding
    private var capturedImageBitmap: Bitmap? = null
    private var capturedImageUri: Uri? = null
    private var detectedProducts = mutableListOf<DetectedProduct>()
    private lateinit var detectedProductAdapter: DetectedProductAdapter
    private val confirmedProducts = mutableListOf<DetectedProduct>()
    private var pendingProduct: DetectedProduct? = null
    private val pendingSelectedProducts = mutableListOf<DetectedProduct>()
    private val productImageUriMap = mutableMapOf<String, String>()

    // Gallery launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                capturedImageBitmap = bitmap
                capturedImageUri = it
                binding.ivPreview.setImageBitmap(bitmap)
                updateImagePreviewVisibility()
                clearDetectedProducts()
                detectProducts(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                Log.e("PublishPantry", "Gallery image error", e)
            }
        }
    }

    private val itemDetailsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val confirmedItem = getConfirmedProduct(data)
            val addAnother = data?.getBooleanExtra(ItemDetailsActivity.EXTRA_ADD_ANOTHER, false) ?: false

            confirmedItem?.let { product ->
                // Update existing product in place if we have a pendingProduct (to prevent duplicates)
                pendingProduct?.let { pending ->
                    // Find the existing product in confirmedProducts by the original name
                    val existingIndex = confirmedProducts.indexOfFirst { 
                        it.name.equals(pending.name, ignoreCase = true) 
                    }
                    
                    if (existingIndex >= 0) {
                        // Update existing product in place (handles name changes)
                        val existing = confirmedProducts[existingIndex]
                        confirmedProducts[existingIndex] = product.copy(
                            quantity = if (addAnother) existing.quantity + product.quantity else product.quantity
                        )
                        
                        // Update image URI map - remove old name, add new name
                        productImageUriMap.remove(pending.name)
                        capturedImageUri?.toString()?.let { uriString ->
                            productImageUriMap[product.name] = uriString
                        }
                    } else {
                        // Product not found, add it (shouldn't happen normally)
                        addConfirmedProduct(product)
                        capturedImageUri?.toString()?.let { uriString ->
                            productImageUriMap[product.name] = uriString
                        }
                    }
                    
                    // Mark the product as approved in the adapter instead of removing
                    detectedProductAdapter.markProductAsApproved(pending)
                    // Also update the list
                    val index = detectedProducts.indexOf(pending)
                    if (index != -1) {
                        detectedProducts[index] = pending.copy(approved = true)
                    }
                } ?: run {
                    // No pending product (shouldn't happen in normal flow, but handle gracefully)
                    addConfirmedProduct(product)
                    capturedImageUri?.toString()?.let { uriString ->
                        productImageUriMap[product.name] = uriString
                    }
                }
                
                // Remove processed item from pendingSelectedProducts queue
                // Match by original pendingProduct name since product name might have changed
                pendingProduct?.let { pending ->
                    val indexToRemove = pendingSelectedProducts.indexOfFirst { 
                        it.name.equals(pending.name, ignoreCase = true) 
                    }
                    if (indexToRemove >= 0) {
                        pendingSelectedProducts.removeAt(indexToRemove)
                    }
                }
                
                // Show confirmation message
                if (addAnother) {
                    Toast.makeText(
                        this, 
                        getString(R.string.toast_item_added_background, product.name, confirmedProducts.size), 
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, getString(R.string.toast_item_added, product.name), Toast.LENGTH_SHORT).show()
                }
                
                updateRecyclerVisibility()
            }

            if (!addAnother) {
                // Check if there are more items in the pendingSelectedProducts queue
                if (pendingSelectedProducts.isNotEmpty()) {
                    // Process next item in queue
                    navigateToItemDetails(pendingSelectedProducts[0])
                } else {
                    // All selected items processed, navigate to ReviewSavedItemsActivity
                    navigateToReviewSavedItems()
                }
            }
            
            // Clear pendingProduct after handling (whether addAnother is true or false)
            pendingProduct = null
        } else {
            pendingProduct = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishPantryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        clearDetectedProducts()
        setupClickListeners()
        setupBackButton()
        updateImagePreviewVisibility()
        setupSquareImageContainer()
    }
    
    private fun setupSquareImageContainer() {
        binding.imageContainer.post {
            val width = binding.imageContainer.width
            if (width > 0) {
                binding.imageContainer.layoutParams.height = width
                binding.imageContainer.requestLayout()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            Toast.makeText(this, "Camera feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnFromGallery.setOnClickListener {
            openGalleryPicker()
        }

        binding.btnProceedManually.setOnClickListener {
            showProceedManuallyDialog()
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateImagePreviewVisibility() {
        if (capturedImageBitmap != null) {
            binding.ivPreview.visibility = View.VISIBLE
            binding.placeholderContent.visibility = View.GONE
        } else {
            binding.ivPreview.visibility = View.GONE
            binding.placeholderContent.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        detectedProductAdapter = DetectedProductAdapter(
            onProductConfirm = { product ->
                navigateToItemDetails(product)
            },
            onProductReject = { product ->
                // Just collapse the item, no action needed
                // The adapter will handle hiding the buttons
            },
            onSelectionChanged = { selectedCount ->
                updateContinueButton(selectedCount)
            }
        )

        binding.recyclerDetectedProducts.apply {
            layoutManager = LinearLayoutManager(this@PublishPantryActivity)
            adapter = detectedProductAdapter
            setHasFixedSize(true)
        }
        
        // Setup Continue button
        binding.btnContinue.setOnClickListener {
            handleContinueWithSelectedItems()
        }
    }
    
    private fun updateContinueButton(selectedCount: Int) {
        binding.btnContinue.isEnabled = selectedCount > 0
        binding.btnContinue.alpha = if (selectedCount > 0) 1f else 0.5f
    }
    
    private fun handleContinueWithSelectedItems() {
        val selectedProducts = detectedProductAdapter.getSelectedProducts()
        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Store all selected products to process sequentially
        pendingSelectedProducts.clear()
        pendingSelectedProducts.addAll(selectedProducts)
        
        // Process first selected item
        if (pendingSelectedProducts.isNotEmpty()) {
            navigateToItemDetails(pendingSelectedProducts[0])
        }
    }

    private fun clearDetectedProducts() {
        detectedProducts.clear()
        if (::detectedProductAdapter.isInitialized) {
            detectedProductAdapter.submitList(emptyList())
        }
        binding.btnProceedManually.visibility = View.GONE
        updateRecyclerVisibility()
    }

    private fun updateRecyclerVisibility() {
        val hasProducts = detectedProducts.isNotEmpty()
        binding.recyclerDetectedProducts.visibility =
            if (hasProducts) View.VISIBLE else View.GONE
        binding.btnContinue.visibility =
            if (hasProducts) View.VISIBLE else View.GONE
    }

    private fun navigateToItemDetails(product: DetectedProduct) {
        pendingProduct = product
        try {
            val intent = Intent(this, ItemDetailsActivity::class.java).apply {
                putExtra(ItemDetailsActivity.EXTRA_PRODUCT_NAME, product.name)
                putExtra(ItemDetailsActivity.EXTRA_PRODUCT_CONFIDENCE, product.confidence)
                putExtra(ItemDetailsActivity.EXTRA_PRODUCT_QUANTITY, product.quantity)
                // Prefer URI to avoid large binder transactions
                capturedImageUri?.let { uri ->
                    putExtra(ItemDetailsActivity.EXTRA_PRODUCT_IMAGE_URI, uri.toString())
                } ?: run {
                    // Fallback to a small compressed byte array if URI isn't available
                    capturedImageBitmap?.let { bitmap ->
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                        putExtra(ItemDetailsActivity.EXTRA_PRODUCT_IMAGE, stream.toByteArray())
                    }
                }
            }
            // Ensure we pass temporary read permission for the URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            itemDetailsLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("PublishPantry", "Error navigating to ItemDetailsActivity", e)
            Toast.makeText(this, "Error opening item details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addConfirmedProduct(product: DetectedProduct) {
        val existingIndex = confirmedProducts.indexOfFirst {
            it.name.equals(product.name, ignoreCase = true)
        }

        if (existingIndex >= 0) {
            val existing = confirmedProducts[existingIndex]
            confirmedProducts[existingIndex] = existing.copy(
                quantity = existing.quantity + product.quantity
            )
        } else {
            confirmedProducts.add(product)
        }
    }

    private fun navigateToReviewSavedItems() {
        if (confirmedProducts.isEmpty()) {
            Toast.makeText(this, "No items to review", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ReviewSavedItemsActivity::class.java).apply {
            putParcelableArrayListExtra(
                ReviewSavedItemsActivity.EXTRA_SAVED_ITEMS,
                ArrayList(confirmedProducts)
            )
            // Pass image URI map as bundle
            val bundle = Bundle()
            productImageUriMap.forEach { (key, value) ->
                bundle.putString(key, value)
            }
            putExtra(ReviewSavedItemsActivity.EXTRA_IMAGE_URI_MAP, bundle)
        }
        startActivity(intent)
    }

    private fun getConfirmedProduct(data: Intent?): DetectedProduct? {
        return data?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra(
                    ItemDetailsActivity.EXTRA_CONFIRMED_ITEM,
                    DetectedProduct::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra(ItemDetailsActivity.EXTRA_CONFIRMED_ITEM)
            }
        }
    }

    private fun openGalleryPicker() {
        pickImageLauncher.launch("image/*")
    }

    /**
     * ULTRA PERMISSIVE DETECTION
     * Accept anything food-related from ML Kit
     */
    private fun detectProducts(bitmap: Bitmap) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvDetectionStatus.text = "🔍 Detecting products..."

        lifecycleScope.launch {
            try {
                // Step 1: Get labels from ML Kit
                val image = InputImage.fromBitmap(bitmap, 0)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                val labels = labeler.process(image).await()

                Log.d("Detection", "===== ML KIT FOUND ${labels.size} LABELS =====")
                labels.forEach { label ->
                    Log.d("Detection", "${label.text}: ${(label.confidence * 100).toInt()}%")
                }
                Log.d("Detection", "==========================================")

                // Step 2: Accept ANY label with 60%+ confidence
                val allProducts = mutableListOf<DetectedProduct>()

                labels.forEach { label ->
                    if (label.confidence >= 0.6f) {
                        val labelText = label.text

                        // ✅ ACCEPT ANYTHING FOOD-RELATED (no strict filter!)
                        if (isFoodRelated(labelText)) {
                            allProducts.add(
                                DetectedProduct(
                                    name = labelText,
                                    confidence = label.confidence,
                                    quantity = 1,
                                    approved = false
                                )
                            )
                            Log.d("Detection", "✅ Added: $labelText")
                        } else {
                            Log.d("Detection", "⚠️ Skipped: $labelText")
                        }
                    }
                }

                // Step 3: Display results
                if (allProducts.isNotEmpty()) {
                    displayResults(allProducts, "ML Kit")
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.tvDetectionStatus.text = "❌ No food items detected. Try another photo."
                    binding.btnProceedManually.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvDetectionStatus.text = "❌ Detection failed."
                binding.btnProceedManually.visibility = View.VISIBLE
                Log.e("Detection", "Error", e)
            }
        }
    }

    /**
     * ULTRA PERMISSIVE food filter
     * Accept almost anything food-related!
     */
    private fun isFoodRelated(label: String): Boolean {
        val lower = label.lowercase()

        // ✅ Accept these keywords
        val foodKeywords = listOf(
            "food", "vegetable", "fruit", "plant", "produce", "ingredient",
            "dairy", "milk", "cheese", "yogurt", "butter", "cream",
            "onion", "tomato", "potato", "carrot", "pepper", "cucumber",
            "apple", "banana", "orange", "berry", "melon", "grape",
            "meat", "chicken", "beef", "pork", "fish", "seafood",
            "bread", "grain", "rice", "pasta", "cereal",
            "natural foods", "whole food", "fresh", "organic",
            "bottle", "package", "container", "jar", "carton" // For packaged items
        )

        // ❌ Reject these keywords
        val rejectKeywords = listOf(
            "person", "human", "face", "hand", "finger",
            "table", "wood", "furniture", "room", "wall",
            "sky", "cloud", "tree", "grass", "flower" // outdoor stuff
        )

        // Check if it matches food keywords
        val matchesFood = foodKeywords.any { lower.contains(it) }

        // Check if it matches reject keywords
        val matchesReject = rejectKeywords.any { lower.contains(it) }

        return matchesFood && !matchesReject
    }

    private fun displayResults(products: List<DetectedProduct>, method: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnProceedManually.visibility = View.GONE // Hide button when items are detected

        // Remove duplicates
        val productMap = mutableMapOf<String, DetectedProduct>()
        products.forEach { product ->
            if (productMap.containsKey(product.name)) {
                val existing = productMap[product.name]!!
                productMap[product.name] = existing.copy(quantity = existing.quantity + 1)
            } else {
                productMap[product.name] = product
            }
        }

        val uniqueProducts = productMap.values.map { it.copy(approved = false) }
        detectedProducts = uniqueProducts.toMutableList()
        detectedProductAdapter.submitList(detectedProducts)
        updateRecyclerVisibility()

        val displayText = uniqueProducts.joinToString("\n") { product ->
            val category = ProductDatabase.getCategoryForProduct(product.name)
            val confidence = "(${(product.confidence * 100).toInt()}%)"
            "✓ ${product.name} $confidence - Qty: ${product.quantity} - $category"
        }

        binding.tvDetectionStatus.text =
            "🎯 Detected ${uniqueProducts.size} product(s) [$method]\nTap the checkmark beside each item to confirm.\n\n$displayText"

        Log.d("ProductDetection", "Found ${uniqueProducts.size} products via $method")
    }

    private fun showProceedManuallyDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_item_not_detected_title))
            .setMessage(getString(R.string.dialog_item_not_detected_message))
            .setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                proceedManually()
            }
            .setNegativeButton(getString(android.R.string.no), null)
            .show()
    }

    private fun proceedManually() {
        // Navigate to ItemDetailsActivity with empty/default values
        val intent = Intent(this, ItemDetailsActivity::class.java).apply {
            putExtra(ItemDetailsActivity.EXTRA_PRODUCT_NAME, "")
            putExtra(ItemDetailsActivity.EXTRA_PRODUCT_CONFIDENCE, 0f)
            putExtra(ItemDetailsActivity.EXTRA_PRODUCT_QUANTITY, 1)
            
            // Pass the image if available
            capturedImageUri?.let { uri ->
                putExtra(ItemDetailsActivity.EXTRA_PRODUCT_IMAGE_URI, uri.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } ?: run {
                capturedImageBitmap?.let { bitmap ->
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                    putExtra(ItemDetailsActivity.EXTRA_PRODUCT_IMAGE, stream.toByteArray())
                }
            }
        }
        itemDetailsLauncher.launch(intent)
    }
}
