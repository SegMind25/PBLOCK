<p align="center">
  <img src="AndroidVersion/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="PBLOCK Icon" width="120" />
</p>

<h1 align="center">PBLOCK - Content Blocker</h1>

<p align="center">
A self-accountability content blocking tool that prevents access to adult websites by modifying the system hosts file. Available for <b>Linux</b>, <b>Windows</b>, and <b>Android</b>.
</p>

## Project Structure

```
PBLOCK/
├── README.md
├── .gitignore
│
├── LinuxVersion/
│   └── mainPBLOCK.cpp           # Linux CLI tool (C++)
│
├── WindowsVersion/
│   └── mainPBLOCK.cpp           # Windows CLI tool (C++)
│
├── AndroidVersion/              # Android app (Java + C++ JNI)
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── setup.sh
│   ├── app/
│   │   ├── build.gradle
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── cpp/
│   │       │   ├── CMakeLists.txt
│   │       │   └── mainPBLOCK.cpp       # Native JNI code
│   │       ├── java/com/pblock/app/
│   │       │   └── MainActivity.java    # Android UI + logic
│   │       └── res/
│   │           ├── layout/activity_main.xml
│   │           └── values/
│   │               ├── strings.xml
│   │               └── colors.xml
│   └── gradle/wrapper/
│
└── scripts/                     # NSFW blocking scripts (no root needed)
    ├── README.md                # Detailed usage instructions
    ├── nsfw_hosts.txt           # Hosts file with 200+ blocked domains
    ├── block_nsfw_adb.sh        # ADB-based hosts file push script
    └── setup_private_dns.sh     # Private DNS setup (Android 9+, no root)
```

## How It Works

1. **Set a password** - You create a strong password (min 8 characters) that will be required to disable blocking
2. **Enable blocking** - The tool adds entries to your system's hosts file, redirecting blocked domains to `127.0.0.1`
3. **Intentional delay** - When you try to disable blocking, there is a mandatory 30-second wait period to help you reconsider
4. **Password required** - After the delay, you must enter your password to remove the blocks

The hosts file entries are wrapped with `# CONTENT_BLOCKER_START` and `# CONTENT_BLOCKER_END` markers for clean insertion and removal.

## Features

- System-level domain blocking via hosts file modification
- Password-protected unblocking with SHA-256 hashing
- 30-second intentional delay before unblocking (time to reconsider)
- Status display showing active/inactive state and domain count
- Cross-platform: Linux, Windows, and Android
- **NSFW blocking scripts** for non-rooted Android phones (no root needed!)

## Android NSFW Blocking (No Root Required)

**Don't have a rooted phone?** Use the scripts in the [`scripts/`](scripts/) folder to block NSFW content without root access.

### Quick Start: Private DNS (Easiest Method)

Set this up directly on your phone — no computer needed:

1. Open **Settings** → **Network & Internet** → **Private DNS**
2. Select **Private DNS provider hostname**
3. Enter: `family-filter-dns.cleanbrowsing.org`
4. Tap **Save**

All adult content is now blocked on every app and browser. See [`scripts/README.md`](scripts/README.md) for more options.

### Other Methods

| Method | Root | Computer | Details |
|--------|:---:|:---:|---------|
| **Private DNS** | No | No | Set a family-safe DNS on your phone. [Guide](scripts/README.md#method-1-private-dns-recommended---no-root-needed) |
| **ADB Hosts File** | No | Yes | Push a blocking hosts file via USB. [Guide](scripts/README.md#method-2-adb-hosts-file-no-root-needed-requires-computer) |
| **PBLOCK App** | Yes | No | Full app with password protection. See below. |

## Technology Stack

- **C++17** - Core blocking logic (all platforms)
- **JNI (Java Native Interface)** - Android native integration
- **Java** - Android app UI and system interaction
- **Android SDK 35** - Android build target
- **CMake 3.22** - Native build system for Android
- **Gradle 8.x** - Android build tool

## Prerequisites

### Linux
- **g++** with C++17 support
- **Root/sudo access** (required to modify `/etc/hosts`)

### Windows
- **MSVC** or **MinGW** with C++17 support
- **Administrator privileges** (required to modify the hosts file)

### Android (App - requires root)
- **Android Studio** or Android SDK
- **NDK** (for native C++ compilation)
- **Rooted device** (required to modify `/system/etc/hosts`)

### Android (Scripts - no root required)
- **Android 9+** for Private DNS method (no other requirements)
- **ADB** for hosts file method (requires a computer with USB cable)

## Build & Run

### Linux
```bash
cd LinuxVersion
g++ -std=c++17 -o blocker mainPBLOCK.cpp -lcrypt -lpthread
sudo ./blocker setup    # Set password
sudo ./blocker block    # Enable blocking
sudo ./blocker unblock  # Disable blocking (requires password + 30s delay)
sudo ./blocker status   # Show current status
```

### Windows
Compile `WindowsVersion/mainPBLOCK.cpp` with MSVC or MinGW (requires linking against `advapi32`):
```bash
cl /EHsc /std:c++17 mainPBLOCK.cpp /link advapi32.lib
# Run as Administrator:
blocker.exe setup
blocker.exe block
blocker.exe unblock
blocker.exe status
```

### Android (App)
```bash
cd AndroidVersion
# Set your Android SDK path in local.properties
# Then build with Gradle:
chmod +x gradlew
./gradlew assembleDebug
# Or open the project in Android Studio
```

The APK will be generated at `AndroidVersion/app/build/outputs/apk/debug/app-debug.apk`.

To install on your phone:
1. Transfer the APK to your phone
2. Open it and tap **Install** (you may need to enable "Install from unknown sources")
3. Open PBLOCK and follow the on-screen instructions

### Android (Scripts - No Root)
```bash
cd scripts/

# Option 1: Set up Private DNS (recommended, Android 9+)
chmod +x setup_private_dns.sh
./setup_private_dns.sh

# Option 2: Push hosts file via ADB
chmod +x block_nsfw_adb.sh
./block_nsfw_adb.sh
```

See [`scripts/README.md`](scripts/README.md) for detailed instructions.

## Security Notes

1. **Run with appropriate privileges** - Linux requires `sudo`, Windows requires Administrator, Android requires root (for the app)
2. **Use a strong password** - Minimum 8 characters, write it down somewhere safe
3. **The 30-second delay is intentional** - It gives you time to reconsider before unblocking
4. **Hosts file modification** - This app modifies system files; back up your hosts file before first use
5. **Private DNS is the safest method** for non-rooted Android phones — it blocks content at the DNS level across all apps

## Contributing

Contributions are welcome. If you find security vulnerabilities, please report them responsibly.

## License

MIT License

## Support

For issues or questions:
- Open an issue on GitHub
- Email: modistotube2004@gmail.com
