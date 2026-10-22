package com.reelblock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** One screen: is the blocker on, a switch, and a shortcut to turn the service on. */
public class MainActivity extends Activity {
    private TextView status;
    private TextView count;
    private Button settingsButton;
    private Switch toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(32);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(18);
        status.setPadding(0, pad, 0, pad / 2);
        root.addView(status);

        settingsButton = new Button(this);
        settingsButton.setText("Turn on in Accessibility settings");
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(settingsButton);

        toggle = new Switch(this);
        toggle.setText("Block Instagram Reels");
        toggle.setTextSize(18);
        toggle.setPadding(0, pad, 0, pad);
        toggle.setOnCheckedChangeListener((b, on) -> {
            Prefs.setEnabled(this, on);
            refresh();
        });
        root.addView(toggle);

        count = new TextView(this);
        count.setTextSize(16);
        root.addView(count);

        TextView help = new TextView(this);
        help.setTextSize(14);
        help.setGravity(Gravity.START);
        help.setPadding(0, pad, 0, 0);
        help.setText("How to turn it on:\n"
                + "1. Tap the button above, open ReelBlock and switch it on.\n"
                + "2. If Android says the setting is restricted: go to Settings → Apps → "
                + "ReelBlock → ⋮ (top right) → Allow restricted settings, then try step 1 again.\n\n"
                + "ReelBlock only reads Instagram's screen. It stores nothing and has no internet access.");
        root.addView(help);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean serviceOn = isServiceEnabled();
        boolean on = Prefs.enabled(this);
        toggle.setChecked(on);
        settingsButton.setVisibility(serviceOn ? Button.GONE : Button.VISIBLE);
        if (!serviceOn) {
            status.setText("⚠ Not active yet. Turn on the accessibility service.");
        } else if (on) {
            status.setText("✅ Active. Reels will close as soon as they open.");
        } else {
            status.setText("⏸ Paused. Reels are allowed.");
        }
        count.setText("Reels blocked so far: " + Prefs.blockedCount(this));
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName me = new ComponentName(this, BlockerService.class);
        for (String s : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
