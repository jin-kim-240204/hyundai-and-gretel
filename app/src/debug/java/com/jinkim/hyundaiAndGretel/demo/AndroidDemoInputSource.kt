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

/** 디버그 전용 입력이다. 모의 위치 권한 오류를 표시하고, 실패 시 앱 위치값을 직접 만들어 대체하지 않는다. */
class AndroidDemoInputSource(context: Context) : DemoInputSource {
    private val context = context.applicationContext
    private val manager = context.getSystemService(LocationManager::class.java)
    private var installed = false
    private val marker = java.io.File(this.context.noBackupFilesDir, "demo-owned-gps")

    @Suppress("DEPRECATION")
    // 리뷰용: 운영 환경·모의 위치 앱 권한·콘솔 응답을 확인한다. 앱 스스로 권한을 올리거나 가짜 성공을 만들지 않는다.
    override suspend fun prepare() {
        check(Build.HARDWARE in setOf("ranchu", "goldfish") || Build.FINGERPRINT.contains("crp_avd")) { "에뮬레이터 전용 입력입니다" }
        val allowed = context.getSystemService(AppOpsManager::class.java)
            .checkOpNoThrow("android:mock_location", Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        check(allowed) { "mock 위치 권한이 없습니다" }
        readMotion() // 위치 제공자를 등록하기 전에 콘솔 응답부터 확인한다.
    }

    @SuppressLint("InlinedApi")
    @Suppress("DEPRECATION")
    // 리뷰용: 합성 좌표를 Android GPS 제공자에 넣는다. 앱의 현재 위치를 직접 바꾸는 대신 실제 Pleos SDK 콜백을 기다린다.
    override fun inject(progress: Double) {
        if (!installed) {
            // 에뮬레이터 운영자가 이 앱을 유일한 모의 위치 제공 앱으로 선택해야 한다.
            check(marker.exists() || marker.createNewFile()) { "GPS 소유 기록 실패" }
            manager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, true, true, true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_FINE)
            installed = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        }
        val fix = DemoGeometry.fix(progress, SystemClock.elapsedRealtime())
        manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, Location(LocationManager.GPS_PROVIDER).apply {
            latitude = fix.latitude; longitude = fix.longitude
            accuracy = 5f // 합성 좌표 주입용 값이며, Pleos SDK가 측정한 실제 정확도가 아니다.
            bearing = 90f
            time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        })
    }

    // 리뷰용: 로컬 웹콘솔에서 속도와 기어를 읽는다. 네트워크 작업은 IO 실행 문맥에서 수행하고 전체 응답을 검증한다.
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
            consoleDelta(speed, gear, 0) // 알 수 없는 기어와 후진은 정차로 간주하지 않고 오류로 처리한다.
            speed to gear
        } finally { connection.disconnect() }
    }

    // 리뷰용: 이 앱이 소유한 모의 GPS만 제거한다. 중지 후 외부 ADB 입력이 가로막히지 않도록 원래 제공자를 복원한다.
    override fun close() {
        if (installed || marker.exists()) {
            manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            installed = false
            check(!marker.exists() || marker.delete()) { "GPS 소유 기록 정리 실패" }
        }
    }
}
