#!/usr/bin/env python3
"""Scrolls the LibreCans dashboard (demo headphones) and reports frame statistics from gfxinfo.

usage: measure-scroll.py <package> [--serial S] [--swipes N] [--idle SECONDS]
"""
import argparse
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = os.path.join(os.environ.get("ANDROID_HOME", os.path.expanduser("~/Android/Sdk")), "platform-tools", "adb")


def adb(serial, *args, check=True):
    cmd = [ADB] + (["-s", serial] if serial else []) + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, check=check).stdout


def tap_text(serial, text, timeout=15):
    deadline = time.time() + timeout
    while time.time() < deadline:
        xml = adb(serial, "exec-out", "uiautomator", "dump", "/dev/tty", check=False)
        xml = xml[xml.find("<?xml"):xml.rfind(">") + 1]
        try:
            root = ET.fromstring(xml)
        except ET.ParseError:
            time.sleep(0.5)
            continue
        for node in root.iter("node"):
            if node.get("text") == text:
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
                adb(serial, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                return
        time.sleep(0.5)
    raise SystemExit(f"'{text}' not found on screen")


def screen_size(serial):
    w, h = map(int, re.search(r"(\d+)x(\d+)", adb(serial, "shell", "wm", "size")).groups())
    return w, h


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("package")
    parser.add_argument("--serial")
    parser.add_argument("--swipes", type=int, default=12)
    parser.add_argument("--idle", type=float, default=0, help="also measure N seconds of idle dashboard")
    parser.add_argument("--runs", type=int, default=1, help="repeat the scroll and report medians")
    args = parser.parse_args()
    s, pkg = args.serial, args.package

    adb(s, "shell", "am", "force-stop", pkg)
    adb(s, "shell", "pm", "grant", pkg, "android.permission.BLUETOOTH_CONNECT", check=False)
    adb(s, "shell", "am", "start", "-W", "-n", f"{pkg}/org.librecans.app.MainActivity")
    time.sleep(2)
    w, h = screen_size(s)
    adb(s, "shell", "input", "swipe", str(w // 2), str(h * 4 // 5), str(w // 2), str(h // 5), "200")
    time.sleep(1)
    tap_text(s, "Demo headphones")
    time.sleep(4)

    if args.idle:
        adb(s, "shell", "dumpsys", "gfxinfo", pkg, "reset")
        time.sleep(args.idle)
        print("== idle dashboard", args.idle, "s")
        report(adb(s, "shell", "dumpsys", "gfxinfo", pkg))

    runs = []
    for _ in range(args.runs):
        adb(s, "shell", "dumpsys", "gfxinfo", pkg, "reset")
        for i in range(args.swipes):
            y1, y2 = (h * 4 // 5, h // 4) if i % 2 == 0 else (h // 4, h * 4 // 5)
            adb(s, "shell", "input", "swipe", str(w // 2), str(y1), str(w // 2), str(y2), "300")
            time.sleep(0.6)
        runs.append(metrics(adb(s, "shell", "dumpsys", "gfxinfo", pkg)))
    print(f"== scroll, {args.swipes} swipes x {args.runs} runs (median)")
    for key in runs[0]:
        values = sorted(r[key] for r in runs)
        print(f"  {key}: {values[len(values) // 2]}   all={values}")


def metrics(gfx):
    def num(pattern):
        m = re.search(pattern, gfx)
        return float(m.group(1)) if m else -1
    return {
        "frames": num(r"Total frames rendered: (\d+)"),
        "jank %": num(r"\n\s*Janky frames: \d+ \(([\d.]+)%\)"),
        "legacy jank %": num(r"Janky frames \(legacy\): \d+ \(([\d.]+)%\)"),
        "p50 ms": num(r"\n\s*50th percentile: (\d+)ms"),
        "p90 ms": num(r"\n\s*90th percentile: (\d+)ms"),
        "p99 ms": num(r"\n\s*99th percentile: (\d+)ms"),
        "gpu p50 ms": num(r"50th gpu percentile: (\d+)ms"),
        "gpu p90 ms": num(r"90th gpu percentile: (\d+)ms"),
        "slow ui thread": num(r"Number Slow UI thread: (\d+)"),
    }


def report(gfx):
    keys = ["Total frames rendered", "Janky frames", "Janky frames (legacy)", "50th percentile",
            "90th percentile", "95th percentile", "99th percentile", "Number Missed Vsync",
            "Number Slow UI thread", "Number Slow bitmap uploads", "Number Slow issue draw commands",
            "50th gpu percentile", "90th gpu percentile"]
    for line in gfx.splitlines():
        line = line.strip()
        if any(line.startswith(k + ":") for k in keys):
            print("  " + line)
        if line.startswith("---PROFILEDATA"):
            break


if __name__ == "__main__":
    main()
