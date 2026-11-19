#!/usr/bin/env python3
"""Tiny UI driver for the emulator test: find a view by text / resource-id and tap it.

usage: ui.py tap  <text-or-id> [tries]   -> exit 0 if tapped
       ui.py has  <text-or-id> [tries]   -> exit 0 if visible
Matching is case-insensitive: exact text, resource-id suffix ":id/<name>",
then "text contains". Scrolls down between tries when not found.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True, timeout=60).stdout


def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml")
    start = xml.find("<?xml")
    if start < 0:
        return []
    try:
        root = ET.fromstring(xml[start:])
    except ET.ParseError:
        return []
    return list(root.iter("node"))


def find(nodes, key):
    k = key.lower()
    exact, by_id, contains = None, None, None
    for n in nodes:
        text = (n.get("text") or "").strip().lower()
        desc = (n.get("content-desc") or "").strip().lower()
        rid = (n.get("resource-id") or "").lower()
        if exact is None and (text == k or desc == k):
            exact = n
        if by_id is None and rid.endswith(":id/" + k):
            by_id = n
        if contains is None and k in text:
            contains = n
    return exact or by_id or contains


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def main():
    action, key = sys.argv[1], sys.argv[2]
    tries = int(sys.argv[3]) if len(sys.argv) > 3 else 6
    for i in range(tries):
        node = find(dump(), key)
        if node is not None:
            if action == "tap":
                x, y = center(node)
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(1.2)
            print(f"ui: {action} '{key}' OK")
            return 0
        if i % 2 == 1:
            adb("shell", "input", "swipe", "540", "1600", "540", "700", "300")
        time.sleep(1.5)
    print(f"ui: {action} '{key}' NOT FOUND")
    return 1


if __name__ == "__main__":
    sys.exit(main())
