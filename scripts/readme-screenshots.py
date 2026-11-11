"""Renews the README screenshots in docs/screenshots from a release build on the attached phone.

Installs the release build, so the starter set appears as a user sees it, and runs the starter
workout twice: once aborted, once to the end, so the history shows both. Takes about two
minutes. The phone must stay unlocked. The release app is uninstalled again at the end.

    python scripts/readme-screenshots.py
"""
import time

from PIL import Image

from android_tools import (REPO, RELEASE_PACKAGE, adb, fail, find, gradle, install, is_locked,
                           key, require_one_device, screenshot, tap, tap_view, ui_nodes)

SHOTS = REPO / "docs" / "screenshots"
PKG = RELEASE_PACKAGE
COUNTDOWN_SECONDS = 3
EXERCISE_SECONDS = 60


def pause_button_centre(image_path):
    """While the player's timer runs, uiautomator sees nothing, so find the large round pause
    button in the screenshot: the longest run of one colour down the centre column that is
    about as tall as the button and coloured, unlike the greys of background and pictures."""
    image = Image.open(image_path).convert("RGB")
    width, height = image.size
    x = width // 2

    def differs(a, b, by):
        return max(abs(p - q) for p, q in zip(a, b)) > by

    best, run_start, run_colour = None, 0, None
    for y in range(height):
        colour = image.getpixel((x, y))
        if run_colour is None or differs(colour, run_colour, 12):
            if run_colour is not None:
                length = y - run_start
                coloured = max(run_colour) - min(run_colour) > 40
                if (width * 0.12 < length < width * 0.3 and coloured
                        and (best is None or length > best[1])):
                    best = (run_start, length)
            run_start, run_colour = y, colour
    if best is None:
        fail("pause button not found in " + str(image_path))
    return x, best[0] + best[1] // 2


def wait_for_text(text, seconds):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if any(node.get("text") == text for node in ui_nodes(attempts=1)):
            return
        time.sleep(2)
    fail("'%s' did not appear; a tap probably missed its button" % text)


def start_and_pause_first_exercise(probe):
    """Starts the first recent workout and pauses it once the first exercise runs. Returns the
    positions of the transport buttons, which only uiautomator can read while paused."""
    tap_view(PKG, "btnStart")
    time.sleep(COUNTDOWN_SECONDS + 1.5)
    screenshot(probe)
    tap(*pause_button_centre(probe))
    time.sleep(1)
    return {name: find(PKG, name) for name in ("btnPause", "btnNext", "btnCancel")}


def main():
    require_one_device()
    if is_locked():
        fail("the phone is locked; unlock it and keep it awake while this runs")
    gradle("assembleRelease")
    install(REPO / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk")
    probe = REPO / "app" / "build" / "screenshot-probe.png"

    try:
        adb("shell", "am", "start", "-W", "-n", PKG + "/com.wob.workout.MainActivity")
        time.sleep(2.5)
        screenshot(SHOTS / "home.png")

        tap_view(PKG, "txtName")
        time.sleep(2)
        screenshot(SHOTS / "workout-edit.png")
        key("BACK")
        time.sleep(1.5)

        # First run: abort it, so the history shows an aborted session too.
        buttons = start_and_pause_first_exercise(probe)
        tap(*buttons["btnCancel"])
        time.sleep(1)
        tap_view(PKG, text="Abbrechen")
        time.sleep(2)

        # Second run: step to the second exercise for the player shot, then on to the last
        # and let it run out.
        buttons = start_and_pause_first_exercise(probe)
        tap(*buttons["btnPause"])
        tap(*buttons["btnNext"])
        time.sleep(1.5)
        screenshot(SHOTS / "player.png")
        for _ in range(8):
            tap(*buttons["btnNext"])
            time.sleep(0.5)
        print("waiting for the last exercise to run out")
        time.sleep(EXERCISE_SECONDS)
        wait_for_text("GESAMTZEIT", 15)
        tap_view(PKG, "btnCancel")
        time.sleep(1.5)

        tap_view(PKG, "btnHistory")
        time.sleep(2)
        screenshot(SHOTS / "history.png")
    finally:
        adb("uninstall", PKG, check=False)
        probe.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
