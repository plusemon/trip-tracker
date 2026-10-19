package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize Firebase Realtime Database with explicit URL and disk persistence
            val database = FirebaseDatabase.getInstance(FIREBASE_DB_URL).apply {
                setPersistenceEnabled(true)
            }
            Log.d(TAG, "Firebase Realtime Database initialized with offline persistence: $FIREBASE_DB_URL")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Realtime Database persistence: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MainApplication"
        const val FIREBASE_DB_URL = "https://trip-tracker-e602d-default-rtdb.asia-southeast1.firebasedatabase.app/"
    }
}
