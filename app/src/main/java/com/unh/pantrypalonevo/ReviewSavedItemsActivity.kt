package com.unh.pantrypalonevo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityReviewSavedItemsBinding

/**
 * REVIEW ALL PRODUCTS SCREEN
 * Shows all products added in background list
 * User can edit quantities and publish to Firebase
 */
class ReviewSavedItemsActivity : AppCompatActivity() {

	private lateinit var binding: ActivityReviewSavedItemsBinding
	private val savedItems = mutableListOf<DetectedProduct>()
	private val imageUriMap = mutableMapOf<String, String>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityReviewSavedItemsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		setupBackButton()
		loadSavedItems()
		loadImageUris()
		setupRecyclerView()
		setupButtons()
		updateStats()
	}

	private fun setupBackButton() {
		binding.btnBack.setOnClickListener {
			finish()
		}
	}

	private fun loadSavedItems() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableArrayListExtra(EXTRA_SAVED_ITEMS, DetectedProduct::class.java)?.let {
				savedItems.clear(); savedItems.addAll(it)
			}
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableArrayListExtra<DetectedProduct>(EXTRA_SAVED_ITEMS)?.let {
				savedItems.clear(); savedItems.addAll(it)
			}
		}
	}

	private fun loadImageUris() {
		// Load image URI map from intent
		intent.getStringArrayListExtra(EXTRA_IMAGE_URIS)?.forEachIndexed { index, uri ->
			if (index < savedItems.size) {
				imageUriMap[savedItems[index].name] = uri
			}
		}
		// Also try loading from bundle if available
		intent.getBundleExtra(EXTRA_IMAGE_URI_MAP)?.let { bundle ->
			bundle.keySet().forEach { key ->
				bundle.getString(key)?.let { uri ->
					imageUriMap[key] = uri
				}
			}
		}
	}

	private fun setupRecyclerView() {
		if (savedItems.isEmpty()) {
			binding.recyclerSavedItems.visibility = View.GONE
			binding.tvEmptyMessage.visibility = View.VISIBLE
			binding.tvEmptyMessage.text = getString(R.string.message_no_items_saved)
			return
		}

		binding.recyclerSavedItems.visibility = View.VISIBLE
		binding.tvEmptyMessage.visibility = View.GONE

		val adapter = SavedItemsAdapter(savedItems, imageUriMap) { updatedItems ->
			savedItems.clear()
			savedItems.addAll(updatedItems)
			updateStats()
			toggleEmptyState()
		}

		binding.recyclerSavedItems.apply {
			layoutManager = LinearLayoutManager(this@ReviewSavedItemsActivity)
			this.adapter = adapter
			setHasFixedSize(true)
		}
	}

	private fun setupButtons() {
		binding.btnAddMore.setOnClickListener {
			// Return to previous screen to add more items; data remains in memory in previous activity
			finish()
		}

		binding.btnPublishPantry.setOnClickListener {
			val intent = Intent(this, PublishPantryFormActivity::class.java).apply {
				putParcelableArrayListExtra(PublishPantryFormActivity.EXTRA_PANTRY_ITEMS, ArrayList(savedItems))
				// Pass image URI map as bundle
				val bundle = Bundle()
				imageUriMap.forEach { (key, value) ->
					bundle.putString(key, value)
				}
				putExtra(PublishPantryFormActivity.EXTRA_IMAGE_URI_MAP, bundle)
			}
			startActivity(intent)
		}

		binding.btnDisassemblePantry.setOnClickListener { disassemblePantry() }
	}

	private fun updateStats() {
		val itemCount = savedItems.size
		val totalQuantity = savedItems.sumOf { it.quantity }
		
		binding.tvItemCount.text = itemCount.toString()
		binding.tvTotalQuantity.text = totalQuantity.toString()
	}

	private fun toggleEmptyState() {
		if (savedItems.isEmpty()) {
			binding.recyclerSavedItems.visibility = View.GONE
			binding.tvEmptyMessage.visibility = View.VISIBLE
		} else {
			binding.recyclerSavedItems.visibility = View.VISIBLE
			binding.tvEmptyMessage.visibility = View.GONE
		}
		updateStats()
	}

	private fun disassemblePantry() {
		if (savedItems.isEmpty()) {
			Toast.makeText(this, getString(R.string.message_no_items_to_remove), Toast.LENGTH_SHORT).show()
			return
		}
		savedItems.clear()
		setupRecyclerView()
		updateStats()
		Toast.makeText(this, getString(R.string.toast_items_removed), Toast.LENGTH_SHORT).show()
		finish()
	}

	companion object { 
		const val EXTRA_SAVED_ITEMS = "extra_saved_items"
		const val EXTRA_IMAGE_URIS = "extra_image_uris"
		const val EXTRA_IMAGE_URI_MAP = "extra_image_uri_map"
	}
}
