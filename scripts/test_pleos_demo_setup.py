import subprocess
import unittest

from pleos_demo_setup import PACKAGE, main


class DemoSetupTest(unittest.TestCase):
    def setUp(self):
        self.output = []
        self.calls = []

    def run_fake(self, responses, *options):
        def fake(command):
            self.calls.append(command)
            result = next(responses)
            if isinstance(result, Exception):
                raise result
            return result

        return main(["--serial", "emulator-5556", *options], fake, self.output.append)

    def test_print_modes_never_call_runner(self):
        self.assertEqual(0, self.run_fake(iter(())))
        self.assertEqual(0, self.run_fake(iter(()), "--setup-commands"))
        self.assertEqual([], self.calls)
        output = "\n".join(self.output)
        self.assertIn('install -r --user "$pleos_user"', output)
        self.assertIn("forward --no-rebind", output)
        self.assertNotIn("appops set", output)

    def test_diagnostics_use_current_user_and_matching_forward(self):
        self.assertEqual(0, self.run_fake(iter(["23\n", f"package:{PACKAGE}\n",
            "MOCK_LOCATION: allow; time=+1s\n", "emulator-5556 tcp:8088 tcp:8088\n"]), "--check"))
        self.assertEqual([
            ["shell", "am", "get-current-user"],
            ["shell", "pm", "list", "packages", "--user", "23", PACKAGE],
            ["shell", "cmd", "appops", "get", "--user", "23", PACKAGE, "android:mock_location"],
            ["forward", "--list"],
        ], [command[3:] for command in self.calls])
        self.assertTrue(all(command[:3] == ["adb", "-s", "emulator-5556"] for command in self.calls))

    def test_missing_package_and_wrong_serial_forward_fail(self):
        self.assertEqual(1, self.run_fake(iter(["0", f"package:{PACKAGE}.other",
            "emulator-5554 tcp:8088 tcp:8088"]), "--check"))
        self.assertEqual(3, len(self.calls))
        self.assertIn("앱 설치 FAIL", "\n".join(self.output))

    def test_default_mock_mode_fails(self):
        self.assertEqual(1, self.run_fake(iter(["23", f"package:{PACKAGE}", "Default mode: deny",
            "emulator-5556 tcp:8088 tcp:8088"]), "--check"))
        self.assertIn("mock GPS FAIL", "\n".join(self.output))

    def test_unusable_current_user_stops_diagnostics(self):
        for response in ("23; echo 24", subprocess.TimeoutExpired("adb", 10)):
            with self.subTest(response=response):
                self.calls.clear()
                self.assertEqual(1, self.run_fake(iter([response]), "--check"))
                self.assertEqual(1, len(self.calls))
