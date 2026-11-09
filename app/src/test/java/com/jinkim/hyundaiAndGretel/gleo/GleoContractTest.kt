package com.jinkim.hyundaiAndGretel.gleo

import org.junit.Assert.*
import org.junit.Test

class GleoContractTest {
    private val values = mapOf("content" to "  pothole  ", "locationMode" to "CURRENT_ROAD", "category" to "WARNING")
    @Test fun validatesWithoutTypeCoercionOrExtraInformation() {
        assertEquals("pothole", validateReport(values)!!.content)
        assertNull(validateReport(values + ("content" to 123)))
        assertNull(validateReport(values + ("content" to " ")))
        assertNull(validateReport(values + ("content" to "a".repeat(501))))
        assertNull(validateReport(values + ("category" to "OTHER")))
        assertNull(validateReport(values + ("locationMode" to "NAMED_PLACE")))
        assertNotNull(validateReport(values + ("observedAt" to "ignored")))
    }
}
