package com.pblock.app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private static final String HOSTS_FILE = "/system/etc/hosts";
    private String configFile;

    private TextView statusText;
    private EditText passwordInput;

    // Load native library
    static {
        System.loadLibrary("pblock");
    }

    // Native method declarations
    public native String stringFromJNI();
    public native String getBlockedDomainsNative();
    public native int getBlockedDomainCountNative();
    public native String generateBlockEntriesNative();
    public native String getStartMarkerNative();
    public native String getEndMarkerNative();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configFile = getFilesDir().getAbsolutePath() + "/password.conf";

        statusText = findViewById(R.id.statusText);
        passwordInput = findViewById(R.id.passwordInput);

        Button setupBtn = findViewById(R.id.setupButton);
        Button blockBtn = findViewById(R.id.blockButton);
        Button unblockBtn = findViewById(R.id.unblockButton);
        Button statusBtn = findViewById(R.id.statusButton);

        setupBtn.setOnClickListener(v -> setupPassword());
        blockBtn.setOnClickListener(v -> blockContent());
        unblockBtn.setOnClickListener(v -> unblockContent());
        statusBtn.setOnClickListener(v -> showStatus());

        checkRootAccess();
        showStatus();
    }

    private boolean checkRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                Toast.makeText(this, "Root access required! Please root your device.",
                    Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Root access not available.",
                Toast.LENGTH_LONG).show();
            return false;
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
            e.printStackTrace();
            return null;
        }
    }

    private void savePasswordHash(String hash) {
        try {
            FileWriter writer = new FileWriter(configFile);
            writer.write(hash);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("cat " + HOSTS_FILE + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            String startMarker = getStartMarkerNative();
            while ((line = reader.readLine()) != null) {
                if (line.contains(startMarker)) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
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
        savePasswordHash(hash);
        Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show();
        passwordInput.setText("");
        showStatus();
    }

    private void blockContent() {
        if (isBlocked()) {
            Toast.makeText(this, "Blocking is already active!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            // Remount /system as read-write
            os.writeBytes("mount -o remount,rw /system\n");

            // Get block entries from native code and write them
            String blockEntries = generateBlockEntriesNative();
            for (String entryLine : blockEntries.split("\n")) {
                os.writeBytes("echo '" + entryLine + "' >> " + HOSTS_FILE + "\n");
            }

            // Remount as read-only
            os.writeBytes("mount -o remount,ro /system\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            Toast.makeText(this, "Content blocking activated!", Toast.LENGTH_SHORT).show();
            showStatus();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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
        if (!inputHash.equals(storedHash)) {
            Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 30-second delay to reconsider
        statusText.setText("Waiting 30 seconds...\nUse this time to reconsider.");
        setButtonsEnabled(false);

        new Thread(() -> {
            try {
                for (int i = 30; i > 0; i--) {
                    final int count = i;
                    runOnUiThread(() ->
                        statusText.setText(count + " seconds remaining...\nUse this time to reconsider."));
                    Thread.sleep(1000);
                }

                runOnUiThread(() -> {
                    try {
                        Process process = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(process.getOutputStream());

                        String startMarker = getStartMarkerNative();
                        String endMarker = getEndMarkerNative();

                        os.writeBytes("mount -o remount,rw /system\n");
                        os.writeBytes("sed -i '/" + startMarker + "/,/" + endMarker + "/d' "
                            + HOSTS_FILE + "\n");
                        os.writeBytes("mount -o remount,ro /system\n");
                        os.writeBytes("exit\n");
                        os.flush();
                        process.waitFor();

                        Toast.makeText(this, "Content blocking removed!",
                            Toast.LENGTH_SHORT).show();
                        passwordInput.setText("");
                        showStatus();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                    setButtonsEnabled(true);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> setButtonsEnabled(true));
            }
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.setupButton).setEnabled(enabled);
        findViewById(R.id.blockButton).setEnabled(enabled);
        findViewById(R.id.unblockButton).setEnabled(enabled);
        findViewById(R.id.statusButton).setEnabled(enabled);
    }

    private void showStatus() {
        boolean blocked = isBlocked();
        boolean passwordSet = loadPasswordHash() != null;
        int domainCount = getBlockedDomainCountNative();

        String status = "=== PBLOCK STATUS ===\n\n" +
                       "Protection: " + (blocked ? "ACTIVE" : "INACTIVE") + "\n" +
                       "Blocked domains: " + domainCount + "\n" +
                       "Password set: " + (passwordSet ? "Yes" : "No") + "\n";

        statusText.setText(status);
    }
}
