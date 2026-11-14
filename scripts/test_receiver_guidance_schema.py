"""Verify the checked-in Gleo schema contract without running Android."""
import json
from pathlib import Path
import re
import unittest


class ReceiverGuidanceSchemaTest(unittest.TestCase):
    def test_required_fields_and_english_description_contract(self):
        schema_path = Path(__file__).resolve().parents[1] / "app/src/main/assets/schema.json"
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        report = next(call["function"] for call in schema["actionCalls"]
                      if call["function"]["name"] == "CREATE_ROAD_NOTE")
        parameters = report["parameters"]
        self.assertCountEqual(
            parameters["required"], ["content", "receiverGuidance", "locationMode", "category"],
        )
        expected_phrases = {
            "content": ("own wording and language", "reported facts, negation, and uncertainty"),
            "receiverGuidance": (
                "In the user's language", "explicitly attribute the condition to a report",
                "receiving driver", "120 characters", "preserving negation and uncertainty",
                "without asserting that it occurred or claiming firsthand observation",
                "A brief general caution is allowed", "do not add unsupported locations, directions, distances",
                "timing, or other details", "or give specific driving instructions",
            ),
        }
        for field, phrases in expected_phrases.items():
            with self.subTest(field=field):
                property_schema = parameters["properties"][field]
                self.assertEqual("string", property_schema["type"])
                description = property_schema["description"]
                self.assertTrue(description.isascii())
                self.assertRegex(description, r"^[A-Z].*[.!?]$")
                self.assertIn(len(re.findall(r"[.!?](?:\s+|$)", description)), (1, 2))
                self.assertNotRegex(description.lower(), r"\b(?:examples?|e\.g\.|for instance)\b")
                for phrase in phrases:
                    self.assertIn(phrase, description)


if __name__ == "__main__":
    unittest.main()
