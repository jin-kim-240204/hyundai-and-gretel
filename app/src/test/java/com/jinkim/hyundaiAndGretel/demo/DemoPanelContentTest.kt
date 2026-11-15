package com.jinkim.hyundaiAndGretel.demo

import android.car.drivingstate.CarUxRestrictions
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.RoadNote
import com.jinkim.hyundaiAndGretel.notes.pickup.InboxItem
import com.jinkim.hyundaiAndGretel.notes.pickup.InboxState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.RoadNotesUiState
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions
import org.junit.Assert.*
import org.junit.Test

class DemoPanelContentTest {
    private fun restricted(items: Int, depth: Int = 3) = UiRestrictions(
        connected = true, optimized = true,
        flags = CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT,
        itemLimit = items, depthLimit = depth,
    )

    @Test fun productReceiptReservesTheLastItemBeforeDebugSectionsAndDoesNotAcknowledge() {
        val note = RoadNote("00000000-0000-0000-0000-000000000001", "A", "구덩이", NoteCategory.WARNING,
            37.5, 127.03, 0, 1_000, 601_000)
        val item = InboxItem(note, "B", 2_000, 10.0, null)
        val ui = RoadNotesUiState(inbox = InboxState(items = listOf(item)), restrictions = restricted(1))
        assertTrue(visibleDemoContent(ui, details = true).isEmpty())
        assertNull(ui.inbox.pending!!.displayedAtUtcMillis)
        assertEquals(setOf("settings"), visibleDemoContent(ui.copy(restrictions = restricted(2)), details = true))
    }

    @Test fun recomputingSectionsDoesNotAccumulateTemporaryReservations() {
        val ui = RoadNotesUiState(restrictions = restricted(3))
        val expected = setOf("settings", "scenario", "source")
        repeat(3) { assertEquals(expected, visibleDemoContent(ui, details = true)) }
        assertEquals(setOf("settings", "plot"), visibleDemoContent(ui, details = false))
    }

    @Test fun previouslyPresentedSectionsRemainWithinTheLimitAndDepthRestrictionStillApplies() {
        val ui = RoadNotesUiState(restrictions = restricted(1), visitedContent = setOf("debug:settings"))
        assertEquals(setOf("settings"), visibleDemoContent(ui, details = true))
        assertTrue(visibleDemoContent(ui.copy(restrictions = restricted(1, depth = 1)), details = true).isEmpty())
    }
}
