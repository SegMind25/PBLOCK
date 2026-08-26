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

        Intent serviceIntent = new Intent(context, PBlockService.class);
        context.stopService(serviceIntent);

        SharedPreferences prefs = context.getSharedPreferences(BootReceiver.PREFS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(context, "PBLOCK: Device Admin deactivated. "
            + "You can now uninstall the app.", Toast.LENGTH_LONG).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "WARNING: Disabling Device Admin will allow PBLOCK to be uninstalled. "
            + "Content blocking protection will be removed.\n\n"
            + "You must solve the puzzle challenges first to deactivate Device Admin.";
    }
}
