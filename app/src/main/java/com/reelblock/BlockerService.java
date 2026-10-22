package com.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Watches Instagram's screen and leaves the Reels viewer whenever it shows up,
 * whether it was opened from the Reels tab, the feed, Explore or a DM.
 *
 * Instagram calls Reels "clips" internally, so the viewer's views have ids like
 * "clips_viewer_view_pager". ("reel_viewer" is Stories, which we leave alone.)
 */
public class BlockerService extends AccessibilityService {
    private static final String TAG = "ReelBlock";
    private static final String IG = "com.instagram.android";
    private static final String ID_PREFIX = IG + ":id/";

    /** Wait for a burst of events to settle before looking at the screen. */
    private static final long SETTLE_MS = 120;
    /** Give Instagram time to react to one action before trying the next. */
    private static final long ACTION_GAP_MS = 700;
    /** Blocks closer together than this count as "Back didn't work". */
    private static final long STRIKE_WINDOW_MS = 2500;
    private static final long TOAST_GAP_MS = 4000;
    private static final int MAX_DEPTH = 40;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable check = this::checkScreen;

    private long lastActionAt;
    private long lastToastAt;
    private int strikes;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !IG.contentEquals(pkg)) return;
        handler.removeCallbacks(check);
        handler.postDelayed(check, SETTLE_MS);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(check);
    }

    private void checkScreen() {
        if (!Prefs.enabled(this)) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !IG.contentEquals(root.getPackageName())) {
            return;
        }
        String hit = findReels(root, 0);
        if (hit == null) {
            strikes = 0;
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now - lastActionAt < ACTION_GAP_MS) {
            handler.postDelayed(check, ACTION_GAP_MS);
            return;
        }
        strikes = now - lastActionAt < STRIKE_WINDOW_MS ? strikes + 1 : 1;
        lastActionAt = now;
        Log.d(TAG, "Reels detected via " + hit + ", strike " + strikes);

        // Back usually works. On the Reels tab itself Back can be a no-op, so
        // then switch to the Home tab, and as a last resort leave Instagram.
        if (strikes == 1) {
            Prefs.countBlock(this);
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if (strikes == 2 && clickById(root, "feed_tab")) {
            Log.d(TAG, "Switched to Home tab");
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME);
            strikes = 0;
        }

        if (now - lastToastAt > TOAST_GAP_MS) {
            lastToastAt = now;
            Toast.makeText(this, "Reels blocked", Toast.LENGTH_SHORT).show();
        }
        // Check again in case the action didn't take us out.
        handler.postDelayed(check, ACTION_GAP_MS);
    }

    /** Returns the view id that gave Reels away, or null if this isn't Reels. */
    private String findReels(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > MAX_DEPTH) return null;
        String id = node.getViewIdResourceName();
        if (id != null && id.startsWith(ID_PREFIX)) {
            String name = id.substring(ID_PREFIX.length());
            if (name.startsWith("clips_viewer") || name.equals("clips_video_container")) {
                return name;
            }
            if (name.equals("clips_tab") && node.isSelected()) {
                return name;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            String hit = findReels(node.getChild(i), depth + 1);
            if (hit != null) return hit;
        }
        return null;
    }

    private boolean clickById(AccessibilityNodeInfo root, String name) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(ID_PREFIX + name);
        for (AccessibilityNodeInfo n : found) {
            for (AccessibilityNodeInfo c = n; c != null; c = c.getParent()) {
                if (c.isClickable()) return c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        return false;
    }
}
