package com.reelblock;

import android.content.Context;
import android.os.Bundle;

import java.time.LocalDate;

/** Settings and block counts, shared across processes via {@link PrefsProvider}. */
final class Prefs {
    private Prefs() {}

    private static Bundle call(Context c, String method, String arg) {
        return c.getContentResolver().call(PrefsProvider.URI, method, arg, null);
    }

    static boolean enabled(Context c) {
        return call(c, "enabled", null).getBoolean(PrefsProvider.VALUE);
    }

    static void setEnabled(Context c, boolean on) {
        call(c, "setEnabled", String.valueOf(on));
    }

    static int blockedCount(Context c) {
        return call(c, "total", null).getInt(PrefsProvider.VALUE);
    }

    static int blockedOn(Context c, LocalDate day) {
        return call(c, "day", day.toString()).getInt(PrefsProvider.VALUE);
    }

    static void countBlock(Context c) {
        call(c, "countBlock", LocalDate.now().toString());
    }
}
