package com.pblock.app;

import androidx.appcompat.app.AppCompatActivity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PBLOCK";
    static final String HOSTS_FILE = "/system/etc/hosts";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;
    private static final int REQUEST_CODE_VPN = 1003;
    private String configFile;

    private TextView statusText;
    private TextView deviceAdminWarning;
    private EditText passwordInput;
    private Button setupBtn;
    private Button statusBtn;
    private Button deviceAdminBtn;
    private Button vpnBtn;
    private Button blockBtn;
    private Button unblockBtn;
    private Button consoleBtn;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isCountingDown = new AtomicBoolean(false);
    private boolean adminPromptedThisSession;
    private boolean hasRoot;

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
        vpnBtn = findViewById(R.id.vpnButton);
        statusBtn = findViewById(R.id.statusButton);
        deviceAdminBtn = findViewById(R.id.deviceAdminButton);
        blockBtn = findViewById(R.id.blockButton);
        unblockBtn = findViewById(R.id.unblockButton);
        consoleBtn = findViewById(R.id.consoleButton);

        setupBtn.setOnClickListener(v -> setupPassword());
        vpnBtn.setOnClickListener(v -> toggleVpn());
        statusBtn.setOnClickListener(v -> showStatusAsync());
        deviceAdminBtn.setOnClickListener(v -> toggleDeviceAdmin());
        blockBtn.setOnClickListener(v -> blockHosts());
        unblockBtn.setOnClickListener(v -> promptUnblockHosts());
        consoleBtn.setOnClickListener(v ->
            startActivity(new Intent(this, ConsoleActivity.class)));
        statusText.setOnLongClickListener(v -> {
            promptOwnerUnlock();
            return true;
        });

        updateDeviceAdminUI();

        requestNeededPermissions();

        autoPromptDeviceAdmin();

        if (isDeviceAdminActive()) {
            startForegroundServiceSafe();
        }

        executor.execute(() -> {
            hasRoot = checkRootAccess();
            mainHandler.post(() -> {
                int vis = hasRoot ? View.VISIBLE : View.GONE;
                blockBtn.setVisibility(vis);
                unblockBtn.setVisibility(vis);
                consoleBtn.setVisibility(vis);
                if (!hasRoot) {
                    vpnBtn.setVisibility(View.VISIBLE);
                }
                updateVpnButton();
                showStatusAsync();

                if (hasRoot && isHostsBlocked()) {
                    startProtectionService();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateVpnButton();
        autoPromptDeviceAdmin();
        if (isDeviceAdminActive()) {
            startForegroundServiceSafe();
        }
    }

    private void toggleVpn() {
        if (isCountingDown.get()) {
            Toast.makeText(this, "Already counting down...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (PBlockVpnService.isRunning()) {
            if (loadPasswordHash() == null) {
                Intent stopIntent = new Intent(this, PBlockVpnService.class);
                stopIntent.setAction(PBlockVpnService.ACTION_STOP);
                startService(stopIntent);
                setVpnUserEnabled(false);
                updateVpnButton();
                showStatusAsync();
                return;
            }
            promptOwnerUnlock();
            return;
        }

        if (loadPasswordHash() == null) {
            Toast.makeText(this,
                "Set your password FIRST - after enabling, blocking can only be "
                    + "disabled with it!",
                Toast.LENGTH_LONG).show();
            return;
        }

        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQUEST_CODE_VPN);
        } else {
            startVpnBlocking();
        }
    }

    private static class SuResult {
        int exitCode = -1;
        String stdout = "";
        String stderr = "";
    }

    private SuResult runSu(String commands) {
        SuResult result = new SuResult();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            final Process proc = process;
            final StringBuilder errBuf = new StringBuilder();
            Thread errThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(proc.getErrorStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        errBuf.append(line).append('\n');
                    }
                } catch (Exception ignored) {
                }
            });
            errThread.start();

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

    private boolean checkRootAccess() {
        return runSu("id").exitCode == 0;
    }

    private String readHostsViaSu() throws Exception {
        SuResult r = runSu("cat " + HOSTS_FILE);
        if (r.exitCode != 0 && !r.stdout.contains("#")) {
            throw new Exception(r.stderr.isEmpty() ? "cannot read hosts" : r.stderr);
        }
        return r.stdout;
    }

    private boolean writeHostsViaSu(String content) throws Exception {
        java.io.File tmp = new java.io.File(getFilesDir(), "hosts.tmp");
        FileWriter writer = new FileWriter(tmp);
        writer.write(content);
        writer.close();

        String script = "mount -o remount,rw /system 2>/dev/null\n"
            + "mount -o remount,rw / 2>/dev/null || true\n"
            + "if cp -f '" + tmp.getAbsolutePath() + "' " + HOSTS_FILE + "; then "
            + "echo PBLOCK_WRITE_OK; else echo PBLOCK_WRITE_FAIL; fi\n"
            + "chmod 644 " + HOSTS_FILE + " 2>/dev/null || true\n"
            + "mount -o remount,ro /system 2>/dev/null || true\n"
            + "mount -o remount,ro / 2>/dev/null || true\n";
        SuResult r = runSu(script);
        return r.stdout.contains("PBLOCK_WRITE_OK");
    }

    private boolean isHostsBlocked() {
        try {
            return readHostsViaSu().contains(PBlockHelper.BLOCK_START_MARKER);
        } catch (Exception e) {
            return false;
        }
    }

    private void blockHosts() {
        if (isCountingDown.get()) {
            return;
        }
        if (!hasRoot) {
            Toast.makeText(this, "Root not available!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (loadPasswordHash() == null) {
            Toast.makeText(this,
                "Set your password FIRST! Unblocking requires it.", Toast.LENGTH_LONG).show();
            return;
        }
        setButtonsEnabled(false);
        statusText.setText("Activating ROOT blocking...");
        executor.execute(() -> {
            try {
                String hosts = readHostsViaSu();
                if (hosts.contains(PBlockHelper.BLOCK_START_MARKER)) {
                    BootHostsManager hostsManager = new BootHostsManager(this);
                    hostsManager.saveRootBlockingState(true);
                    startProtectionService();
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Root blocking already active!",
                            Toast.LENGTH_SHORT).show();
                        setButtonsEnabled(true);
                        showStatusAsync();
                    });
                    return;
                }
                boolean ok = writeHostsViaSu(
                    PBlockHelper.applyBlockSection(hosts));
                boolean verified = ok && isHostsBlocked();
                if (verified) {
                    BootHostsManager hostsManager = new BootHostsManager(this);
                    hostsManager.saveRootBlockingState(true);
                    startProtectionService();
                }
                final boolean finalVerified = verified;
                mainHandler.post(() -> {
                    if (finalVerified) {
                        Toast.makeText(this,
                            "ROOT BLOCKING ACTIVE! All blocked domains are now "
                                + "blocked system-wide. Survives reboot. "
                                + "Solve puzzles to deactivate.",
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                            "Write failed. Grant su to PBLOCK in Magisk, or your ROM "
                                + "blocks /system writes. Use VPN mode instead.",
                            Toast.LENGTH_LONG).show();
                    }
                    setButtonsEnabled(true);
                    showStatusAsync();
                });
            } catch (Exception e) {
                Log.e(TAG, "blockHosts error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void startProtectionService() {
        try {
            Intent serviceIntent = new Intent(this, PBlockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start protection service: " + e.getMessage());
        }
    }

    private void promptUnblockHosts() {
        if (isCountingDown.get()) {
            Toast.makeText(this, "Already counting down...", Toast.LENGTH_SHORT).show();
            return;
        }
        startFullUnlockChallenge();
    }

    private int unlockPuzzlesSolved;

    private void startFullUnlockChallenge() {
        unlockPuzzlesSolved = 0;
        Toast.makeText(this,
            "Solve " + UNLOCK_PUZZLE_COUNT + " challenges to disable EVERYTHING!",
            Toast.LENGTH_LONG).show();
        mainHandler.postDelayed(this::launchUnlockPuzzle, 500);
    }

    private void launchUnlockPuzzle() {
        new UltimatePuzzleDialog(this, unlockPuzzlesSolved, new UltimatePuzzleDialog.Listener() {
            @Override
            public void onSolved() {
                unlockPuzzlesSolved++;
                if (unlockPuzzlesSolved >= UNLOCK_PUZZLE_COUNT) {
                    Toast.makeText(MainActivity.this,
                        "ALL 10 CHALLENGES PASSED! Disabling everything...",
                        Toast.LENGTH_LONG).show();
                    mainHandler.postDelayed(MainActivity.this::disableAllProtection, 500);
                } else {
                    Toast.makeText(MainActivity.this,
                        "Challenge " + unlockPuzzlesSolved + "/" + UNLOCK_PUZZLE_COUNT
                            + " passed! Next is harder...",
                        Toast.LENGTH_SHORT).show();
                    mainHandler.postDelayed(MainActivity.this::launchUnlockPuzzle, 700);
                }
            }

            @Override
            public void onGivenUp() {
                unlockPuzzlesSolved = 0;
                Toast.makeText(MainActivity.this,
                    "Reset! 10 NEW challenges generated - solve all from the start!",
                    Toast.LENGTH_LONG).show();
                mainHandler.postDelayed(MainActivity.this::launchUnlockPuzzle, 700);
            }
        }).show();
    }

    private void disableAllProtection() {
        isCountingDown.set(true);
        setButtonsEnabled(false);
        statusText.setText("DISABLE IN 30s...\nThis will turn off ALL protection.");
        executor.execute(() -> {
            try {
                for (int i = 30; i > 0; i--) {
                    if (Thread.currentThread().isInterrupted()) {
                        mainHandler.post(() -> { setButtonsEnabled(true); isCountingDown.set(false); });
                        return;
                    }
                    final int count = i;
                    mainHandler.post(() ->
                        statusText.setText("DISABLE IN " + count + "s...\nAll protection will be removed."));
                    Thread.sleep(1000);
                }

                BootHostsManager hostsManager = new BootHostsManager(this);
                hostsManager.saveRootBlockingState(false);

                boolean hostsOk = true;
                if (hasRoot) {
                    try {
                        String hosts = readHostsViaSu();
                        if (hosts.contains(PBlockHelper.BLOCK_START_MARKER)) {
                            hostsOk = writeHostsViaSu(PBlockHelper.removeBlockSection(hosts));
                        }
                    } catch (Exception e) {
                        hostsOk = false;
                    }
                }

                Intent stopVpn = new Intent(this, PBlockVpnService.class);
                stopVpn.setAction(PBlockVpnService.ACTION_STOP);
                startService(stopVpn);
                setVpnUserEnabled(false);

                Intent stopService = new Intent(this, PBlockService.class);
                stopService(stopService);

                boolean adminDeactivated = false;
                if (isDeviceAdminActive()) {
                    try {
                        devicePolicyManager.removeActiveAdmin(adminComponent);
                        adminDeactivated = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Admin deactivation failed: " + e.getMessage());
                    }
                }

                final boolean hOk = hostsOk;
                final boolean aOk = adminDeactivated;
                mainHandler.post(() -> {
                    StringBuilder msg = new StringBuilder("ALL PROTECTION DISABLED:\n\n");
                    if (hasRoot) msg.append("Hosts blocking: ").append(hOk ? "REMOVED" : "FAILED").append("\n");
                    msg.append("VPN blocking: STOPPED\n");
                    if (aOk) msg.append("Device Admin: DEACTIVATED\n");
                    msg.append("\nPBLOCK is now fully inactive.");

                    new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Protection Removed")
                        .setMessage(msg.toString())
                        .setCancelable(false)
                        .setPositiveButton("OK", null)
                        .show();

                    isCountingDown.set(false);
                    setButtonsEnabled(true);
                    updateVpnButton();
                    updateDeviceAdminUI();
                    showStatusAsync();
                });
            } catch (InterruptedException e) {
                mainHandler.post(() -> { setButtonsEnabled(true); isCountingDown.set(false); });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            }
        });
    }

    private void startHostsDisableCountdown() {
        isCountingDown.set(true);
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

                String hosts = readHostsViaSu();
                boolean ok;
                if (hosts.contains(PBlockHelper.BLOCK_START_MARKER)) {
                    ok = writeHostsViaSu(PBlockHelper.removeBlockSection(hosts));
                    ok = ok && !isHostsBlocked();
                } else {
                    ok = true;
                }

                final boolean success = ok;
                mainHandler.post(() -> {
                    Toast.makeText(this, success
                            ? "Root blocking removed."
                            : "Failed to remove blocking (hosts not writable).",
                        Toast.LENGTH_LONG).show();
                    passwordInput.setText("");
                    isCountingDown.set(false);
                    setButtonsEnabled(true);
                    showStatusAsync();
                });
            } catch (InterruptedException e) {
                mainHandler.post(() -> {
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "unblock error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isCountingDown.set(false);
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void promptOwnerUnlock() {
        if (!PBlockVpnService.isRunning() || isCountingDown.get()) {
            return;
        }
        startFullUnlockChallenge();
    }

    private void setVpnUserEnabled(boolean enabled) {
        getSharedPreferences(BootReceiver.PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(BootReceiver.KEY_ENABLED, enabled)
            .apply();
    }

    private void startDisableCountdown() {
        isCountingDown.set(true);
        statusText.setText("Disabling in 30 seconds...\nUse this time to reconsider.");
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

                Intent stopIntent = new Intent(this, PBlockVpnService.class);
                stopIntent.setAction(PBlockVpnService.ACTION_STOP);
                startService(stopIntent);

                mainHandler.post(() -> {
                    Toast.makeText(this, "VPN blocking disabled.", Toast.LENGTH_LONG).show();
                    passwordInput.setText("");
                    isCountingDown.set(false);
                    setVpnUserEnabled(false);
                    setButtonsEnabled(true);
                    updateVpnButton();
                    showStatusAsync();
                });
            } catch (InterruptedException e) {
                Log.w(TAG, "Countdown interrupted");
                mainHandler.post(() -> {
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Disable error: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setButtonsEnabled(true);
                    isCountingDown.set(false);
                });
            }
        });
    }

    private void startVpnBlocking() {
        setVpnUserEnabled(true);
        startService(new Intent(this, PBlockVpnService.class));
        Toast.makeText(this,
            "VPN blocking enabled! It will stay on and restart after reboot.",
            Toast.LENGTH_LONG).show();
        updateVpnButton();
        showStatusAsync();
        requestBatteryExemption();
    }

    private void requestBatteryExemption() {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                startActivity(new Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
            }
        } catch (Exception e) {
            Log.w(TAG, "Battery exemption request failed: " + e.getMessage());
        }
    }

    private boolean isAlwaysOnVpnEnabled() {
        try {
            String pkg = android.provider.Settings.Secure.getString(
                getContentResolver(), "always_on_vpn_app");
            return getPackageName().equals(pkg);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateVpnButton() {
        if (vpnBtn == null) return;
        if (hasRoot) {
            vpnBtn.setVisibility(View.GONE);
            return;
        }
        if (PBlockVpnService.isRunning()) {
            vpnBtn.setText(R.string.btn_vpn_disable);
            vpnBtn.setVisibility(View.VISIBLE);
        } else {
            vpnBtn.setText(R.string.btn_vpn_enable);
            vpnBtn.setVisibility(View.VISIBLE);
        }
    }

    private void toggleDeviceAdmin() {
        if (isDeviceAdminActive()) {
            startDeactivationChallenges();
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

    private static final int UNLOCK_PUZZLE_COUNT = 10;
    private static final int PUZZLE_COUNT = 5;
    private static final int[] PUZZLE_LEGS = {1, 1, 2, 2, 3};
    private int puzzlesSolved;

    private void startDeactivationChallenges() {
        puzzlesSolved = 0;
        Toast.makeText(this,
            "Owner verification: solve " + PUZZLE_COUNT + " algorithm puzzles!",
            Toast.LENGTH_LONG).show();
        mainHandler.postDelayed(this::launchPuzzle, 500);
    }

    private void launchPuzzle() {
        int legs = PUZZLE_LEGS[Math.min(puzzlesSolved, PUZZLE_LEGS.length - 1)];
        new AlgoPuzzleDialog(this, legs, new AlgoPuzzleDialog.Listener() {
            @Override
            public void onSolved() {
                puzzlesSolved++;
                if (puzzlesSolved >= PUZZLE_COUNT) {
                    mainHandler.postDelayed(MainActivity.this::confirmDeactivation, 400);
                } else {
                    Toast.makeText(MainActivity.this,
                        "Puzzle " + puzzlesSolved + "/" + PUZZLE_COUNT
                            + " solved! Next one is harder...",
                        Toast.LENGTH_LONG).show();
                    mainHandler.postDelayed(MainActivity.this::launchPuzzle, 900);
                }
            }

            @Override
            public void onGivenUp() {
                puzzlesSolved = 0;
                Toast.makeText(MainActivity.this,
                    "Puzzle set reset. 5 NEW puzzles generated - solve them all "
                        + "from the start!",
                    Toast.LENGTH_LONG).show();
                mainHandler.postDelayed(MainActivity.this::launchPuzzle, 900);
            }
        }).show();
    }

    private void confirmDeactivation() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Impressive!")
            .setMessage("All 5 challenges solved.\n\nDevice Admin will now be deactivated. "
                + "PBLOCK can be uninstalled until it is activated again.")
            .setCancelable(false)
            .setPositiveButton("Deactivate Admin", (dialog, which) -> {
                devicePolicyManager.removeActiveAdmin(adminComponent);
                updateDeviceAdminUI();
                updateVpnButton();
                showStatusAsync();
                Toast.makeText(this, "Device Admin deactivated.",
                    Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Keep protected", null)
            .show();
    }

    private void autoPromptDeviceAdmin() {
        updateDeviceAdminUI();
        if (isDeviceAdminActive() || adminPromptedThisSession) {
            return;
        }
        adminPromptedThisSession = true;
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Device Admin prevents PBLOCK from being uninstalled. "
                    + "This keeps content blocking protection active.");
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        } catch (Exception e) {
            Log.e(TAG, "Admin prompt failed: " + e.getMessage());
        }
    }

    private boolean isDeviceAdminActive() {
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent);
    }

    private void updateDeviceAdminUI() {
        boolean active = isDeviceAdminActive();
        if (active) {
            deviceAdminBtn.setVisibility(View.GONE);
            deviceAdminWarning.setVisibility(View.GONE);
        } else {
            deviceAdminBtn.setText(R.string.btn_device_admin);
            deviceAdminBtn.setVisibility(View.VISIBLE);
            deviceAdminWarning.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            updateDeviceAdminUI();
            if (isDeviceAdminActive()) {
                startForegroundServiceSafe();
                Toast.makeText(this, "Device Admin activated successfully!",
                    Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_VPN) {
            if (resultCode == RESULT_OK) {
                startVpnBlocking();
            } else {
                Toast.makeText(this,
                    "VPN permission denied. Blocking stays OFF.",
                    Toast.LENGTH_LONG).show();
            }
        }
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

        boolean existingPassword = loadPasswordHash() != null;

        if (existingPassword) {
            final EditText oldPassInput = new EditText(this);
            oldPassInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            oldPassInput.setHint("Current password");

            final EditText newPassConfirm = new EditText(this);
            newPassConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            newPassConfirm.setHint("Confirm new password");

            android.widget.LinearLayout dialogLayout = new android.widget.LinearLayout(this);
            dialogLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            dialogLayout.setPadding(pad, pad, pad, 0);
            dialogLayout.addView(oldPassInput);
            dialogLayout.addView(newPassConfirm);

            new android.app.AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("Enter your current password and the new password (twice).")
                .setView(dialogLayout)
                .setCancelable(false)
                .setPositiveButton("Change", (dialog, which) -> {
                    String oldPass = oldPassInput.getText().toString();
                    String confirmPass = newPassConfirm.getText().toString();
                    String storedHash = loadPasswordHash();
                    String oldHash = hashPassword(oldPass);
                    if (storedHash == null || oldHash == null || !oldHash.equals(storedHash)) {
                        Toast.makeText(this, "Current password is wrong!",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!password.equals(confirmPass)) {
                        Toast.makeText(this, "New passwords don't match!",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doSavePassword(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            final EditText confirmInput = new EditText(this);
            confirmInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmInput.setHint("Confirm password");

            new android.app.AlertDialog.Builder(this)
                .setTitle("Set Password")
                .setMessage("Confirm your new password.")
                .setView(confirmInput)
                .setCancelable(false)
                .setPositiveButton("Set", (dialog, which) -> {
                    if (!password.equals(confirmInput.getText().toString())) {
                        Toast.makeText(this, "Passwords don't match!",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    doSavePassword(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void doSavePassword(String password) {
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

    private String hashPassword(String password) {
        try {
            String stored = loadPasswordHash();
            String salt;
            if (stored != null && stored.contains(":")) {
                salt = stored.split(":")[0];
            } else {
                byte[] saltBytes = new byte[16];
                new java.security.SecureRandom().nextBytes(saltBytes);
                StringBuilder sb = new StringBuilder();
                for (byte b : saltBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) sb.append('0');
                    sb.append(hex);
                }
                salt = sb.toString();
            }
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt.getBytes("UTF-8"), 100000, 256);
            java.security.Key key = factory.generateSecret(spec);
            byte[] hashBytes = key.getEncoded();
            StringBuilder hexString = new StringBuilder(salt).append(":");
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            spec.clearPassword();
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
            setFilePermissions(configFile);
        } catch (Exception e) {
            Log.e(TAG, "Save password error: " + e.getMessage());
        }
    }

    private String loadPasswordHash() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String hash = reader.readLine();
            reader.close();
            return (hash != null && !hash.isEmpty()) ? hash : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void setFilePermissions(String path) {
        try {
            Runtime.getRuntime().exec("chmod 600 " + path).waitFor();
        } catch (Exception e) {
            Log.w(TAG, "chmod failed: " + e.getMessage());
        }
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            List<String> needed = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (!needed.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (isDeviceAdminActive()) {
                startForegroundServiceSafe();
            }
        }
    }

    private void startForegroundServiceSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNeededPermissions();
                return;
            }
        }
        try {
            Intent serviceIntent = new Intent(this, PBlockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service: " + e.getMessage());
        }
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, PBlockService.class);
        stopService(serviceIntent);
    }

    private void setButtonsEnabled(boolean enabled) {
        if (setupBtn != null) setupBtn.setEnabled(enabled);
        if (vpnBtn != null) vpnBtn.setEnabled(enabled);
        if (statusBtn != null) statusBtn.setEnabled(enabled);
        if (deviceAdminBtn != null && deviceAdminBtn.getVisibility() == View.VISIBLE) {
            deviceAdminBtn.setEnabled(enabled);
        }
    }

    private void showStatusAsync() {
        executor.execute(() -> {
            final boolean vpnActive = PBlockVpnService.isRunning();
            final boolean alwaysOn = isAlwaysOnVpnEnabled();
            final int domainCount = PBlockHelper.getBlockedDomainCount();
            final boolean hostsBlocked = hasRoot && isHostsBlocked();
            final boolean serviceRunning = isServiceRunning(PBlockService.class);
            final BootHostsManager hostsManager = new BootHostsManager(this);
            final boolean bootPersistence = hostsManager.wasRootBlockingActive();

            mainHandler.post(() -> {
                StringBuilder s = new StringBuilder("=== PBLOCK STATUS ===\n\n");
                if (hasRoot) {
                    s.append("ROOT BLOCKING: ").append(hostsBlocked ? "ACTIVE" : "INACTIVE")
                        .append("\n");
                }
                if (!hasRoot) {
                    s.append("VPN Blocking: ")
                        .append(vpnActive ? "ACTIVE (locked)" : "INACTIVE").append("\n")
                        .append("Auto-restart on boot: ")
                        .append(alwaysOn ? "GUARANTEED (Always-on)" : (vpnActive ? "ON" : "-"))
                        .append("\n");
                }
                s.append("Blocked domains: ").append(domainCount).append("\n")
                    .append("SafeSearch forced: Google + Bing\n")
                    .append("Mode: ").append(hasRoot ? "ROOT (hosts file)" : "VPN (DNS)")
                    .append("\n")
                    .append("Device Admin: ")
                    .append(isDeviceAdminActive() ? "ACTIVE (protected)" : "INACTIVE").append("\n")
                    .append("Protection Service: ").append(serviceRunning ? "RUNNING" : "STOPPED").append("\n")
                    .append("Boot Persistence: ").append(bootPersistence ? "ENABLED" : "DISABLED").append("\n");
                statusText.setText(s.toString());
            });
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
