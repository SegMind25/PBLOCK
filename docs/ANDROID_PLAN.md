# PBLOCK for Android — Roadmap

Status as of November 2026. The app lives in `AndroidVersion/` (pure Java, minSdk 21,
targetSdk 35). It has two blocking modes:

- **VPN mode (no root):** `PBlockVpnService` runs a local VPN that only routes a fake
  DNS server. It answers blocked domains with `0.0.0.0`, forces SafeSearch on Google
  and Bing, and forwards everything else to 8.8.8.8 (falling back to 1.1.1.1).
- **Root mode:** writes a marked block section into `/system/etc/hosts`.
  `PBlockService` and `BootReceiver` re-apply it.

Uninstall protection comes from a device admin plus puzzle challenges.

## Fixed in this pass

| Area | Bug | Effect before the fix |
|------|-----|-----------------------|
| VPN DNS | Reply builder copied the whole 12-byte header and then wrote a second header | Every blocked or SafeSearch reply was malformed, so google.com and bing.com failed to resolve |
| VPN DNS | SafeSearch matching used suffixes (`*.google.com`) | Gmail, Play Store, Drive, Maps etc. were sent to the SafeSearch IP and broke |
| VPN IPv6 | IPv6 header missing 2 flow-label bytes, wrong payload length, zero UDP checksum | All IPv6 DNS replies were dropped by the kernel |
| VPN perf | Upstream lookups ran on the tun read thread (3 s timeout each) | One slow lookup stalled every DNS query on the phone |
| VPN perf | Blocklist match looped over the whole set for every query | O(n) per lookup; now O(labels) |
| Root mode | `BootHostsManager.runSu` read stdout before draining stderr | `su` could deadlock the protection thread |
| Build | `gradle.properties` hard-coded `org.gradle.java.home` | The build failed on any machine without that exact JDK path |
| Resources | Raw `#0F0F23` color literal for `windowBackground` | aapt2 can reject this; it now uses `@color/protectionBg` |
| Cleanup | Three unused countdown/stop methods in `MainActivity`; false-positive domain `xxxtentacion.org` | — |

The DNS reply bytes and IPv6 checksum were checked with a JVM test harness. A full
`./gradlew assembleDebug` still needs to run on a machine with the Android SDK.

## Phase 1 — Make it trustworthy (next 2–3 weeks)

1. **Get CI building the APK.** Add `.github/workflows/android.yml`
   (`actions/setup-java@v4` with JDK 17, `android-actions/setup-android`,
   `./gradlew assembleDebug lint`) and upload `PBLOCK.apk` as an artifact. Today nothing
   proves the app compiles.
2. **Add unit tests for the DNS code.** Move packet building and parsing out of
   `PBlockVpnService` into a plain-Java `DnsPacket` class and test it with JUnit:
   blocked A and AAAA queries, SafeSearch rewrites, compressed names, truncated
   packets, and the IPv6 checksum.
3. **Make the password do something.** Today it is required up front but never asked
   for again; unlocking is 10 puzzles plus a 30-second countdown. Pick one:
   - (a) ask for the password after the puzzles (recommended, and matches the Linux
     and Windows versions), or
   - (b) remove the password step.
4. **Single source of truth for su.** `MainActivity` and `BootHostsManager` each have
   their own copy of `runSu`/`readHostsViaSu`/`writeHostsViaSu`. Move them into one
   `RootShell` class.
5. **Handle DNS over TCP and long replies.** Replies larger than the MTU are
   currently dropped. Set the TC bit so clients retry over TCP, or keep the reply
   under 1,452 bytes.

## Phase 2 — Harder to bypass (1–2 months)

1. **Block DNS-over-HTTPS bypasses.** Chrome and Firefox "Secure DNS" and Android
   Private DNS skip the VPN's DNS. To counter this:
   - Block well-known DoH hosts (`dns.google`, `cloudflare-dns.com`,
     `mozilla.cloudflare-dns.com`, `doh.opendns.com`, …).
   - Show a warning when `Settings.Global private_dns_mode` is `hostname` and not a
     family filter.
2. **Always-on VPN + lockdown.** Guide the user to Settings → VPN → PBLOCK →
   *Always-on* and *Block connections without VPN*. The status screen already detects
   always-on; add a one-tap shortcut (`Settings.ACTION_VPN_SETTINGS`).
3. **Updatable blocklist.** The ~150 hard-coded domains are too few. Ship a bundled
   list (for example the StevenBlack "porn" extension, about 70k domains, stored in a
   trie or sorted array), plus an optional signed weekly update. Keep it working
   offline.
4. **Category DNS upstream option.** Let users forward to a family resolver
   (CleanBrowsing Family, Cloudflare 1.1.1.3) instead of 8.8.8.8. That gives broad
   coverage for free.
5. **Background-start limits.** On Android 10+ the activity launched from
   `onDisableRequested` can be blocked. Use a full-screen-intent notification as the
   fallback.

## Phase 3 — Polish and distribution (2–3 months)

1. **UI refresh:** Material 3, a dark theme, a single status card, and onboarding
   steps (password → VPN consent → always-on → device admin → battery exemption).
2. **Accessibility and translations:** content descriptions, strings moved out of
   Java into `strings.xml`, RTL check.
3. **Release pipeline:** a signed release build (keystore in CI secrets), R8 enabled,
   and versionCode bumps from tags.
4. **Store strategy.** Google Play restricts VPN apps and device-admin uninstall
   blocking, and requires a VpnService declaration form. The shortest route is F-Droid
   (fastlane metadata already exists in `fastlane/`) plus GitHub Releases. For Play:
   - remove the root console;
   - keep VPN use limited to on-device filtering;
   - fill in the "VpnService" and "Device admin" policy declarations.
5. **Optional accountability partner:** a partner gets the unlock code instead of
   the user, or receives a notification when protection is turned off.

## Out of scope / known limits

- A plain device admin cannot actually stop removal from Settings. Real uninstall
  blocking needs device-owner provisioning
  (`adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver`) on a
  freshly reset phone. Document this as an advanced option.
- Root hosts mode does not work on most modern phones: `/system` is read-only
  (dm-verity/EROFS). Magisk users should use its systemless-hosts module. VPN mode
  should stay the default.
