package com.unh.pantrypalonevo

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DetectedItem(
    val name: String,
    val confidence: Float
)

class PantryCloudDetector {

    private val CLOUD_FUNCTION_URL = "https://us-central1-black-cirrus-477305-m5.cloudfunctions.net/detectPantryItems"

    suspend fun detect(bitmap: Bitmap): List<DetectedItem> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting detection...")

            // Convert bitmap to Base64
            val base64Image = bitmapToBase64(bitmap)

            // Call Cloud Function
            val response = callCloudFunction(base64Image)

            // Parse response
            val items = parseResponse(response)
            Log.d(TAG, "Found ${items.size} items")

            items

        } catch (e: Exception) {
            Log.e(TAG, "Detection error", e)
            emptyList()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun callCloudFunction(imageBase64: String): String {
        val url = URL(CLOUD_FUNCTION_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("imageBase64", imageBase64)
            }

            connection.outputStream.use {
                it.write(jsonBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Cloud function error: $error")
                return """{"items":[],"count":0}"""
            }

        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(jsonResponse: String): List<DetectedItem> {
        val json = JSONObject(jsonResponse)
        val itemsArray = json.getJSONArray("items")
        val items = mutableListOf<DetectedItem>()

        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            val name = item.getString("name")
            val confidence = item.getDouble("confidence").toFloat()

            items.add(DetectedItem(
                name = name,
                confidence = confidence
            ))
        }

        return items
    }

    companion object {
        private const val TAG = "PantryCloudDetector"
    }
}
