package com.pblock.app;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class PBlockDeviceAdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "PBLOCK";
    private static final String KEY_UNLOCK_OK = "admin_unlock_ok";

    /** Call right before removeActiveAdmin() once the puzzles are solved. */
    public static void allowDeactivation(Context context) {
        context.getSharedPreferences(BootReceiver.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_UNLOCK_OK, true).commit();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.i(TAG, "Device Admin enabled");

        DevicePolicyManager dpm = (DevicePolicyManager)
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, PBlockDeviceAdminReceiver.class);

        try {
            dpm.setUninstallBlocked(adminComponent, context.getPackageName(), true);
            Log.i(TAG, "Uninstall blocked via setUninstallBlocked");
        } catch (SecurityException e) {
            Log.w(TAG, "setUninstallBlocked requires device owner. "
                + "App must be set as device owner via: "
                + "adb shell dpm set-device-owner com.pblock.app/.PBlockDeviceAdminReceiver");
        } catch (Exception e) {
            Log.w(TAG, "setUninstallBlocked failed: " + e.getMessage());
        }

        try {
            Intent serviceIntent = new Intent(context, PBlockService.class);
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PBlockService: " + e.getMessage());
        }

        Toast.makeText(context, "PBLOCK: Device Admin activated. "
            + "The app cannot be uninstalled while Device Admin is active. "
            + "Solve all puzzles to deactivate.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.i(TAG, "Device Admin disabled");

        DevicePolicyManager dpm = (DevicePolicyManager)
            context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(context, PBlockDeviceAdminReceiver.class);

        try {
            dpm.setUninstallBlocked(adminComponent, context.getPackageName(), false);
        } catch (Exception e) {
            Log.w(TAG, "setUninstallBlocked reset failed: " + e.getMessage());
        }

        SharedPreferences prefs = context.getSharedPreferences(BootReceiver.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_UNLOCK_OK, false)) {
            // Admin was switched off from Settings without solving the puzzles.
            // Android doesn't let a plain device admin veto this, so keep blocking
            // running and ask the user to re-activate protection.
            Log.w(TAG, "Device Admin disabled without solving puzzles; keeping protection");
            try {
                androidx.core.content.ContextCompat.startForegroundService(context,
                    new Intent(context, PBlockService.class));
            } catch (Exception e) {
                Log.e(TAG, "Failed to restart PBlockService: " + e.getMessage());
            }
            Intent main = new Intent(context, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { context.startActivity(main); } catch (Exception ignored) {}
            Toast.makeText(context, "PBLOCK: Device Admin was turned off without solving the puzzles. "
                + "Content blocking stays on - please re-activate Device Admin.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent serviceIntent = new Intent(context, PBlockService.class);
        context.stopService(serviceIntent);

        prefs.edit().clear().apply();

        Toast.makeText(context, "PBLOCK: Device Admin deactivated. "
            + "You can now uninstall the app.", Toast.LENGTH_LONG).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Launch the puzzle protection activity
        Intent protectIntent = new Intent(context, UninstallProtectionActivity.class);
        protectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        protectIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        protectIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(protectIntent);

        return "WARNING: To deactivate Device Admin, you must solve all 10 algorithm puzzles.\n\n"
            + "Content blocking protection will remain active until all challenges are completed.\n\n"
            + "The puzzle challenge has been launched.";
    }
}
