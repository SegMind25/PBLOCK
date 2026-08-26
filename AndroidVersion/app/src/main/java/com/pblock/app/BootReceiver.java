package com.pblock.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "PBLOCK";
    public static final String PREFS = "pblock_prefs";
    public static final String KEY_ENABLED = "vpn_user_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action)
            || "android.intent.action.QUICKBOOT_POWERON".equals(action)
            || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!boot) {
            return;
        }

        Log.i(TAG, "Boot completed, re-applying protection...");

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean vpnEnabledByUser = prefs.getBoolean(KEY_ENABLED, false);

        if (vpnEnabledByUser && !PBlockVpnService.isRunning()) {
            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    context, new Intent(context, PBlockVpnService.class));
                Log.i(TAG, "VPN blocking restarted after boot");
            } catch (Exception e) {
                Log.e(TAG, "Boot VPN start failed: " + e.getMessage());
            }
        }

        BootHostsManager hostsManager = new BootHostsManager(context);
        hostsManager.reapplyRootBlockingOnBoot();

        try {
            Intent serviceIntent = new Intent(context, PBlockService.class);
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent);
            Log.i(TAG, "PBlockService started after boot");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start PBlockService after boot: " + e.getMessage());
        }
    }
}
