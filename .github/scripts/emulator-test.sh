#!/usr/bin/env bash
# End-to-end test of PBLOCK on an Android emulator, including phone restarts.
# Runs inside reactivecircus/android-emulator-runner. Results -> results.md + job summary.
set -u
PKG=com.pblock.app
APK=AndroidVersion/app/build/outputs/apk/release/PBLOCK.apk
UI="python3 .github/scripts/ui.py"
OUT=emulator-results
mkdir -p "$OUT"
RESULTS="$OUT/results.md"
FAILS=0
echo "| # | Check | Result | Detail |" > "$RESULTS"
echo "|---|-------|--------|--------|" >> "$RESULTS"
N=0

record() { # status name detail
  N=$((N + 1))
  echo "| $N | $2 | $1 | ${3//|//} |" >> "$RESULTS"
  echo "==> [$1] $2 :: $3"
  [ "$1" = "❌ FAIL" ] && FAILS=$((FAILS + 1))
}
check() { # name detail command...
  local name=$1 detail=$2; shift 2
  if "$@"; then record "✅ PASS" "$name" "$detail"; else record "❌ FAIL" "$name" "$detail"; fi
}
shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null || true; }
sh_() { adb shell "$@" 2>&1 | tr -d '\r'; }

resolve() { # host -> IP as resolved on the device (via ping's first line)
  sh_ ping -c 1 -W 3 "$1" | head -1 | sed -n 's/.*(\([0-9a-f.:]*\)).*/\1/p'
}
vpn_up()      { sh_ ip addr show tun0 | grep -q 10.111.222.1; }
porn_blocked() {
  local ip; ip=$(resolve pornhub.com)
  echo "    pornhub.com -> '${ip:-unresolved}'"
  # 0.0.0.0 = PBLOCK VPN; unresolved / CleanBrowsing block page also count as blocked
  [ -z "$ip" ] || [ "$ip" = "0.0.0.0" ] || [[ "$ip" == 185.228.168.* ]] || [[ "$ip" == 185.228.169.* ]]
}
safesearch()  { local ip; ip=$(resolve www.google.com); echo "    www.google.com -> '$ip'"; [ "$ip" = "216.239.38.120" ] || [ "$ip" = "216.239.38.119" ]; }
internet_ok() { local ip; ip=$(resolve example.com); echo "    example.com -> '$ip'"; [ -n "$ip" ] && [ "$ip" != "0.0.0.0" ]; }
resumed()     { sh_ dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1; }

wait_boot() {
  adb wait-for-device
  for _ in $(seq 1 120); do
    [ "$(sh_ getprop sys.boot_completed)" = "1" ] && break
    sleep 3
  done
  sleep 5
  sh_ input keyevent 82 >/dev/null   # wake + dismiss swipe lock
  sh_ wm dismiss-keyguard >/dev/null
}
restart_phone() {
  echo "==> Restarting the phone: $1"
  adb reboot
  sleep 15
  wait_boot
  echo "    booted; waiting 45s for PBLOCK to come back by itself (app NOT opened)"
  sleep 45
}

echo "==> Device: $(sh_ getprop ro.product.model) / Android $(sh_ getprop ro.build.version.release) (API $(sh_ getprop ro.build.version.sdk))"

# ---------------------------------------------------------------- install
adb install -r "$APK" > "$OUT/install.txt" 2>&1
check "Install APK" "$(tail -1 "$OUT/install.txt")" grep -q Success "$OUT/install.txt"
sh_ pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null
# Android only offers Always-on for apps whose VPN service declares the
# android.net.VpnService intent filter (missing before 1.7.0 -> "Not supported by this app").
check "Always-on VPN supported by the app" "VpnService intent filter" bash -c "adb shell cmd package query-services -a android.net.VpnService --components 2>/dev/null | grep -q $PKG"
# Pre-approve the VPN consent dialog (a person taps "OK" on a real phone)
sh_ appops set $PKG ACTIVATE_VPN allow >/dev/null

# ---------------------------------------------------------------- first launch + turn on
sh_ am start -W -n $PKG/.MainActivity >/dev/null
sleep 4
shot 01-first-launch
# Auto "Activate device admin" prompt: skip it for now
if resumed | grep -qi DeviceAdmin; then sh_ input keyevent BACK >/dev/null; sleep 2; fi

$UI tap passwordInput && sh_ input text testpass123 && sh_ input keyevent BACK
$UI tap "Save password"
$UI tap "Confirm password" && sh_ input text testpass123
$UI tap "Set"
sleep 2
sh_ input swipe 540 700 540 1800 300 >/dev/null   # scroll back to top
sleep 1
shot 02-password-set
$UI tap "Turn on protection"
sleep 3
shot 03-after-turn-on
# Battery-exemption prompt (bug fixed in 1.5.0: it never appeared before)
if $UI tap "Allow" 4; then BAT_PROMPT=yes; else BAT_PROMPT=no; fi
sleep 3
check "Battery-exemption prompt appears" "prompt=$BAT_PROMPT" [ "$BAT_PROMPT" = yes ]
check "Battery exemption granted" "deviceidle whitelist" bash -c "adb shell dumpsys deviceidle whitelist | grep -q $PKG"
sleep 3
shot 04-protected
check "VPN filter running" "tun0 has 10.111.222.1" vpn_up
check "Porn site blocked" "pornhub.com" porn_blocked
check "Google SafeSearch forced" "www.google.com" safesearch
check "Normal sites still work" "example.com" internet_ok

# ---------------------------------------------------------------- restart 1: boot receiver + watchdog
sh_ input keyevent HOME >/dev/null
restart_phone "normal mode (boot receiver)"
shot 05-after-restart-1
check "After restart: VPN filter running" "app not opened" vpn_up
check "After restart: porn site blocked" "pornhub.com" porn_blocked
check "Watchdog job scheduled (survives restart)" "jobscheduler" bash -c "adb shell dumpsys jobscheduler $PKG | grep -q WatchdogJobService"

# ---------------------------------------------------------------- restart 2: Always-on VPN
sh_ settings put secure always_on_vpn_app $PKG >/dev/null
sh_ settings put secure always_on_vpn_lockdown 0 >/dev/null
restart_phone "with Always-on VPN"
shot 06-after-restart-alwayson
check "Always-on VPN: running after restart" "app not opened" vpn_up
check "Always-on VPN: porn site blocked" "pornhub.com" porn_blocked

# ---------------------------------------------------------------- Search Shield
sh_ settings put secure enabled_accessibility_services $PKG/$PKG.SearchShieldService >/dev/null
sh_ settings put secure accessibility_enabled 1 >/dev/null
sleep 5
if [ -n "$(sh_ pm path com.android.chrome)" ]; then
  sh_ am set-debug-app --persistent com.android.chrome >/dev/null
  sh_ 'echo "_ --disable-fre --no-default-browser-check --no-first-run" > /data/local/tmp/chrome-command-line'
  sh_ am start -a android.intent.action.VIEW -d "https://www.google.com/search?q=spankbang" com.android.chrome >/dev/null
  sleep 12
  shot 07-search-shield
  TOP=$(resumed)
  check "Search Shield pop-up on 'spankbang' search" "$TOP" bash -c "echo '$TOP' | grep -q StopActivity"
  sh_ input keyevent HOME >/dev/null
  sh_ am start -a android.intent.action.VIEW -d "https://www.google.com/search?q=python+tutorial" com.android.chrome >/dev/null
  sleep 10
  TOP=$(resumed)
  check "No pop-up on a normal search" "$TOP" bash -c "! echo '$TOP' | grep -q StopActivity"
  sh_ input keyevent HOME >/dev/null
else
  record "⚪ SKIP" "Search Shield" "Chrome not installed on this emulator image"
fi

# ---------------------------------------------------------------- Private DNS backup filter
sh_ settings put global private_dns_mode hostname >/dev/null
sh_ settings put global private_dns_specifier family-filter-dns.cleanbrowsing.org >/dev/null
sleep 20
check "Private DNS + VPN: normal sites still work" "example.com" internet_ok
check "Private DNS + VPN: porn site blocked" "pornhub.com" porn_blocked

# ---------------------------------------------------------------- Lock mode (device owner)
sh_ dpm set-device-owner $PKG/.PBlockDeviceAdminReceiver > "$OUT/dpm.txt" 2>&1
cat "$OUT/dpm.txt"
check "Lock mode: device owner set" "$(head -1 "$OUT/dpm.txt")" grep -qi success "$OUT/dpm.txt"
sh_ am start -W -n $PKG/.MainActivity >/dev/null
sleep 6
shot 08-locked
adb uninstall $PKG > "$OUT/uninstall.txt" 2>&1
check "Lock mode: uninstall blocked" "$(head -1 "$OUT/uninstall.txt")" bash -c "! grep -q '^Success' $OUT/uninstall.txt"
check "Lock mode: always-on VPN set by PBLOCK" "always_on_vpn_app" bash -c "[ \"\$(adb shell settings get secure always_on_vpn_app | tr -d '\r')\" = $PKG ]"
check "Lock mode: family Private DNS kept" "private_dns_mode" bash -c "[ \"\$(adb shell settings get global private_dns_mode | tr -d '\r')\" = hostname ]"
sh_ input keyevent HOME >/dev/null
restart_phone "lock mode"
shot 09-after-restart-locked
check "Lock mode after restart: VPN filter running" "app not opened" vpn_up
check "Lock mode after restart: porn site blocked" "pornhub.com" porn_blocked
adb uninstall $PKG > "$OUT/uninstall2.txt" 2>&1
check "Lock mode after restart: uninstall still blocked" "$(head -1 "$OUT/uninstall2.txt")" bash -c "! grep -q '^Success' $OUT/uninstall2.txt"

# ---------------------------------------------------------------- report
adb logcat -d -s PBLOCK:* > "$OUT/logcat-pblock.txt" 2>&1 || true
{
  echo "## PBLOCK emulator test"
  echo
  echo "Device: $(sh_ getprop ro.product.model), Android $(sh_ getprop ro.build.version.release)"
  echo
  cat "$RESULTS"
  echo
  echo "**Failures: $FAILS**"
} | tee "$OUT/summary.md"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && cat "$OUT/summary.md" >> "$GITHUB_STEP_SUMMARY"
echo "RESULT_FAILS=$FAILS"
exit 0
