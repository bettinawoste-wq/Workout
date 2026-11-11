"""Renews the README screenshots in docs/screenshots from a release build on the attached phone.

Installs the release build, so the starter set appears as a user sees it, and runs the starter
workout twice: once aborted, once to the end, so the history shows both. Takes about two
minutes. The phone must stay unlocked. The release app is uninstalled again at the end, so
the script refuses to run while a release build is already installed: its data would be lost.
The status bar is painted over, so no personal notifications end up in the repo.

    python scripts/readme-screenshots.py
"""
import time

from PIL import Image

from android_tools import (REPO, RELEASE_PACKAGE, adb, centre, density, fail, find_all,
                           gradle, install, is_installed, is_locked, key, require_one_device,
                           screenshot, tap, tap_view, ui_nodes)

SHOTS = REPO / "docs" / "screenshots"
PKG = RELEASE_PACKAGE
COUNTDOWN_SECONDS = 3
STATUS_BAR_DP = 40


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
    for y in range(height + 1):
        colour = image.getpixel((x, y)) if y < height else None
        if run_colour is None or colour is None or differs(colour, run_colour, 12):
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


def save_shot(path):
    """A screenshot with the status bar painted in the colour just below it."""
    screenshot(path)
    image = Image.open(path).convert("RGB")
    bar = int(STATUS_BAR_DP * density())
    image.paste(image.getpixel((2, bar + 2)), (0, 0, image.width, bar))
    image.save(path)


def seconds_of(timer_text):
    minutes, seconds = timer_text.split(":")
    return int(minutes) * 60 + int(seconds)


def paused_player():
    """While paused, the player is idle and uiautomator can read it."""
    nodes = find_all(PKG, "btnPause", "btnNext", "btnCancel", "txtProgress", "txtTimer")
    return {name: centre(node) for name, node in nodes.items()}, nodes


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
    return paused_player()


def main():
    require_one_device()
    if is_installed(PKG):
        fail(PKG + " is installed; this script uninstalls it at the end, which would delete "
             "its data. Uninstall it yourself first if that is fine.")
    gradle("assembleRelease")
    if is_locked():
        fail("the phone is locked; unlock it and keep it awake while this runs")
    install(REPO / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk")
    probe = REPO / "app" / "build" / "screenshot-probe.png"

    try:
        adb("shell", "am", "start", "-W", "-n", PKG + "/com.wob.workout.MainActivity")
        time.sleep(2.5)
        save_shot(SHOTS / "home.png")

        tap_view(PKG, "txtName")
        time.sleep(2)
        save_shot(SHOTS / "workout-edit.png")
        key("BACK")
        time.sleep(1.5)

        # First run: abort it, so the history shows an aborted session too.
        buttons, _ = start_and_pause_first_exercise(probe)
        tap(*buttons["btnCancel"])
        time.sleep(1)
        tap_view(PKG, text="Abbrechen")
        time.sleep(2)

        # Second run: step to the second exercise for the player shot, then on to the last
        # and let it run out.
        buttons, nodes = start_and_pause_first_exercise(probe)
        exercises = int(nodes["txtProgress"].get("text").split("/")[1])
        tap(*buttons["btnPause"])
        tap(*buttons["btnNext"])
        time.sleep(1.5)
        save_shot(SHOTS / "player.png")
        for _ in range(exercises - 2):
            tap(*buttons["btnNext"])
            time.sleep(0.5)
        tap(*buttons["btnPause"])
        time.sleep(1)
        _, last = paused_player()
        left = seconds_of(last["txtTimer"].get("text"))
        tap(*buttons["btnPause"])
        print("waiting %d s for the last exercise to run out" % left)
        time.sleep(left)
        wait_for_text("GESAMTZEIT", 15)
        tap_view(PKG, "btnCancel")
        time.sleep(1.5)

        tap_view(PKG, "btnHistory")
        time.sleep(2)
        save_shot(SHOTS / "history.png")
    finally:
        adb("uninstall", PKG, check=False)
        probe.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
