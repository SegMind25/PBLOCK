package com.pblock.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/** Pop-up warning "This is harmful for you" shown by {@link SearchShieldService}. */
public class StopActivity extends Activity {

    private static final String[] LINES = {
        "You promised yourself something better.",
        "This feeling will pass in a few minutes. Ride the wave.",
        "Every time you walk away, you get stronger.",
        "You are not your urges. You are the one who chooses.",
        "Think of how good you'll feel tomorrow for saying no today.",
        "Close this, stand up, drink some water. You've got this.",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float d = getResources().getDisplayMetrics().density;

        // Dimmed screen with a centered warning card ("pop-up").
        android.widget.FrameLayout screen = new android.widget.FrameLayout(this);
        screen.setBackgroundColor(Color.parseColor("#D9000000"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding((int) (24 * d), (int) (28 * d), (int) (24 * d), (int) (16 * d));
        GradientDrawable card = new GradientDrawable();
        card.setColor(Color.parseColor("#151B2E"));
        card.setCornerRadius(24 * d);
        card.setStroke((int) (2 * d), Color.parseColor("#EF4444"));
        root.setBackground(card);
        android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        cardLp.leftMargin = cardLp.rightMargin = (int) (24 * d);
        screen.addView(root, cardLp);

        TextView icon = new TextView(this);
        icon.setText("\u26A0\uFE0F");
        icon.setTextSize(56);
        icon.setGravity(Gravity.CENTER);
        root.addView(icon);

        TextView title = new TextView(this);
        title.setText("This is harmful for you");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#E8ECF6"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, (int) (12 * d), 0, (int) (10 * d));
        root.addView(title);

        TextView msg = new TextView(this);
        msg.setText(LINES[new Random().nextInt(LINES.length)]);
        msg.setTextSize(16);
        msg.setLineSpacing(0, 1.3f);
        msg.setTextColor(Color.parseColor("#8A93AD"));
        msg.setGravity(Gravity.CENTER);
        root.addView(msg);

        Button back = new Button(this);
        back.setText("Get back to my day");
        back.setAllCaps(false);
        back.setTextSize(16);
        back.setTypeface(Typeface.DEFAULT_BOLD);
        back.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#22C55E"));
        bg.setCornerRadius(14 * d);
        back.setBackground(bg);
        back.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int) (56 * d));
        lp.topMargin = (int) (24 * d);
        root.addView(back, lp);
        back.setOnClickListener(v -> goHome());

        Button breathe = new Button(this);
        breathe.setText("I need help with this urge");
        breathe.setAllCaps(false);
        breathe.setTextColor(Color.parseColor("#4F8CFF"));
        breathe.setBackgroundColor(Color.TRANSPARENT);
        root.addView(breathe, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d)));
        breathe.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_OPEN_URGE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        setContentView(screen);
    }

    @Override
    public void onBackPressed() {
        goHome(); // never back into the browser page
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }
}
