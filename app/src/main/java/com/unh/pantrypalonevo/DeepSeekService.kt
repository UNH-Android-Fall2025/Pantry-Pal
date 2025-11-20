package com.unh.pantrypalonevo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for generating recipes using DeepSeek API
 */
class DeepSeekService private constructor(context: Context) {
    
    private val apiKey: String
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // Increased to 2 minutes for recipe generation
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "DeepSeekService"
        private const val DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"
        
        @Volatile
        private var INSTANCE: DeepSeekService? = null
        
        fun getInstance(context: Context): DeepSeekService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeepSeekService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        // Get API key from resources (you'll need to add this to strings.xml)
        val resourceId = context.resources.getIdentifier(
            "deepseek_api_key",
            "string",
            context.packageName
        )
        apiKey = if (resourceId != 0) {
            val key = context.getString(resourceId)
            if (key.isEmpty() || key == "YOUR_DEEPSEEK_API_KEY_HERE") {
                Log.w(TAG, "⚠️ DeepSeek API key not configured. Add it to strings.xml")
            } else {
                Log.d(TAG, "✅ DeepSeek API key loaded")
            }
            key
        } else {
            Log.w(TAG, "⚠️ DeepSeek API key resource not found in strings.xml")
            ""
        }
    }
    
    /**
     * Generate recipes for given items using DeepSeek API
     * @param items List of item names to generate recipes for
     * @return List of Recipe objects
     */
    suspend fun generateRecipes(items: List<String>): List<Recipe> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_DEEPSEEK_API_KEY_HERE") {
            Log.e(TAG, "❌ DeepSeek API key not configured or empty")
            Log.e(TAG, "❌ API key length: ${apiKey.length}")
            return@withContext emptyList()
        }
        
        Log.d(TAG, "✅ API key loaded (length: ${apiKey.length}, starts with: ${apiKey.take(7)})")
        
        if (items.isEmpty()) {
            Log.w(TAG, "⚠️ No items provided for recipe generation")
            return@withContext emptyList()
        }
        
        try {
            val itemsText = items.joinToString(", ")
            val prompt = """Generate exactly 10 recipes using: $itemsText.

Return ONLY a JSON array. No markdown, no explanations.

Format:
[{"title":"Name","ingredients":["item1","item2"],"steps":["Step 1","Step 2"],"time":"30 mins","difficulty":"Easy"}]

Each recipe needs: title, ingredients array, steps array (3-5 steps), time, difficulty (Easy/Medium/Hard).

Return exactly 10 recipes as JSON array only."""
            
            Log.d(TAG, "🔍 Generating recipes for: $itemsText")
            Log.d(TAG, "🔑 API Key present: ${apiKey.isNotEmpty()}")
            
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 5000)  // Reduced slightly for faster response
            }
            
            val request = Request.Builder()
                .url(DEEPSEEK_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "📡 DeepSeek API response code: ${response.code}")
            
            if (response.isSuccessful && responseBody != null) {
                Log.d(TAG, "✅ DeepSeek API response received (length: ${responseBody.length})")
                Log.d(TAG, "📄 Response preview: ${responseBody.take(300)}")
                
                val recipes = parseDeepSeekResponse(responseBody)
                Log.d(TAG, "📝 Successfully parsed ${recipes.size} recipes")
                
                if (recipes.isEmpty()) {
                    Log.e(TAG, "❌ No recipes parsed! Full response: ${responseBody.take(1000)}")
                }
                
                return@withContext recipes
            } else {
                val errorPreview = responseBody?.take(500) ?: "No response body"
                Log.e(TAG, "❌ DeepSeek API error: ${response.code}")
                Log.e(TAG, "❌ Error details: $errorPreview")
                
                // Try to parse error message
                try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    if (errorJson.has("error")) {
                        val error = errorJson.getJSONObject("error")
                        val errorMsg = error.optString("message", "Unknown error")
                        Log.e(TAG, "❌ API Error message: $errorMsg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Could not parse error response")
                }
                
                return@withContext emptyList()
            }
            
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Request timeout - API took too long to respond", e)
            Log.e(TAG, "💡 Try again or check your internet connection")
            return@withContext emptyList()
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ Network error - Cannot reach DeepSeek API", e)
            Log.e(TAG, "💡 Check your internet connection")
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating recipes: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "❌ Error message: ${e.message}")
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    
    /**
     * Parse DeepSeek API response and extract recipes
     */
    private fun parseDeepSeekResponse(json: String): List<Recipe> {
        return try {
            Log.d(TAG, "🔍 Starting to parse response...")
            val jsonObject = JSONObject(json)
            
            // Check for error in response
            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                val errorMsg = error.optString("message", "Unknown error")
                Log.e(TAG, "❌ API returned error: $errorMsg")
                return emptyList()
            }
            
            val choices = jsonObject.getJSONArray("choices")
            
            if (choices.length() == 0) {
                Log.w(TAG, "⚠️ No choices in DeepSeek response")
                return emptyList()
            }
            
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            
            Log.d(TAG, "📄 Content extracted (length: ${content.length})")
            Log.d(TAG, "📄 Content preview: ${content.take(200)}")
            
            // Try to extract JSON from the content (might have markdown code blocks)
            val jsonContent = extractJsonFromContent(content)
            
            Log.d(TAG, "📄 Extracted JSON (length: ${jsonContent.length})")
            Log.d(TAG, "📄 Extracted JSON preview: ${jsonContent.take(200)}")
            
            if (jsonContent.isEmpty() || !jsonContent.startsWith("[")) {
                Log.e(TAG, "❌ Invalid JSON content extracted")
                return emptyList()
            }
            
            val recipesArray = JSONArray(jsonContent)
            Log.d(TAG, "📊 Found ${recipesArray.length()} recipes in JSON array")
            
            val recipes = mutableListOf<Recipe>()
            
            for (i in 0 until recipesArray.length()) {
                try {
                    val recipeObj = recipesArray.getJSONObject(i)
                    val recipe = Recipe(
                        title = recipeObj.optString("title", "Untitled Recipe $i"),
                        ingredients = try {
                            parseStringArray(recipeObj.getJSONArray("ingredients"))
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Could not parse ingredients for recipe $i: ${e.message}")
                            emptyList()
                        },
                        steps = try {
                            parseStringArray(recipeObj.getJSONArray("steps"))
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Could not parse steps for recipe $i: ${e.message}")
                            emptyList()
                        },
                        time = recipeObj.optString("time", "N/A"),
                        difficulty = recipeObj.optString("difficulty", "Medium")
                    )
                    recipes.add(recipe)
                    Log.d(TAG, "✅ Parsed recipe ${i + 1}: ${recipe.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing recipe at index $i: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            Log.d(TAG, "✅ Successfully parsed ${recipes.size} out of ${recipesArray.length()} recipes")
            recipes
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing DeepSeek response: ${e.message}")
            Log.e(TAG, "❌ Response JSON preview: ${json.take(500)}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Extract JSON from content (handles markdown code blocks and extra text)
     */
    private fun extractJsonFromContent(content: String): String {
        var cleaned = content.trim()
        
        // Find the JSON array start and end
        val jsonStart = cleaned.indexOf('[')
        val jsonEnd = cleaned.lastIndexOf(']')
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            // Extract JSON array
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            // Try to remove markdown code blocks
            if (cleaned.contains("```")) {
                // Find content between code blocks
                val startMarker = cleaned.indexOf("```")
                val endMarker = cleaned.lastIndexOf("```")
                
                if (startMarker >= 0 && endMarker > startMarker) {
                    cleaned = cleaned.substring(startMarker, endMarker)
                    cleaned = cleaned.substringAfter("```")
                    if (cleaned.startsWith("json")) {
                        cleaned = cleaned.substringAfter("json")
                    }
                    cleaned = cleaned.trim()
                    if (cleaned.endsWith("```")) {
                        cleaned = cleaned.substringBeforeLast("```")
                    }
                }
            }
        }
        
        // Final cleanup
        cleaned = cleaned.trim()
        
        // Ensure it's valid JSON array
        if (!cleaned.startsWith("[")) {
            val startIdx = cleaned.indexOf('[')
            if (startIdx >= 0) {
                cleaned = cleaned.substring(startIdx)
            }
        }
        
        if (!cleaned.endsWith("]")) {
            val endIdx = cleaned.lastIndexOf(']')
            if (endIdx >= 0) {
                cleaned = cleaned.substring(0, endIdx + 1)
            }
        }
        
        return cleaned.trim()
    }
    
    /**
     * Parse JSON array to List<String>
     */
    private fun parseStringArray(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}

