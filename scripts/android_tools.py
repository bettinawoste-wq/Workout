"""Shared helpers for the scripts in this folder: Gradle, adb and the attached phone."""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
TEST_PACKAGE = "com.wob.workout.debug.test"
RELEASE_PACKAGE = "com.wob.workout"
ANDROID_STUDIO_JDK = Path(r"C:\Program Files\Android\Android Studio\jbr")


def fail(message):
    print("error: " + message, file=sys.stderr)
    sys.exit(1)


def sdk_dir():
    local = REPO / "local.properties"
    if local.exists():
        for line in local.read_text(encoding="utf-8").splitlines():
            if line.startswith("sdk.dir="):
                return Path(line.split("=", 1)[1].replace("\\:", ":").replace("\\\\", "\\"))
    for variable in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(variable):
            return Path(os.environ[variable])
    fail("no Android SDK: set sdk.dir in local.properties or ANDROID_HOME")


def adb_path():
    name = "adb.exe" if os.name == "nt" else "adb"
    return str(sdk_dir() / "platform-tools" / name)


def adb(*args, check=True):
    result = subprocess.run([adb_path(), *args], capture_output=True)
    if check and result.returncode != 0:
        fail("adb " + " ".join(args) + " failed: " + result.stderr.decode(errors="replace"))
    return result.stdout


def adb_text(*args, check=True):
    return adb(*args, check=check).decode("utf-8", "replace")


def gradle(*tasks):
    """Runs Gradle, falling back to Android Studio's JDK when no Java is configured."""
    env = dict(os.environ)
    user_props = Path.home() / ".gradle" / "gradle.properties"
    configured = user_props.exists() and "org.gradle.java.home" in user_props.read_text(
        encoding="utf-8", errors="replace")
    if "JAVA_HOME" not in env and not configured and ANDROID_STUDIO_JDK.exists():
        env["JAVA_HOME"] = str(ANDROID_STUDIO_JDK)
    wrapper = str(REPO / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    print("gradle " + " ".join(tasks))
    result = subprocess.run([wrapper, "--console=plain", "-q", *tasks], cwd=REPO, env=env)
    if result.returncode != 0:
        fail("gradle build failed")


def require_one_device():
    if os.environ.get("ANDROID_SERIAL"):
        return
    lines = adb_text("devices").strip().splitlines()[1:]
    devices = [line.split()[0] for line in lines if line.endswith("device")]
    if len(devices) != 1:
        fail("expected exactly one attached device, found %d; set ANDROID_SERIAL" % len(devices))


def is_locked():
    window = adb_text("shell", "dumpsys", "window")
    power = adb_text("shell", "dumpsys", "power")
    return "isKeyguardShowing=true" in window or "mWakefulness=Awake" not in power


def install(apk):
    adb("install", "-r", "-t", str(apk))


def is_installed(package):
    return ("package:" + package) in adb_text("shell", "pm", "list", "packages", package).split()


def screenshot(path):
    Path(path).write_bytes(adb("exec-out", "screencap", "-p"))
    print("saved " + str(path))


def density():
    match = re.search(r"(\d+)\s*$", adb_text("shell", "wm", "density").strip())
    return int(match.group(1)) / 160


def screen_size():
    match = re.search(r"(\d+)x(\d+)", adb_text("shell", "wm", "size"))
    return int(match.group(1)), int(match.group(2))


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def key(name):
    adb("shell", "input", "keyevent", name)


def ui_nodes(attempts=8):
    """The current screen's views. Fails while something animates, as in a running player."""
    for _ in range(attempts):
        text = adb_text("exec-out", "uiautomator", "dump", "/dev/tty", check=False)
        start, end = text.find("<?xml"), text.rfind("</hierarchy>")
        if start >= 0 and end >= 0:
            return list(ET.fromstring(text[start:end + len("</hierarchy>")]).iter("node"))
        time.sleep(0.4)
    return []


def centre(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def find_all(package, *resource_ids):
    """The views with these ids (in the app's package), read from a single dump."""
    found = {}
    for node in ui_nodes():
        resource_id = node.get("resource-id", "").rpartition(":id/")[2]
        if node.get("resource-id", "").startswith(package + ":id/") and resource_id in resource_ids:
            found.setdefault(resource_id, node)
    missing = [resource_id for resource_id in resource_ids if resource_id not in found]
    if missing:
        fail("not on screen: " + ", ".join(missing))
    return found


def find(package, resource_id=None, text=None):
    """Centre of the first view with this id (in the app's package) or this text."""
    if resource_id:
        return centre(find_all(package, resource_id)[resource_id])
    for node in ui_nodes():
        if node.get("text") == text:
            return centre(node)
    fail("not on screen: " + text)


def tap_view(package, resource_id=None, text=None):
    tap(*find(package, resource_id, text))
