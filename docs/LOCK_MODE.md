# PBLOCK Lock Mode: blocking that only a factory reset can remove

Lock mode makes PBLOCK the phone's **device owner**, the same mechanism companies use
to manage work phones. Once it is on:

| What | Result |
|------|--------|
| Uninstall PBLOCK | Blocked. The Uninstall button is greyed out. |
| Turn the VPN filter off | Blocked. VPN settings are locked, and the filter starts by itself on boot. |
| Switch to another VPN app | Blocked |
| Force stop or clear PBLOCK's data (Android 11+) | Blocked |
| Safe mode (which skips the filter) | Blocked |
| Custom Private DNS (Android 10+) | Blocked |
| New user, guest or work profile | Blocked |
| Browser "Secure DNS" bypass | The resolvers are blocked, so browsers fall back to the filter |
| In-app "Disable" buttons and puzzles | Refused |
| **Factory reset** | **Still works. It is the only way out.** |

What lock mode deliberately leaves alone:
- **Factory reset**, so you can never get permanently stuck.
- **Installing and updating apps.**
- **VPN lockdown.** If the filter ever crashes, internet keeps working; the filter
  never cuts your internet off.

> ⚠️ A factory reset **erases everything** on the phone. Before you start, back up
> your photos and chats, and make sure you know your Google account password (it is
> needed after a reset).

---

## Step 1: Install and turn on blocking

(New to PBLOCK? Do [INSTALL.md](INSTALL.md) steps 1–5 first. It covers the "App blocked" warning and restart-proofing.)

1. Download `PBLOCK-x.y.z.apk` from the repo's **Releases** page on your phone and
   open it. Allow "install unknown apps" if asked.
2. Open PBLOCK, **set a password**, then tap **Enable VPN blocking** and accept the
   VPN prompt.
3. Check that a blocked site no longer loads in your browser.

## Step 2: Prepare the phone (temporary)

Android only accepts a device owner when the phone has **no accounts and no extra
users**:

1. **Settings → Accounts / Passwords & accounts →** remove **every** account: Google,
   Samsung, Xiaomi, WhatsApp backup, etc. Nothing is deleted from the cloud. You add
   them back in Step 4.
2. Remove any **Secure Folder** (Samsung), **Second space** or **Dual apps**
   (Xiaomi), **work profile**, or **guest/other users**.
3. Turn on developer mode: **Settings → About phone →** tap **Build number** 7 times.
4. **Settings → Developer options →** turn on **USB debugging**. On Xiaomi, also
   turn on **USB debugging (Security settings)**.

## Step 3: Run one command from a computer

1. On a computer, install Android **platform-tools** (it contains `adb`):
   <https://developer.android.com/tools/releases/platform-tools>
2. Connect the phone by USB and tap **Allow** on the "Allow USB debugging?" prompt.
3. Run:

```bash
adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver
```

You should see `Success: Device owner set to package com.pblock.app`.

| Error | Fix |
|-------|-----|
| `already some accounts on the device` | An account is still there. Remove all of them (Step 2.1). |
| `already several users` / `already provisioned` | Remove Secure Folder, Second space, work profile or extra users (Step 2.2). |
| `Unknown admin` | PBLOCK isn't installed, or is an old version. Install the latest APK. |
| `no devices/emulators found` | USB debugging is off, or you didn't tap Allow on the phone. |

## Step 4: Finish

1. Open PBLOCK. The status should say **LOCK MODE: ON - permanent until factory
   reset**.
2. Add your accounts back (Settings → Accounts).
3. Turn **USB debugging** off again.

You're done. The filter now survives reboots, and PBLOCK can't be removed or switched
off.

## Updating PBLOCK later

Install a newer `PBLOCK-x.y.z.apk` from Releases over the top; your settings stay.
To publish a new release, change the number in `AndroidVersion/VERSION` (e.g. `1.2.0`) and push to `main`. GitHub builds the APK and creates release `v1.2.0`.
Every release is signed with the same key, so updates always install.

## Battery and data use

- One foreground notification: the VPN.
- No background timers in VPN mode; root mode re-checks the hosts file every 5 minutes.
- Only DNS lookups pass through the VPN. Your actual web traffic doesn't touch it,
  so speed and data use are unchanged.
- The release APK is shrunk with R8.
