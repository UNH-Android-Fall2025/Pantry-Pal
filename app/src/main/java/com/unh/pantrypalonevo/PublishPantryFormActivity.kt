package com.unh.pantrypalonevo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.unh.pantrypalonevo.databinding.ActivityPublishPantryFormBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PublishPantryFormActivity : AppCompatActivity() {

	private lateinit var binding: ActivityPublishPantryFormBinding
	private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityPublishPantryFormBinding.inflate(layoutInflater)
		setContentView(binding.root)

		setupToolbar()
		setupDatePickers()
		setupButtons()
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

			Toast.makeText(this, getString(R.string.toast_pantry_published), Toast.LENGTH_SHORT).show()

			// Navigate to Home with details; Home can read and show on map later
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
	}
}
