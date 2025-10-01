package com.unh.pantrypalonevo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var pantryAdapter: PantryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sample test data for pantry list
        val pantryList = listOf(
            Pantry("Pantry name", "Short Description", "West Haven, CT"),
            Pantry("Pantry name", "Short Description", "West Haven, CT"),
            Pantry("Pantry name", "Short Description", "West Haven, CT")
        )

        pantryAdapter = PantryAdapter(pantryList)
        binding.rvPantryList.layoutManager = LinearLayoutManager(this)
        binding.rvPantryList.adapter = pantryAdapter
    }
}
