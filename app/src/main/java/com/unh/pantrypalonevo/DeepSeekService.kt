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
        val resourceId = context.resources.getIdentifier(
            "deepseek_api_key",
            "string",
            context.packageName
        )
        apiKey = if (resourceId != 0) {
            val key = context.getString(resourceId)
            if (key.isEmpty() || key == "YOUR_DEEPSEEK_API_KEY_HERE") {
                Log.w(TAG, "DeepSeek API key not configured")
            } else {
                Log.d(TAG, "DeepSeek API key loaded")
            }
            key
        } else {
            Log.w(TAG, "DeepSeek API key resource not found")
            ""
        }
    }
    
    suspend fun generateRecipes(items: List<String>): List<Recipe> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_DEEPSEEK_API_KEY_HERE") {
            Log.e(TAG, "DeepSeek API key not configured or empty")
            return@withContext emptyList()
        }
        
        if (items.isEmpty()) {
            Log.w(TAG, "No items provided for recipe generation")
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
            
            Log.d(TAG, "Generating recipes for: $itemsText")
            
            val requestBody = JSONObject().apply {
                put("model", "deepseek-chat")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 5000)
            }
            
            val request = Request.Builder()
                .url(DEEPSEEK_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "API response code: ${response.code}")
            
            if (response.isSuccessful && responseBody != null) {
                val recipes = parseDeepSeekResponse(responseBody)
                Log.d(TAG, "Parsed ${recipes.size} recipes")
                
                if (recipes.isEmpty()) {
                    Log.e(TAG, "No recipes parsed. Response: ${responseBody.take(1000)}")
                }
                
                return@withContext recipes
            } else {
                val errorPreview = responseBody?.take(500) ?: "No response body"
                Log.e(TAG, "API error: ${response.code} - $errorPreview")
                
                try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    if (errorJson.has("error")) {
                        val error = errorJson.getJSONObject("error")
                        val errorMsg = error.optString("message", "Unknown error")
                        Log.e(TAG, "API error message: $errorMsg")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Could not parse error response")
                }
                
                return@withContext emptyList()
            }
            
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            return@withContext emptyList()
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error - cannot reach API", e)
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recipes: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    private fun parseDeepSeekResponse(json: String): List<Recipe> {
        return try {
            val jsonObject = JSONObject(json)
            
            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                val errorMsg = error.optString("message", "Unknown error")
                Log.e(TAG, "API returned error: $errorMsg")
                return emptyList()
            }
            
            val choices = jsonObject.getJSONArray("choices")
            
            if (choices.length() == 0) {
                Log.w(TAG, "No choices in response")
                return emptyList()
            }
            
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            
            val jsonContent = extractJsonFromContent(content)
            
            if (jsonContent.isEmpty() || !jsonContent.startsWith("[")) {
                Log.e(TAG, "Invalid JSON content extracted")
                return emptyList()
            }
            
            val recipesArray = JSONArray(jsonContent)
            
            val recipes = mutableListOf<Recipe>()
            
            for (i in 0 until recipesArray.length()) {
                try {
                    val recipeObj = recipesArray.getJSONObject(i)
                    val recipe = Recipe(
                        title = recipeObj.optString("title", "Untitled Recipe $i"),
                        ingredients = try {
                            parseStringArray(recipeObj.getJSONArray("ingredients"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not parse ingredients for recipe $i: ${e.message}")
                            emptyList()
                        },
                        steps = try {
                            parseStringArray(recipeObj.getJSONArray("steps"))
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not parse steps for recipe $i: ${e.message}")
                            emptyList()
                        },
                        time = recipeObj.optString("time", "N/A"),
                        difficulty = recipeObj.optString("difficulty", "Medium")
                    )
                    recipes.add(recipe)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing recipe at index $i: ${e.message}")
                }
            }
            
            Log.d(TAG, "Parsed ${recipes.size} out of ${recipesArray.length()} recipes")
            recipes
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}")
            emptyList()
        }
    }
    
    private fun extractJsonFromContent(content: String): String {
        var cleaned = content.trim()
        
        val jsonStart = cleaned.indexOf('[')
        val jsonEnd = cleaned.lastIndexOf(']')
        
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            if (cleaned.contains("```")) {
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
        
        cleaned = cleaned.trim()
        
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
    
    private fun parseStringArray(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}

