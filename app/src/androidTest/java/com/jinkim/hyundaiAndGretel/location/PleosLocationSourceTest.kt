package com.jinkim.hyundaiAndGretel.location

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class PleosLocationSourceTest {
    @Test fun unregisterPermissionFailureStillReleasesSdkConnection() = runBlocking {
        val bound = CompletableDeferred<Unit>()
        var denyPermission = false
        var unbinds = 0
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
            override fun bindService(intent: Intent, connection: ServiceConnection, flags: Int): Boolean {
                bound.complete(Unit)
                return true
            }
            override fun unbindService(connection: ServiceConnection) { unbinds++ }
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
                if (permission == "pleos.car.permission.FUSED_LOCATION") {
                    if (denyPermission) PackageManager.PERMISSION_DENIED else PackageManager.PERMISSION_GRANTED
                } else super.checkPermission(permission, pid, uid)
        }
        val source = PleosLocationSource(context)
        try {
            withContext(Dispatchers.Main) { assertTrue(source.start({}, {})) }
            withTimeout(5_000) { bound.await() }
            withContext(Dispatchers.Main) {
                denyPermission = true
                try {
                    source.stop()
                    fail("Expected unregister permission failure")
                } catch (_: SecurityException) {
                    assertEquals("SDK release must disconnect even when unregister throws", 1, unbinds)
                }
                source.stop()
                assertEquals("Repeated stop must not release again", 1, unbinds)
            }
        } finally {
            withContext(Dispatchers.Main) { source.stop() }
        }
    }
}
