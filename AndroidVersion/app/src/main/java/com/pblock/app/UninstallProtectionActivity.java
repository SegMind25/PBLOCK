package com.pblock.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Random;

public class UninstallProtectionActivity extends Activity {

    private static final int VOID = 0, RED = 1, GREEN = 2, BLUE = 3, YELLOW = 4, PURPLE = 5;
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    private static final String[] ARROW = {"\u2191", "\u2193", "\u2190", "\u2192"};
    private static final int MAX_STEPS = 50;
    private static final int STEP_MS = 180;
    private static final int TOTAL_PUZZLES = 10;

    private int[][] cells;
    private boolean[][] goals;
    private boolean[][] collected;
    private int W, H, startX, startY;
    private int botX, botY, botDir;
    private int goalsTotal, goalsCollected;

    private static class Cmd {
        boolean isFunc;
        int func, dir, guard;
        boolean isTurnLeft, isTurnRight, isJump;

        String label() {
            if (isTurnLeft) return "\u21BA";
            if (isTurnRight) return "\u21BB";
            if (isJump) return "\u2B62";
            String s = isFunc ? ("F" + func) : ARROW[dir];
            if (guard == RED) return "\u25CF" + s;
            if (guard == GREEN) return "\u25CF" + s;
            if (guard == BLUE) return "\u25CF" + s;
            if (guard == YELLOW) return "\u25CF" + s;
            if (guard == PURPLE) return "\u25CF" + s;
            return s;
        }

        int guardColor() {
            switch (guard) {
                case RED: return Color.parseColor("#FF4757");
                case GREEN: return Color.parseColor("#2ED573");
                case BLUE: return Color.parseColor("#3742FA");
                case YELLOW: return Color.parseColor("#FFA502");
                case PURPLE: return Color.parseColor("#A855F7");
                default: return Color.parseColor("#2F3542");
            }
        }

        int cmdColor() {
            if (isTurnLeft || isTurnRight) return Color.parseColor("#FF6348");
            if (isJump) return Color.parseColor("#A855F7");
            if (isFunc) return Color.parseColor("#7C3AED");
            switch (guard) {
                case RED: return Color.parseColor("#FF4757");
                case GREEN: return Color.parseColor("#2ED573");
                case BLUE: return Color.parseColor("#3742FA");
                case YELLOW: return Color.parseColor("#FFA502");
                case PURPLE: return Color.parseColor("#A855F7");
                default: return Color.parseColor("#3742FA");
            }
        }
    }

    private static final int SLOTS = 12;
    private final Cmd[] f1 = new Cmd[SLOTS];
    private final Cmd[] f2 = new Cmd[SLOTS];
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<int[]> frames = new ArrayDeque<>();

    private PuzzleBoardView board;
    private TextView msgView, progressText, titleView;
    private ProgressBar progressBar;
    private Button[][] f1Btns = new Button[SLOTS][];
    private Button[][] f2Btns = new Button[SLOTS][];
    private LinearLayout root;
    private boolean running;
    private int stepsUsed;
    private int puzzlesSolved;

    private static final int BG_DARK = Color.parseColor("#0F0F23");
    private static final int BG_CARD = Color.parseColor("#1A1A2E");
    private static final int ACCENT_BLUE = Color.parseColor("#4F8CFF");
    private static final int ACCENT_GREEN = Color.parseColor("#00E676");
    private static final int ACCENT_RED = Color.parseColor("#FF4757");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(BG_DARK);
        getWindow().setNavigationBarColor(BG_DARK);

        puzzlesSolved = 0;
        buildUI();
        generateNewPuzzle();
        show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("Solving all 10 puzzles is required to deactivate protection.\n\n"
                + "Content filtering will remain active until all challenges are completed.")
            .setPositiveButton("Continue", null)
            .setNegativeButton("Exit App", (d, w) -> finish())
            .show();
    }

    private void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_DARK);

        int pad = dp(16);
        root.setPadding(pad, dp(24), pad, pad);

        // Header with progress
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);

        titleView = new TextView(this);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(20);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(ACCENT_BLUE);
        header.addView(titleView);

        progressText = new TextView(this);
        progressText.setTextSize(13);
        progressText.setGravity(Gravity.CENTER);
        progressText.setTextColor(Color.parseColor("#8892B0"));
        progressText.setPadding(0, dp(4), 0, dp(8));
        header.addView(progressText);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(TOTAL_PUZZLES);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        plp.setMargins(dp(32), 0, dp(32), dp(12));
        header.addView(progressBar, plp);

        root.addView(header);

        // Board
        FrameLayout boardWrap = new FrameLayout(this);
        board = new PuzzleBoardView(this);
        boardWrap.addView(board, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(240)));
        boardWrap.setPadding(0, dp(4), 0, dp(4));
        root.addView(boardWrap);

        // Message
        msgView = new TextView(this);
        msgView.setTextSize(12);
        msgView.setGravity(Gravity.CENTER);
        msgView.setTextColor(Color.parseColor("#FFD93D"));
        msgView.setPadding(0, dp(4), 0, dp(4));
        root.addView(msgView);

        // F1 label
        root.addView(makeLabel("\u25B6 F1 (main)", ACCENT_BLUE));
        HorizontalScrollView sv1 = new HorizontalScrollView(this);
        sv1.addView(makeSlotRow(true));
        sv1.setHorizontalScrollBarEnabled(false);
        root.addView(sv1);

        // F2 label
        root.addView(makeLabel("\u25B6 F2", Color.parseColor("#A855F7")));
        HorizontalScrollView sv2 = new HorizontalScrollView(this);
        sv2.addView(makeSlotRow(false));
        sv2.setHorizontalScrollBarEnabled(false);
        root.addView(sv2);

        // Controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button runBtn = makeBtn("  RUN  \u25B6  ", ACCENT_GREEN);
        runBtn.setOnClickListener(v -> startRun());
        Button resetBtn = makeBtn("  RESET  ", Color.parseColor("#5352ED"));
        resetBtn.setOnClickListener(v -> resetRun());

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.setMargins(dp(4), dp(8), dp(4), dp(4));
        controls.addView(runBtn, clp);
        controls.addView(resetBtn, clp);
        root.addView(controls);

        // Exit
        TextView exitHint = new TextView(this);
        exitHint.setText("Solve ALL " + TOTAL_PUZZLES + " puzzles to allow uninstall");
        exitHint.setTextSize(11);
        exitHint.setGravity(Gravity.CENTER);
        exitHint.setTextColor(Color.parseColor("#576574"));
        exitHint.setPadding(0, dp(8), 0, 0);
        root.addView(exitHint);
    }

    private TextView makeLabel(String text, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(11);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(4), dp(10), 0, dp(2));
        return t;
    }

    private LinearLayout makeSlotRow(boolean isF1) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < SLOTS; i++) {
            Button btn = makeSlotButton();
            int idx = i;
            btn.setOnClickListener(v -> editSlot(isF1, idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(btn, lp);
            if (isF1) f1Btns[i] = new Button[]{btn};
            else f2Btns[i] = new Button[]{btn};
        }
        return row;
    }

    private Button makeSlotButton() {
        Button b = new Button(this);
        b.setText("\u2014");
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(Color.parseColor("#1A1A2E"));
        b.setElevation(dp(2));
        return b;
    }

    private Button makeBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        b.setElevation(dp(4));
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void generateNewPuzzle() {
        int legs = 6 + puzzlesSolved * 2;
        for (int attempt = 0; attempt < 500; attempt++) {
            W = 7 + rnd.nextInt(5);
            H = 6 + rnd.nextInt(4);
            cells = new int[H][W];
            goals = new boolean[H][W];
            startX = 1 + rnd.nextInt(W - 2);
            startY = 1 + rnd.nextInt(H - 2);
            botDir = rnd.nextInt(4);
            int curX = startX, curY = startY;
            int lastDir = -1;
            int prevColor = 1 + rnd.nextInt(5);
            cells[curY][curX] = prevColor;
            boolean ok = true;

            for (int leg = 0; leg < legs && ok; leg++) {
                java.util.List<Integer> dirs = new java.util.ArrayList<>();
                for (int d = 0; d < 4; d++) {
                    if (d != lastDir && (lastDir == -1 || d != opposite(lastDir))) dirs.add(d);
                }
                java.util.Collections.shuffle(dirs, rnd);
                int len = 2 + rnd.nextInt(5);
                int color;
                do { color = 1 + rnd.nextInt(5); } while (color == prevColor && rnd.nextBoolean());
                boolean placed = false;
                for (int dir : dirs) {
                    int tx = curX, ty = curY;
                    boolean fits = true;
                    for (int i = 0; i < len; i++) {
                        tx += DX[dir]; ty += DY[dir];
                        if (tx < 0 || ty < 0 || tx >= W || ty >= H || cells[ty][tx] != VOID) { fits = false; break; }
                    }
                    if (!fits) continue;
                    tx = curX; ty = curY;
                    for (int i = 0; i < len; i++) { tx += DX[dir]; ty += DY[dir]; cells[ty][tx] = color; }
                    curX = tx; curY = ty; lastDir = dir; prevColor = color; placed = true;
                    break;
                }
                if (!placed) ok = false;
            }
            if (!ok || countCells() < 10) continue;

            goals[curY][curX] = true;
            goalsTotal = 1;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (!goals[y][x] && cells[y][x] != VOID && rnd.nextInt(3) == 0) { goals[y][x] = true; goalsTotal++; }
                }
            }
            if (goalsTotal < 4) goalsTotal = 4;
            botX = startX; botY = startY;
            return;
        }
        // Fallback
        W = 9; H = 7;
        cells = new int[H][W]; goals = new boolean[H][W];
        for (int x = 0; x < W; x++) { cells[2][x] = RED; cells[4][x] = GREEN; }
        for (int y = 2; y < H; y++) { cells[y][3] = BLUE; cells[y][6] = YELLOW; }
        startX = 0; startY = 2; botDir = 3;
        botX = startX; botY = startY;
        goals[2][W-1] = true; goals[4][0] = true; goals[6][6] = true;
        goalsTotal = 3;
    }

    private int opposite(int d) { return d == 0 ? 1 : d == 1 ? 0 : d == 2 ? 3 : 2; }
    private int countCells() { int n=0; for (int y=0;y<H;y++) for (int x=0;x<W;x++) if (cells[y][x]!=VOID) n++; return n; }

    private void show() {
        setContentView(root);
        refreshUI();
        refreshSlots();
        resetBotPosition();
    }

    private void refreshUI() {
        String[] names = {"NEON GENESIS", "QUANTUM BREACH", "VOID PROTOCOL", "CYBER NEXUS",
            "HEX CASCADE", "PIXEL STORM", "MATRIX CORE", "FLUX ENGINE", "DATA VORTEX", "ALPHA SEQUENCE"};
        titleView.setText("CHALLENGE " + (puzzlesSolved + 1) + "/" + TOTAL_PUZZLES
            + ": " + names[puzzlesSolved % names.length]);
        progressText.setText("Solve puzzles to unlock uninstall | " + puzzlesSolved + "/" + TOTAL_PUZZLES + " completed");
        progressBar.setProgress(puzzlesSolved);
        msgView.setText("Collect all " + goalsTotal + " targets | Max steps: " + MAX_STEPS);
        msgView.setTextColor(Color.parseColor("#FFD93D"));
    }

    private void refreshSlots() {
        for (int i = 0; i < SLOTS; i++) {
            applySlot(f1Btns[i][0], f1[i]);
            applySlot(f2Btns[i][0], f2[i]);
        }
    }

    private void applySlot(Button btn, Cmd c) {
        if (c == null) {
            btn.setText("\u2014");
            btn.setBackgroundColor(Color.parseColor("#1A1A2E"));
        } else {
            btn.setText(c.label());
            btn.setBackgroundColor(c.cmdColor());
        }
    }

    private void editSlot(boolean isF1, int index) {
        if (running) return;
        final Cmd[] arr = isF1 ? f1 : f2;
        final int[] guard = {0};
        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), 0);

        TextView gTitle = new TextView(this);
        gTitle.setText("Color condition (optional)");
        gTitle.setTextColor(Color.parseColor("#8892B0"));
        gTitle.setTextSize(12);
        box.addView(gTitle);

        LinearLayout colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] cNames = {"None", "Red", "Green", "Blue", "Yellow", "Purple"};
        int[] cVals = {0, RED, GREEN, BLUE, YELLOW, PURPLE};
        int[] cColors = {Color.parseColor("#2F3542"), Color.parseColor("#FF4757"),
            Color.parseColor("#2ED573"), Color.parseColor("#3742FA"),
            Color.parseColor("#FFA502"), Color.parseColor("#A855F7")};
        final Button[] cBtns = new Button[6];
        for (int i = 0; i < 6; i++) {
            Button cb = makeBtn(cNames[i], cColors[i]);
            final int val = cVals[i];
            cBtns[i] = cb;
            cb.setOnClickListener(v -> {
                guard[0] = val;
                for (int j = 0; j < 6; j++) cb.setAlpha(cBtns[j] == cb ? 1f : 0.4f);
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(dp(2), 0, dp(2), 0);
            colorRow.addView(cb, clp);
        }
        box.addView(colorRow);

        TextView cTitle = new TextView(this);
        cTitle.setText("Command");
        cTitle.setTextColor(Color.parseColor("#8892B0"));
        cTitle.setTextSize(12);
        cTitle.setPadding(0, dp(12), 0, dp(4));
        box.addView(cTitle);

        LinearLayout moveRow = new LinearLayout(this);
        moveRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] dirColors = {Color.parseColor("#3742FA"), Color.parseColor("#3742FA"),
            Color.parseColor("#3742FA"), Color.parseColor("#3742FA")};
        for (int d = 0; d < 4; d++) {
            Button mb = makeBtn(ARROW[d], dirColors[d]);
            final int dir = d;
            mb.setOnClickListener(v -> {
                Cmd c = new Cmd(); c.dir = dir; c.guard = guard[0];
                arr[index] = c; refreshSlots(); holder[0].dismiss();
            });
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            mlp.setMargins(dp(2), 0, dp(2), 0);
            moveRow.addView(mb, mlp);
        }
        box.addView(moveRow);

        LinearLayout specialRow = new LinearLayout(this);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        Button tlBtn = makeBtn("\u21BA TL", Color.parseColor("#FF6348"));
        tlBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isTurnLeft = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button trBtn = makeBtn("\u21BB TR", Color.parseColor("#FF6348"));
        trBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isTurnRight = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button jpBtn = makeBtn("\u2B62 JP", Color.parseColor("#A855F7"));
        jpBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isJump = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button clrBtn = makeBtn("\u2715 CLR", ACCENT_RED);
        clrBtn.setOnClickListener(v -> {
            arr[index] = null; refreshSlots(); holder[0].dismiss();
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        flp.setMargins(dp(2), 0, dp(2), 0);
        specialRow.addView(tlBtn, flp); specialRow.addView(trBtn, flp);
        specialRow.addView(jpBtn, flp); specialRow.addView(clrBtn, flp);
        box.addView(specialRow);

        LinearLayout funcRow = new LinearLayout(this);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        Button f1b = makeBtn("F1", Color.parseColor("#7C3AED"));
        f1b.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isFunc = true; c.func = 1; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button f2b = makeBtn("F2", Color.parseColor("#7C3AED"));
        f2b.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isFunc = true; c.func = 2; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        funcRow.addView(f1b, flp); funcRow.addView(f2b, flp);
        box.addView(funcRow);

        AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle((isF1 ? "F1" : "F2") + " slot " + (index + 1))
            .setView(box)
            .setNegativeButton("Cancel", null)
            .create();
        holder[0] = dlg;
        dlg.show();
    }

    private void startRun() {
        if (running) return;
        boolean empty = true;
        for (Cmd c : f1) { if (c != null) { empty = false; break; } }
        if (empty) { msgView.setText("F1 is empty!"); return; }
        stepsUsed = 0;
        running = true;
        resetBotPosition();
        msgView.setText("Running...");
        frames.clear();
        frames.push(new int[]{1, 0});
        handler.postDelayed(this::step, STEP_MS);
    }

    private void step() {
        if (!running) return;
        if (frames.isEmpty()) { fail("Program ended - targets not collected"); return; }
        int[] frame = frames.peek();
        Cmd[] fn = frame[0] == 1 ? f1 : f2;
        if (frame[1] >= fn.length) { frames.pop(); handler.postDelayed(this::step, STEP_MS); return; }
        Cmd c = fn[frame[1]];
        frame[1]++;
        stepsUsed++;

        if (c == null) { handler.postDelayed(this::step, STEP_MS); return; }
        if (stepsUsed > MAX_STEPS) { fail("Step limit exceeded! Optimize your program."); return; }

        if (c.isFunc) {
            if (frames.size() >= 25) { fail("Stack overflow!"); return; }
            frames.push(new int[]{c.func, 0});
            handler.postDelayed(this::step, STEP_MS);
            return;
        }
        if (c.isTurnLeft) { botDir = (botDir + 3) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isTurnRight) { botDir = (botDir + 1) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isJump) {
            int nx = botX + DX[botDir] * 2, ny = botY + DY[botDir] * 2;
            if (nx<0||ny<0||nx>=W||ny>=H||cells[ny][nx]==VOID) { fail("Jump crash!"); return; }
            botX = nx; botY = ny;
            if (goals[ny][nx]&&!collected[ny][nx]) { collected[ny][nx]=true; goalsCollected++; }
            board.invalidate();
            if (goalsCollected>=goalsTotal) { win(); return; }
            handler.postDelayed(this::step, STEP_MS);
            return;
        }

        if (c.guard != 0) {
            if (cells[botY][botX] != c.guard) { handler.postDelayed(this::step, STEP_MS); return; }
        }

        int nx = botX + DX[c.dir], ny = botY + DY[c.dir];
        if (nx<0||ny<0||nx>=W||ny>=H||cells[ny][nx]==VOID) { fail("CRASH!"); return; }
        botX = nx; botY = ny; botDir = c.dir;
        if (goals[ny][nx]&&!collected[ny][nx]) { collected[ny][nx]=true; goalsCollected++; }
        board.invalidate();
        if (goalsCollected>=goalsTotal) { win(); return; }
        handler.postDelayed(this::step, STEP_MS);
    }

    private void win() {
        running = false;
        msgView.setText("CHALLENGE COMPLETE!");
        msgView.setTextColor(ACCENT_GREEN);
        puzzlesSolved++;
        handler.postDelayed(() -> {
            if (puzzlesSolved >= TOTAL_PUZZLES) {
                onAllPuzzlesSolved();
            } else {
                generateNewPuzzle();
                refreshUI();
                refreshSlots();
                resetBotPosition();
            }
        }, 1200);
    }

    private void fail(String reason) {
        running = false;
        msgView.setText(reason);
        msgView.setTextColor(ACCENT_RED);
        handler.postDelayed(this::resetRun, 800);
    }

    private void resetRun() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        resetBotPosition();
        if (msgView != null) {
            msgView.setTextColor(Color.parseColor("#FFD93D"));
            msgView.setText(goalsCollected + "/" + goalsTotal + " targets | Steps: 0/" + MAX_STEPS);
        }
    }

    private void resetBotPosition() {
        botX = startX; botY = startY;
        collected = new boolean[H][W];
        goalsCollected = 0;
        if (goals[startY][startX]) { collected[startY][startX] = true; goalsCollected = 1; }
        if (board != null) board.invalidate();
    }

    private void onAllPuzzlesSolved() {
        new AlertDialog.Builder(this)
            .setTitle("ALL CHALLENGES COMPLETED!")
            .setMessage("All " + TOTAL_PUZZLES + " puzzles solved.\n\n"
                + "Device Admin will now be deactivated.\n"
                + "You can then uninstall PBLOCK from Settings.")
            .setCancelable(false)
            .setPositiveButton("Deactivate & Uninstall", (d, w) -> {
                try {
                    DevicePolicyManager dpm = (DevicePolicyManager)
                        getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName admin = new ComponentName(this, PBlockDeviceAdminReceiver.class);
                    dpm.removeActiveAdmin(admin);
                } catch (Exception ignored) {}
                // Launch uninstall intent
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Keep Protected", (d, w) -> finish())
            .show();
    }

    private class PuzzleBoardView extends View {
        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint botPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PuzzleBoardView(Context ctx) { super(ctx); }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(BG_DARK);
            if (cells == null) return;
            float vW = getWidth(), vH = getHeight();
            float tile = Math.min(vW / W, vH / H) * 0.88f;
            float oX = (vW - tile * W) / 2f;
            float oY = (vH - tile * H) / 2f;

            // Grid lines
            gridPaint.setColor(Color.parseColor("#1A1A3E"));
            gridPaint.setStrokeWidth(1);
            for (int y = 0; y <= H; y++) {
                canvas.drawLine(oX, oY + y * tile, oX + W * tile, oY + y * tile, gridPaint);
            }
            for (int x = 0; x <= W; x++) {
                canvas.drawLine(oX + x * tile, oY, oX + x * tile, oY + H * tile, gridPaint);
            }

            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (cells[y][x] == VOID) continue;
                    float left = oX + x * tile + tile * 0.06f;
                    float top = oY + y * tile + tile * 0.06f;
                    float r = tile * 0.12f;
                    RectF rect = new RectF(left, top, left + tile * 0.88f, top + tile * 0.88f);

                    int color;
                    switch (cells[y][x]) {
                        case RED: color = Color.parseColor("#FF4757"); break;
                        case GREEN: color = Color.parseColor("#2ED573"); break;
                        case BLUE: color = Color.parseColor("#3742FA"); break;
                        case YELLOW: color = Color.parseColor("#FFA502"); break;
                        case PURPLE: color = Color.parseColor("#A855F7"); break;
                        default: color = Color.parseColor("#3742FA");
                    }

                    // Glow
                    glowPaint.setColor(color);
                    glowPaint.setAlpha(60);
                    RectF glowRect = new RectF(left - tile * 0.04f, top - tile * 0.04f,
                        left + tile * 0.96f, top + tile * 0.96f);
                    canvas.drawRoundRect(glowRect, r + tile * 0.04f, r + tile * 0.04f, glowPaint);

                    // Tile
                    tilePaint.setColor(color);
                    canvas.drawRoundRect(rect, r, r, tilePaint);

                    // Inner highlight
                    tilePaint.setColor(Color.WHITE);
                    tilePaint.setAlpha(30);
                    canvas.drawRoundRect(new RectF(left + tile * 0.08f, top + tile * 0.08f,
                        left + tile * 0.5f, top + tile * 0.5f), r * 0.5f, r * 0.5f, tilePaint);
                    tilePaint.setAlpha(255);

                    // Goal
                    if (goals[y][x]) {
                        boolean collectedHere = collected != null && collected[y][x];
                        goalPaint.setColor(collectedHere ? Color.parseColor("#44FFFFFF") : Color.WHITE);
                        canvas.drawCircle(rect.centerX(), rect.centerY(), tile * 0.15f, goalPaint);
                        if (!collectedHere) {
                            goalPaint.setColor(Color.WHITE);
                            goalPaint.setAlpha(120);
                            canvas.drawCircle(rect.centerX(), rect.centerY(), tile * 0.22f, goalPaint);
                            goalPaint.setAlpha(255);
                        }
                    }
                }
            }

            // Bot
            float bx = oX + botX * tile + tile * 0.06f;
            float by = oY + botY * tile + tile * 0.06f;
            float cx = bx + tile * 0.44f, cy = by + tile * 0.44f;

            // Bot glow
            glowPaint.setColor(ACCENT_GREEN);
            glowPaint.setAlpha(80);
            canvas.drawCircle(cx, cy, tile * 0.4f, glowPaint);

            // Bot body
            botPaint.setColor(ACCENT_GREEN);
            canvas.drawCircle(cx, cy, tile * 0.28f, botPaint);

            // Bot ring
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(tile * 0.04f);
            ringPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, tile * 0.28f, ringPaint);

            // Direction indicator
            float dx = DX[botDir] * tile * 0.2f;
            float dy = DY[botDir] * tile * 0.2f;
            botPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx + dx, cy + dy, tile * 0.06f, botPaint);
        }
    }
}
