"""Runs the instrumented tests on the attached phone without wiping the app's data.

`./gradlew connectedDebugAndroidTest` uninstalls the debug app when it is done, and with it
everything stored in it. This script installs over the existing app instead and only removes
the test app afterwards.

    python scripts/device-test.py                                  # all instrumented tests
    python scripts/device-test.py com.wob.workout.player.PlayerViewModelTest   # one class
"""
import sys

from android_tools import (REPO, TEST_PACKAGE, adb, adb_text, gradle, install,
                           require_one_device)


def main():
    test_class = sys.argv[1] if len(sys.argv) > 1 else None

    gradle("assembleDebug", "assembleDebugAndroidTest")
    require_one_device()

    outputs = REPO / "app" / "build" / "outputs" / "apk"
    install(outputs / "debug" / "app-debug.apk")
    install(outputs / "androidTest" / "debug" / "app-debug-androidTest.apk")

    command = ["shell", "am", "instrument", "-w"]
    if test_class:
        command += ["-e", "class", test_class]
    command.append(TEST_PACKAGE + "/androidx.test.runner.AndroidJUnitRunner")
    try:
        output = adb_text(*command, check=False)
    finally:
        adb("uninstall", TEST_PACKAGE, check=False)

    print(output)
    passed = "OK (" in output and "FAILURES" not in output
    sys.exit(0 if passed else 1)


if __name__ == "__main__":
    main()
