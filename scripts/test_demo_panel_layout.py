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
        dialog = debug.split("private fun DemoDialog(", 1)[1].split("private fun DemoPanelContents", 1)[0]
        self.assertIn("DialogProperties(usePlatformDefaultWidth = false)", dialog)
        self.assertIn("onDismissRequest = onClose", dialog)
        self.assertIn("demoDialogMaxWidth = 780.dp", debug)
        self.assertIn("padding(12.dp).widthIn(max = demoDialogMaxWidth).fillMaxWidth()", dialog)
        self.assertIn("shape = RoundedCornerShape(18.dp)", dialog)
        self.assertIn("shadowElevation = 24.dp", dialog)
        self.assertRegex(dialog, r"IconButton\(onClick = onClose, modifier = Modifier.size\(56\.dp\)\)")
        self.assertIn("contentDescription = policy.label(stringResource(R.string.debug_tools_close))", dialog)
        self.assertLess(dialog.index("IconButton(onClick = onClose"), dialog.index(".verticalScroll("))
        self.assertIn("Modifier.weight(1f, fill = false).verticalScroll", dialog)
        self.assertIn("val narrow = maxWidth <= 580.dp", debug)
        self.assertIn("maxItemsInEachRow = if (narrow) 1 else 2", debug)

    def test_backdrop_taps_are_handled_on_a_sibling_behind_the_panel(self):
        debug = next((SOURCE / "debug").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        dialog = debug.split("private fun DemoDialog(", 1)[1].split("private fun DemoPanelContents", 1)[0]
        self.assertRegex(
            dialog,
            r"Box\(\s*Modifier.matchParentSize\(\).background\(Color\.White\.copy\(alpha = 0\.4f\)\).pointerInput\(Unit\) \{\s*"
            r"detectTapGestures\(onTap = \{ closeBackdrop\(\) \}\)\s*\},\s*\)"
            r"\s*(?://[^\n]*\n\s*)?Surface\(\s*modifier = Modifier.padding\(12.dp\)",
        )
        self.assertEqual(dialog.count(".pointerInput("), 1)
        self.assertNotIn(".clickable(", dialog)
        self.assertNotIn("dismissOnBackPress = false", dialog)

    def test_web_console_only_switch(self):
        debug = next((SOURCE / "debug").rglob("DemoPanel.kt")).read_text(encoding="utf-8")
        self.assertIn("window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)", debug)
        self.assertIn("if (demo.mode == DemoMode.EXTERNAL_ADB)", debug)
        self.assertIn("selectMode(DemoMode.WEB_CONSOLE)", debug)
        self.assertNotIn("selectMode(DemoMode.EXTERNAL_ADB)", debug)

    def test_debug_label_and_close_icon_are_in_debug_resources(self):
        resources = SOURCE / "debug/res"
        strings = ET.parse(resources / "values/strings.xml")
        self.assertEqual(strings.findtext("./string[@name='debug_tools_badge']"), "Debug")
        for name in ("debug_close", "debug_disclosure", "debug_reset"):
            self.assertEqual(ET.parse(resources / f"drawable/{name}.xml").getroot().tag, "vector")
        self.assertFalse(any("·" in (item.text or "") for item in strings.getroot()))


if __name__ == "__main__":
    unittest.main()
