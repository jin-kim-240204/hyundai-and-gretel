"""Check the variant-specific scroll clearance contract without running Android."""
from pathlib import Path
import unittest


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


if __name__ == "__main__":
    unittest.main()
