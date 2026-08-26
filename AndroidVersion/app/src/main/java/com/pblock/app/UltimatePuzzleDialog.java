package com.pblock.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

    private static final int VOID = 0;
    private static final int RED = 1;
    private static final int GREEN = 2;
    private static final int BLUE = 3;
    private static final int YELLOW = 4;
    private static final int PURPLE = 5;

    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 1};
    private static final String[] ARROW = {"\u2191", "\u2193", "\u2190", "\u2192"};
    private static final int[] DIRS = {0, 1, 2, 3};

    private static final int MAX_STEPS = 50;
    private static final int STEP_MS = 200;
    private static final int SLOTS = 12;

    private static class Cmd {
        boolean isFunc;
        int func;
        int dir;
        int guard;
        boolean isTurnLeft;
        boolean isTurnRight;
        boolean isJump;

        String label() {
            if (isTurnLeft) return "TL";
            if (isTurnRight) return "TR";
            if (isJump) return "JP";
            String s = isFunc ? ("F" + func) : ARROW[dir];
            if (guard == RED) return "R" + s;
            if (guard == GREEN) return "G" + s;
            if (guard == BLUE) return "B" + s;
            if (guard == YELLOW) return "Y" + s;
            if (guard == PURPLE) return "P" + s;
            return s;
        }

        int guardColorValue() {
            if (guard == RED) return Color.parseColor("#E53935");
            if (guard == GREEN) return Color.parseColor("#43A047");
            if (guard == BLUE) return Color.parseColor("#1E88E5");
            if (guard == YELLOW) return Color.parseColor("#FDD835");
            if (guard == PURPLE) return Color.parseColor("#8E24AA");
            return Color.parseColor("#37474F");
        }
    }

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final int puzzleIndex;

    private int[][] cells;
    private boolean[][] goals;
    private boolean[][] collected;
    private int W, H, startX, startY, startDir;
    private int botX, botY, botDir;
    private int goalsTotal, goalsCollected;

    private final Cmd[] f1 = new Cmd[SLOTS];
    private final Cmd[] f2 = new Cmd[SLOTS];

    private AlgoBoardView board;
    private TextView msgView;
    private Button[][] f1Btns = new Button[SLOTS][];
    private Button[][] f2Btns = new Button[SLOTS][];
    private AlertDialog dialog;
    private LinearLayout root;

    private boolean running;
    private int stepsUsed;
    private final ArrayDeque<int[]> frames = new ArrayDeque<>();

    public UltimatePuzzleDialog(Context context, int puzzleIndex, Listener listener) {
        this.context = context;
        this.puzzleIndex = puzzleIndex;
        this.listener = listener;
        generateHardLevel(puzzleIndex);
    }

    private void generateHardLevel(int idx) {
        int legs = 4 + (idx * 2);
        for (int attempt = 0; attempt < 500; attempt++) {
            W = 7 + rnd.nextInt(5);
            H = 6 + rnd.nextInt(4);
            cells = new int[H][W];
            goals = new boolean[H][W];
            startX = 1 + rnd.nextInt(W - 2);
            startY = 1 + rnd.nextInt(H - 2);
            startDir = rnd.nextInt(4);
            int curX = startX, curY = startY;
            int lastDir = -1;
            int prevColor = 1 + rnd.nextInt(5);
            cells[curY][curX] = prevColor;
            boolean ok = true;
            int lastX = curX, lastY = curY;

            for (int leg = 0; leg < legs && ok; leg++) {
                List<Integer> dirs = new ArrayList<>();
                for (int d = 0; d < 4; d++) {
                    if (d != lastDir && (lastDir == -1 || d != opposite(lastDir))) {
                        dirs.add(d);
                    }
                }
                Collections.shuffle(dirs, rnd);
                int len = 2 + rnd.nextInt(5);
                int color;
                do {
                    color = 1 + rnd.nextInt(5);
                } while (color == prevColor && rnd.nextBoolean());
                boolean placed = false;
                for (int dir : dirs) {
                    int tx = curX, ty = curY;
                    boolean fits = true;
                    for (int i = 0; i < len; i++) {
                        tx += DX[dir];
                        ty += DY[dir];
                        if (tx < 0 || ty < 0 || tx >= W || ty >= H || cells[ty][tx] != VOID) {
                            fits = false;
                            break;
                        }
                    }
                    if (!fits) continue;
                    tx = curX;
                    ty = curY;
                    for (int i = 0; i < len; i++) {
                        tx += DX[dir];
                        ty += DY[dir];
                        cells[ty][tx] = color;
                    }
                    curX = tx;
                    curY = ty;
                    lastDir = dir;
                    prevColor = color;
                    placed = true;
                    break;
                }
                if (!placed) ok = false;
                lastX = curX;
                lastY = curY;
            }
            if (!ok || countCells() < 10) continue;

            goals[lastY][lastX] = true;
            goalsTotal = 1;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (!goals[y][x] && cells[y][x] != VOID && rnd.nextInt(3) == 0) {
                        goals[y][x] = true;
                        goalsTotal++;
                    }
                }
            }
            if (goalsTotal < 4) goalsTotal = 4;
            botX = startX;
            botY = startY;
            botDir = startDir;
            return;
        }
        W = 9;
        H = 7;
        cells = new int[H][W];
        goals = new boolean[H][W];
        for (int x = 0; x < W; x++) { cells[2][x] = RED; cells[4][x] = GREEN; }
        for (int y = 2; y < H; y++) { cells[y][3] = BLUE; cells[y][6] = YELLOW; }
        cells[3][1] = PURPLE; cells[3][5] = PURPLE;
        startX = 0; startY = 2; startDir = 3;
        botX = startX; botY = startY; botDir = startDir;
        goals[2][W - 1] = true; goals[4][0] = true; goals[6][6] = true; goals[0][3] = true;
        goalsTotal = 4;
    }

    private int opposite(int dir) {
        if (dir == 0) return 1;
        if (dir == 1) return 0;
        if (dir == 2) return 3;
        return 2;
    }

    private int countCells() {
        int n = 0;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++)
                if (cells[y][x] != VOID) n++;
        return n;
    }

    private String getPuzzleName() {
        String[] names = {
            "TURBO BOT", "CYBER MAZE", "QUANTUM PATH", "NEON LABYRINTH", "MATRIX RUNNER",
            "VOID NAVIGATOR", "PIXEL STORM", "HEX TRACE", "FLUX GRID", "CORE BREACH"
        };
        return names[puzzleIndex % names.length];
    }

    public void show() {
        int pad = dp(14);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#0D1117"));

        TextView title = new TextView(context);
        title.setText("CHALLENGE " + (puzzleIndex + 1) + "/10: " + getPuzzleName() + "\nCollect ALL targets. Harder path, tighter constraints!");
        title.setTextColor(Color.parseColor("#58A6FF"));
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        FrameLayout boardWrap = new FrameLayout(context);
        board = new AlgoBoardView(context);
        int boardH = dp(220);
        boardWrap.addView(board, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, boardH));
        boardWrap.setPadding(0, dp(8), 0, dp(4));
        root.addView(boardWrap);

        msgView = new TextView(context);
        msgView.setTextColor(Color.parseColor("#FFD54F"));
        msgView.setTextSize(12);
        msgView.setGravity(Gravity.CENTER);
        msgView.setText("Slots: " + SLOTS + " | Max steps: " + MAX_STEPS
            + " | TL=Turn Left, TR=Turn Right\nR/G/B/Y/P = only on that color. "
            + "Loop: put F1 in F1.");
        root.addView(msgView);

        root.addView(sectionLabel("F1 (main)"));
        HorizontalScrollView sv1 = new HorizontalScrollView(context);
        sv1.addView(slotRow(true));
        root.addView(sv1);

        root.addView(sectionLabel("F2"));
        HorizontalScrollView sv2 = new HorizontalScrollView(context);
        sv2.addView(slotRow(false));
        root.addView(sv2);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        Button runBtn = styledButton("RUN \u25B6", "#238636");
        runBtn.setOnClickListener(v -> startRun());
        Button resetBtn = styledButton("Reset", "#30363D");
        resetBtn.setOnClickListener(v -> resetRun());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rlp.setMargins(dp(4), dp(6), dp(4), 0);
        LinearLayout.LayoutParams rlp2 = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rlp2.setMargins(dp(4), dp(6), dp(4), 0);
        controls.addView(runBtn, rlp);
        controls.addView(resetBtn, rlp2);
        root.addView(controls);

        Button giveUp = styledButton("Give up (restart ALL 10)", "#DA3633");
        giveUp.setOnClickListener(v -> {
            stopRun();
            dialog.dismiss();
            listener.onGivenUp();
        });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, dp(6), 0, 0);
        root.addView(giveUp, glp);

        resetRun();
        refreshSlots();

        AlertDialog.Builder b = new AlertDialog.Builder(context,
            android.R.style.Theme_DeviceDefault_Dialog_Alert);
        b.setTitle(null);
        b.setView(root);
        b.setCancelable(false);
        dialog = b.create();
        dialog.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE);
        dialog.show();
    }

    private TextView sectionLabel(String s) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextColor(Color.parseColor("#8B949E"));
        t.setTextSize(11);
        t.setPadding(dp(2), dp(6), 0, dp(2));
        return t;
    }

    private LinearLayout slotRow(boolean isF1) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < SLOTS; i++) {
            Button btn = styledButton("\u2013", "#21262D");
            int idx = i;
            btn.setOnClickListener(v -> editSlot(isF1, idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(btn, lp);
            if (isF1) f1Btns[i] = new Button[]{btn};
            else f2Btns[i] = new Button[]{btn};
        }
        return row;
    }

    private Button styledButton(String text, String colorHex) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextSize(10);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            Color.parseColor(colorHex)));
        return b;
    }

    private int dp(int v) {
        return (int) (v * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refreshSlots() {
        for (int i = 0; i < SLOTS; i++) {
            applySlotLabel(f1Btns[i][0], f1[i]);
            applySlotLabel(f2Btns[i][0], f2[i]);
        }
    }

    private void applySlotLabel(Button btn, Cmd c) {
        if (c == null) {
            btn.setText("\u2013");
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor("#21262D")));
        } else {
            btn.setText(c.label());
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                c.guardColorValue()));
        }
    }

    private void editSlot(boolean isF1, int index) {
        if (running) return;
        final Cmd[] arr = isF1 ? f1 : f2;
        final Cmd current = arr[index];
        final int[] guard = {current == null ? 0 : current.guard};
        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), 0);

        TextView gTitle = new TextView(context);
        gTitle.setText("1. Color condition:");
        gTitle.setTextSize(12);
        box.addView(gTitle);

        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"None", "Red", "Green", "Blue", "Yellow", "Purple"};
        int[] vals = {0, RED, GREEN, BLUE, YELLOW, PURPLE};
        final Button[] colorBtns = new Button[6];
        for (int i = 0; i < 6; i++) {
            final int val = vals[i];
            Button cb = styledButton(names[i], "#30363D");
            colorBtns[i] = cb;
            cb.setOnClickListener(v -> {
                guard[0] = val;
                for (int j = 0; j < 6; j++)
                    colorBtns[j].setAlpha(vals[j] == guard[0] ? 1f : 0.4f);
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(dp(2), 0, dp(2), 0);
            colorRow.addView(cb, clp);
        }
        box.addView(colorRow);
        for (int j = 0; j < 6; j++)
            colorBtns[j].setAlpha(vals[j] == guard[0] ? 1f : 0.4f);

        TextView cTitle = new TextView(context);
        cTitle.setText("2. Command:");
        cTitle.setTextSize(12);
        cTitle.setPadding(0, dp(8), 0, dp(2));
        box.addView(cTitle);

        LinearLayout moveRow = new LinearLayout(context);
        moveRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int d = 0; d < 4; d++) {
            final int dir = d;
            Button mb = styledButton(ARROW[d], "#1F6FEB");
            mb.setOnClickListener(v -> {
                Cmd c = new Cmd();
                c.dir = dir;
                c.guard = guard[0];
                arr[index] = c;
                refreshSlots();
                holder[0].dismiss();
            });
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            mlp.setMargins(dp(2), 0, dp(2), 0);
            moveRow.addView(mb, mlp);
        }
        box.addView(moveRow);

        LinearLayout specialRow = new LinearLayout(context);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        Button tlBtn = styledButton("TL", "#B08800");
        tlBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isTurnLeft = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button trBtn = styledButton("TR", "#B08800");
        trBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isTurnRight = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button jpBtn = styledButton("JP", "#8957E5");
        jpBtn.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isJump = true; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button clrBtn = styledButton("X", "#DA3633");
        clrBtn.setOnClickListener(v -> {
            arr[index] = null; refreshSlots(); holder[0].dismiss();
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        flp.setMargins(dp(2), 0, dp(2), 0);
        specialRow.addView(tlBtn, flp);
        specialRow.addView(trBtn, flp);
        specialRow.addView(jpBtn, flp);
        specialRow.addView(clrBtn, flp);
        box.addView(specialRow);

        LinearLayout funcRow = new LinearLayout(context);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        Button f1b = styledButton("F1", "#8957E5");
        f1b.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isFunc = true; c.func = 1; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        Button f2b = styledButton("F2", "#8957E5");
        f2b.setOnClickListener(v -> {
            Cmd c = new Cmd(); c.isFunc = true; c.func = 2; c.guard = guard[0];
            arr[index] = c; refreshSlots(); holder[0].dismiss();
        });
        funcRow.addView(f1b, flp);
        funcRow.addView(f2b, flp);
        box.addView(funcRow);

        AlertDialog dlg = new AlertDialog.Builder(context)
            .setTitle((isF1 ? "F1" : "F2") + " slot " + (index + 1))
            .setView(box)
            .setNegativeButton("Cancel", null)
            .create();
        dlg.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE);
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
        if (frames.isEmpty()) { fail("Program ended - targets not collected."); return; }
        int[] frame = frames.peek();
        Cmd[] fn = frame[0] == 1 ? f1 : f2;
        if (frame[1] >= fn.length) { frames.pop(); handler.postDelayed(this::step, STEP_MS); return; }
        Cmd c = fn[frame[1]];
        frame[1]++;
        stepsUsed++;

        if (c == null) { handler.postDelayed(this::step, STEP_MS); return; }
        if (stepsUsed > MAX_STEPS) { fail("Step limit exceeded! Optimize."); return; }
        if (c.isFunc) {
            if (frames.size() >= 25) { fail("Stack overflow!"); return; }
            frames.push(new int[]{c.func, 0});
            handler.postDelayed(this::step, STEP_MS);
            return;
        }

        if (c.isTurnLeft) { botDir = (botDir + 3) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isTurnRight) { botDir = (botDir + 1) % 4; board.invalidate(); handler.postDelayed(this::step, STEP_MS); return; }
        if (c.isJump) {
            int nx = botX + DX[botDir] * 2;
            int ny = botY + DY[botDir] * 2;
            if (nx < 0 || ny < 0 || nx >= W || ny >= H || cells[ny][nx] == VOID) {
                fail("Jump crash!"); return;
            }
            botX = nx; botY = ny;
            if (goals[ny][nx] && !collected[ny][nx]) { collected[ny][nx] = true; goalsCollected++; }
            board.invalidate();
            if (goalsCollected >= goalsTotal) { win(); return; }
            handler.postDelayed(this::step, STEP_MS);
            return;
        }

        if (c.guard != 0) {
            int tileColor = cells[botY][botX];
            if (tileColor != c.guard) { handler.postDelayed(this::step, STEP_MS); return; }
        }

        int nx = botX + DX[c.dir];
        int ny = botY + DY[c.dir];
        if (nx < 0 || ny < 0 || nx >= W || ny >= H || cells[ny][nx] == VOID) { fail("CRASH!"); return; }
        botX = nx; botY = ny; botDir = c.dir;
        if (goals[ny][nx] && !collected[ny][nx]) { collected[ny][nx] = true; goalsCollected++; }
        board.invalidate();
        if (goalsCollected >= goalsTotal) { win(); return; }
        handler.postDelayed(this::step, STEP_MS);
    }

    private void win() {
        running = false;
        msgView.setText("CHALLENGE COMPLETE!");
        msgView.setTextColor(Color.parseColor("#3FB950"));
        handler.postDelayed(() -> {
            if (dialog.isShowing()) dialog.dismiss();
            listener.onSolved();
        }, 900);
    }

    private void fail(String reason) {
        running = false;
        msgView.setText(reason);
        msgView.setTextColor(Color.parseColor("#F85149"));
        handler.postDelayed(this::resetRun, 800);
    }

    private void stopRun() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void resetRun() {
        stopRun();
        resetBotPosition();
        if (msgView != null) {
            msgView.setTextColor(Color.parseColor("#FFD54F"));
            msgView.setText(goalsCollected + "/" + goalsTotal + " targets | Steps: 0/" + MAX_STEPS);
        }
    }

    private void resetBotPosition() {
        botX = startX; botY = startY; botDir = startDir;
        collected = new boolean[H][W];
        goalsCollected = 0;
        if (goals[startY][startX]) { collected[startY][startX] = true; goalsCollected = 1; }
        if (board != null) board.invalidate();
    }

    private class AlgoBoardView extends View {
        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint botPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        AlgoBoardView(Context ctx) { super(ctx); }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.parseColor("#0D1117"));
            if (cells == null) return;
            float viewW = getWidth(), viewH = getHeight();
            float tile = Math.min(viewW / W, viewH / H) * 0.9f;
            float offX = (viewW - tile * W) / 2f;
            float offY = (viewH - tile * H) / 2f;

            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (cells[y][x] == VOID) continue;
                    float left = offX + x * tile + tile * 0.04f;
                    float top = offY + y * tile + tile * 0.04f;
                    RectF r = new RectF(left, top, left + tile * 0.92f, top + tile * 0.92f);
                    switch (cells[y][x]) {
                        case RED: tilePaint.setColor(Color.parseColor("#E53935")); break;
                        case GREEN: tilePaint.setColor(Color.parseColor("#43A047")); break;
                        case BLUE: tilePaint.setColor(Color.parseColor("#1E88E5")); break;
                        case YELLOW: tilePaint.setColor(Color.parseColor("#FDD835")); break;
                        case PURPLE: tilePaint.setColor(Color.parseColor("#8E24AA")); break;
                        default: tilePaint.setColor(Color.parseColor("#1E88E5"));
                    }
                    canvas.drawRoundRect(r, tile * 0.15f, tile * 0.15f, tilePaint);
                    if (goals[y][x]) {
                        goalPaint.setColor(collected != null && collected[y][x] ? Color.parseColor("#44FFFFFF") : Color.WHITE);
                        canvas.drawCircle(r.centerX(), r.centerY(), tile * 0.12f, goalPaint);
                    }
                }
            }
            float bx = offX + botX * tile + tile * 0.04f;
            float by = offY + botY * tile + tile * 0.04f;
            botPaint.setColor(Color.parseColor("#C9D1D9"));
            canvas.drawCircle(bx + tile * 0.46f, by + tile * 0.46f, tile * 0.32f, botPaint);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(tile * 0.05f);
            ringPaint.setColor(Color.WHITE);
            canvas.drawCircle(bx + tile * 0.46f, by + tile * 0.46f, tile * 0.32f, ringPaint);
        }
    }
}
