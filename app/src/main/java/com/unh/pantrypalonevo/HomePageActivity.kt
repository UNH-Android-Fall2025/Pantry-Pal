package com.unh.pantrypalonevo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unh.pantrypalonevo.databinding.ActivityHomePageBinding
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unh.pantrypalonevo.ui.theme.PantryPaloneVoTheme
import com.unh.pantrypalonevo.Pantry

class HomePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantryPaloneVoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantryList(
                        pantries = listOf(
                            Pantry("Downtown Pantry", "Fresh food and essentials", "West Haven, CT"),
                            Pantry("Community Pantry", "Student meals and snacks", "New Haven, CT"),
                            Pantry("Food Hub", "Groceries and produce", "Orange, CT")
                        ),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PantryList(pantries: List<Pantry>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(pantries) { pantry ->
            Text(text = "${pantry.name} - ${pantry.location}")
            Text(text = pantry.description, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}
