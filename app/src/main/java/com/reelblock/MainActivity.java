package com.reelblock;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class MainActivity extends Activity {
    /** Rough length of one Reels session avoided, for the "time saved" line. */
    private static final int MINUTES_PER_BLOCK = 2;
    /** How long the "Pause" button stays locked, so pausing isn't a reflex. */
    private static final int PAUSE_DELAY_S = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView statusPill;
    private TextView statusTitle;
    private TextView statusSub;
    private View ring;
    private ImageView shield;
    private Switch toggle;
    private View setupCard;
    private TextView statToday;
    private TextView statWeek;
    private TextView statTotal;
    private TextView timeSaved;
    private WeekChart chart;

    private final CompoundButton.OnCheckedChangeListener onToggle = (b, on) -> {
        if (on) {
            Prefs.setEnabled(this, true);
            refresh();
        } else {
            b.setChecked(true);
            confirmPause();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusPill = findViewById(R.id.status_pill);
        statusTitle = findViewById(R.id.status_title);
        statusSub = findViewById(R.id.status_sub);
        ring = findViewById(R.id.ring);
        shield = findViewById(R.id.shield);
        toggle = findViewById(R.id.toggle);
        setupCard = findViewById(R.id.setup_card);
        statToday = findViewById(R.id.stat_today);
        statWeek = findViewById(R.id.stat_week);
        statTotal = findViewById(R.id.stat_total);
        timeSaved = findViewById(R.id.time_saved);
        chart = findViewById(R.id.chart);

        findViewById(R.id.toggle_row).setOnClickListener(v -> toggle.toggle());
        findViewById(R.id.setup_button).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.app_settings_button).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean serviceOn = isServiceEnabled();
        boolean on = Prefs.enabled(this);

        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener(onToggle);
        setupCard.setVisibility(serviceOn ? View.GONE : View.VISIBLE);

        if (!serviceOn) {
            showStatus("SETUP", R.color.warn, "Not active yet", "Finish the setup below", false);
        } else if (on) {
            showStatus("ON", R.color.good, "You're protected", "Reels close the moment they open", true);
        } else {
            showStatus("PAUSED", R.color.text_dim, "Paused", "Reels are allowed right now", false);
        }

        LocalDate today = LocalDate.now();
        int[] counts = new int[7];
        String[] labels = new String[7];
        int week = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            counts[i] = Prefs.blockedOn(this, day);
            labels[i] = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            week += counts[i];
        }
        int total = Prefs.blockedCount(this);
        statToday.setText(String.valueOf(counts[6]));
        statWeek.setText(String.valueOf(week));
        statTotal.setText(String.valueOf(total));
        timeSaved.setText("≈ " + formatMinutes(total * MINUTES_PER_BLOCK) + " saved");
        chart.setData(counts, labels);
    }

    private void showStatus(String pill, int pillColor, String title, String sub, boolean active) {
        statusPill.setText(pill);
        statusPill.setBackgroundTintList(ColorStateList.valueOf(getColor(pillColor)));
        statusTitle.setText(title);
        statusSub.setText(sub);
        ring.setBackgroundResource(active ? R.drawable.bg_ring_on : R.drawable.bg_ring_off);
        shield.setImageTintList(ColorStateList.valueOf(getColor(active ? R.color.pink : R.color.off)));
    }

    /** Pausing is allowed, but only after a few seconds of second thoughts. */
    private void confirmPause() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_pause);
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(getResources().getDisplayMetrics().widthPixels - 2 * dp(24),
                WindowManager.LayoutParams.WRAP_CONTENT);
        window.setDimAmount(0.75f);

        int blocked = Prefs.blockedOn(this, LocalDate.now());
        TextView message = dialog.findViewById(R.id.pause_message);
        message.setText(blocked > 0
                ? "You've dodged " + blocked + (blocked == 1 ? " Reel" : " Reels")
                        + " today.\nReels will play again until you turn it back on."
                : "Reels will play again until you turn it back on.");

        Button pause = dialog.findViewById(R.id.pause_button);
        dialog.findViewById(R.id.keep_button).setOnClickListener(v -> dialog.dismiss());
        pause.setOnClickListener(v -> {
            Prefs.setEnabled(this, false);
            dialog.dismiss();
            refresh();
        });
        pause.setEnabled(false);
        dialog.show();

        Runnable tick = new Runnable() {
            int left = PAUSE_DELAY_S;

            @Override
            public void run() {
                if (!dialog.isShowing()) return;
                if (left == 0) {
                    pause.setText("Pause anyway");
                    pause.setTextColor(getColor(R.color.text));
                    pause.setEnabled(true);
                    return;
                }
                pause.setText("Pause in " + left-- + "…");
                handler.postDelayed(this, 1000);
            }
        };
        tick.run();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " min";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName me = new ComponentName(this, BlockerService.class);
        for (String s : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(s))) return true;
        }
        return false;
    }
}
