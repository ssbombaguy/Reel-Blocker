package com.reelblock;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

/** Settings and block counts shared by the screen and the service. */
final class Prefs {
    private static final String FILE = "reelblock";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BLOCKED = "blocked_count";
    /** Per-day counts are stored as "day_2026-10-22". */
    private static final String DAY_PREFIX = "day_";

    private Prefs() {}

    private static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) {
        return get(c).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_ENABLED, on).apply();
    }

    static int blockedCount(Context c) {
        return get(c).getInt(KEY_BLOCKED, 0);
    }

    static int blockedOn(Context c, LocalDate day) {
        return get(c).getInt(DAY_PREFIX + day, 0);
    }

    static void countBlock(Context c) {
        SharedPreferences p = get(c);
        String today = DAY_PREFIX + LocalDate.now();
        p.edit()
                .putInt(KEY_BLOCKED, p.getInt(KEY_BLOCKED, 0) + 1)
                .putInt(today, p.getInt(today, 0) + 1)
                .apply();
    }
}
