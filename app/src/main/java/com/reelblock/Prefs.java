package com.reelblock;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings shared by the screen and the service. */
final class Prefs {
    private static final String FILE = "reelblock";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BLOCKED = "blocked_count";

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

    static void countBlock(Context c) {
        SharedPreferences p = get(c);
        p.edit().putInt(KEY_BLOCKED, p.getInt(KEY_BLOCKED, 0) + 1).apply();
    }
}
