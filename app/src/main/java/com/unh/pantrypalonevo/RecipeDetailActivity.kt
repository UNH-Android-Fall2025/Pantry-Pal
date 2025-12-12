package com.unh.pantrypalonevo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.pantrypalonevo.databinding.ActivityRecipeDetailBinding
import java.io.File
import java.io.FileWriter
import java.util.UUID

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        supportActionBar?.title = "Recipe Details"

        // Get recipe data from intent
        val title = intent.getStringExtra("recipe_title") ?: "Recipe"
        val ingredients = intent.getStringArrayExtra("recipe_ingredients")?.toList() ?: emptyList()
        val steps = intent.getStringArrayExtra("recipe_steps")?.toList() ?: emptyList()
        val time = intent.getStringExtra("recipe_time") ?: "N/A"
        val difficulty = intent.getStringExtra("recipe_difficulty") ?: "N/A"
        
        // Check if recipe ID already exists (from shared link)
        recipeId = intent.getStringExtra("recipe_id")

        // Set recipe details
        binding.tvRecipeTitle.text = title
        binding.tvTime.text = "⏱ $time"
        binding.tvDifficulty.text = "📊 $difficulty"

        // Setup ingredients RecyclerView
        val ingredientsAdapter = RecipeIngredientAdapter(ingredients)
        binding.recyclerIngredients.layoutManager = LinearLayoutManager(this)
        binding.recyclerIngredients.adapter = ingredientsAdapter

        // Setup steps RecyclerView
        val stepsAdapter = RecipeStepAdapter(steps)
        binding.recyclerSteps.layoutManager = LinearLayoutManager(this)
        binding.recyclerSteps.adapter = stepsAdapter

        // Setup share button
        binding.btnShareRecipe.setOnClickListener {
            shareRecipe(title, ingredients, steps, time, difficulty)
        }
    }
    
    private fun shareRecipe(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        time: String,
        difficulty: String
    ) {
        // Show loading
        binding.btnShareRecipe.isEnabled = false
        binding.btnShareRecipe.text = "Sharing..."

        try {
            // Create formatted recipe text
            val recipeText = buildRecipeText(title, ingredients, steps, time, difficulty)
            
            // Create text file
            val recipeFile = createRecipeTextFile(title, recipeText)
            
            if (recipeFile != null) {
                // Share as text file
                shareRecipeAsFile(recipeFile, title, recipeText)
            } else {
                // Fallback: Share as plain text
                shareRecipeAsText(title, recipeText)
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error sharing recipe: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            
            // Reset button
            binding.btnShareRecipe.isEnabled = true
            binding.btnShareRecipe.text = "📤 Share Recipe"
        }
    }
    
    private fun buildRecipeText(
        title: String,
        ingredients: List<String>,
        steps: List<String>,
        time: String,
        difficulty: String
    ): String {
        val sb = StringBuilder()
        sb.append("=".repeat(50)).append("\n")
        sb.append("  $title\n")
        sb.append("=".repeat(50)).append("\n\n")
        sb.append("⏱ Cooking Time: $time\n")
        sb.append("📊 Difficulty: $difficulty\n\n")
        sb.append("-".repeat(50)).append("\n")
        sb.append("INGREDIENTS:\n")
        sb.append("-".repeat(50)).append("\n")
        ingredients.forEachIndexed { index, ingredient ->
            sb.append("${index + 1}. $ingredient\n")
        }
        sb.append("\n").append("-".repeat(50)).append("\n")
        sb.append("INSTRUCTIONS:\n")
        sb.append("-".repeat(50)).append("\n")
        steps.forEachIndexed { index, step ->
            sb.append("${index + 1}. $step\n\n")
        }
        sb.append("=".repeat(50)).append("\n")
        sb.append("Shared from Pantry Pal App\n")
        sb.append("=".repeat(50))
        return sb.toString()
    }
    
    private fun createRecipeTextFile(title: String, recipeText: String): File? {
        return try {
            // Create a file in the app's cache directory
            val cacheDir = File(cacheDir, "recipes")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // Create a safe filename from the recipe title
            val safeFileName = title
                .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "_")
                .take(50)
            
            val file = File(cacheDir, "${safeFileName}_recipe.txt")
            
            // Write recipe text to file
            FileWriter(file).use { writer ->
                writer.write(recipeText)
            }
            
            file
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetail", "Error creating recipe file: ${e.message}", e)
            null
        }
    }
    
    private fun shareRecipeAsFile(recipeFile: File, title: String, recipeText: String) {
        try {
            // Get URI using FileProvider
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                recipeFile
            )
            
            // Create share intent for file
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Recipe: $title")
                putExtra(Intent.EXTRA_TEXT, "Check out this recipe: $title\n\nSee attached file for full recipe details.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Also copy recipe text to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Recipe", recipeText)
            clipboard.setPrimaryClip(clip)
            
            // Start share chooser
            startActivity(Intent.createChooser(shareIntent, "Share Recipe as Text File"))
            
            Toast.makeText(
                this,
                "Recipe text copied to clipboard! Choose an app to share the file.",
                Toast.LENGTH_LONG
            ).show()
            
            // Reset button
            binding.btnShareRecipe.isEnabled = true
            binding.btnShareRecipe.text = "📤 Share Recipe"
        } catch (e: Exception) {
            android.util.Log.e("RecipeDetail", "Error sharing file: ${e.message}", e)
            // Fallback to text sharing
            shareRecipeAsText(title, recipeText)
        }
    }
    
    private fun shareRecipeAsText(title: String, recipeText: String) {
        // Copy to clipboard
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Recipe: $title", recipeText)
        clipboard.setPrimaryClip(clip)
        
        // Create share intent for plain text
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Recipe: $title")
            putExtra(Intent.EXTRA_TEXT, recipeText)
        }
        
        // Start share chooser
        startActivity(Intent.createChooser(shareIntent, "Share Recipe"))
        
        Toast.makeText(
            this,
            "Recipe copied to clipboard! Share it with anyone.",
            Toast.LENGTH_LONG
        ).show()
        
        // Reset button
        binding.btnShareRecipe.isEnabled = true
        binding.btnShareRecipe.text = "📤 Share Recipe"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

