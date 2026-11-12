package com.jinkim.hyundaiAndGretel.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.jinkim.hyundaiAndGretel.ui.components.PleosButton
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteHeader
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteMessageCard
import com.jinkim.hyundaiAndGretel.ui.components.RoadNoteStatus
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class PleosComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun onlyEnabledButtonEmitsClick() {
        val clicks = mutableIntStateOf(0)
        compose.setContent {
            HyundaiAndGretelTheme {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PleosButton("Enabled", { clicks.intValue++ })
                    PleosButton("Disabled", { clicks.intValue++ }, enabled = false)
                    PleosButton("Loading", { clicks.intValue++ }, loading = true)
                }
            }
        }
        compose.onNodeWithText("Enabled").assertIsEnabled().performClick()
        compose.onNodeWithText("Disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText("Loading").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks.intValue) }
    }

    @Test fun receivedNoteDayRenders() = captureReceivedNote(false, 1030, 377, "received-day")
    @Test fun receivedNoteNightRenders() = captureReceivedNote(true, 512, 377, "received-night")

    private fun captureReceivedNote(dark: Boolean, width: Int, height: Int, name: String) {
        compose.setContent {
            HyundaiAndGretelTheme(darkTheme = dark) {
                Surface(Modifier.requiredSize(width.dp, height.dp).testTag("fixture"),
                    color = PleosTheme.colors.background) {
                    Column(Modifier.padding(horizontal = RoadNoteDimensions.pagePadding)) {
                        RoadNoteHeader("도로 쪽지")
                        RoadNoteMessageCard("주의", "도로에 구덩이가 있어요.",
                            Modifier.padding(top = RoadNoteDimensions.sectionGap), compact = dark)
                        RoadNoteStatus("●  도로 쪽지 받는 중",
                            Modifier.padding(top = if (PleosTheme.colors.isDark) RoadNoteDimensions.compactStatusGap else RoadNoteDimensions.statusGap))
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("도로에 구덩이가 있어요.", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertFalse(layout.hasVisualOverflow)
        assertEquals(if (dark) 2 else 1, layouts.single().lineCount)
        if (dark) assertEquals("도로에 구덩이가",
            layouts.single().layoutInput.text.text.substring(0, layouts.single().getLineEnd(0, true)).trimEnd())
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(target.filesDir, "$name.png")
        compose.onNodeWithTag("fixture").captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
