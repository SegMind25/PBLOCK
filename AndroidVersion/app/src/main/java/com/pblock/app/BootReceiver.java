package com.pblock.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean enabledByUser = prefs.getBoolean(KEY_ENABLED, false);
        if (!enabledByUser || PBlockVpnService.isRunning()) {
            return;
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(
                context, new Intent(context, PBlockVpnService.class));
        } catch (Exception e) {
            android.util.Log.e(TAG, "Boot start failed: " + e.getMessage());
        }
    }
}
