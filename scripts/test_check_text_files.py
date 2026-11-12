"""Tests for check-text-files.py, run against a throwaway git repo.

    python -m unittest discover -s scripts -p 'test_*.py'
"""
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "check-text-files.py"


class CheckTextFilesTest(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self._tmp.name)
        self.git("init", "-q")

    def tearDown(self):
        self._tmp.cleanup()

    def git(self, *args):
        subprocess.run(["git", *args], cwd=self.repo, check=True)

    def add(self, path, content):
        file = self.repo / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(content if isinstance(content, bytes) else content.encode("utf-8"))
        self.git("add", path)

    def check(self):
        # Without these, Python picks the platform's encoding for piped output, as by hand.
        env = {k: v for k, v in os.environ.items()
               if k not in ("PYTHONIOENCODING", "PYTHONUTF8")}
        result = subprocess.run([sys.executable, str(SCRIPT)], cwd=self.repo, env=env,
                                capture_output=True, text=True, encoding="utf-8")
        return result.returncode, result.stdout.splitlines()

    def test_clean_repo_passes(self):
        self.add("README.md", "# Übungen\n")
        self.add("app/Main.java", "class Main {}\n")

        self.assertEqual(self.check(), (0, []))

    def test_text_file_that_is_not_utf8_fails_naming_file_and_line(self):
        self.add("CONTEXT.md", "# Context\n\nfür\n".encode("cp1252"))

        code, lines = self.check()

        self.assertNotEqual(code, 0)
        self.assertEqual(lines, ["CONTEXT.md:3: not valid UTF-8"])

    def test_binary_file_is_not_checked_for_utf8(self):
        self.add("image.png", b"\x89PNG\r\n\x1a\n\x00\x00\xff\xfe")

        self.assertEqual(self.check(), (0, []))

    def test_java_line_over_100_characters_fails(self):
        ok = "x" * 100
        too_long = "y" * 101
        self.add("app/Main.java", f"{ok}\n{too_long}\n")

        code, lines = self.check()

        self.assertNotEqual(code, 0)
        self.assertEqual(lines, ["app/Main.java:2: line is 101 characters, max 100"])

    def test_kts_line_over_100_characters_fails(self):
        self.add("build.gradle.kts", "z" * 120 + "\n")

        code, lines = self.check()

        self.assertNotEqual(code, 0)
        self.assertEqual(lines, ["build.gradle.kts:1: line is 120 characters, max 100"])

    def test_width_counts_characters_not_bytes(self):
        self.add("app/Main.java", "ü" * 100 + "\r\n")

        self.assertEqual(self.check(), (0, []))

    def test_width_is_not_checked_in_other_files(self):
        long_line = "w" * 150 + "\n"
        self.add("README.md", long_line)
        self.add("app/src/main/res/layout/main.xml", long_line)
        self.add("app/schemas/1.json", long_line)

        self.assertEqual(self.check(), (0, []))

    def test_non_ascii_path_is_printed_as_utf8(self):
        # A path the Windows console code page cannot encode.
        self.add("Übung✓.java", "v" * 101 + "\n")

        code, lines = self.check()

        self.assertNotEqual(code, 0)
        self.assertEqual(lines, ["Übung✓.java:1: line is 101 characters, max 100"])

    def test_comment_cannot_exempt_a_line(self):
        self.add("app/Main.java", "q" * 101 + " // NOSONAR noqa checkstyle:off\n")

        code, _ = self.check()

        self.assertNotEqual(code, 0)


if __name__ == "__main__":
    unittest.main()
