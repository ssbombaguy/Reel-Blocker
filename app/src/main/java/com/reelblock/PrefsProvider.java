package com.reelblock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Owns the settings file inside the blocker's process. The screens live in a
 * different process, and SharedPreferences doesn't sync between processes, so
 * everyone reads and writes through this provider instead (see {@link Prefs}).
 */
public class PrefsProvider extends ContentProvider {
    static final Uri URI = Uri.parse("content://com.reelblock.prefs");
    static final String VALUE = "value";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_BLOCKED = "blocked_count";
    /** Per-day counts are stored as "day_2026-10-22". */
    private static final String DAY_PREFIX = "day_";

    private SharedPreferences prefs;

    @Override
    public boolean onCreate() {
        prefs = getContext().getSharedPreferences("reelblock", android.content.Context.MODE_PRIVATE);
        return true;
    }

    @Override
    public synchronized Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        switch (method) {
            case "enabled":
                out.putBoolean(VALUE, prefs.getBoolean(KEY_ENABLED, true));
                break;
            case "setEnabled":
                prefs.edit().putBoolean(KEY_ENABLED, Boolean.parseBoolean(arg)).apply();
                break;
            case "total":
                out.putInt(VALUE, prefs.getInt(KEY_BLOCKED, 0));
                break;
            case "day":
                out.putInt(VALUE, prefs.getInt(DAY_PREFIX + arg, 0));
                break;
            case "countBlock":
                String day = DAY_PREFIX + arg;
                prefs.edit()
                        .putInt(KEY_BLOCKED, prefs.getInt(KEY_BLOCKED, 0) + 1)
                        .putInt(day, prefs.getInt(day, 0) + 1)
                        .apply();
                break;
            default:
                throw new IllegalArgumentException("Unknown method " + method);
        }
        return out;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
