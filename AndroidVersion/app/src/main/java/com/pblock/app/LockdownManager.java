package com.pblock.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import java.util.Collections;

/**
 * "Lock mode": when PBLOCK is the device owner (set once with
 * {@code adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver}),
 * it uses device-owner policies so that blocking can only be removed by a factory reset.
 *
 * <p>All calls are idempotent and cheap, so they are simply re-applied on app start,
 * on boot and when admin is enabled.
 *
 * <p>Deliberately NOT restricted, so the phone can never be bricked:
 * factory reset (the one way out), installing/updating apps, and VPN lockdown
 * (if the VPN ever crashes, internet keeps working instead of going dark).
 */
public final class LockdownManager {

    private static final String TAG = "PBLOCK";

    private LockdownManager() {}

    public static ComponentName admin(Context context) {
        return new ComponentName(context, PBlockDeviceAdminReceiver.class);
    }

    /** True when PBLOCK is device owner: blocking is permanent until factory reset. */
    public static boolean isLocked(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager)
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }

    /** Apply every lock-mode policy. Does nothing unless PBLOCK is device owner. */
    public static void applyIfDeviceOwner(Context context) {
        if (!isLocked(context)) {
            return;
        }
        DevicePolicyManager dpm = (DevicePolicyManager)
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = admin(context);
        String pkg = context.getPackageName();

        // 1. Cannot be uninstalled.
        try {
            dpm.setUninstallBlocked(admin, pkg, true);
        } catch (Exception e) {
            Log.w(TAG, "setUninstallBlocked: " + e.getMessage());
        }

        // 2. VPN filter is always on and starts by itself on boot, without a consent dialog.
        //    lockdown=false: if the VPN ever fails, internet still works.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                dpm.setAlwaysOnVpnPackage(admin, pkg, false);
            } catch (Exception e) {
                Log.w(TAG, "setAlwaysOnVpnPackage: " + e.getMessage());
            }
        }

        // 3. Close the usual bypasses.
        restrict(dpm, admin, UserManager.DISALLOW_CONFIG_VPN);     // can't turn VPN off / swap VPN app
        restrict(dpm, admin, UserManager.DISALLOW_SAFE_BOOT);      // safe mode would skip the VPN
        restrict(dpm, admin, UserManager.DISALLOW_ADD_USER);       // a new user has no filter
        restrict(dpm, admin, UserManager.DISALLOW_REMOVE_USER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restrict(dpm, admin, UserManager.DISALLOW_ADD_MANAGED_PROFILE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            restrict(dpm, admin, UserManager.DISALLOW_CONFIG_PRIVATE_DNS); // custom DNS bypass
            try {
                dpm.setGlobalPrivateDnsModeOpportunistic(admin);
            } catch (Exception e) {
                Log.w(TAG, "setGlobalPrivateDnsModeOpportunistic: " + e.getMessage());
            }
        }

        // 4. Settings > Apps can't force-stop PBLOCK or clear its data.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                dpm.setUserControlDisabledPackages(admin, Collections.singletonList(pkg));
            } catch (Exception e) {
                Log.w(TAG, "setUserControlDisabledPackages: " + e.getMessage());
            }
        }

        context.getSharedPreferences(BootReceiver.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(BootReceiver.KEY_ENABLED, true).apply();
        Log.i(TAG, "Lock mode policies applied");
    }

    private static void restrict(DevicePolicyManager dpm, ComponentName admin, String key) {
        try {
            dpm.addUserRestriction(admin, key);
        } catch (Exception e) {
            Log.w(TAG, "addUserRestriction " + key + ": " + e.getMessage());
        }
    }

    public static final String ADB_COMMAND =
        "adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver";
}
