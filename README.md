# ReelBlock

An Android app that gets you out of Instagram Reels and blocks porn in Chrome, then shows you something worth looking at instead.

The rest of Instagram (feed, Stories, DMs, search, profile) works as normal. ReelBlock has no internet permission: it stores only an on/off switch and block counts, and sends nothing anywhere.

## What it does

| You open | What ReelBlock does | What you see |
|---|---|---|
| Instagram Reels (tab, feed post, Explore or DM) | Leaves the Reels viewer | A random photo and David Goggins quote, plus today's stats |
| An adult site in Chrome, or a porn word typed in the address bar | Sends the tab to Google | The faith screen: an image of Jesus with a message or Bible verse |
| `instagram.com/reel*` or `youtube.com/shorts` in Chrome | Sends the tab to Google | The photo and quote screen |

- **Reacts as you type.** The address bar is checked whenever it changes, so a blocked word triggers before you press Enter.
- **Should work in incognito.** It reads the same address bar there.
- **Pausing is deliberate.** The Pause switch has a 5-second delay, and it only pauses Reels blocking. Porn blocking stays on.
- **Tracks your progress.** The main screen shows blocks today, this week and in total, an estimate of time saved, and a 7-day chart.

## Make it yours

The images included here are only **starting images. You can change them any time with your own.** They belong to their respective owners and aren't licensed by this project.

| To change | Edit |
|---|---|
| Reels photos | Replace `photo_1.png` to `photo_6.png` in `app/src/main/res/drawable-nodpi/`. To use a different number, update `PHOTOS` in `BlockedActivity.java`. |
| Faith images | Replace `photo_faith_1.jpg` and `photo_faith_2.png` in the same folder |
| Goggins quotes | `QUOTES` in `BlockedActivity.java` (written from memory, so check the wording) |
| Faith text and verses | `FAITH_TEXT` and `FAITH_SOURCE` in `BlockedActivity.java` |
| Blocked sites and words | `NAMES`, `WORDS` and `HELP_WORDS` in `UrlRules.java` |

## Setup

1. Build the app (see below) and install it on your phone.
2. Open **ReelBlock**, tap the setup button, find ReelBlock in Accessibility settings and switch it on.
3. On Android 13+ the switch may be greyed out ("Restricted setting"). Go to **Settings → Apps → ReelBlock → ⋮ → Allow restricted settings**, then repeat step 2.

### Recommended: block adult sites everywhere

ReelBlock only watches Chrome. To block adult sites across every browser and app, turn on a filtering Private DNS. It's a phone setting, so no app has to stay running:

**Settings → Private DNS → Private DNS provider hostname →** `adult-filter-dns.cleanbrowsing.org`

The DNS provider can see which site names you visit, but not the pages or what you type.

### Xiaomi / MIUI phones

MIUI kills background apps aggressively. ReelBlock runs with a small "ReelBlock is on" notification to resist its memory cleaner, so leave that notification on. "Clear all" in Recents can still kill it, so:

- Lock ReelBlock in Recents (long-press its card and tap the lock icon).
- Turn on **Settings → Apps → Manage apps → ReelBlock → Autostart**.
- If the app says your phone stopped it, switch ReelBlock off and on again in Accessibility settings.

## Build

Needs the Android SDK and a JDK 17 or newer (Android Studio's bundled one works).

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## How it works

ReelBlock is an Android accessibility service that only receives events from Instagram and Chrome.

- **Instagram:** Instagram calls Reels "clips" internally. When a `clips_viewer*` view, `clips_video_container` or the selected `clips_tab` appears, ReelBlock switches to the Home tab or presses Back, then opens the blocked screen. If Reels is still showing after that, it goes to the phone's home screen.
- **Chrome:** ReelBlock reads only the address bar (`url_bar`) and checks it against `UrlRules`. On a match it replaces the address with `google.com` in the same tab, so returning to Chrome can't trigger the block again. Typed text is never saved or logged.

## Limits

- ReelBlock's web blocking covers Chrome's address bar only. It doesn't see other browsers, the search box on a Google page, or other apps.
- Word matching is English only and can trigger early. "porn addiction help" is exempt only when pasted whole, because typing it triggers at "porn".
- It's a commitment tool, not a lock. Anyone holding the phone can turn it off.

## If Instagram or Chrome changes

Detection relies on internal view ids, which an app update can rename. Connect the phone, run `adb logcat -s ReelBlock` to see what's detected, and use `uiautomator dump` with the screen open to find the new ids. They're in `BlockerService.findReels` and `CHROME_URL_BAR`.
