"""Check debug modal layout and scroll clearance without running Android."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


SOURCE = Path(__file__).resolve().parents[1] / "app/src"


class DemoPanelLayoutTest(unittest.TestCase):
    def test_scroll_end_clears_debug_button_and_release_adds_no_padding(self):
        debug = next((SOURCE / "debug").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        release = next((SOURCE / "release").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        screen = next((SOURCE / "main").rglob("RoadNotesScreen.kt")).read_text(encoding="utf-8")

        self.assertRegex(debug, r"val demoButtonHeight = 56\.dp\b")
        self.assertRegex(
            debug,
            r"\.align\(Alignment\.BottomEnd\)\s*\.padding\(demoButtonMargin\)"
            r"\s*\.height\(demoButtonHeight\)\s*\.widthIn\(min = demoButtonHeight\)",
        )
        self.assertIn("demoPanelBottomPadding = demoButtonHeight + demoButtonMargin", debug)
        self.assertRegex(release, r"demoPanelBottomPadding = 0\.dp\b")
        self.assertRegex(
            screen,
            r"\.verticalScroll\(rememberScrollState\(\)\)\s*"
            r"\.padding\(horizontal = [^,]+, vertical = [^)]+\)\s*"
            r"\.padding\(bottom = demoPanelBottomPadding\)",
        )

    def test_debug_dialog_is_bounded_and_keeps_close_outside_the_scroll(self):
        debug = next((SOURCE / "debug").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        dialog = debug.split("private fun DemoDialog(", 1)[1].split("@RequiresApi", 1)[0]
        self.assertIn("DialogProperties(usePlatformDefaultWidth = false)", dialog)
        self.assertIn("onDismissRequest = onClose", dialog)
        self.assertIn("minOf(maxWidth * 0.9f, demoDialogMaxWidth)", dialog)
        self.assertIn(".heightIn(max = maxHeight * 0.9f)", dialog)
        self.assertIn("shadowElevation = 8.dp", dialog)
        self.assertRegex(dialog, r"IconButton\(onClick = onClose, modifier = Modifier.size\(56\.dp\)\)")
        self.assertIn("contentDescription = policy.label(stringResource(R.string.debug_tools_close))", dialog)
        self.assertRegex(
            dialog,
            r"\}\s*\}\s*Box\(Modifier.weight\(1f, fill = false\).verticalScroll"
            r"\(rememberScrollState\(\)\)\)\s*\{\s*content\(\)",
        )
        self.assertIn("horizontalAlignment = Alignment.CenterHorizontally", debug)

    def test_debug_window_blur_has_an_api_gate_and_dim_fallback(self):
        debug = next((SOURCE / "debug").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        self.assertRegex(
            debug,
            r"WindowManager.LayoutParams.FLAG_DIM_BEHIND\)\s*"
            r"window.setDimAmount\(dimWithoutBlur\)\s*"
            r"if \(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S\) \{\s*"
            r"val stopObserving = observeDemoBlur\(window, blurRadiusPx\)\s*"
            r"onDispose \{ stopObserving\(\) \}",
        )
        self.assertIn("setBlurBehindRadius(if (enabled) blurRadiusPx else 0)", debug)
        self.assertIn("dimAmount = if (enabled) dimWithBlur else dimWithoutBlur", debug)
        self.assertIn("removeCrossWindowBlurEnabledListener(listener)", debug)

    def test_debug_label_and_close_icon_are_in_debug_resources(self):
        resources = SOURCE / "debug/res"
        strings = ET.parse(resources / "values/strings.xml")
        self.assertEqual(strings.findtext("./string[@name='debug_tools_badge']"), "Debug")
        self.assertEqual(ET.parse(resources / "drawable/debug_close.xml").getroot().tag, "vector")


if __name__ == "__main__":
    unittest.main()
