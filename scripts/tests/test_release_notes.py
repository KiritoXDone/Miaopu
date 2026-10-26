from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

from scripts.release_notes import extract_release_notes


ROOT = Path(__file__).resolve().parents[2]


class ReleaseNotesTest(unittest.TestCase):
    def test_extracts_only_exact_version_and_preserves_subheadings(self):
        changelog = (
            "# 更新日志\n\n## Unreleased\n- future\n## 2.3.50\n- other\n"
            "## 2.3.5\n\n### 修复\n\n- 当前版本\n\n## 2.3.4\n- old\n"
        )
        self.assertEqual("### 修复\n\n- 当前版本\n", extract_release_notes(changelog, "2.3.5"))

    def test_supports_two_component_versions_and_crlf(self):
        self.assertEqual("- fix\n", extract_release_notes("## 2.3\r\n\r\n- fix\r\n", "2.3"))

    def test_missing_duplicate_and_empty_sections_fail(self):
        for changelog in (
            "## 2.3.4\n- old",
            "## 2.3.5\n- first\n## 2.3.5\n- second",
            "## 2.3.5\n \n## 2.3.4\n- old",
        ):
            with self.subTest(changelog=changelog), self.assertRaises(ValueError):
                extract_release_notes(changelog, "2.3.5")

    def test_rejects_invalid_versions(self):
        for version in ("", "v2.3.5", "2.3.5-rc1", "2.3.5\n", "2x3x5"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                extract_release_notes("## 2.3.5\n- fix", version)

    def test_fenced_headings_are_content_not_version_boundaries(self):
        for fence in ("```", "~~~~"):
            notes = f"- fix\n{fence}markdown\n## 2.3.5\n## 2.3.4\n{fence}\n- more"
            changelog = f"## 2.3.5\n{notes}\n## 2.3.4\n- old"
            with self.subTest(fence=fence):
                self.assertEqual(notes + "\n", extract_release_notes(changelog, "2.3.5"))

    def test_current_app_version_has_release_notes(self):
        build_file = (ROOT / "app/build.gradle.kts").read_text()
        version = re.search(r'versionName = "([^"]+)"', build_file).group(1)
        self.assertTrue(extract_release_notes((ROOT / "changelog.md").read_text(), version).strip())

    def test_cli_emits_notes_or_fails_without_a_partial_body(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "changelog.md"
            path.write_text("## 2.3.5\n- 当前版本\n", encoding="utf-8")
            command = [
                sys.executable, str(ROOT / "scripts/release_notes.py"),
                "2.3.5", "--changelog", str(path),
            ]
            success = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(0, success.returncode)
            self.assertEqual("- 当前版本\n", success.stdout)
            for content in ("## 2.3.4\n- old", "## 2.3.5\n"):
                path.write_text(content, encoding="utf-8")
                failure = subprocess.run(command, capture_output=True, text=True)
                self.assertNotEqual(0, failure.returncode)
                self.assertEqual("", failure.stdout)
                self.assertIn("Release notes error:", failure.stderr)
            path.unlink()
            missing = subprocess.run(command, capture_output=True, text=True)
            self.assertNotEqual(0, missing.returncode)
            self.assertEqual("", missing.stdout)


if __name__ == "__main__":
    unittest.main()
