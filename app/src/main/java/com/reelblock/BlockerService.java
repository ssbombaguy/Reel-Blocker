package com.reelblock;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Watches Instagram's screen and leaves the Reels viewer whenever it shows up,
 * whether it was opened from the Reels tab, the feed, Explore or a DM. Also
 * reads Chrome's address bar to block adult sites and short-video pages.
 *
 * Instagram calls Reels "clips" internally, so the viewer's views have ids like
 * "clips_viewer_view_pager". ("reel_viewer" is Stories, which we leave alone.)
 */
public class BlockerService extends AccessibilityService {
    private static final String TAG = "ReelBlock";
    private static final String IG = "com.instagram.android";
    private static final String ID_PREFIX = IG + ":id/";
    private static final String CHROME = "com.android.chrome";
    private static final String CHROME_URL_BAR = CHROME + ":id/url_bar";

    /** Wait for a burst of events to settle before looking at the screen. */
    private static final long SETTLE_MS = 60;
    /** Give Instagram time to react to one action before trying the next. */
    private static final long ACTION_GAP_MS = 700;
    /** Blocks closer together than this count as "Back didn't work". */
    private static final long STRIKE_WINDOW_MS = 2500;
    /** The blocked screen can take over a second to open if MIUI cleared the app from memory. */
    private static final long SCREEN_OPEN_MS = 3000;
    /** Time for a Back press to reach Instagram before our screen opens. */
    private static final long BACK_SETTLE_MS = 300;
    /** Time for Chrome to focus its address bar before we type into it. */
    private static final long CHROME_FOCUS_MS = 250;
    /** Time for Chrome to take the new address before our screen covers it. */
    private static final long CHROME_SETTLE_MS = 800;
    private static final String SAFE_PAGE = "google.com";
    private static final int MAX_DEPTH = 40;
    private static final String CHANNEL = "blocker_status";
    private static final int NOTIFICATION_ID = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable check = this::checkScreen;

    private long lastActionAt;
    private int strikes;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        keepAlive();
    }

    /**
     * MIUI's memory cleaner kills background processes, and once this one dies
     * Android marks the service "crashed" and won't rebind it. A foreground
     * notification puts the process out of the cleaner's reach.
     */
    private void keepAlive() {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Blocker status", NotificationManager.IMPORTANCE_MIN));
            PendingIntent open = PendingIntent.getActivity(this, 0,
                    new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            startForeground(NOTIFICATION_ID, new Notification.Builder(this, CHANNEL)
                    .setSmallIcon(R.drawable.ic_shield)
                    .setContentTitle("ReelBlock is on")
                    .setContentIntent(open)
                    .setOngoing(true)
                    .build());
        } catch (RuntimeException e) {
            // Blocking still works without the notification, just less protected.
            Log.w(TAG, "Couldn't go foreground", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !(IG.contentEquals(pkg) || CHROME.contentEquals(pkg))) return;
        handler.removeCallbacks(check);
        handler.postDelayed(check, SETTLE_MS);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(check);
    }

    private void checkScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;
        // The pause switch is for Reels only; porn stays blocked while paused.
        boolean reelsBlocked = Prefs.enabled(this);
        if (IG.contentEquals(root.getPackageName())) {
            if (reelsBlocked) checkInstagram(root);
        } else if (CHROME.contentEquals(root.getPackageName())) {
            checkChrome(root, reelsBlocked);
        }
    }

    private void checkInstagram(AccessibilityNodeInfo root) {
        String hit = findReels(root, 0);
        if (hit == null) {
            strikes = 0;
            return;
        }
        // On the Reels tab switch to Home, anywhere else (feed, DMs) press Back,
        // so "Go back" on our screen lands on the feed.
        block("Reels via " + hit, false, BACK_SETTLE_MS, () -> {
            if (!(isSelected(root, "clips_tab") && clickById(root, "feed_tab"))) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
        });
    }

    private void checkChrome(AccessibilityNodeInfo root, boolean reelsBlocked) {
        AccessibilityNodeInfo bar = urlBar(root);
        String url = bar == null || bar.getText() == null ? "" : bar.getText().toString();
        boolean porn = UrlRules.isPorn(url);
        if (!porn && !(reelsBlocked && UrlRules.isShortVideo(url))) {
            strikes = 0;
            return;
        }
        // Only the category is logged: what was typed stays off the log.
        block(porn ? "adult site" : "short videos in Chrome", porn, CHROME_SETTLE_MS, () -> leaveChrome(bar));
    }

    /**
     * Sends the tab to a safe page. Back isn't enough: a tab with no history
     * stays on the blocked address and would block again on every return.
     */
    private void leaveChrome(AccessibilityNodeInfo bar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_BACK); // No "press Enter" action before Android 11.
            return;
        }
        bar.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        handler.postDelayed(this::goToSafePage, CHROME_FOCUS_MS);
    }

    private void goToSafePage() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo bar = root == null ? null : urlBar(root);
        if (bar == null) return;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, SAFE_PAGE);
        bar.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        bar.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
    }

    private AccessibilityNodeInfo urlBar(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(CHROME_URL_BAR);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Leaves the blocked content and covers it with the blocked screen. If it
     * shows up again right away, something failed: go to the home screen.
     */
    private void block(String what, boolean faith, long settleMs, Runnable leave) {
        long now = SystemClock.uptimeMillis();
        if (now - lastActionAt < ACTION_GAP_MS) {
            handler.postDelayed(check, ACTION_GAP_MS);
            return;
        }
        strikes = now - lastActionAt < STRIKE_WINDOW_MS ? strikes + 1 : 1;
        lastActionAt = now;
        Log.d(TAG, "Blocked " + what + ", strike " + strikes);

        if (strikes == 1) {
            Prefs.countBlock(this);
            leave.run();
            // Open our screen only after leaving has finished, or a late Back can close it.
            handler.postDelayed(() -> showBlockedScreen(faith), settleMs);
            // Don't judge whether this worked until the screen has had time to open.
            lastActionAt = now + SCREEN_OPEN_MS;
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME);
            strikes = 0;
        }
        // Check again in case the action didn't take us out.
        handler.postDelayed(check, ACTION_GAP_MS);
    }

    private void showBlockedScreen(boolean faith) {
        startActivity(new Intent(this, BlockedActivity.class)
                .putExtra(BlockedActivity.EXTRA_FAITH, faith)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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

    private boolean isSelected(AccessibilityNodeInfo root, String name) {
        for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByViewId(ID_PREFIX + name)) {
            if (n.isSelected()) return true;
        }
        return false;
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
