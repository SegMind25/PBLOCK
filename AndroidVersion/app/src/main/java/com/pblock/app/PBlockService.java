package com.pblock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class PBlockService extends Service {

    private static final String TAG = "PBLOCK";
    private static final String CHANNEL_ID = "pblock_service";
    private static final int NOTIFICATION_ID = 1;
    private static final long PROTECTION_CHECK_INTERVAL_MS = 60000;

    private Handler protectionHandler;
    private Runnable protectionRunnable;
    private BootHostsManager hostsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        hostsManager = new BootHostsManager(this);
        protectionHandler = new Handler(Looper.getMainLooper());
        Log.i(TAG, "PBlockService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("PBLOCK Active")
                .setContentText("Content blocking protection is running")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setContentTitle("PBLOCK Active")
                .setContentText("Content blocking protection is running")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        }

        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed: " + e.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }

        reapplyUninstallBlock();
        startPeriodicProtectionCheck();

        return START_STICKY;
    }

    private void startPeriodicProtectionCheck() {
        protectionRunnable = new Runnable() {
            @Override
            public void run() {
                enforceProtection();
                protectionHandler.postDelayed(this, PROTECTION_CHECK_INTERVAL_MS);
            }
        };
        protectionHandler.postDelayed(protectionRunnable, PROTECTION_CHECK_INTERVAL_MS);
    }

    private void enforceProtection() {
        new Thread(() -> {
            try {
                DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(DEVICE_POLICY_SERVICE);
                ComponentName adminComponent = new ComponentName(
                    PBlockService.this, PBlockDeviceAdminReceiver.class);

                if (dpm.isAdminActive(adminComponent)) {
                    try {
                        dpm.setUninstallBlocked(adminComponent, getPackageName(), true);
                    } catch (SecurityException e) {
                        Log.w(TAG, "setUninstallBlocked requires device owner: " + e.getMessage());
                    }
                }

                hostsManager.enforceProtection();

            } catch (Exception e) {
                Log.e(TAG, "Protection enforcement error: " + e.getMessage());
            }
        }).start();
    }

    private void reapplyUninstallBlock() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, PBlockDeviceAdminReceiver.class);

            if (dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.setUninstallBlocked(adminComponent, getPackageName(), true);
                    Log.i(TAG, "Uninstall block re-applied");
                } catch (SecurityException e) {
                    Log.w(TAG, "setUninstallBlocked requires device owner "
                        + "(app must be set via adb dpm set-device-owner): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not re-apply uninstall block: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (protectionHandler != null && protectionRunnable != null) {
            protectionHandler.removeCallbacks(protectionRunnable);
        }
        super.onDestroy();
        Log.i(TAG, "PBlockService destroyed");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "PBLOCK Protection",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps PBLOCK content blocking active");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
