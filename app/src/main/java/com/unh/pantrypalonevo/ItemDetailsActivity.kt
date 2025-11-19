package com.unh.pantrypalonevo

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.unh.pantrypalonevo.databinding.ActivityItemDetailsBinding

class ItemDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailsBinding

    private var confidence: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        initializeFromIntent()
        setupClearButton()
        setupActionButtons()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initializeFromIntent() {
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME).orEmpty()
        confidence = intent.getFloatExtra(EXTRA_PRODUCT_CONFIDENCE, 0f)

        binding.etProductName.setText(productName)
        
        // Show confidence chip - "N/A" for manual entries (confidence = 0), otherwise show percentage
        binding.tvConfidenceValue.text = if (confidence > 0f) {
            "Confidence ${(confidence * 100).toInt()}%"
        } else {
            "Confidence N/A"
        }

        // Load and display image
        loadProductImage()
    }

    private fun loadProductImage() {
        var imageLoaded = false
        
        // Prefer loading from URI to avoid binder limits
        val imageUriString = intent.getStringExtra(EXTRA_PRODUCT_IMAGE_URI)
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri: Uri = imageUriString.toUri()
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    binding.ivProductThumbnail.setImageBitmap(bitmap)
                    binding.ivProductThumbnail.visibility = View.VISIBLE
                    binding.placeholderContent.visibility = View.GONE
                    imageLoaded = true
                }
            } catch (_: Exception) { /* fall back to byte[] */ }
        }
        
        if (!imageLoaded) {
            intent.getByteArrayExtra(EXTRA_PRODUCT_IMAGE)?.let { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivProductThumbnail.setImageBitmap(bitmap)
                binding.ivProductThumbnail.visibility = View.VISIBLE
                binding.placeholderContent.visibility = View.GONE
                imageLoaded = true
            }
        }
        
        if (!imageLoaded) {
            // No image available, show placeholder
            binding.ivProductThumbnail.visibility = View.GONE
            binding.placeholderContent.visibility = View.VISIBLE
        }
    }

    private fun setupClearButton() {
        // Show/hide clear button based on text content
        binding.etProductName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClearName.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        })
        
        binding.btnClearName.setOnClickListener {
            binding.etProductName.setText("")
            binding.etProductName.requestFocus()
        }
    }

    private fun setupActionButtons() {
        binding.btnAddAnother.setOnClickListener {
            buildDetectedProduct()?.let { product ->
                sendResult(product, addAnother = true)
            }
        }

        binding.btnSaveFinish.setOnClickListener {
            buildDetectedProduct()?.let { product ->
                sendResult(product, addAnother = false)
            }
        }
    }

    private fun buildDetectedProduct(): DetectedProduct? {
        val name = binding.etProductName.text?.toString()?.trim().orEmpty()

        return if (name.isNotEmpty()) {
            // Quantity is always 1 for this screen (removed from UI)
            DetectedProduct(
                name = name,
                confidence = confidence,
                quantity = 1,
                approved = true
            )
        } else {
            binding.etProductName.error = getString(R.string.error_product_name_required)
            null
        }
    }

    private fun sendResult(product: DetectedProduct, addAnother: Boolean) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_CONFIRMED_ITEM, product)
            putExtra(EXTRA_ADD_ANOTHER, addAnother)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_PRODUCT_NAME = "extra_product_name"
        const val EXTRA_PRODUCT_CONFIDENCE = "extra_product_confidence"
        const val EXTRA_PRODUCT_QUANTITY = "extra_product_quantity"
        const val EXTRA_PRODUCT_IMAGE = "extra_product_image"
        const val EXTRA_PRODUCT_IMAGE_URI = "extra_product_image_uri"

        const val EXTRA_CONFIRMED_ITEM = "extra_confirmed_item"
        const val EXTRA_ADD_ANOTHER = "extra_add_another"
    }
}
