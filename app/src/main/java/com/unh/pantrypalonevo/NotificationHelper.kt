package com.unh.pantrypalonevo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Helper class for managing push notifications
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Get FCM token and save it to Firestore for the current user
     */
    fun getAndSaveToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            // Save token to Firestore
            db.collection("users")
                .document(userId)
                .update("notificationToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token saved successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving token", e)
                }
        }
    }

    /**
     * Send notification to all users when a new pantry is posted
     * This should be called from a Cloud Function or backend
     * For now, we'll create a helper that can be used from Cloud Functions
     */
    suspend fun sendPantryNotificationToAllUsers(
        pantryName: String,
        pantryAddress: String,
        pantryId: String
    ) {
        try {
            // Get all user tokens
            val usersSnapshot = db.collection("users").get().await()
            val tokens = mutableListOf<String>()

            usersSnapshot.documents.forEach { doc ->
                val token = doc.getString("notificationToken")
                if (!token.isNullOrEmpty()) {
                    tokens.add(token)
                }
            }

            Log.d(TAG, "Sending notification to ${tokens.size} users about pantry: $pantryName")

            // Note: In production, you should use Firebase Cloud Functions to send notifications
            // This is just a helper. The actual sending should be done server-side.
            // For now, we'll log it and you can implement Cloud Function later

        } catch (e: Exception) {
            Log.e(TAG, "Error sending pantry notification", e)
        }
    }

    /**
     * Send notification to nearby users when items are added to a pantry
     */
    suspend fun sendItemNotificationToNearbyUsers(
        pantryName: String,
        itemCount: Int,
        pantryId: String
    ) {
        try {
            // Similar to sendPantryNotificationToAllUsers
            // In production, filter by location and send to nearby users only
            val usersSnapshot = db.collection("users").get().await()
            val tokens = mutableListOf<String>()

            usersSnapshot.documents.forEach { doc ->
                val token = doc.getString("notificationToken")
                if (!token.isNullOrEmpty()) {
                    tokens.add(token)
                }
            }

            Log.d(TAG, "Sending item notification to ${tokens.size} users about pantry: $pantryName")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending item notification", e)
        }
    }
}

