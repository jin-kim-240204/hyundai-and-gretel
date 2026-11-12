package com.jinkim.hyundaiAndGretel.demo

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.location.provider.ProviderProperties
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AndroidDemoInputSource(context: Context) : DemoInputSource {
    private val context = context.applicationContext
    private val manager = context.getSystemService(LocationManager::class.java)
    private var installed = false
    private val marker = java.io.File(this.context.noBackupFilesDir, "demo-owned-gps")

    @Suppress("DEPRECATION")

    override suspend fun prepare() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish") || Build.FINGERPRINT.contains("crp_avd")) { "에뮬레이터 전용 입력입니다" }
        val allowed = context.getSystemService(AppOpsManager::class.java)
            .checkOpNoThrow("android:mock_location", Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        check(allowed) { "mock 위치 권한이 없습니다" }
        readMotion()
    }

    @SuppressLint("InlinedApi")
    @Suppress("DEPRECATION")

    override fun inject(progress: Double) {
        if (!installed) {

            check(marker.exists() || marker.createNewFile()) { "GPS 소유 기록 실패" }
            manager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE)
            installed = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        }
        val fix = DemoGeometry.fix(progress, SystemClock.elapsedRealtime())
        manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
            latitude = fix.latitude; longitude = fix.longitude
            accuracy = 5f
            bearing = 90f
            time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        })
    }

    override suspend fun readMotion(): Pair<Double, String> = withContext(Dispatchers.IO) {
        val connection = URL("http://127.0.0.1:8088/vehicle/state").openConnection() as HttpURLConnection
        connection.connectTimeout = 1_000; connection.readTimeout = 1_000
        try {
            check(connection.responseCode == 200) { "웹콘솔 연결 실패" }
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(4_096)
                while (true) {
                    ensureActive()
                    val count = stream.read(chunk)
                    if (count < 0) break
                    check(output.size() + count <= 65_536) { "콘솔 응답 크기 초과" }
                    output.write(chunk, 0, count)
                }
                output.toByteArray()
            }
            check(bytes.size <= 65_536) { "콘솔 응답 크기 초과" }
            ensureActive()
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            val speed = json.getJSONObject("controls").getDouble("speed_kph")
            val gear = json.getJSONObject("gear").getJSONObject("actual").getString("value")
            consoleDelta(speed, gear, 0)
            speed to gear
        } finally { connection.disconnect() }
    }

    override fun close() {
        if (installed || marker.exists()) {
            manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            installed = false
            check(!marker.exists() || marker.delete()) { "GPS 소유 기록 정리 실패" }
        }
    }
}
