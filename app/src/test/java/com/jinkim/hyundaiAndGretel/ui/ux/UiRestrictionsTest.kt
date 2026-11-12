package com.jinkim.hyundaiAndGretel.ui.ux

import android.car.drivingstate.CarUxRestrictions as F
import org.junit.Assert.*
import org.junit.Test

class UiRestrictionsTest {
    @Test fun unknownIsNotSetupPermissionAndKnownLimitsSurviveDisconnection() {
        assertFalse(UiRestrictions().canSetup)
        assertTrue(UiRestrictions(connected = true).canSetup)
        val policy = UiRestrictions(true, true, F.UX_RESTRICTIONS_NO_SETUP or F.UX_RESTRICTIONS_NO_TEXT_MESSAGE or F.UX_RESTRICTIONS_NO_VIDEO)
        assertFalse(policy.canSetup); assertFalse(policy.canShowBody); assertFalse(policy.canShowPlot)
        assertFalse(policy.copy(connected = false).canShowBody)
    }
    @Test fun platformStringAndContentLimitsDoNotResetWhenPanelCloses() {
        val policy = UiRestrictions(true, true, F.UX_RESTRICTIONS_LIMIT_STRING_LENGTH or F.UX_RESTRICTIONS_LIMIT_CONTENT, 2, 1, 1)
        assertEquals("가😀", policy.label("가😀나"))
        assertFalse(policy.fits("가😀나"))
        val seen = setOf("receipt:B/1")
        assertTrue(policy.allowsContent("receipt:B/1", seen))
        assertFalse(policy.allowsContent("receipt:B/2", seen))
        assertFalse(policy.allowsContent("panel", emptySet(), 2))
        assertFalse(policy.copy(itemLimit = 0).allowsContent("first", emptySet()))
    }
}
