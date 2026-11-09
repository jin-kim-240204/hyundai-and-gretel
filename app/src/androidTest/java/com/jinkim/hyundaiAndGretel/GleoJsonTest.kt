package com.jinkim.hyundaiAndGretel

import com.jinkim.hyundaiAndGretel.gleo.*
import com.jinkim.hyundaiAndGretel.demo.ReportResult
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GleoJsonTest {
    @Test fun malformedAndTypedBoundaryAreRejected() {
        assertNull(decode("""{"content":"pothole","category":"WARNING" "locationMode":"CURRENT_ROAD"}"""))
        assertNull(decode("""{"content":12,"category":"WARNING","locationMode":"CURRENT_ROAD"}"""))
        assertNull(decode("{} garbage"))
        assertNotNull(decode("""{"content":"pothole","category":"WARNING","locationMode":"CURRENT_ROAD"}"""))
        val callback = JSONObject(callbackJson(ReportResult.Failed("INVALID_INPUT")))
        assertEquals("fail", callback.getString("status"))
        assertEquals("voice", callback.getString("mode"))
        assertTrue(callback.get("data") is String)
    }
}
