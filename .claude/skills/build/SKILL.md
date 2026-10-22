---
name: build
description: Build PBLOCK for Linux (GTK3 GUI + .deb), Windows (MinGW cross-compiled .exe) and Android (Gradle APK). Use when asked to build, compile, package, or verify that any PBLOCK app still compiles.
---

# Build PBLOCK

Build only the platform(s) asked for; default to all three. Treat any compiler
warning in changed code as something to fix before committing.

## Linux GUI (`LinuxVersion/pblock-gui`)

```bash
sudo apt-get install -y g++ make pkg-config libgtk-3-dev fakeroot dpkg-dev   # once
make -C LinuxVersion/pblock-gui            # -> LinuxVersion/pblock-gui/pblock
make -C LinuxVersion/pblock-gui deb        # -> pblock_1.0_amd64.deb
```

The package installs a polkit policy (`polkit/`), so the menu entry launches via
`pkexec`. Check the maintainer scripts with `sh -n debian/postinst debian/prerm`.

Linux CLI: `g++ -std=c++17 -O2 -Wall -o pblock-cli LinuxVersion/mainPBLOCK.cpp -lcrypt`

## Windows GUI (`WindowsVersion/pblock-gui`)

```bash
sudo apt-get install -y g++-mingw-w64-x86-64 binutils-mingw-w64-x86-64       # once
make -C WindowsVersion/pblock-gui          # -> pblock-gui.exe (requests admin via manifest)
```

Windows CLI: `x86_64-w64-mingw32-g++ -std=c++17 -O2 -static -o pblock-cli.exe WindowsVersion/mainPBLOCK.cpp -ladvapi32`

## Android (`AndroidVersion`)

Needs JDK 17 and the Android SDK (`ANDROID_HOME` set, or `sdk.dir` in
`AndroidVersion/local.properties`).

```bash
cd AndroidVersion && ./gradlew assembleDebug   # -> app/build/outputs/apk/debug/app-debug.apk
```

If no SDK is available, say the Android build was not verified instead of
claiming it passes.

## After building

Run `make clean` in each GUI directory (or delete the outputs) so binaries
aren't committed; `.gitignore` covers the Linux outputs only.
