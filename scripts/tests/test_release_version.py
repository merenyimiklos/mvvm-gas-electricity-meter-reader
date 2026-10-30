"""Pure version resolution tests; do not call Google Play or GitHub."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_version import Version, next_code, next_version


class ReleaseVersionTests(unittest.TestCase):
    def setUp(self):
        self.releases = [
            {"tag_name": "v1.0.0", "body": "- Version code: 20"},
            {"tag_name": "v1.0.1", "body": "- Version code: 21"},
            {"tag_name": "v1.2.0-beta.19", "prerelease": True, "body": "- Version code: 22"},
        ]
        self.tracks = [{"track": "production", "releases": [
            {"name": "Mérőóra Napló 1.0.1", "versionCodes": ["21"]}
        ]}]

    def test_minor_release_increments_1_0_1_to_1_1_0(self):
        result = next_version(self.releases, self.tracks, Version(1, 0, 0), "minor")
        self.assertEqual("1.1.0", str(result))

    def test_patch_release_increments_patch(self):
        result = next_version(self.releases, self.tracks, Version(1, 0, 0), "patch")
        self.assertEqual("1.0.2", str(result))

    def test_major_release_resets_minor_and_patch(self):
        result = next_version(self.releases, self.tracks, Version(1, 0, 0), "major")
        self.assertEqual("2.0.0", str(result))

    def test_ignores_beta_and_draft_tags_for_stable_version(self):
        releases = self.releases + [{"tag_name": "v10.0.0", "draft": True}]
        result = next_version(releases, self.tracks, Version(1, 0, 0), "minor")
        self.assertEqual("1.1.0", str(result))

    def test_uses_production_name_when_github_release_missing(self):
        result = next_version([], self.tracks, Version(1, 0, 0), "patch")
        self.assertEqual("1.0.2", str(result))

    def test_highest_code_across_all_play_tracks_and_uploaded_bundles(self):
        play = {
            "bundles": [{"versionCode": 25}],
            "apks": [{"versionCode": 26}],
            "tracks": self.tracks + [{"track": "closed-beta", "releases": [
                {"name": "Beta", "versionCodes": ["29"]}
            ]}],
        }
        self.assertEqual(30, next_code(self.releases, play, 20))

    def test_reads_previous_release_codes_even_when_not_in_play(self):
        self.assertEqual(23, next_code(self.releases, None, 20))

    def test_wont_reuse_default_code_when_no_previous_release(self):
        self.assertEqual(21, next_code([], None, 20))

    def test_google_play_upper_bound(self):
        with self.assertRaises(ValueError):
            next_code([], {"bundles": [{"versionCode": 2_100_000_000}]}, 20)

    def test_rejects_invalid_version(self):
        self.assertIsNone(Version.parse("1.0"))
        self.assertIsNone(Version.parse("v1.2.3-beta.1"))
        self.assertEqual(Version(1, 2, 3), Version.parse("v1.2.3"))


if __name__ == "__main__":
    unittest.main()
