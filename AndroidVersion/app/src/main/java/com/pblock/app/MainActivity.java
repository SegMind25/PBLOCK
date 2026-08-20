package com.pblock.app;

import androidx.appcompat.app.AppCompatActivity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PBLOCK";
    private static final String HOSTS_FILE = "/system/etc/hosts";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1001;
    private String configFile;

    private TextView statusText;
    private TextView deviceAdminWarning;
    private EditText passwordInput;
    private Button setupBtn;
    private Button blockBtn;
    private Button unblockBtn;
    private Button statusBtn;
    private Button deviceAdminBtn;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isCountingDown = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configFile = getFilesDir().getAbsolutePath() + "/password.conf";

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, PBlockDeviceAdminReceiver.class);

        statusText = findViewById(R.id.statusText);
        passwordInput = findViewById(R.id.passwordInput);
        deviceAdminWarning = findViewById(R.id.deviceAdminWarning);

        setupBtn = findViewById(R.id.setupButton);
        blockBtn = findViewById(R.id.blockButton);
        unblockBtn = findViewById(R.id.unblockButton);
        statusBtn = findViewById(R.id.statusButton);
        deviceAdminBtn = findViewById(R.id.deviceAdminButton);

        setupBtn.setOnClickListener(v -> setupPassword());
        blockBtn.setOnClickListener(v -> blockContent());
        unblockBtn.setOnClickListener(v -> unblockContent());
        statusBtn.setOnClickListener(v -> showStatus());
        deviceAdminBtn.setOnClickListener(v -> toggleDeviceAdmin());

        updateDeviceAdminUI();

        if (isDeviceAdminActive()) {
            startForegroundService();
        }

        executor.execute(() -> {
            final boolean hasRoot = checkRootAccess();
            mainHandler.post(() -> {
                if (!hasRoot) {
                    statusText.setText("Warning: Root access not available.\n\n"
                        + "This app requires a rooted device to modify the hosts file.\n\n"
                        + "For non-rooted devices, use the NSFW blocking scripts "
                        + "in the scripts/ folder on GitHub.\n\n"
                        + "Options for non-rooted phones:\n"
                        + "1. Use Private DNS (Android 9+)\n"
                        + "2. Use ADB from a computer\n"
                        + "3. Install a DNS-based blocker app");
                }
                showStatusAsync();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private boolean checkRootAccess() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.w(TAG, "Root access not available: " + e.getMessage());
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Hash error: " + e.getMessage());
            return null;
        }
    }

    private void savePasswordHash(String hash) {
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(hash);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "Save password error: " + e.getMessage());
        }
    }

    private String loadPasswordHash() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String hash = reader.readLine();
            reader.close();
            return hash;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBlocked() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("cat " + HOSTS_FILE + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(PBlockHelper.BLOCK_START_MARKER)) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "isBlocked check error: " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }

    private void setupPassword() {
        String password = passwordInput.getText().toString();

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters!",
                Toast.LENGTH_SHORT).show();
            return;
        }

        String hash = hashPassword(password);
        if (hash == null) {
            Toast.makeText(this, "Error hashing password. Please try again.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        savePasswordHash(hash);
        Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show();
        passwordInput.setText("");
        showStatusAsync();
    }

    private void blockContent() {
        setButtonsEnabled(false);
        statusText.setText("Activating content blocking...");

        executor.execute(() -> {
            try {
                if (isBlocked()) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Blocking is already active!",
                            Toast.LENGTH_SHORT).show();
                        setButtonsEnabled(true);
                        showStatusAsync();
                    });
                    return;
                }

                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());

                os.writeBytes("mount -o remount,rw /system\n");

                String blockEntries = PBlockHelper.generateBlockEntries();
                for (String entryLine : blockEntries.split("\n")) {
                    os.writeBytes("echo '" + entryLine + "' >> " + HOSTS_FILE + "\n");
                }

                os.writeBytes("mount -o remount,ro /system\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                process.destroy();

                mainHandler.post(() -> {
                    Toast.makeText(this, "Content blocking activated!",
                        Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                    showStatusAsync();
                });
            } catch (Exception e) {
                Log.e(TAG, "Block error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void unblockContent() {
        String storedHash = loadPasswordHash();
        if (storedHash == null) {
            Toast.makeText(this, "No password set! Set password first.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        String password = passwordInput.getText().toString();
        if (password.isEmpty()) {
            Toast.makeText(this, "Enter your password to unblock.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        String inputHash = hashPassword(password);
        if (inputHash == null) {
            Toast.makeText(this, "Error verifying password.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (!inputHash.equals(storedHash)) {
            Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCountingDown.get()) {
            Toast.makeText(this, "Already counting down...", Toast.LENGTH_SHORT).show();
            return;
        }

        isCountingDown.set(true);
        statusText.setText("Waiting 30 seconds...\nUse this time to reconsider.");
        setButtonsEnabled(false);

        executor.execute(() -> {
            try {
                for (int i = 30; i > 0; i--) {
                    if (Thread.currentThread().isInterrupted()) {
                        mainHandler.post(() -> {
                            setButtonsEnabled(true);
                            isCountingDown.set(false);
                        });
                        return;
                    }
                    final int count = i;
                    mainHandler.post(() ->
                        statusText.setText(count + " seconds remaining...\n"
                            + "Use this time to reconsider."));
                    Thread.sleep(1000);
                }

                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());

                String startMarker = PBlockHelper.BLOCK_START_MARKER;
                String endMarker = PBlockHelper.BLOCK_END_MARKER;

                os.writeBytes("mount -o remount,rw /system\n");
                os.writeBytes("sed -i '/" + startMarker + "/,/" + endMarker
                    + "/d' " + HOSTS_FILE + "\n");
                os.writeBytes("mount -o remount,ro /system\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                process.destroy();

                mainHandler.post(() -> {
                    Toast.makeText(this, "Content blocking removed!",
                        Toast.LENGTH_SHORT).show();
                    passwordInput.setText("");
                    isCountingDown.set(false);
                    setButtonsEnabled(true);
                    showStatusAsync();
                });
            } catch (InterruptedException e) {
                Log.w(TAG, "Countdown interrupted");
                mainHandler.post(() -> {
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Unblock error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            }
        });
    }

    private boolean isDeviceAdminActive() {
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent);
    }

    private void toggleDeviceAdmin() {
        if (isDeviceAdminActive()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Deactivate Device Admin")
                .setMessage("WARNING: This will allow PBLOCK to be uninstalled.\n\n"
                    + "To remove PBLOCK after deactivating:\n"
                    + "1. Go to Settings > Security > Device Administrators\n"
                    + "2. Deactivate PBLOCK\n"
                    + "3. Uninstall the app")
                .setPositiveButton("Deactivate", (dialog, which) -> {
                    devicePolicyManager.removeActiveAdmin(adminComponent);
                    updateDeviceAdminUI();
                    Toast.makeText(this, "Device Admin deactivated. "
                        + "You can now uninstall PBLOCK.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Activate Device Admin")
                .setMessage("Activating Device Admin will:\n\n"
                    + "- Prevent PBLOCK from being uninstalled\n"
                    + "- Protect content blocking from being bypassed\n\n"
                    + "To remove PBLOCK later, you must first deactivate "
                    + "Device Admin in Settings > Security > Device Administrators.")
                .setPositiveButton("Activate", (dialog, which) -> {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Device Admin prevents PBLOCK from being uninstalled. "
                        + "This ensures content blocking protection stays active.");
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void updateDeviceAdminUI() {
        boolean active = isDeviceAdminActive();
        if (active) {
            deviceAdminBtn.setText(R.string.btn_deactivate_device_admin);
            deviceAdminWarning.setVisibility(View.VISIBLE);
            startForegroundService();
        } else {
            deviceAdminBtn.setText(R.string.btn_device_admin);
            deviceAdminWarning.setVisibility(View.GONE);
            stopForegroundService();
        }
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, PBlockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, PBlockService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            updateDeviceAdminUI();
            if (isDeviceAdminActive()) {
                Toast.makeText(this, "Device Admin activated successfully!",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        if (setupBtn != null) setupBtn.setEnabled(enabled);
        if (blockBtn != null) blockBtn.setEnabled(enabled);
        if (unblockBtn != null) unblockBtn.setEnabled(enabled);
        if (statusBtn != null) statusBtn.setEnabled(enabled);
    }

    private void showStatus() {
        showStatusAsync();
    }

    private void showStatusAsync() {
        executor.execute(() -> {
            final boolean blocked = isBlocked();
            final boolean passwordSet = loadPasswordHash() != null;
            final int domainCount = PBlockHelper.getBlockedDomainCount();

            mainHandler.post(() -> {
                String status = "=== PBLOCK STATUS ===\n\n"
                    + "Protection: " + (blocked ? "ACTIVE" : "INACTIVE") + "\n"
                    + "Blocked domains: " + domainCount + "\n"
                    + "Password set: " + (passwordSet ? "Yes" : "No") + "\n"
                    + "Device Admin: " + (isDeviceAdminActive() ? "ACTIVE" : "INACTIVE") + "\n"
                    + "Foreground Service: " + (isDeviceAdminActive() ? "RUNNING" : "STOPPED") + "\n";
                statusText.setText(status);
            });
        });
    }
}
