// AndroidManifest.xml - Add these permissions:
// <uses-permission android:name="android.permission.INTERNET" />
// <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

package com.accountability.contentblocker;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    
    private static final String HOSTS_FILE = "/system/etc/hosts";
    private static final String CONFIG_FILE = "/data/data/com.accountability.contentblocker/password.conf";
    
    private TextView statusText;
    private EditText passwordInput;
    
    private List<String> blockedDomains = new ArrayList<String>() {{
        add("pornhub.com");
        add("xvideos.com");
        add("xnxx.com");
        add("xhamster.com");
        add("redtube.com");
        add("youporn.com");
        add("tube8.com");
        add("spankbang.com");
        add("eporner.com");
        add("txxx.com");
    }};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
            Toast.makeText(this, "Root access denied!", Toast.LENGTH_LONG).show();
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
            FileWriter writer = new FileWriter(CONFIG_FILE);
            writer.write(hash);
            writer.close();
            
            // Set file permissions to be readable only by app
            Runtime.getRuntime().exec("chmod 600 " + CONFIG_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String loadPasswordHash() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE));
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
            while ((line = reader.readLine()) != null) {
                if (line.contains("# CONTENT_BLOCKER_START")) {
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
        
        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters!", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        String hash = hashPassword(password);
        savePasswordHash(hash);
        Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show();
        passwordInput.setText("");
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
            
            // Append blocking rules
            os.writeBytes("echo '\n# CONTENT_BLOCKER_START' >> " + HOSTS_FILE + "\n");
            for (String domain : blockedDomains) {
                os.writeBytes("echo '127.0.0.1 " + domain + "' >> " + HOSTS_FILE + "\n");
                os.writeBytes("echo '127.0.0.1 www." + domain + "' >> " + HOSTS_FILE + "\n");
                os.writeBytes("echo '::1 " + domain + "' >> " + HOSTS_FILE + "\n");
                os.writeBytes("echo '::1 www." + domain + "' >> " + HOSTS_FILE + "\n");
            }
            os.writeBytes("echo '# CONTENT_BLOCKER_END' >> " + HOSTS_FILE + "\n");
            
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
        String inputHash = hashPassword(password);
        
        if (!inputHash.equals(storedHash)) {
            Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 30-second delay
        statusText.setText("⏳ Waiting 30 seconds...\nUse this time to reconsider.");
        new Thread(() -> {
            try {
                for (int i = 30; i > 0; i--) {
                    final int count = i;
                    runOnUiThread(() -> statusText.setText("⏳ " + count + " seconds remaining..."));
                    Thread.sleep(1000);
                }
                
                runOnUiThread(() -> {
                    try {
                        Process process = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(process.getOutputStream());
                        
                        // Read hosts file, remove blocking section
                        os.writeBytes("mount -o remount,rw /system\n");
                        os.writeBytes("sed -i '/# CONTENT_BLOCKER_START/,/# CONTENT_BLOCKER_END/d' " 
                            + HOSTS_FILE + "\n");
                        os.writeBytes("mount -o remount,ro /system\n");
                        os.writeBytes("exit\n");
                        os.flush();
                        process.waitFor();
                        
                        Toast.makeText(this, "Content blocking removed!", 
                            Toast.LENGTH_SHORT).show();
                        showStatus();
                        passwordInput.setText("");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void showStatus() {
        boolean blocked = isBlocked();
        boolean passwordSet = loadPasswordHash() != null;
        
        String status = "=== CONTENT BLOCKER STATUS ===\n\n" +
                       "Status: " + (blocked ? "ACTIVE ✓" : "INACTIVE") + "\n" +
                       "Blocked domains: " + blockedDomains.size() + "\n" +
                       "Password set: " + (passwordSet ? "Yes" : "No") + "\n";
        
        statusText.setText(status);
    }
}
