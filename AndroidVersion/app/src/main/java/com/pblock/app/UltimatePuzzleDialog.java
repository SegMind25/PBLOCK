package com.pblock.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UltimatePuzzleDialog {

    public interface Listener {
        void onSolved();
        void onGivenUp();
    }

    private static final int VOID = 0, RED = 1, GREEN = 2, BLUE = 3, YELLOW = 4, PURPLE = 5;
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    private static final String[] ARROW = {"\u2191", "\u2193", "\u2190", "\u2192"};
    private static final int MAX_STEPS = 50, STEP_MS = 180, SLOTS = 12;

    private static class Cmd {
        boolean isFunc;
        int func, dir, guard;
        boolean isTurnLeft, isTurnRight, isJump;

        String label() {
            if (isTurnLeft) return "\u21BA";
            if (isTurnRight) return "\u21BB";
            if (isJump) return "\u2B62";
            String s = isFunc ? ("F" + func) : ARROW[dir];
            if (guard != 0) return "\u25CF" + s;
            return s;
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

    private final Context ctx;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final Cmd[] f1 = new Cmd[SLOTS];
    private final Cmd[] f2 = new Cmd[SLOTS];
    private final ArrayDeque<int[]> frames = new ArrayDeque<>();
    private final int puzzleIndex;

    private int[][] cells;
    private boolean[][] goals, collected;
    private int W, H, startX, startY, startDir;
    private int botX, botY, botDir;
    private int goalsTotal, goalsCollected;

    private PuzzleBoardView board;
    private TextView msgView, progressView;
    private Button[][] f1Btns = new Button[SLOTS][];
    private Button[][] f2Btns = new Button[SLOTS][];
    private AlertDialog dialog;
    private LinearLayout root;
    private boolean running;
    private int stepsUsed;

    private static final int BG = Color.parseColor("#0F0F23");
    private static final int CARD = Color.parseColor("#1A1A2E");

    public UltimatePuzzleDialog(Context ctx, int puzzleIndex, Listener listener) {
        this.ctx = ctx;
        this.puzzleIndex = puzzleIndex;
        this.listener = listener;
        generateHardLevel(puzzleIndex);
    }

    private void generateHardLevel(int idx) {
        int legs = 4 + (idx * 2);
        for (int attempt = 0; attempt < 500; attempt++) {
            W = 7 + rnd.nextInt(5); H = 6 + rnd.nextInt(4);
            cells = new int[H][W]; goals = new boolean[H][W];
            startX = 1 + rnd.nextInt(W - 2); startY = 1 + rnd.nextInt(H - 2);
            startDir = rnd.nextInt(4);
            int curX = startX, curY = startY, lastDir = -1;
            int prevColor = 1 + rnd.nextInt(5);
            cells[curY][curX] = prevColor;
            boolean ok = true;
            for (int leg = 0; leg < legs && ok; leg++) {
                List<Integer> dirs = new ArrayList<>();
                for (int d = 0; d < 4; d++) {
                    if (d != lastDir && (lastDir == -1 || d != opposite(lastDir))) dirs.add(d);
                }
                Collections.shuffle(dirs, rnd);
                int len = 2 + rnd.nextInt(5);
                int color; do { color = 1 + rnd.nextInt(5); } while (color == prevColor && rnd.nextBoolean());
                boolean placed = false;
                for (int dir : dirs) {
                    int tx = curX, ty = curY; boolean fits = true;
                    for (int i = 0; i < len; i++) {
                        tx += DX[dir]; ty += DY[dir];
                        if (tx < 0 || ty < 0 || tx >= W || ty >= H || cells[ty][tx] != VOID) { fits = false; break; }
                    }
                    if (!fits) continue;
                    tx = curX; ty = curY;
                    for (int i = 0; i < len; i++) { tx += DX[dir]; ty += DY[dir]; cells[ty][tx] = color; }
                    curX = tx; curY = ty; lastDir = dir; prevColor = color; placed = true; break;
                }
                if (!placed) ok = false;
            }
            if (!ok || countCells() < 10) continue;
            goals[curY][curX] = true; goalsTotal = 1;
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++)
                    if (!goals[y][x] && cells[y][x] != VOID && rnd.nextInt(3) == 0) { goals[y][x] = true; goalsTotal++; }
            if (goalsTotal < 4) goalsTotal = 4;
            botX = startX; botY = startY; botDir = startDir; return;
        }
        W = 9; H = 7; cells = new int[H][W]; goals = new boolean[H][W];
        for (int x = 0; x < W; x++) { cells[2][x] = RED; cells[4][x] = GREEN; }
        for (int y = 2; y < H; y++) { cells[y][3] = BLUE; cells[y][6] = YELLOW; }
        startX = 0; startY = 2; startDir = 3; botX = startX; botY = startY; botDir = startDir;
        goals[2][W - 1] = true; goals[4][0] = true; goals[6][6] = true; goalsTotal = 3;
    }

    private int opposite(int d) { return d == 0 ? 1 : d == 1 ? 0 : d == 2 ? 3 : 2; }
    private int countCells() { int n = 0; for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if (cells[y][x] != VOID) n++; return n; }
    private int dp(int v) { return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f); }

    private String getPuzzleName() {
        String[] names = {"NEON GENESIS", "QUANTUM BREACH", "VOID PROTOCOL", "CYBER NEXUS",
            "HEX CASCADE", "PIXEL STORM", "MATRIX CORE", "FLUX ENGINE", "DATA VORTEX", "ALPHA SEQUENCE"};
        return names[puzzleIndex % names.length];
    }

    public void show() {
        int pad = dp(14);
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(BG);

        TextView title = new TextView(ctx);
        title.setText("\u26A1 " + getPuzzleName());
        title.setTextColor(Color.parseColor("#4F8CFF"));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        progressView = new TextView(ctx);
        progressView.setText("Challenge " + (puzzleIndex + 1) + "/10 | Collect ALL targets");
        progressView.setTextColor(Color.parseColor("#8892B0"));
        progressView.setTextSize(12);
        progressView.setGravity(Gravity.CENTER);
        progressView.setPadding(0, dp(2), 0, dp(8));
        root.addView(progressView);

        FrameLayout boardWrap = new FrameLayout(ctx);
        board = new PuzzleBoardView(ctx);
        boardWrap.addView(board, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(220)));
        boardWrap.setPadding(0, dp(4), 0, dp(4));
        root.addView(boardWrap);

        msgView = new TextView(ctx);
        msgView.setTextColor(Color.parseColor("#FFD93D"));
        msgView.setTextSize(12);
        msgView.setGravity(Gravity.CENTER);
        msgView.setPadding(0, dp(4), 0, dp(4));
        root.addView(msgView);

        root.addView(makeLabel("F1 (main)", Color.parseColor("#4F8CFF")));
        HorizontalScrollView sv1 = new HorizontalScrollView(ctx);
        sv1.setHorizontalScrollBarEnabled(false);
        sv1.addView(makeSlotRow(true));
        root.addView(sv1);

        root.addView(makeLabel("F2", Color.parseColor("#A855F7")));
        HorizontalScrollView sv2 = new HorizontalScrollView(ctx);
        sv2.setHorizontalScrollBarEnabled(false);
        sv2.addView(makeSlotRow(false));
        root.addView(sv2);

        LinearLayout controls = new LinearLayout(ctx);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        Button runBtn = makeBtn("  RUN  \u25B6  ", Color.parseColor("#2ED573"));
        runBtn.setOnClickListener(v -> startRun());
        Button resetBtn = makeBtn("  RESET  ", Color.parseColor("#5352ED"));
        resetBtn.setOnClickListener(v -> resetRun());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.setMargins(dp(4), dp(8), dp(4), dp(4));
        controls.addView(runBtn, clp); controls.addView(resetBtn, clp);
        root.addView(controls);

        Button giveUp = makeBtn("Give up (restart ALL 10)", Color.parseColor("#FF4757"));
        giveUp.setOnClickListener(v -> { stopRun(); dialog.dismiss(); listener.onGivenUp(); });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, dp(6), 0, 0);
        root.addView(giveUp, glp);

        resetRun(); refreshSlots();

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(null); b.setView(root); b.setCancelable(false);
        dialog = b.create(); dialog.show();
    }

    private TextView makeLabel(String text, int color) {
        TextView t = new TextView(ctx);
        t.setText(text); t.setTextColor(color); t.setTextSize(11);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(2), dp(6), 0, dp(2));
        return t;
    }

    private LinearLayout makeSlotRow(boolean isF1) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < SLOTS; i++) {
            Button btn = new Button(ctx);
            btn.setText("\u2014"); btn.setTextSize(10); btn.setAllCaps(false);
            btn.setTextColor(Color.WHITE); btn.setPadding(0, 0, 0, 0);
            btn.setBackgroundColor(CARD);
            int idx = i;
            btn.setOnClickListener(v -> editSlot(isF1, idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(btn, lp);
            if (isF1) f1Btns[i] = new Button[]{btn}; else f2Btns[i] = new Button[]{btn};
        }
        return row;
    }

    private Button makeBtn(String text, int color) {
        Button b = new Button(ctx);
        b.setText(text); b.setTextSize(12); b.setTypeface(null, Typeface.BOLD);
        b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundColor(color);
        return b;
    }

    private void refreshSlots() {
        for (int i = 0; i < SLOTS; i++) {
            applySlot(f1Btns[i][0], f1[i]); applySlot(f2Btns[i][0], f2[i]);
        }
    }

    private void applySlot(Button btn, Cmd c) {
        if (c == null) { btn.setText("\u2014"); btn.setBackgroundColor(CARD); }
        else { btn.setText(c.label()); btn.setBackgroundColor(c.cmdColor()); }
    }

    private void editSlot(boolean isF1, int index) {
        if (running) return;
        final Cmd[] arr = isF1 ? f1 : f2;
        final int[] guard = {0};
        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), 0);

        TextView gTitle = new TextView(ctx);
        gTitle.setText("Color condition"); gTitle.setTextColor(Color.parseColor("#8892B0")); gTitle.setTextSize(12);
        box.addView(gTitle);

        LinearLayout colorRow = new LinearLayout(ctx);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] cN = {"None", "Red", "Green", "Blue", "Yellow", "Purple"};
        int[] cV = {0, RED, GREEN, BLUE, YELLOW, PURPLE};
        int[] cC = {CARD, Color.parseColor("#FF4757"), Color.parseColor("#2ED573"),
            Color.parseColor("#3742FA"), Color.parseColor("#FFA502"), Color.parseColor("#A855F7")};
        final Button[] cBtns = new Button[6];
        for (int i = 0; i < 6; i++) {
            Button cb = makeBtn(cN[i], cC[i]); cBtns[i] = cb; final int val = cV[i];
            cb.setOnClickListener(v -> { guard[0] = val; for (int j = 0; j < 6; j++) cb.setAlpha(cBtns[j] == cb ? 1f : 0.4f); });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(dp(2), 0, dp(2), 0);
            colorRow.addView(cb, clp);
        }
        box.addView(colorRow);

        TextView cTitle = new TextView(ctx);
        cTitle.setText("Command"); cTitle.setTextColor(Color.parseColor("#8892B0")); cTitle.setTextSize(12);
        cTitle.setPadding(0, dp(12), 0, dp(4));
        box.addView(cTitle);

        LinearLayout moveRow = new LinearLayout(ctx);
        moveRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int d = 0; d < 4; d++) {
            Button mb = makeBtn(ARROW[d], Color.parseColor("#3742FA")); final int dir = d;
            mb.setOnClickListener(v -> {
                Cmd c = new Cmd(); c.dir = dir; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss();
            });
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            mlp.setMargins(dp(2), 0, dp(2), 0);
            moveRow.addView(mb, mlp);
        }
        box.addView(moveRow);

        LinearLayout specialRow = new LinearLayout(ctx);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        Button tlBtn = makeBtn("\u21BA TL", Color.parseColor("#FF6348"));
        tlBtn.setOnClickListener(v -> { Cmd c = new Cmd(); c.isTurnLeft = true; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss(); });
        Button trBtn = makeBtn("\u21BB TR", Color.parseColor("#FF6348"));
        trBtn.setOnClickListener(v -> { Cmd c = new Cmd(); c.isTurnRight = true; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss(); });
        Button jpBtn = makeBtn("\u2B62 JP", Color.parseColor("#A855F7"));
        jpBtn.setOnClickListener(v -> { Cmd c = new Cmd(); c.isJump = true; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss(); });
        Button clrBtn = makeBtn("\u2715", Color.parseColor("#FF4757"));
        clrBtn.setOnClickListener(v -> { arr[index] = null; refreshSlots(); holder[0].dismiss(); });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        flp.setMargins(dp(2), 0, dp(2), 0);
        specialRow.addView(tlBtn, flp); specialRow.addView(trBtn, flp);
        specialRow.addView(jpBtn, flp); specialRow.addView(clrBtn, flp);
        box.addView(specialRow);

        LinearLayout funcRow = new LinearLayout(ctx);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        Button f1b = makeBtn("F1", Color.parseColor("#7C3AED"));
        f1b.setOnClickListener(v -> { Cmd c = new Cmd(); c.isFunc = true; c.func = 1; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss(); });
        Button f2b = makeBtn("F2", Color.parseColor("#7C3AED"));
        f2b.setOnClickListener(v -> { Cmd c = new Cmd(); c.isFunc = true; c.func = 2; c.guard = guard[0]; arr[index] = c; refreshSlots(); holder[0].dismiss(); });
        funcRow.addView(f1b, flp); funcRow.addView(f2b, flp);
        box.addView(funcRow);

        AlertDialog dlg = new AlertDialog.Builder(ctx)
            .setTitle((isF1 ? "F1" : "F2") + " slot " + (index + 1))
            .setView(box).setNegativeButton("Cancel", null).create();
        holder[0] = dlg; dlg.show();
    }

    private void startRun() {
        if (running) return;
        boolean empty = true; for (Cmd c : f1) { if (c != null) { empty = false; break; } }
        if (empty) { msgView.setText("F1 is empty!"); return; }
        stepsUsed = 0; running = true; resetBotPosition();
        msgView.setText("Running...");
        frames.clear(); frames.push(new int[]{1, 0});
        handler.postDelayed(this::step, STEP_MS);
    }

    private void step() {
        if (!running) return;
        if (frames.isEmpty()) { fail("Program ended - targets not collected"); return; }
        int[] frame = frames.peek();
        Cmd[] fn = frame[0] == 1 ? f1 : f2;
        if (frame[1] >= fn.length) { frames.pop(); handler.postDelayed(this::step, STEP_MS); return; }
        Cmd c = fn[frame[1]]; frame[1]++; stepsUsed++;
        if (c == null) { handler.postDelayed(this::step, STEP_MS); return; }
        if (stepsUsed > MAX_STEPS) { fail("Step limit exceeded! Optimize."); return; }
        if (c.isFunc) { if (frames.size() >= 25) { fail("Stack overflow!"); return; } frames.push(new int[]{c.func, 0}); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isTurnLeft) { botDir = (botDir + 3) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isTurnRight) { botDir = (botDir + 1) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isJump) {
            int nx = botX + DX[botDir] * 2, ny = botY + DY[botDir] * 2;
            if (nx < 0 || ny < 0 || nx >= W || ny >= H || cells[ny][nx] == VOID) { fail("Jump crash!"); return; }
            botX = nx; botY = ny;
            if (goals[ny][nx] && !collected[ny][nx]) { collected[ny][nx] = true; goalsCollected++; }
            board.invalidate();
            if (goalsCollected >= goalsTotal) { win(); return; }
            handler.postDelayed(this::step, STEP_MS); return;
        }
        if (c.guard != 0 && cells[botY][botX] != c.guard) { handler.postDelayed(this::step, STEP_MS); return; }
        int nx = botX + DX[c.dir], ny = botY + DY[c.dir];
        if (nx < 0 || ny < 0 || nx >= W || ny >= H || cells[ny][nx] == VOID) { fail("CRASH!"); return; }
        botX = nx; botY = ny; botDir = c.dir;
        if (goals[ny][nx] && !collected[ny][nx]) { collected[ny][nx] = true; goalsCollected++; }
        board.invalidate();
        if (goalsCollected >= goalsTotal) { win(); return; }
        handler.postDelayed(this::step, STEP_MS);
    }

    private void win() {
        running = false;
        msgView.setText("\u2714 CHALLENGE COMPLETE!");
        msgView.setTextColor(Color.parseColor("#2ED573"));
        handler.postDelayed(() -> { if (dialog.isShowing()) dialog.dismiss(); listener.onSolved(); }, 900);
    }

    private void fail(String reason) {
        running = false;
        msgView.setText(reason); msgView.setTextColor(Color.parseColor("#FF4757"));
        handler.postDelayed(this::resetRun, 800);
    }

    private void stopRun() { running = false; handler.removeCallbacksAndMessages(null); }

    private void resetRun() {
        stopRun(); resetBotPosition();
        if (msgView != null) {
            msgView.setTextColor(Color.parseColor("#FFD93D"));
            msgView.setText(goalsCollected + "/" + goalsTotal + " targets | Steps: 0/" + MAX_STEPS);
        }
    }

    private void resetBotPosition() {
        botX = startX; botY = startY; botDir = startDir;
        collected = new boolean[H][W]; goalsCollected = 0;
        if (goals[startY][startX]) { collected[startY][startX] = true; goalsCollected = 1; }
        if (board != null) board.invalidate();
    }

    private class PuzzleBoardView extends View {
        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint botPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PuzzleBoardView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(BG);
            if (cells == null) return;
            float vW = getWidth(), vH = getHeight();
            float tile = Math.min(vW / W, vH / H) * 0.88f;
            float oX = (vW - tile * W) / 2f, oY = (vH - tile * H) / 2f;

            gridPaint.setColor(Color.parseColor("#1A1A3E")); gridPaint.setStrokeWidth(1);
            for (int y = 0; y <= H; y++) canvas.drawLine(oX, oY + y * tile, oX + W * tile, oY + y * tile, gridPaint);
            for (int x = 0; x <= W; x++) canvas.drawLine(oX + x * tile, oY, oX + x * tile, oY + H * tile, gridPaint);

            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (cells[y][x] == VOID) continue;
                    float left = oX + x * tile + tile * 0.06f;
                    float top = oY + y * tile + tile * 0.06f;
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
                    glowPaint.setColor(color); glowPaint.setAlpha(50);
                    canvas.drawRoundRect(new RectF(left - tile * 0.03f, top - tile * 0.03f,
                        left + tile * 0.94f, top + tile * 0.94f), tile * 0.15f, tile * 0.15f, glowPaint);
                    tilePaint.setColor(color);
                    canvas.drawRoundRect(rect, tile * 0.12f, tile * 0.12f, tilePaint);
                    tilePaint.setColor(Color.WHITE); tilePaint.setAlpha(25);
                    canvas.drawRoundRect(new RectF(left + tile * 0.08f, top + tile * 0.08f,
                        left + tile * 0.45f, top + tile * 0.45f), tile * 0.06f, tile * 0.06f, tilePaint);
                    tilePaint.setAlpha(255);
                    if (goals[y][x]) {
                        boolean collectedHere = collected != null && collected[y][x];
                        goalPaint.setColor(collectedHere ? Color.parseColor("#44FFFFFF") : Color.WHITE);
                        canvas.drawCircle(rect.centerX(), rect.centerY(), tile * 0.15f, goalPaint);
                        if (!collectedHere) { goalPaint.setAlpha(100); canvas.drawCircle(rect.centerX(), rect.centerY(), tile * 0.22f, goalPaint); goalPaint.setAlpha(255); }
                    }
                }
            }
            float bx = oX + botX * tile + tile * 0.06f, by = oY + botY * tile + tile * 0.06f;
            float cx = bx + tile * 0.44f, cy = by + tile * 0.44f;
            glowPaint.setColor(Color.parseColor("#2ED573")); glowPaint.setAlpha(70);
            canvas.drawCircle(cx, cy, tile * 0.38f, glowPaint);
            botPaint.setColor(Color.parseColor("#2ED573"));
            canvas.drawCircle(cx, cy, tile * 0.28f, botPaint);
            ringPaint.setStyle(Paint.Style.STROKE); ringPaint.setStrokeWidth(tile * 0.04f);
            ringPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, tile * 0.28f, ringPaint);
            float dx = DX[botDir] * tile * 0.18f, dy = DY[botDir] * tile * 0.18f;
            botPaint.setColor(Color.WHITE);
            canvas.drawCircle(cx + dx, cy + dy, tile * 0.05f, botPaint);
        }
    }
}
