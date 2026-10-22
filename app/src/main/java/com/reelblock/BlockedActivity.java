package com.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Random;

/**
 * Shown on top of Instagram right after a Reel is blocked. Instagram is already
 * back on its feed underneath, so closing this screen returns there.
 */
public class BlockedActivity extends Activity {
    private static final String[] LINES = {
        "That scroll can wait.\nYour time can't.",
        "Nothing in there is worth\nthe next 20 minutes.",
        "You opened Instagram for something.\nWas it this?",
        "Go outside, text a friend,\nor drink some water.",
        "Future you says thanks.",
        "One less rabbit hole today.",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        findViewById(R.id.go_home).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        findViewById(R.id.back_to_ig).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        int today = Prefs.blockedOn(this, LocalDate.now());
        ((TextView) findViewById(R.id.blocked_today)).setText(String.valueOf(today));
        ((TextView) findViewById(R.id.blocked_saved)).setText(
                MainActivity.formatMinutes(today * MainActivity.MINUTES_PER_BLOCK));
        ((TextView) findViewById(R.id.blocked_line)).setText(LINES[new Random().nextInt(LINES.length)]);

        View ring = findViewById(R.id.blocked_ring);
        ring.setScaleX(0.6f);
        ring.setScaleY(0.6f);
        ring.animate().scaleX(1f).scaleY(1f).setDuration(450)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();
    }

    /** A new block while this screen is already up just refreshes it. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
