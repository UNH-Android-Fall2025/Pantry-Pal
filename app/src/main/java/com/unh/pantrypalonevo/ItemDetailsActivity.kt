package com.unh.pantrypalonevo

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.unh.pantrypalonevo.databinding.ActivityItemDetailsBinding

class ItemDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailsBinding

    private var confidence: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeFromIntent()
        setupQuantityControls()
        setupActionButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initializeFromIntent() {
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME).orEmpty()
        confidence = intent.getFloatExtra(EXTRA_PRODUCT_CONFIDENCE, 0f)
        val quantity = intent.getIntExtra(EXTRA_PRODUCT_QUANTITY, 1)

        binding.etProductName.setText(productName)
        // Show "N/A" for manual entries (confidence = 0), otherwise show percentage
        binding.tvConfidenceValue.text = if (confidence > 0f) {
            "${(confidence * 100).toInt()}%"
        } else {
            "N/A"
        }
        binding.tvQuantityValue.text = quantity.coerceAtLeast(1).toString()

        // Prefer loading from URI to avoid binder limits
        val imageUriString = intent.getStringExtra(EXTRA_PRODUCT_IMAGE_URI)
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val uri: Uri = imageUriString.toUri()
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    binding.ivProductThumbnail.setImageBitmap(bitmap)
                }
            } catch (_: Exception) { /* fall back to byte[] */ }
        } else {
            intent.getByteArrayExtra(EXTRA_PRODUCT_IMAGE)?.let { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivProductThumbnail.setImageBitmap(bitmap)
            }
        }
    }

    private fun setupQuantityControls() {
        binding.btnQuantityMinus.setOnClickListener { adjustQuantity(-1) }
        binding.btnQuantityPlus.setOnClickListener { adjustQuantity(1) }
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

    private fun adjustQuantity(delta: Int) {
        val current = binding.tvQuantityValue.text.toString().toIntOrNull() ?: 1
        val updated = (current + delta).coerceAtLeast(1)
        binding.tvQuantityValue.text = updated.toString()
    }

    private fun buildDetectedProduct(): DetectedProduct? {
        val name = binding.etProductName.text?.toString()?.trim().orEmpty()

        return if (name.isNotEmpty()) {
            binding.etProductName.error = null
            val quantity = binding.tvQuantityValue.text.toString().toIntOrNull() ?: 1
            DetectedProduct(
                name = name,
                confidence = confidence,
                quantity = quantity.coerceAtLeast(1),
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

