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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityReviewSavedItemsBinding.inflate(layoutInflater)
		setContentView(binding.root)

		setupToolbar()
		loadSavedItems()
		setupRecyclerView()
		setupButtons()
		updateItemCount()
	}

	private fun setupToolbar() {
		setSupportActionBar(binding.toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.title = getString(R.string.title_review_saved_items)
		binding.toolbar.setNavigationOnClickListener { finish() }
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

	private fun setupRecyclerView() {
		if (savedItems.isEmpty()) {
			binding.recyclerSavedItems.visibility = View.GONE
			binding.tvEmptyMessage.visibility = View.VISIBLE
			binding.tvEmptyMessage.text = getString(R.string.message_no_items_saved)
			return
		}

		binding.recyclerSavedItems.visibility = View.VISIBLE
		binding.tvEmptyMessage.visibility = View.GONE

		val adapter = SavedItemsAdapter(savedItems) { updatedItems ->
			savedItems.clear(); savedItems.addAll(updatedItems)
			updateItemCount()
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
			startActivity(Intent(this, PublishPantryFormActivity::class.java))
		}

		binding.btnDisassemblePantry.setOnClickListener { disassemblePantry() }
	}

	private fun updateItemCount() {
		val totalItems = savedItems.sumOf { it.quantity }
		binding.tvItemCount.text = "${savedItems.size} item(s) • Total quantity: $totalItems"
	}

	private fun toggleEmptyState() {
		if (savedItems.isEmpty()) {
			binding.recyclerSavedItems.visibility = View.GONE
			binding.tvEmptyMessage.visibility = View.VISIBLE
		} else {
			binding.recyclerSavedItems.visibility = View.VISIBLE
			binding.tvEmptyMessage.visibility = View.GONE
		}
	}

	private fun disassemblePantry() {
		if (savedItems.isEmpty()) {
			Toast.makeText(this, getString(R.string.message_no_items_to_remove), Toast.LENGTH_SHORT).show()
			return
		}
		savedItems.clear()
		setupRecyclerView()
		updateItemCount()
		Toast.makeText(this, getString(R.string.toast_items_removed), Toast.LENGTH_SHORT).show()
		finish()
	}

	companion object { const val EXTRA_SAVED_ITEMS = "extra_saved_items" }
}

