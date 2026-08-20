package com.pblock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class PBlockService extends Service {

    private static final String TAG = "PBLOCK";
    private static final String CHANNEL_ID = "pblock_service";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.i(TAG, "PBlockService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PBLOCK Active")
            .setContentText("Content blocking protection is running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);

        reapplyUninstallBlock();

        return START_STICKY;
    }

    private void reapplyUninstallBlock() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, PBlockDeviceAdminReceiver.class);

            if (dpm.isAdminActive(adminComponent)) {
                dpm.setUninstallBlocked(adminComponent, getPackageName(), true);
                Log.i(TAG, "Uninstall block re-applied");
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
