# Install & set up PBLOCK on Android

Follow the steps in order. It takes about 10 minutes.

- [1. Download](#1-download)
- [2. Install (and get past "App blocked")](#2-install-and-get-past-app-blocked)
- [3. Turn on protection](#3-turn-on-protection)
- [4. Keep it on after a restart](#4-keep-it-on-after-a-restart)
- [5. Turn on Search Shield](#5-turn-on-search-shield)
- [6. Optional: lock it until factory reset](#6-optional-lock-it-until-factory-reset)
- [Troubleshooting](#troubleshooting)

---

## 1. Download

On your phone, open the **[Releases page](https://github.com/SegMind25/PBLOCK/releases/latest)** and download
`PBLOCK-x.y.z.apk` (the newest version).

## 2. Install (and get past "App blocked")

Open the downloaded APK, ideally from the **Files** app rather than straight from
Chrome. Allow **"Install unknown apps"** if Android asks.

### If you see "App blocked to protect your device"

This is a **false alarm**. PBLOCK isn't from the Play Store and it uses blocker
permissions: VPN, device admin and accessibility. Scanners flag that combination
in any app from outside the Play Store. Try these in order and stop at the first
one that works:

**A. Install anyway**
Tap **More details** (or the small ⌄ arrow), then **Install anyway**.

**B. Pause Play Protect while you install**
1. Play Store → your profile picture → **Play Protect** → ⚙ (top right).
2. Turn off **Scan apps with Play Protect**.
3. Install the APK.
4. **Turn the scan back on.** If it later warns about PBLOCK, choose **Keep app**.

**C. Turn off your phone brand's own blocker while you install, then turn it back on**

| Phone | Setting |
|-------|---------|
| Samsung | Settings → Security and privacy → **Auto Blocker** → Off |
| Xiaomi / Redmi / POCO | Settings → Apps → Manage apps → ⋮ → Settings → **Security check** → Off. Older MIUI: Security app → ⚙ → **Scan before installing** → Off |
| Oppo / Realme / OnePlus | Settings → Security → **Install apps safely** / **Security check** → Off |
| Vivo | i Manager → Security → **Install apps safely** → Off |

**D. Install from a computer with adb** (works almost always)
1. On the phone: **Settings → About phone** → tap **Build number** 7 times.
2. **Settings → Developer options** → turn on **USB debugging**. On Xiaomi, also turn on
   **Install via USB**.
3. On the computer, download Android **platform-tools**:
   <https://developer.android.com/tools/releases/platform-tools>
4. Put `PBLOCK-x.y.z.apk` in the platform-tools folder and connect the phone by USB.
   Tap **Allow** on the phone.
5. Run:

```bash
adb install -r PBLOCK-x.y.z.apk
```

You should see `Success`. To update later, run the same command with the new APK.
Your settings are kept.

## 3. Turn on protection

1. Open PBLOCK.
2. Under **Password**, type a password (8+ characters) and tap **Save password**.
3. Tap the big **Turn on protection** button and accept the VPN request.
4. Allow notifications. If asked to ignore battery optimisation, tap **Allow**.
5. The circle turns **green: "You're protected"**. Check that an adult site no longer
   opens.

## 4. Keep it on after a restart

Many phones (Xiaomi, Samsung, Oppo, Vivo, Huawei, …) stop apps from starting
after a reboot. Android's **Always-on VPN** makes the system itself start PBLOCK
every time the phone boots.

1. In PBLOCK tap **Keep it on after restart** → **Open VPN settings**.
2. Tap the **⚙ gear** next to PBLOCK.
3. Turn on **Always-on VPN**. Leave **Block connections without VPN** off.
4. Back in PBLOCK, tap **Keep it on after restart** → **Autostart**, then allow PBLOCK to
   auto-start / run in the background (if your phone has this).
5. Also check: Settings → Apps → PBLOCK → Battery → **Unrestricted** (or "No
   restrictions" / "Don't optimise").
6. **Restart the phone** and open PBLOCK. It should still be green.

If protection ever fails to start after a reboot, PBLOCK shows the notification
**"PBLOCK protection is OFF – tap to turn it back on"**.

## 5. Turn on Search Shield

When you search for adult content in Chrome, Google or another browser, Search Shield
closes the page and pops up **"⚠️ This is harmful for you"**. This includes searches for any adult platform on the blocklist (e.g. "spankbang", "chaturbate.com", "only fans").

1. In PBLOCK tap **Turn on Search Shield** → **Open settings**.
2. Find **PBLOCK Search Shield** (it may be under *Installed apps* or *Downloaded apps*)
   and switch it **ON**.
3. **If the switch is greyed out ("Restricted setting")** on Android 13 or newer:
   1. Settings → Apps → **PBLOCK** → **⋮** (top right) → **Allow restricted settings**.
      Confirm with your PIN.
   2. Go back to Accessibility and switch Search Shield on.
4. Test it: search for an adult term on Google. You should see the stop screen.

Nothing you type is stored or sent anywhere. The check runs only on your phone.

## 6. Optional: lock it until factory reset

Lock mode makes PBLOCK **impossible to uninstall or switch off**. Only a factory
reset removes it. It needs one command from a computer (the same adb setup as
step 2-D). Full guide: **[LOCK_MODE.md](LOCK_MODE.md)**.

Quick version:
1. Back up your photos and chats first.
2. Settings → Accounts: remove **all** accounts for now. Also remove Secure Folder,
   Second space, work profile and any extra users.
3. Run:

```bash
adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver
```

4. Add your accounts back and turn off USB debugging. PBLOCK shows **🔒 LOCKED**.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "App blocked to protect your device" | Step 2: A → B → C → D |
| "App not installed" / "conflicts with an existing package" | An older PBLOCK signed with a different key is installed. Uninstall it first (not possible in lock mode; there, install a newer version with `adb install -r`). |
| Protection is off after a restart | Do all of step 4, especially **Always-on VPN** and **Battery → Unrestricted** |
| Adult sites still open | Make sure the circle is green. In Chrome: Settings → Privacy and security → **Use secure DNS** → Off. Restart the browser. |
| Google search still shows explicit results | Turn on Search Shield (step 5) and switch Chrome's secure DNS off |
| Search Shield switch is greyed out | **Allow restricted settings** (step 5.3) |
| Search Shield stopped working | Some phones turn accessibility services off to save battery: set PBLOCK's battery to **Unrestricted** and switch the service on again |
| `adb: no devices found` | USB debugging is off, or you didn't tap **Allow** on the phone. Try another cable or USB port. |
| `Not allowed to set the device owner…accounts` | Remove every account (step 6.2) and try again |

Still stuck? Open an issue with your **phone brand and model**, your **Android version**,
and the **exact message** or a screenshot.
