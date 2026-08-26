package com.pblock.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AlgoPuzzleDialog {

    public interface Listener {
        void onSolved();
        void onGivenUp();
    }

    private static final int VOID = 0;
    private static final int RED = 1;
    private static final int GREEN = 2;
    private static final int BLUE = 3;

    private static final int DIR_UP = 0;
    private static final int DIR_DOWN = 1;
    private static final int DIR_LEFT = 2;
    private static final int DIR_RIGHT = 3;
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    private static final String[] ARROW = {"\u2191", "\u2193", "\u2190", "\u2192"};

    private static final int MAX_STEPS = 90;
    private static final int STEP_MS = 320;
    private static final int SLOTS = 8;

    private static class Cmd {
        boolean isFunc;
        int func;
        int dir;
        int guard;

        String label() {
            String s = isFunc ? ("F" + func) : ARROW[dir];
            if (guard == RED) return "R" + s;
            if (guard == GREEN) return "G" + s;
            if (guard == BLUE) return "B" + s;
            return s;
        }

        int guardColorValue() {
            if (guard == RED) return Color.parseColor("#E53935");
            if (guard == GREEN) return Color.parseColor("#43A047");
            if (guard == BLUE) return Color.parseColor("#1E88E5");
            return Color.parseColor("#37474F");
        }
    }

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();

    private int[][] cells;
    private boolean[][] goals;
    private boolean[][] collected;
    private int W, H, startX, startY;
    private int botX, botY;
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

    public AlgoPuzzleDialog(Context context, int legs, Listener listener) {
        this.context = context;
        this.listener = listener;
        generateLevel(legs);
    }

    private void generateLevel(int legs) {
        for (int attempt = 0; attempt < 200; attempt++) {
            W = 5 + rnd.nextInt(3);
            H = 5 + rnd.nextInt(2);
            cells = new int[H][W];
            goals = new boolean[H][W];
            startX = 1 + rnd.nextInt(W - 2);
            startY = 1 + rnd.nextInt(H - 2);
            int curX = startX, curY = startY;
            int lastDir = -1;
            int prevColor = 1 + rnd.nextInt(3);
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
                int len = 3 + rnd.nextInt(3);
                int color;
                do {
                    color = 1 + rnd.nextInt(3);
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
            if (!ok || countCells() < 6) continue;

            goals[lastY][lastX] = true;
            goalsTotal = 1;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (!goals[y][x] && cells[y][x] != VOID && rnd.nextInt(4) == 0) {
                        goals[y][x] = true;
                        goalsTotal++;
                    }
                }
            }
            botX = startX;
            botY = startY;
            return;
        }
        W = 6;
        H = 5;
        cells = new int[H][W];
        goals = new boolean[H][W];
        for (int x = 0; x < W; x++) cells[2][x] = RED;
        for (int y = 2; y < H; y++) cells[y][W - 1] = BLUE;
        startX = 0;
        startY = 2;
        botX = startX;
        botY = startY;
        goals[4][W - 1] = true;
        goalsTotal = 1;
    }

    private int opposite(int dir) {
        if (dir == DIR_UP) return DIR_DOWN;
        if (dir == DIR_DOWN) return DIR_UP;
        if (dir == DIR_LEFT) return DIR_RIGHT;
        return DIR_LEFT;
    }

    private int countCells() {
        int n = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (cells[y][x] != VOID) n++;
            }
        }
        return n;
    }

    public void show() {
        float density = context.getResources().getDisplayMetrics().density;
        int pad = dp(16);

        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#1C262E"));

        TextView title = new TextView(context);
        title.setText("ALGORITHM PUZZLE\nProgram the bot: collect ALL targets");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        FrameLayout boardWrap = new FrameLayout(context);
        board = new AlgoBoardView(context);
        int boardH = dp(230);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, boardH);
        board.setLayoutParams(bp);
        boardWrap.addView(board);
        boardWrap.setPadding(0, dp(10), 0, dp(6));
        root.addView(boardWrap);

        msgView = new TextView(context);
        msgView.setTextColor(Color.parseColor("#FFD54F"));
        msgView.setTextSize(13);
        msgView.setGravity(Gravity.CENTER);
        msgView.setText("Tap slots to build your program. F1 runs first. "
            + "Put F1 inside itself to LOOP.\nLetters R/G/B = only run when standing on that color.");
        root.addView(msgView);

        root.addView(sectionLabel("F1 (main)"));
        HorizontalScrollView scroll1 = new HorizontalScrollView(context);
        scroll1.addView(slotRow(true));
        root.addView(scroll1);

        root.addView(sectionLabel("F2"));
        HorizontalScrollView scroll2 = new HorizontalScrollView(context);
        scroll2.addView(slotRow(false));
        root.addView(scroll2);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        Button runBtn = styledButton("RUN \u25B6", "#43A047");
        runBtn.setOnClickListener(v -> startRun());
        Button resetBtn = styledButton("Reset Bot", "#546E7A");
        resetBtn.setOnClickListener(v -> resetRun());
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rlp.setMargins(dp(4), dp(8), dp(4), 0);
        LinearLayout.LayoutParams rlp2 = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rlp2.setMargins(dp(4), dp(8), dp(4), 0);
        controls.addView(runBtn, rlp);
        controls.addView(resetBtn, rlp2);
        root.addView(controls);

        Button giveUp = styledButton("Give up (new puzzle set)", "#C62828");
        giveUp.setOnClickListener(v -> {
            stopRun();
            dialog.dismiss();
            listener.onGivenUp();
        });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        glp.setMargins(0, dp(8), 0, 0);
        root.addView(giveUp, glp);

        resetRun();
        refreshSlots();

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Algorithm Challenge");
        b.setView(root);
        b.setCancelable(false);
        dialog = b.create();
        dialog.show();
    }

    private TextView sectionLabel(String s) {
        TextView t = new TextView(context);
        t.setText(s);
        t.setTextColor(Color.parseColor("#90CAF9"));
        t.setTextSize(12);
        t.setPadding(dp(2), dp(8), 0, dp(2));
        return t;
    }

    private LinearLayout slotRow(boolean isF1) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 0; i < SLOTS; i++) {
            Button btn = styledButton("\u2013", "#37474F");
            int idx = i;
            btn.setOnClickListener(v -> editSlot(isF1, idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(46));
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(btn, lp);
            if (isF1) {
                f1Btns[i] = new Button[]{btn};
            } else {
                f2Btns[i] = new Button[]{btn};
            }
        }
        return row;
    }

    private Button styledButton(String text, String colorHex) {
        Button b = new Button(context);
        b.setText(text);
        b.setTextSize(11);
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
                Color.parseColor("#37474F")));
        } else {
            btn.setText(c.label());
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                c.guardColorValue()));
        }
    }

    private void editSlot(boolean isF1, int index) {
        if (running) {
            Toast.makeText(context, "Stop the program first", Toast.LENGTH_SHORT).show();
            return;
        }
        final Cmd[] arr = isF1 ? f1 : f2;
        final Cmd current = arr[index];
        final int[] guard = {current == null ? 0 : current.guard};
        final AlertDialog[] holder = new AlertDialog[1];

        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), 0);

        TextView gTitle = new TextView(context);
        gTitle.setText("1. Color condition (optional):");
        gTitle.setTextSize(12);
        box.addView(gTitle);

        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"None", "Red", "Green", "Blue"};
        int[] vals = {0, RED, GREEN, BLUE};
        final Button[] colorBtns = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int val = vals[i];
            Button cb = styledButton(names[i], "#37474F");
            colorBtns[i] = cb;
            cb.setOnClickListener(v -> {
                guard[0] = val;
                for (int j = 0; j < 4; j++) {
                    boolean selected = vals[j] == guard[0];
                    colorBtns[j].setAlpha(selected ? 1f : 0.45f);
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(dp(3), 0, dp(3), 0);
            colorRow.addView(cb, clp);
        }
        box.addView(colorRow);
        for (int j = 0; j < 4; j++) {
            colorBtns[j].setAlpha(vals[j] == guard[0] ? 1f : 0.45f);
        }

        TextView cTitle = new TextView(context);
        cTitle.setText("2. Command:");
        cTitle.setTextSize(12);
        cTitle.setPadding(0, dp(10), 0, dp(2));
        box.addView(cTitle);

        LinearLayout moveRow = new LinearLayout(context);
        moveRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int d = 0; d < 4; d++) {
            final int dir = d;
            Button mb = styledButton(ARROW[d], "#00695C");
            mb.setOnClickListener(v -> {
                Cmd c = new Cmd();
                c.dir = dir;
                c.guard = guard[0];
                arr[index] = c;
                refreshSlots();
                holder[0].dismiss();
            });
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            mlp.setMargins(dp(3), 0, dp(3), 0);
            moveRow.addView(mb, mlp);
        }
        box.addView(moveRow);

        LinearLayout funcRow = new LinearLayout(context);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        Button f1b = styledButton("F1", "#6A1B9A");
        f1b.setOnClickListener(v -> {
            Cmd c = new Cmd();
            c.isFunc = true;
            c.func = 1;
            c.guard = guard[0];
            arr[index] = c;
            refreshSlots();
            holder[0].dismiss();
        });
        Button f2b = styledButton("F2", "#6A1B9A");
        f2b.setOnClickListener(v -> {
            Cmd c = new Cmd();
            c.isFunc = true;
            c.func = 2;
            c.guard = guard[0];
            arr[index] = c;
            refreshSlots();
            holder[0].dismiss();
        });
        Button erase = styledButton("Clear", "#C62828");
        erase.setOnClickListener(v -> {
            arr[index] = null;
            refreshSlots();
            holder[0].dismiss();
        });
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        flp.setMargins(dp(3), 0, dp(3), 0);
        funcRow.addView(f1b, flp);
        funcRow.addView(f2b, flp);
        funcRow.addView(erase, flp);
        box.addView(funcRow);

        AlertDialog dlg = new AlertDialog.Builder(context)
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
        for (Cmd c : f1) {
            if (c != null) {
                empty = false;
                break;
            }
        }
        if (empty) {
            msgView.setText("F1 is empty! Add commands first.");
            return;
        }
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

        if (frames.isEmpty()) {
            fail("Program ended before collecting all targets.");
            return;
        }
        int[] frame = frames.peek();
        Cmd[] fn = frame[0] == 1 ? f1 : f2;
        if (frame[1] >= fn.length) {
            frames.pop();
            handler.postDelayed(this::step, STEP_MS);
            return;
        }
        Cmd c = fn[frame[1]];
        frame[1]++;
        stepsUsed++;

        if (c == null) {
            handler.postDelayed(this::step, STEP_MS);
            return;
        }
        if (stepsUsed > MAX_STEPS) {
            fail("Too many steps! Make your loop tighter.");
            return;
        }
        if (c.isFunc) {
            if (frames.size() >= 20) {
                fail("Too many nested calls!");
                return;
            }
            frames.push(new int[]{c.func, 0});
            handler.postDelayed(this::step, STEP_MS);
            return;
        }

        if (c.guard != 0) {
            int tileColor = cells[botY][botX];
            if (tileColor != c.guard) {
                handler.postDelayed(this::step, STEP_MS);
                return;
            }
        }

        int nx = botX + DX[c.dir];
        int ny = botY + DY[c.dir];
        if (nx < 0 || ny < 0 || nx >= W || ny >= H || cells[ny][nx] == VOID) {
            fail("CRASH! The bot fell off the path.");
            return;
        }

        botX = nx;
        botY = ny;

        if (goals[ny][nx] && !collected[ny][nx]) {
            collected[ny][nx] = true;
            goalsCollected++;
        }
        board.invalidate();

        if (goalsCollected >= goalsTotal) {
            win();
            return;
        }
        handler.postDelayed(this::step, STEP_MS);
    }

    private void win() {
        running = false;
        msgView.setText("PUZZLE SOLVED! Well programmed!");
        msgView.setTextColor(Color.parseColor("#66BB6A"));
        handler.postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            listener.onSolved();
        }, 1100);
    }

    private void fail(String reason) {
        running = false;
        msgView.setText(reason + "\nEdit your program and RUN again.");
        msgView.setTextColor(Color.parseColor("#EF5350"));
        handler.postDelayed(this::resetRun, 900);
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
            msgView.setText(goalsCollected + " / " + goalsTotal
                + " targets collected.\nEdit slots and press RUN.");
        }
    }

    private void resetBotPosition() {
        botX = startX;
        botY = startY;
        collected = new boolean[H][W];
        goalsCollected = 0;
        if (goals[startY][startX]) {
            collected[startY][startX] = true;
            goalsCollected = 1;
        }
        if (board != null) {
            board.invalidate();
        }
    }

    private class AlgoBoardView extends View {

        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint botPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        AlgoBoardView(Context ctx) {
            super(ctx);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.parseColor("#10171D"));
            if (cells == null) return;
            float viewW = getWidth();
            float viewH = getHeight();
            float tile = Math.min(viewW / W, viewH / H) * 0.92f;
            float offX = (viewW - tile * W) / 2f;
            float offY = (viewH - tile * H) / 2f;

            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    if (cells[y][x] == VOID) continue;
                    float left = offX + x * tile + tile * 0.05f;
                    float top = offY + y * tile + tile * 0.05f;
                    RectF r = new RectF(left, top, left + tile * 0.9f, top + tile * 0.9f);
                    switch (cells[y][x]) {
                        case RED:
                            tilePaint.setColor(Color.parseColor("#E53935"));
                            break;
                        case GREEN:
                            tilePaint.setColor(Color.parseColor("#43A047"));
                            break;
                        default:
                            tilePaint.setColor(Color.parseColor("#1E88E5"));
                    }
                    canvas.drawRoundRect(r, tile * 0.18f, tile * 0.18f, tilePaint);

                    if (goals[y][x]) {
                        if (collected != null && collected[y][x]) {
                            goalPaint.setColor(Color.parseColor("#FFFFFF"));
                            goalPaint.setAlpha(70);
                        } else {
                            goalPaint.setColor(Color.WHITE);
                            goalPaint.setAlpha(255);
                        }
                        canvas.drawCircle(r.centerX(), r.centerY(), tile * 0.14f, goalPaint);
                        goalPaint.setAlpha(255);
                    }
                }
            }

            float bx = offX + botX * tile + tile * 0.05f;
            float by = offY + botY * tile + tile * 0.05f;
            botPaint.setColor(Color.parseColor("#212121"));
            canvas.drawCircle(bx + tile * 0.45f, by + tile * 0.45f, tile * 0.34f, botPaint);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(tile * 0.06f);
            ringPaint.setColor(Color.WHITE);
            canvas.drawCircle(bx + tile * 0.45f, by + tile * 0.45f, tile * 0.34f, ringPaint);
        }
    }
}
