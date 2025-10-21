package com.unh.pantrypalonevo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.unh.pantrypalonevo.databinding.ActivityPublishPantryBinding

class PublishPantryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishPantryBinding
    private var capturedImageBitmap: Bitmap? = null

    private val CAMERA_PERMISSION_CODE = 100

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedImageBitmap = bitmap
            binding.ivPreview.setImageBitmap(bitmap)
            detectProducts(bitmap)
        }
    }

    // Gallery launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                capturedImageBitmap = bitmap
                binding.ivPreview.setImageBitmap(bitmap)
                detectProducts(bitmap)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishPantryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            if (checkCameraPermission()) {
                takePictureLauncher.launch(null)
            } else {
                requestCameraPermission()
            }
        }

        binding.btnFromGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    // ML Kit Image Labeling detection
    private fun detectProducts(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        binding.progressBar.visibility = View.VISIBLE
        binding.tvDetectionStatus.text = "Scanning products..."

        labeler.process(image)
            .addOnSuccessListener { labels ->
                binding.progressBar.visibility = View.GONE
                if (labels.isNotEmpty()) {
                    val groceryKeywords = listOf(
                        "food", "fruit", "vegetable", "drink", "beverage", "dairy",
                        "meat", "bread", "snack", "candy", "juice", "milk", "cheese",
                        "yogurt", "cereal", "pasta", "rice", "sauce", "oil", "spice",
                        "can", "bottle", "package", "carton", "apple", "banana", "carrot",
                        "tomato", "potato", "onion", "chicken", "beef", "fish", "egg"
                    )

                    val groceryProducts = labels.filter { label ->
                        groceryKeywords.any { keyword ->
                            label.text.lowercase().contains(keyword)
                        }
                    }.map { label ->
                        "${label.text} (${(label.confidence * 100).toInt()}%)"
                    }

                    if (groceryProducts.isNotEmpty()) {
                        binding.tvDetectionStatus.text = "Detected: ${groceryProducts.joinToString(", ")}"
                    } else {
                        binding.tvDetectionStatus.text = "No grocery items detected."
                    }
                } else {
                    binding.tvDetectionStatus.text = "No products detected."
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvDetectionStatus.text = "Detection failed: ${e.message}"
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
