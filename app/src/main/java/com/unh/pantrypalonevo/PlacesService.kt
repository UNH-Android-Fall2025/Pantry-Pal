package com.unh.pantrypalonevo

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.unh.pantrypalonevo.model.Pantry
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray

/**
 * Service class for fetching nearby pantry shops using Google Places API
 */
class PlacesService private constructor(context: Context) {
    
    private val placesClient: PlacesClient
    private val apiKey: String
    
    companion object {
        private const val TAG = "PlacesService"
        private const val SEARCH_RADIUS_METERS = 10000 // 10 km radius (increased)
        private const val MAX_RESULTS = 50 // Increased results
        
        // Keywords to search for pantry-related places
        private val PANTRY_KEYWORDS = listOf(
            "food pantry",
            "food bank",
            "community pantry",
            "free food",
            "food assistance",
            "pantry",
            "food distribution"
        )
        
        @Volatile
        private var INSTANCE: PlacesService? = null
        
        fun getInstance(context: Context): PlacesService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlacesService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        // Get API key from resources
        val resourceId = context.resources.getIdentifier(
            "google_places_api_key",
            "string",
            context.packageName
        )
        apiKey = if (resourceId != 0) {
            val key = context.getString(resourceId)
            // Log API key status (first 10 chars only for security)
            if (key.isEmpty() || key == "YOUR_API_KEY_HERE") {
                Log.e(TAG, "API KEY ISSUE: API key is empty or placeholder")
                Log.e(TAG, "Please add your Google Places API key to strings.xml")
            } else {
                Log.d(TAG, "API Key loaded: ${key.take(10)}... (length: ${key.length})")
            }
            key
        } else {
            Log.e(TAG, "API KEY NOT FOUND: google_places_api_key not found in strings.xml")
            ""
        }
        
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            if (apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE") {
                Places.initialize(context, apiKey)
                Log.d(TAG, "Places SDK initialized successfully")
            } else {
                Log.e(TAG, "Places SDK NOT initialized - invalid API key")
            }
        }
        placesClient = Places.createClient(context)
    }
    
    /**
     * Search for nearby pantry shops using Google Places API
     * @param location User's current location
     * @return List of Pantry objects found nearby
     */
    suspend fun searchNearbyPantries(location: Location): List<Pantry> {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e(TAG, "Invalid API key. Please add your Google Places API key to strings.xml")
            return emptyList()
        }
        
        val allResults = mutableListOf<Pantry>()
        
        try {
            // Search for each keyword
            for (keyword in PANTRY_KEYWORDS) {
                val results = searchPlacesByKeyword(location, keyword)
                allResults.addAll(results)
            }
            
            // Remove duplicates based on place ID or name+address
            val uniqueResults = allResults.distinctBy { "${it.name}_${it.address}" }
            
            Log.d(TAG, "Found ${uniqueResults.size} unique pantry places")
            return uniqueResults.take(MAX_RESULTS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for nearby pantries", e)
            return emptyList()
        }
    }
    
    /**
     * Search places using a specific keyword
     */
    private suspend fun searchPlacesByKeyword(location: Location, keyword: String): List<Pantry> {
        return try {
            // Use FindCurrentPlaceRequest for nearby search
            // Note: This requires location permission
            val placeFields = listOf(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.RATING
            )
            
            val request = FindCurrentPlaceRequest.newInstance(placeFields)
            val response = placesClient.findCurrentPlace(request).await()
            
            // Filter results by keyword and pantry-related types
            response.placeLikelihoods
                .filter { likelihood ->
                    val place = likelihood.place
                    val name = place.name?.lowercase() ?: ""
                    val address = place.address?.lowercase() ?: ""
                    val types = place.types?.map { it.name.lowercase() } ?: emptyList()
                    
                    // Check if name/address contains keyword or pantry-related terms
                    name.contains(keyword, ignoreCase = true) ||
                    address.contains(keyword, ignoreCase = true) ||
                    types.any { it.contains("food") || it.contains("store") || it.contains("establishment") }
                }
                .mapNotNull { likelihood ->
                    val place = likelihood.place
                    val latLng = place.latLng
                    
                    if (latLng != null && place.name != null) {
                        Pantry(
                            name = place.name ?: "Unknown",
                            description = "Community pantry",
                            address = place.address ?: "Address not available",
                            distance = "", // Will be calculated by HomePageActivity
                            latitude = latLng.latitude,
                            longitude = latLng.longitude,
                            distanceMeters = null // Will be calculated
                        )
                    } else null
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error searching places for keyword: $keyword", e)
            emptyList()
        }
    }
    
    /**
     * Use Places API Nearby Search (better for location-based searches)
     * This method uses OkHttp to call the Places API REST endpoint
     */
    suspend fun searchNearbyPantriesRestApi(location: Location): List<Pantry> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting Places API Search")
        
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e(TAG, "API KEY ERROR: Invalid or missing API key")
            Log.e(TAG, "   - API key length: ${apiKey.length}")
            Log.e(TAG, "   - Is placeholder: ${apiKey == "YOUR_API_KEY_HERE"}")
            Log.e(TAG, "   - Fix: Add your API key to app/src/main/res/values/strings.xml")
            return@withContext emptyList()
        }
        
        Log.d(TAG, "LOCATION INFO:")
        Log.d(TAG, "   - Latitude: ${location.latitude}")
        Log.d(TAG, "   - Longitude: ${location.longitude}")
        Log.d(TAG, "   - Accuracy: ${location.accuracy}m")
        Log.d(TAG, "   - Provider: ${location.provider}")
        
        // Validate location
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.e(TAG, "LOCATION ERROR: Invalid location (0,0)")
            return@withContext emptyList()
        }
        
        val allResults = mutableListOf<Pantry>()
        val client = OkHttpClient()
        
        try {
            val lat = location.latitude
            val lng = location.longitude
            
            // Use Nearby Search API - search for food places first
            // Try multiple searches to get more results
            val searches = listOf(
                // Search 1: Food places (broader)
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$lat,$lng&" +
                        "radius=$SEARCH_RADIUS_METERS&" +
                        "type=establishment&" +  // Broader than just "food"
                        "key=$apiKey",
                // Search 2: Food places specifically
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=$lat,$lng&" +
                        "radius=$SEARCH_RADIUS_METERS&" +
                        "type=food&" +
                        "key=$apiKey"
            )
            
            // Try each search
            for ((index, url) in searches.withIndex()) {
                Log.d(TAG, "API CALL #${index + 1}:")
                Log.d(TAG, "   URL: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val startTime = System.currentTimeMillis()
                val response = client.newCall(request).execute()
                val responseTime = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string()
                
                Log.d(TAG, "RESPONSE:")
                Log.d(TAG, "   - HTTP Code: ${response.code}")
                Log.d(TAG, "   - Response Time: ${responseTime}ms")
                Log.d(TAG, "   - Response Body (first 500 chars): ${responseBody?.take(500)}")
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val status = jsonObject.optString("status", "UNKNOWN")
                    val errorMessage = jsonObject.optString("error_message", "")
                    
                    Log.d(TAG, "API STATUS: $status")
                    if (errorMessage.isNotEmpty()) {
                        Log.e(TAG, "Error Message: $errorMessage")
                    }
                    
                    when (status) {
                        "OK" -> {
                            val results = parsePlacesJson(responseBody, location)
                            allResults.addAll(results)
                            Log.d(TAG, "SUCCESS: Found ${results.size} places")
                        }
                        "ZERO_RESULTS" -> {
                            Log.w(TAG, "No results found for this search")
                        }
                        "REQUEST_DENIED" -> {
                            Log.e(TAG, "REQUEST DENIED")
                            Log.e(TAG, "   Possible causes:")
                            Log.e(TAG, "   1. API key restrictions blocking this app")
                            Log.e(TAG, "   2. Places API not enabled")
                            Log.e(TAG, "   3. Billing not enabled")
                            Log.e(TAG, "   4. API key invalid")
                        }
                        "OVER_QUERY_LIMIT" -> {
                            Log.e(TAG, "QUOTA EXCEEDED: API quota limit reached")
                        }
                        else -> {
                            Log.e(TAG, "UNKNOWN ERROR: Status = $status")
                        }
                    }
                } else {
                    Log.e(TAG, "HTTP ERROR: ${response.code} - ${response.message}")
                }
            }
            
            // Also do text searches for pantry-specific terms
            val pantrySearches = listOf("food pantry", "food bank", "community pantry", "free food")
            for (keyword in pantrySearches) {
                val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                        "query=$keyword&" +
                        "location=$lat,$lng&" +
                        "radius=$SEARCH_RADIUS_METERS&" +
                        "key=$apiKey"
                
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    
                    if (response.isSuccessful && responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        if (jsonObject.optString("status") == "OK") {
                            val results = parsePlacesJson(responseBody, location)
                            // Filter to only pantry-related for these searches
                            val pantryResults = results.filter { isPantryRelated(it.name) }
                            allResults.addAll(pantryResults)
                            Log.d(TAG, "Found ${pantryResults.size} pantries for keyword: $keyword")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for $keyword", e)
                }
            }
            
            // Remove duplicates and return
            val uniqueResults = allResults.distinctBy { "${it.name}_${it.address}" }
            
            Log.d(TAG, "FINAL RESULTS:")
            Log.d(TAG, "   - Total places found: ${allResults.size}")
            Log.d(TAG, "   - Unique places: ${uniqueResults.size}")
            Log.d(TAG, "   - Returning: ${uniqueResults.take(MAX_RESULTS).size} places")
            
            return@withContext uniqueResults.take(MAX_RESULTS)
            
        } catch (e: Exception) {
            Log.e(TAG, "═══════════════════════════════════════════════════")
            Log.e(TAG, "EXCEPTION in Places API search:")
            Log.e(TAG, "   - Error: ${e.javaClass.simpleName}")
            Log.e(TAG, "   - Message: ${e.message}")
            Log.e(TAG, "   - Stack trace:")
            e.printStackTrace()
            Log.e(TAG, "═══════════════════════════════════════════════════")
            emptyList()
        }
    }
    
    
    /**
     * Fallback: Use Text Search API with pantry keywords
     */
    private suspend fun searchWithTextSearch(location: Location, client: OkHttpClient): List<Pantry> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pantry>()
        
        try {
            // Use the most relevant keywords
            val keywords = listOf("food pantry", "food bank", "community pantry")
            
            for (keyword in keywords) {
                val lat = location.latitude
                val lng = location.longitude
                
                // URL encode the keyword
                val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")
                
                val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                        "query=$encodedKeyword&" +
                        "location=$lat,$lng&" +
                        "radius=$SEARCH_RADIUS_METERS&" +
                        "key=$apiKey"
                
                Log.d(TAG, "Trying text search with keyword: $keyword")
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val status = jsonObject.optString("status", "UNKNOWN")
                    
                    if (status == "OK") {
                        val parsedResults = parsePlacesJson(responseBody, location)
                        results.addAll(parsedResults)
                        Log.d(TAG, "Text search found ${parsedResults.size} results for '$keyword'")
                    } else {
                        Log.d(TAG, "Text search status for '$keyword': $status")
                    }
                }
            }
                } catch (e: Exception) {
            Log.e(TAG, "Error in text search fallback", e)
        }
        
        results
    }
    
    /**
     * Parse JSON response from Places API
     */
    private fun parsePlacesJson(json: String, userLocation: Location?): List<Pantry> {
        return try {
            val jsonObject = JSONObject(json)
            val resultsArray = jsonObject.optJSONArray("results")
            
            if (resultsArray == null) {
                Log.w(TAG, "No 'results' array in Places API response")
                return emptyList()
            }
            
            val pantries = mutableListOf<Pantry>()
            
            for (i in 0 until resultsArray.length()) {
                try {
                    val place = resultsArray.getJSONObject(i)
                    val name = place.optString("name", "Unknown")
                    
                    // Skip if name is empty
                    if (name.isBlank()) {
                        continue
                    }
                    // Note: We're not filtering by isPantryRelated here to show more results
                    // The filtering happens in the calling code if needed
                    
                    val address = place.optString("formatted_address", "Address not available")
                    val geometry = place.optJSONObject("geometry")
                    
                    if (geometry == null) {
                        Log.w(TAG, "Place '$name' has no geometry, skipping")
                        continue
                    }
                    
                    val locationObj = geometry.optJSONObject("location")
                    if (locationObj == null) {
                        Log.w(TAG, "Place '$name' has no location, skipping")
                        continue
                    }
                    
                    val lat = locationObj.optDouble("lat", 0.0)
                    val lng = locationObj.optDouble("lng", 0.0)
                    
                    if (lat == 0.0 || lng == 0.0) {
                        Log.w(TAG, "Place '$name' has invalid coordinates, skipping")
                        continue
                    }
                    
                    // Calculate distance if user location is available
                    val distanceMeters = if (userLocation != null) {
                        val distanceResults = FloatArray(1)
                        android.location.Location.distanceBetween(
                            userLocation.latitude,
                            userLocation.longitude,
                            lat,
                            lng,
                            distanceResults
                        )
                        distanceResults[0].toDouble()
                    } else {
                        null
                    }
                    
                    // Get types to determine if it's pantry-related
                    val types = place.optJSONArray("types")
                    val typeList = mutableListOf<String>()
                    if (types != null) {
                        for (j in 0 until types.length()) {
                            typeList.add(types.getString(j))
                        }
                    }
                    
                    pantries.add(
                        Pantry(
                            name = name,
                            description = "Community pantry",
                            address = address,
                            distance = distanceMeters?.let { formatDistance(it) } ?: "",
                            latitude = lat,
                            longitude = lng,
                            distanceMeters = distanceMeters
                        )
                    )
                    
                    val distanceText = distanceMeters?.let { formatDistance(it) } ?: "distance unknown"
                    Log.d(TAG, "Added pantry: $name at $address ($distanceText away)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing place at index $i", e)
                }
            }
            
            Log.d(TAG, "Successfully parsed ${pantries.size} pantries from JSON")
            pantries
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Places JSON", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Check if a place name is related to pantries/food banks
     */
    private fun isPantryRelated(name: String): Boolean {
        val lowerName = name.lowercase()
        val pantryKeywords = listOf(
            "pantry", "food bank", "foodbank", "food pantry", 
            "community pantry", "free food", "food assistance",
            "food distribution", "soup kitchen", "food shelf"
        )
        return pantryKeywords.any { lowerName.contains(it) }
    }
    
    /**
     * Geocode an address to get latitude and longitude
     * @param address The address to geocode
     * @return Pair of (latitude, longitude) or null if geocoding fails
     */
    suspend fun geocodeAddress(address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val encodedAddress = java.net.URLEncoder.encode(address, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$apiKey"
            
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val status = json.getString("status")
                
                if (status == "OK") {
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val result = results.getJSONObject(0)
                        val geometry = result.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        
                        Log.d(TAG, "Geocoded address: $address -> ($lat, $lng)")
                        return@withContext Pair(lat, lng)
                    }
                } else {
                    Log.e(TAG, "Geocoding failed with status: $status")
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding address: $address", e)
            null
        }
    }
    
    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"
        } else {
            val miles = distanceMeters / 1609.34
            String.format(java.util.Locale.getDefault(), "%.1f mi", miles)
        }
    }
    
    /**
     * Search for places using a user's search query
     * @param query User's search text
     * @param location User's current location (optional, for distance calculation)
     * @return List of Pantry objects matching the search
     */
    suspend fun searchPlacesByQuery(query: String, location: Location?): List<Pantry> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e(TAG, "Invalid API key")
            return@withContext emptyList()
        }
        
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        
        Log.d(TAG, "Searching Places API for query: $query")
        
        val results = mutableListOf<Pantry>()
        val client = OkHttpClient()
        
        try {
            // Build search query - combine user query with pantry-related terms
            val searchQuery = if (query.lowercase().contains("pantry") || 
                                 query.lowercase().contains("food bank") ||
                                 query.lowercase().contains("food")) {
                query // User already searching for pantry-related terms
            } else {
                "$query food pantry" // Add pantry context to search
            }
            
            val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
            
            // Build URL with location if available
            val url = if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                        "query=$encodedQuery&" +
                        "location=$lat,$lng&" +
                        "radius=$SEARCH_RADIUS_METERS&" +
                        "key=$apiKey"
            } else {
                "https://maps.googleapis.com/maps/api/place/textsearch/json?" +
                        "query=$encodedQuery&" +
                        "key=$apiKey"
            }
            
            Log.d(TAG, "Search URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Search response code: ${response.code}")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val status = jsonObject.optString("status", "UNKNOWN")
                
                if (status == "OK") {
                    val parsedResults = parsePlacesJson(responseBody, location)
                    results.addAll(parsedResults)
                    Log.d(TAG, "Search found ${parsedResults.size} results for '$query'")
                } else {
                    val errorMessage = jsonObject.optString("error_message", "Unknown error")
                    Log.e(TAG, "Search failed ($status): $errorMessage")
                }
            } else {
                Log.e(TAG, "Search HTTP error: ${response.code}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Places API", e)
            e.printStackTrace()
        }
        
        results
    }
}

