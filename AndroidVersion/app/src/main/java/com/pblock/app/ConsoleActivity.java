package com.pblock.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class ConsoleActivity extends Activity {

    private TextView outputView;
    private EditText inputField;

    private Process shellProcess;
    private DataOutputStream shellIn;
    private BufferedReader shellOut;
    private boolean shellReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        outputView = findViewById(R.id.consoleOutput);
        inputField = findViewById(R.id.consoleInput);
        outputView.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.consoleBack).setOnClickListener(v -> finish());
        findViewById(R.id.consoleRun).setOnClickListener(v -> sendInput());
        findViewById(R.id.quickStart).setOnClickListener(v -> startBlocking());
        findViewById(R.id.quickStop).setOnClickListener(v -> stopBlocking());
        findViewById(R.id.quickStatus).setOnClickListener(v -> sendLine(
            "echo '--- PBLOCK status ---'; "
                + "if grep -q '" + PBlockHelper.BLOCK_START_MARKER + "' /system/etc/hosts; then "
                + "echo 'Blocking: ACTIVE'; "
                + "echo 'Entries:' $(sed -n '/" + PBlockHelper.BLOCK_START_MARKER + "/,/" + PBlockHelper.BLOCK_END_MARKER + "/p' /system/etc/hosts | grep -c '127.0.0.1\\|^[0-9]'); "
                + "else echo 'Blocking: INACTIVE'; fi; "
                + "echo '--- id ---'; id"));
        findViewById(R.id.quickId).setOnClickListener(v -> sendLine("id; whoami"));
        findViewById(R.id.quickClear).setOnClickListener(v -> outputView.setText(""));

        startShell();
    }

    private void append(final String text) {
        runOnUiThread(() -> {
            outputView.append(text);
            final int scrollAmount =
                outputView.getLayout() != null
                    ? outputView.getLayout().getLineTop(outputView.getLineCount())
                        - outputView.getHeight()
                    : 0;
            if (scrollAmount > 0) {
                outputView.scrollTo(0, scrollAmount);
            } else {
                outputView.scrollTo(0, 0);
            }
        });
    }

    private void startShell() {
        append("PBLOCK ROOT CONSOLE v1.0\n");
        append("Requesting su (check Magisk prompt)...\n\n");
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("su");
                pb.redirectErrorStream(true);
                shellProcess = pb.start();
                shellIn = new DataOutputStream(shellProcess.getOutputStream());
                shellOut = new BufferedReader(
                    new InputStreamReader(shellProcess.getInputStream()));

                sendLineInternal("id");

                char[] buf = new char[512];
                int n;
                while ((n = shellOut.read(buf)) != -1) {
                    append(new String(buf, 0, n));
                    if (!shellReady) {
                        shellReady = true;
                        runOnUiThread(() -> {
                            append("\n");
                            Toast.makeText(this,
                                "ROOT shell ready! Type commands below.",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                shellReady = false;
                append("\n[su session closed]\n");
            } catch (Exception e) {
                append("[ERROR] No root shell: " + e.getMessage()
                    + "\nGrant su access in Magisk for PBLOCK!\n");
            }
        }).start();
    }

    private void sendLineInternal(String cmd) {
        try {
            if (shellIn == null) {
                return;
            }
            shellIn.writeBytes(cmd + "\n");
            shellIn.flush();
        } catch (Exception e) {
            append("[shell write error] " + e.getMessage() + "\n");
        }
    }

    private void sendLine(String cmd) {
        append("$ " + cmd + "\n");
        sendLineInternal(cmd);
    }

    private void sendInput() {
        String cmd = inputField.getText().toString().trim();
        if (cmd.isEmpty()) {
            return;
        }
        if ("exit".equals(cmd)) {
            finish();
            return;
        }
        inputField.setText("");
        sendLine(cmd);
    }

    private File prepareBlockFile() {
        try {
            File f = new File(getFilesDir(), "blocklist.hosts");
            FileWriter w = new FileWriter(f);
            w.write(PBlockHelper.generateBlockEntries());
            w.close();
            return f;
        } catch (Exception e) {
            Toast.makeText(this, "Cannot prepare blocklist: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private void startBlocking() {
        File f = prepareBlockFile();
        if (f == null) {
            return;
        }
        String path = f.getAbsolutePath();
        String startMarker = PBlockHelper.BLOCK_START_MARKER;
        sendLine("mount -o remount,rw /system 2>/dev/null; "
            + "mount -o remount,rw / 2>/dev/null || true; "
            + "if grep -q '" + startMarker + "' " + MainActivity.HOSTS_FILE
            + "; then echo ALREADY_ACTIVE; else "
            + "{ cat " + MainActivity.HOSTS_FILE + "; cat " + path + "; } > /data/local/tmp/pb_hosts && "
            + "cp -f /data/local/tmp/pb_hosts " + MainActivity.HOSTS_FILE
            + " && chmod 644 " + MainActivity.HOSTS_FILE
            + " && echo PBLOCK_STARTED || echo PBLOCK_START_FAILED; fi");
    }

    private void stopBlocking() {
        String startMarker = PBlockHelper.BLOCK_START_MARKER;
        String endMarker = PBlockHelper.BLOCK_END_MARKER;
        sendLine("mount -o remount,rw /system 2>/dev/null; "
            + "mount -o remount,rw / 2>/dev/null || true; "
            + "sed '/" + startMarker + "/,/" + endMarker + "/d' " + MainActivity.HOSTS_FILE
            + " > /data/local/tmp/pb_hosts && "
            + "cp -f /data/local/tmp/pb_hosts " + MainActivity.HOSTS_FILE
            + " && chmod 644 " + MainActivity.HOSTS_FILE
            + " && echo PBLOCK_STOPPED || echo PBLOCK_STOP_FAILED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (shellIn != null) {
                shellIn.writeBytes("exit\n");
                shellIn.flush();
            }
            if (shellProcess != null) {
                shellProcess.destroy();
            }
        } catch (Exception ignored) {
        }
    }
}
