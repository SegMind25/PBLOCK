package com.pblock.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

/**
 * Safety net for phones that block BOOT_COMPLETED: Android's JobScheduler re-runs this
 * every ~15 minutes (and the schedule survives reboots). If protection should be on but
 * isn't, it restarts the VPN, or posts "Protection is OFF - tap to turn it back on".
 *
 * <p>Starting the VPN's foreground service from the background is allowed because the
 * app asks to be exempt from battery optimisation.
 */
public class WatchdogJobService extends JobService {

    private static final String TAG = "PBLOCK";
    private static final int JOB_ID = 4242;

    public static void schedule(Context context) {
        try {
            JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null || js.getPendingJob(JOB_ID) != null) return;
            JobInfo job = new JobInfo.Builder(JOB_ID,
                    new ComponentName(context, WatchdogJobService.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true) // survives reboots (needs RECEIVE_BOOT_COMPLETED)
                .build();
            js.schedule(job);
        } catch (Exception e) {
            Log.w(TAG, "Watchdog schedule failed: " + e.getMessage());
        }
    }

    /** Restart protection if it should be running. Safe to call from anywhere. */
    static void heal(Context context) {
        boolean wanted = LockdownManager.isLocked(context)
            || context.getSharedPreferences(BootReceiver.PREFS, Context.MODE_PRIVATE)
                .getBoolean(BootReceiver.KEY_ENABLED, false);
        if (!wanted || PBlockVpnService.isRunning()) return;
        if (VpnService.prepare(context) != null) {
            BootReceiver.notifyProtectionOff(context); // VPN permission lost: user must tap
            return;
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(
                context, new Intent(context, PBlockVpnService.class));
            Log.i(TAG, "Watchdog restarted protection");
        } catch (Exception e) {
            // Background start refused (no battery exemption on this phone).
            Log.w(TAG, "Watchdog could not start VPN: " + e.getMessage());
            BootReceiver.notifyProtectionOff(context);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        LockdownManager.applyIfDeviceOwner(this);
        heal(this);
        return false; // work is done synchronously
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
