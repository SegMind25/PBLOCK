package com.pblock.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String ALERT_CHANNEL = "pblock_alert";

    /** "Protection is OFF - tap to turn it back on" (opens the app, which restarts it). */
    static void notifyProtectionOff(Context context) {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new android.app.NotificationChannel(ALERT_CHANNEL,
                    "Protection alerts", android.app.NotificationManager.IMPORTANCE_HIGH));
            }
            Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, 7, open,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.Notification.Builder b = android.os.Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, ALERT_CHANNEL)
                : new android.app.Notification.Builder(context);
            nm.notify(3, b.setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("PBLOCK protection is OFF")
                .setContentText("Tap to turn it back on")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build());
        } catch (Exception e) {
            Log.w(TAG, "notifyProtectionOff: " + e.getMessage());
        }
    }

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

        LockdownManager.applyIfDeviceOwner(context);

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean vpnEnabledByUser = prefs.getBoolean(KEY_ENABLED, false);

        // prepare() != null means VPN consent was revoked; starting would just fail,
        // so tell the user instead of silently staying unprotected.
        if (vpnEnabledByUser && !PBlockVpnService.isRunning()
                && android.net.VpnService.prepare(context) != null) {
            notifyProtectionOff(context);
        } else if (vpnEnabledByUser && !PBlockVpnService.isRunning()) {
            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    context, new Intent(context, PBlockVpnService.class));
                Log.i(TAG, "VPN blocking restarted after boot");
            } catch (Exception e) {
                Log.e(TAG, "Boot VPN start failed: " + e.getMessage());
            }
        }

        BootHostsManager hostsManager = new BootHostsManager(context);
        if (!hostsManager.wasRootBlockingActive()) {
            // VPN mode needs no watchdog: saves battery and a second notification.
            return;
        }
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
