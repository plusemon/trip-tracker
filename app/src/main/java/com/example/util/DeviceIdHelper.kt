package com.example.util

import android.content.Context
import java.util.UUID

object DeviceIdHelper {
    private const val PREFS_NAME = "trip_device_prefs"
    private const val KEY_DEVICE_ID = "unique_device_id"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = "dev_" + UUID.randomUUID().toString().replace("-", "").take(10)
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
