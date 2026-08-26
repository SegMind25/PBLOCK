package com.pblock.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class BootHostsManager {

    private static final String TAG = "PBLOCK";
    private static final String PREFS = "pblock_prefs";
    private static final String KEY_ROOT_BLOCKING_ACTIVE = "root_blocking_active";
    private static final String KEY_BLOCKING_TIMESTAMP = "blocking_timestamp";

    private final Context context;
    private final SharedPreferences prefs;

    public BootHostsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveRootBlockingState(boolean active) {
        prefs.edit()
            .putBoolean(KEY_ROOT_BLOCKING_ACTIVE, active)
            .putLong(KEY_BLOCKING_TIMESTAMP, System.currentTimeMillis())
            .apply();
    }

    public boolean wasRootBlockingActive() {
        return prefs.getBoolean(KEY_ROOT_BLOCKING_ACTIVE, false);
    }

    public boolean reapplyRootBlockingOnBoot() {
        if (!wasRootBlockingActive()) {
            Log.i(TAG, "No previous root blocking state found, skipping");
            return false;
        }

        new Thread(() -> {
            try {
                Thread.sleep(5000);

                if (!checkRootAccess()) {
                    Log.w(TAG, "Root not available after boot, cannot re-apply hosts blocking");
                    return;
                }

                String hosts = readHostsViaSu();
                if (hosts == null) {
                    Log.e(TAG, "Cannot read hosts file");
                    return;
                }

                if (hosts.contains(PBlockHelper.BLOCK_START_MARKER)) {
                    Log.i(TAG, "Root blocking already present in hosts file");
                    saveRootBlockingState(true);
                    return;
                }

                String newContent = PBlockHelper.applyBlockSection(hosts);
                boolean writeOk = writeHostsViaSu(newContent);

                if (writeOk) {
                    String verify = readHostsViaSu();
                    if (verify != null && verify.contains(PBlockHelper.BLOCK_START_MARKER)) {
                        Log.i(TAG, "Root blocking re-applied successfully after boot");
                        saveRootBlockingState(true);
                    } else {
                        Log.e(TAG, "Root blocking verification failed after write");
                    }
                } else {
                    Log.e(TAG, "Failed to write hosts file after boot");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error re-applying root blocking: " + e.getMessage());
            }
        }).start();

        return true;
    }

    public boolean enforceProtection() {
        new Thread(() -> {
            try {
                if (!checkRootAccess()) {
                    return;
                }

                if (!wasRootBlockingActive()) {
                    return;
                }

                String hosts = readHostsViaSu();
                if (hosts == null) {
                    return;
                }

                if (!hosts.contains(PBlockHelper.BLOCK_START_MARKER)) {
                    Log.w(TAG, "Hosts blocking missing, re-applying...");
                    String newContent = PBlockHelper.applyBlockSection(hosts);
                    writeHostsViaSu(newContent);
                }
            } catch (Exception e) {
                Log.e(TAG, "Protection enforcement error: " + e.getMessage());
            }
        }).start();
        return true;
    }

    private boolean checkRootAccess() {
        SuResult result = runSu("id");
        return result.exitCode == 0;
    }

    private String readHostsViaSu() {
        SuResult result = runSu("cat " + MainActivity.HOSTS_FILE);
        if (result.exitCode != 0 && !result.stdout.contains("#")) {
            return null;
        }
        return result.stdout;
    }

    private boolean writeHostsViaSu(String content) {
        try {
            File tmp = new File(context.getFilesDir(), "hosts_boot.tmp");
            FileWriter writer = new FileWriter(tmp);
            writer.write(content);
            writer.close();

            String script = "mount -o remount,rw /system 2>/dev/null\n"
                + "mount -o remount,rw / 2>/dev/null || true\n"
                + "if cp -f '" + tmp.getAbsolutePath() + "' " + MainActivity.HOSTS_FILE + "; then "
                + "echo PBLOCK_BOOT_OK; else echo PBLOCK_BOOT_FAIL; fi\n"
                + "chmod 644 " + MainActivity.HOSTS_FILE + " 2>/dev/null || true\n"
                + "mount -o remount,ro /system 2>/dev/null || true\n"
                + "mount -o remount,ro / 2>/dev/null || true\n";

            SuResult result = runSu(script);
            return result.stdout.contains("PBLOCK_BOOT_OK");
        } catch (Exception e) {
            Log.e(TAG, "writeHostsViaSu error: " + e.getMessage());
            return false;
        }
    }

    private SuResult runSu(String commands) {
        SuResult result = new SuResult();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            final Process proc = process;

            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());
            os.write(commands.getBytes("UTF-8"));
            os.writeBytes("\nexit\n");
            os.flush();

            StringBuilder outBuf = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outBuf.append(line).append('\n');
            }

            StringBuilder errBuf = new StringBuilder();
            Thread errThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(proc.getErrorStream()))) {
                    String errLine;
                    while ((errLine = r.readLine()) != null) {
                        errBuf.append(errLine).append('\n');
                    }
                } catch (Exception ignored) {
                }
            });
            errThread.start();

            result.exitCode = process.waitFor();
            errThread.join(2000);
            result.stdout = outBuf.toString();
            result.stderr = errBuf.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "runSu error: " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    private static class SuResult {
        int exitCode = -1;
        String stdout = "";
        String stderr = "";
    }
}
