import unittest
import xml.etree.ElementTree as ET
from verify_road_notes_artifacts import check_activity_do


class DrivingManifestTest(unittest.TestCase):
    def manifest(self, activity='', application='', service=''):
        return ET.fromstring(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application>
            {application}<activity android:name=".MainActivity">{activity}</activity>
            <service android:name=".LocationTrackingService">{service}</service></application></manifest>''')

    metadata = '<meta-data android:name="distractionOptimized" android:value="true" />'

    def test_activity_child_true(self):
        check_activity_do(self.manifest(activity=self.metadata))

    def test_missing_or_false_or_duplicate(self):
        for value in ['', self.metadata.replace('true', 'false'), self.metadata * 2]:
            with self.subTest(value=value), self.assertRaises(AssertionError):
                check_activity_do(self.manifest(activity=value))

    def test_wrong_parent(self):
        for value in [self.manifest(application=self.metadata), self.manifest(service=self.metadata)]:
            with self.assertRaises(AssertionError):
                check_activity_do(value)

    def test_debug_only_cannot_hide_missing_release(self):
        check_activity_do(self.manifest(activity=self.metadata))
        with self.assertRaises(AssertionError):
            check_activity_do(self.manifest())


if __name__ == '__main__':
    unittest.main()
