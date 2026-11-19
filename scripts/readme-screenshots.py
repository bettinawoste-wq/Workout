"""Renews the README screenshots in docs/screenshots from a release build on the attached phone.

Installs the release build, so the starter set appears as a user sees it, sets a weekly goal of
3 training days and adds a reminder (allowing its notifications), puts a rest before the starter workout's last exercise, and runs the workout
twice: once aborted, once to the end through the rest, so the history shows both, the detail
its rest time and the home screen the week's progress. Takes about
five minutes. The phone must stay unlocked. The release app is uninstalled again at the end, so
the script refuses to run while a release build is already installed: its data would be lost.
The status bar is painted over, so no personal notifications end up in the repo.

    python scripts/readme-screenshots.py
"""
import re
import time

from PIL import Image

from android_tools import (REPO, RELEASE_PACKAGE, adb, centre, density, fail, find_all,
                           gradle, install, is_installed, is_locked, key, require_one_device,
                           screen_size, screenshot, tap, tap_view, ui_nodes)

SHOTS = REPO / "docs" / "screenshots"
PKG = RELEASE_PACKAGE
COUNTDOWN_SECONDS = 3
WEEKLY_GOAL = 3
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


def drag(start, end, steps=10):
    """A long press on start, then a slow move to end, as a finger drags a list row."""
    adb("shell", "input", "motionevent", "DOWN", str(start[0]), str(start[1]))
    time.sleep(1)
    for step in range(1, steps + 1):
        y = start[1] + (end[1] - start[1]) * step // steps
        adb("shell", "input", "motionevent", "MOVE", str(start[0]), str(y))
        time.sleep(0.05)
    time.sleep(0.5)
    adb("shell", "input", "motionevent", "UP", str(start[0]), str(end[1]))


def add_rest_before_last_exercise():
    """In the open workout editor: adds a rest, which lands at the end, scrolls there and
    drags it above the last exercise, so that it runs."""
    tap_view(PKG, "btnAddRest")
    time.sleep(1)
    x1, y1, x2, y2 = map(int, re.findall(
        r"\d+", find_all(PKG, "listExercises")["listExercises"].get("bounds")))
    for _ in range(3):
        adb("shell", "input", "swipe", str((x1 + x2) // 2), str(y2 - 60),
            str((x1 + x2) // 2), str(y1 + 60), "300")
        time.sleep(0.8)
    titles = [node for node in ui_nodes()
              if node.get("resource-id") == PKG + ":id/txtTitle"]
    if len(titles) < 2 or titles[-1].get("text") != "Pause":
        fail("the added rest is not the last row of the editor")
    rest, last_exercise = centre(titles[-1]), centre(titles[-2])
    drag(rest, (rest[0], last_exercise[1] - 30))
    time.sleep(1)


def set_weekly_goal(days):
    """In the settings screen: taps the weekly goal's slider (0 to 7) until it shows days.
    uiautomator does not list sliders, so it is found 24 dp (half its height) below the row
    that shows the goal, across the screen's width less the 16 dp padding."""
    _, _, _, row_bottom = map(int, re.findall(
        r"\d+", find_all(PKG, "txtWeeklyGoal")["txtWeeklyGoal"].get("bounds")))
    width, _ = screen_size()
    padding = 16 * density()
    y = int(row_bottom + 24 * density())
    step = (width - 2 * padding) / 8
    x = padding + step / 2 + days * step
    for _ in range(5):
        tap(int(x), y)
        time.sleep(0.8)
        shown = find_all(PKG, "txtWeeklyGoal")["txtWeeklyGoal"].get("text")
        value = int(shown.split()[0]) if shown[0].isdigit() else 0
        if value == days:
            return
        x += (days - value) * step / 2
    fail("the weekly goal did not reach %d" % days)


def add_reminder():
    """In the settings screen: adds a reminder at its default time and weekdays and allows
    notifications when the app asks for them, so the screen shows no hint."""
    tap_view(PKG, "btnAddReminder")
    time.sleep(1.5)
    tap_view(PKG, text="Speichern")
    time.sleep(2)
    allow = [node for node in ui_nodes()
             if node.get("resource-id", "").endswith(":id/permission_allow_button")]
    if allow:
        tap(*centre(allow[0]))
        time.sleep(1.5)
    find_all(PKG, "txtReminderTime")


def start_and_pause_first_exercise(probe):
    """Starts the first recent workout and pauses it once the first exercise runs. Returns the
    positions of the transport buttons, which only uiautomator can read while paused."""
    tap_view(PKG, "btnStart")
    time.sleep(COUNTDOWN_SECONDS + 1.5)
    screenshot(probe)
    tap(*pause_button_centre(probe))
    time.sleep(1)
    return paused_player()


def run_out_current(buttons, what):
    """Reads the running exercise's time left while paused, then lets it run out."""
    tap(*buttons["btnPause"])
    time.sleep(1)
    _, nodes = paused_player()
    left = seconds_of(nodes["txtTimer"].get("text"))
    tap(*buttons["btnPause"])
    print("waiting %d s for %s to run out" % (left, what))
    time.sleep(left + 1)


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
        # An open notification shade would end up in the screenshots, notifications and all.
        adb("shell", "cmd", "statusbar", "collapse")
        adb("shell", "am", "start", "-W", "-n", PKG + "/com.wob.workout.MainActivity")
        time.sleep(2.5)

        tap_view(PKG, "fabSettings")
        time.sleep(2)
        set_weekly_goal(WEEKLY_GOAL)
        add_reminder()
        save_shot(SHOTS / "settings.png")
        key("BACK")
        time.sleep(1.5)

        tap_view(PKG, "txtName")
        time.sleep(2)
        add_rest_before_last_exercise()
        save_shot(SHOTS / "workout-edit.png")
        tap_view(PKG, "btnSave")
        time.sleep(1.5)

        # First run: abort it, so the history shows an aborted session too.
        buttons, _ = start_and_pause_first_exercise(probe)
        tap(*buttons["btnCancel"])
        time.sleep(1)
        tap_view(PKG, text="Abbrechen")
        time.sleep(2)

        # Second run: step to the exercise before the rest and let it run out, shoot the
        # player in the rest, then let the rest and the last exercise run out.
        buttons, nodes = start_and_pause_first_exercise(probe)
        exercises = int(nodes["txtProgress"].get("text").split("/")[1])
        tap(*buttons["btnPause"])
        for _ in range(exercises - 2):
            tap(*buttons["btnNext"])
            time.sleep(0.5)
        run_out_current(buttons, "the exercise before the rest")
        time.sleep(2.5)
        save_shot(SHOTS / "player.png")
        time.sleep(30)
        run_out_current(buttons, "the last exercise")
        wait_for_text("Fertig", 15)
        tap_view(PKG, "btnDone")
        time.sleep(1.5)
        find_all(PKG, "txtWeeklyProgress")
        save_shot(SHOTS / "home.png")

        tap_view(PKG, "btnHistory")
        time.sleep(2)
        save_shot(SHOTS / "history.png")
        tap_view(PKG, "txtName")
        time.sleep(2)
        save_shot(SHOTS / "history-detail.png")
    finally:
        adb("uninstall", PKG, check=False)
        probe.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
