package com.jinkim.hyundaiAndGretel.demo

import android.app.AppOpsManager
import android.os.Process
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.util.Log
import android.os.SystemClock
import com.jinkim.hyundaiAndGretel.notes.DemoRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Only compiled into debug; matches Pleos's official sample's mock-GPS mechanism. */
class DebugGps(context: Context) {
    private val appContext = context.applicationContext
    val available = Build.HARDWARE in setOf("ranchu", "goldfish") || Build.FINGERPRINT.contains("crp_avd")
    @Suppress("DEPRECATION")
    fun permissionGranted() = appContext.getSystemService(AppOpsManager::class.java)
        .checkOpNoThrow("android:mock_location", Process.myUid(), appContext.packageName) == AppOpsManager.MODE_ALLOWED
    private val manager = context.getSystemService(LocationManager::class.java)
    private val marker = File(context.noBackupFilesDir, "road-note-mock-gps")
    private var active = false
    init {
        if (marker.exists()) runCatching { stop() }.onFailure { Log.w("RoadNotes", "MOCK_GPS_CLEANUP_FAILED", it) }
    }

    @SuppressLint("InlinedApi")
    @Suppress("DEPRECATION")
    fun setPosition(progress: Double) {
        check(available) { "EMULATOR_ONLY" }
        require(progress.isFinite())
        if (!active) {
            manager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE)
            marker.createNewFile()
            active = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        }
        manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
            latitude = DemoRoute.LATITUDE
            longitude = DemoRoute.longitudeAt(progress)
            accuracy = 5f
            bearing = 90f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        })
    }

    suspend fun speedKph(): Double = withContext(Dispatchers.IO) {
        val connection = URL("http://127.0.0.1:8088/vehicle/state").openConnection() as HttpURLConnection
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        try {
            check(connection.responseCode == 200) { "VIRTUAL_CONSOLE_UNAVAILABLE" }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val state = JSONObject(json)
            val speed = state.getJSONObject("controls").getDouble("speed_kph")
            check(speed.isFinite() && speed in 0.0..240.0) { "INVALID_CONSOLE_SPEED" }
            when (state.getJSONObject("gear").getJSONObject("actual").getString("value")) {
                "Drive" -> speed
                "Reverse" -> -speed
                else -> 0.0
            }
        } finally { connection.disconnect() }
    }

    fun stop() {
        if (!active && !marker.exists()) return
        manager.removeTestProvider(LocationManager.GPS_PROVIDER)
        active = false
        check(!marker.exists() || marker.delete()) { "MOCK_GPS_CLEANUP_FAILED" }
    }
}
