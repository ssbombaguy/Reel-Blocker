package com.reelblock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Random;

/**
 * Shown right after something is blocked, on top of the app it came from. That
 * app has already left the blocked content, so closing this screen returns there.
 *
 * Reels get a random photo and Goggins quote; adult sites get the faith version.
 */
public class BlockedActivity extends Activity {
    /** Boolean extra: show the faith version of this screen. */
    static final String EXTRA_FAITH = "faith";

    private static final int[] PHOTOS = {
        R.drawable.photo_1,
        R.drawable.photo_2,
        R.drawable.photo_3,
        R.drawable.photo_4,
        R.drawable.photo_5,
        R.drawable.photo_6,
    };

    private static final String[] QUOTES = {
        "You are in danger of living a life so comfortable and soft, that you will die without ever realizing your true potential.",
        "The most important conversations you'll ever have are the ones you'll have with yourself.",
        "Be more than motivated, be more than driven, become literally obsessed to the point where people think you're nuts.",
        "Everybody comes to a point in their life when they want to quit, but it's what you do at that moment that determines who you are.",
        "Suffering is the true test of life.",
        "It takes relentless self-discipline to schedule suffering into your day, every day.",
        "Don't stop when you're tired. Stop when you're done.",
        "Motivation is crap. Motivation comes and goes.",
        "Who's gonna carry the boats?",
        "Stay hard.",
    };

    /** Each photo goes with the text and source at the same position. */
    private static final int[] FAITH_PHOTOS = {
        R.drawable.photo_faith_1,
        R.drawable.photo_faith_2,
    };
    private static final String[] FAITH_TEXT = {
        "Look at me in the eyes.",
        "Rejoice with me; for I have found my sheep which was lost.",
    };
    private static final String[] FAITH_SOURCE = {
        "",
        "Luke 15:6",
    };

    private static final Random RANDOM = new Random();
    private static int lastPhoto = -1;
    private static int lastQuote = -1;
    private static int lastFaith = -1;

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
        boolean faith = getIntent().getBooleanExtra(EXTRA_FAITH, false);
        int extras = faith ? View.GONE : View.VISIBLE;
        findViewById(R.id.blocked_title).setVisibility(extras);
        findViewById(R.id.blocked_stats).setVisibility(extras);

        if (faith) {
            lastFaith = pickOther(FAITH_PHOTOS.length, lastFaith);
            show(FAITH_PHOTOS[lastFaith], FAITH_TEXT[lastFaith], FAITH_SOURCE[lastFaith]);
        } else {
            int today = Prefs.blockedOn(this, LocalDate.now());
            ((TextView) findViewById(R.id.blocked_today)).setText(String.valueOf(today));
            ((TextView) findViewById(R.id.blocked_saved)).setText(
                    MainActivity.formatMinutes(today * MainActivity.MINUTES_PER_BLOCK));
            lastQuote = pickOther(QUOTES.length, lastQuote);
            lastPhoto = pickOther(PHOTOS.length, lastPhoto);
            show(PHOTOS[lastPhoto], QUOTES[lastQuote], "David Goggins");
        }
    }

    private void show(int photoRes, String line, String source) {
        ((TextView) findViewById(R.id.blocked_line)).setText(line);
        TextView sourceView = findViewById(R.id.blocked_source);
        sourceView.setText(source);
        sourceView.setVisibility(TextUtils.isEmpty(source) ? View.GONE : View.VISIBLE);

        ImageView photo = findViewById(R.id.blocked_photo);
        photo.setImageResource(photoRes);
        photo.setAlpha(0f);
        photo.setScaleX(1.08f);
        photo.setScaleY(1.08f);
        photo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start();
    }

    /** A random index in [0, count) that isn't {@code last}, so nothing shows twice in a row. */
    private static int pickOther(int count, int last) {
        int pick = RANDOM.nextInt(count);
        return pick == last ? (pick + 1) % count : pick;
    }

    /** A new block while this screen is already up just refreshes it. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
