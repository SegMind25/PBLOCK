# PBLOCK Scripts - NSFW Content Blocking for Android

These scripts help you block NSFW/adult content on your Android phone. Choose the method that works best for your device.

## Quick Comparison

| Method | Root Required | Computer Required | Android Version | Blocks All Apps |
|--------|:---:|:---:|:---:|:---:|
| Private DNS (recommended) | No | No* | 9+ (Pie) | Yes |
| ADB Hosts File | No | Yes | Any | Yes |
| PBLOCK App | Yes | No | 5+ (Lollipop) | Yes |

\* Can be set up manually on phone, or via ADB from a computer.

---

## Method 1: Private DNS (Recommended - No Root Needed)

**Best for:** Most users with Android 9 or higher. No root or computer needed.

### Option A: Set Up Manually on Your Phone

1. Open **Settings** on your phone
2. Go to **Network & Internet** (or **Connections** on Samsung)
3. Tap **Private DNS** (or **More connection settings** → **Private DNS**)
4. Select **Private DNS provider hostname**
5. Enter one of these hostnames:

   | Provider | Hostname | What it blocks |
   |----------|----------|----------------|
   | **CleanBrowsing** (recommended) | `family-filter-dns.cleanbrowsing.org` | Adult content, mixed content, malware |
   | **AdGuard Family** | `dns.adguard-dns.com` | Adult content, ads, trackers |
   | **Cloudflare Families** | `family.cloudflare-dns.com` | Adult content, malware |

6. Tap **Save**

That's it! All adult websites will be blocked on every app and browser.

### Option B: Set Up Using ADB (from Computer)

```bash
# Connect your phone via USB with USB Debugging enabled
cd scripts/
chmod +x setup_private_dns.sh
./setup_private_dns.sh
```

The script will guide you through selecting a DNS provider.

### How to Verify It Works

1. Open your browser
2. Try visiting any adult website — it should fail to load
3. Search on Google — SafeSearch should be enforced automatically

---

## Method 2: ADB Hosts File (No Root Needed, Requires Computer)

**Best for:** Users who want hosts-file-level blocking and have access to a computer.

### Prerequisites

1. **Install ADB on your computer:**
   - **Windows:** Download [Platform Tools](https://developer.android.com/studio/releases/platform-tools), extract, and add to PATH
   - **macOS:** `brew install android-platform-tools`
   - **Linux:** `sudo apt install adb`

2. **Enable USB Debugging on your phone:**
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times to enable Developer Options
   - Go to **Settings** → **Developer Options**
   - Enable **USB Debugging**

3. **Connect your phone via USB cable**

### Usage

```bash
cd scripts/
chmod +x block_nsfw_adb.sh
./block_nsfw_adb.sh
```

The script will show you a menu:
1. **Block NSFW content** — Pushes the blocking hosts file to your phone
2. **Remove NSFW blocking** — Restores the default hosts file
3. **Check status** — Shows if blocking is active
4. **Backup hosts** — Saves your current hosts file

### What Gets Blocked

The `nsfw_hosts.txt` file blocks **200+ adult domains** including:
- Major adult video sites
- Cam/live streaming sites
- OnlyFans and similar platforms
- Adult image boards and forums
- Hentai/anime adult sites
- Adult content aggregators

It also:
- **Forces Google SafeSearch** on all Google domains
- **Forces Bing strict mode** on all Bing domains
- **Blocks search engines** that don't support SafeSearch (DuckDuckGo, Yandex, etc.)

---

## Method 3: PBLOCK App (Root Required)

If your phone is rooted, you can use the PBLOCK Android app directly. See the main [README.md](../README.md) for build instructions.

---

## Files in This Folder

| File | Description |
|------|-------------|
| `nsfw_hosts.txt` | Hosts file with 200+ blocked NSFW domains + SafeSearch enforcement |
| `block_nsfw_adb.sh` | Script to push/remove hosts file via ADB |
| `setup_private_dns.sh` | Script to configure Private DNS on Android 9+ |
| `README.md` | This file — usage instructions |

---

## Troubleshooting

### "Private DNS" option not found
- Your phone may be running Android 8 or older. Use Method 2 (ADB) instead.
- On Samsung phones, look under **Connections** → **More connection settings**.

### ADB says "no devices found"
- Make sure USB Debugging is enabled
- Try a different USB cable (some cables are charge-only)
- Accept the "Allow USB debugging?" prompt on your phone
- On Windows, install [Google USB drivers](https://developer.android.com/studio/run/win-usb)

### Blocking stopped working
- **Private DNS:** Check Settings → Network → Private DNS — it may have been reset after a system update
- **Hosts file:** The hosts file may have been overwritten by a system update. Re-run `block_nsfw_adb.sh`

### Some sites still load
- Clear your browser cache: Settings → Apps → Chrome → Storage → Clear Cache
- Toggle airplane mode on and off
- Restart your phone
- Some sites use CDNs or alternate domains not in the block list. Open an issue on GitHub to report them.

---

## Adding Custom Domains

To block additional domains, edit `nsfw_hosts.txt` and add entries in this format:

```
127.0.0.1 example-site.com
127.0.0.1 www.example-site.com
::1 example-site.com
::1 www.example-site.com
```

Then re-run `block_nsfw_adb.sh` to push the updated file to your phone.
