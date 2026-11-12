"""Checks the repo's text files: every file git treats as text must be valid UTF-8, and no
Java or Kotlin-script line may be longer than 100 characters. There is no way to exempt a line.

Run from anywhere inside the repo; CI runs it before Gradle.

    python scripts/check-text-files.py

Prints one `path:line: message` per violation and exits non-zero if there is any.
"""
import subprocess
import sys

MAX_WIDTH = 100
WIDTH_CHECKED_SUFFIXES = (".java", ".kts")


def git(*args, ok_codes=(0,)):
    result = subprocess.run(["git", *args], capture_output=True)
    if result.returncode not in ok_codes:
        sys.exit(f"git {' '.join(args)} failed: {result.stderr.decode(errors='replace')}")
    return result.stdout


def text_files():
    """Tracked files that git does not consider binary, relative to the repo root."""
    # -I skips binary files; '' matches every non-empty text file. Exit code 1 means no match.
    output = git("grep", "-I", "-l", "-z", "--full-name", "-e", "", "--", ":/", ok_codes=(0, 1))
    return [path.decode("utf-8") for path in output.split(b"\0") if path]


def violations(path, content):
    check_width = path.endswith(WIDTH_CHECKED_SUFFIXES)
    for number, raw_line in enumerate(content.splitlines(), start=1):
        try:
            line = raw_line.decode("utf-8")
        except UnicodeDecodeError:
            yield f"{path}:{number}: not valid UTF-8"
            continue
        if check_width and len(line) > MAX_WIDTH:
            yield f"{path}:{number}: line is {len(line)} characters, max {MAX_WIDTH}"


def main():
    # Piped output on Windows would otherwise use the code page and fail on some paths.
    sys.stdout.reconfigure(encoding="utf-8")
    root = git("rev-parse", "--show-toplevel").decode("utf-8").strip()
    found = 0
    for path in text_files():
        with open(f"{root}/{path}", "rb") as file:
            content = file.read()
        for violation in violations(path, content):
            print(violation)
            found += 1
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
