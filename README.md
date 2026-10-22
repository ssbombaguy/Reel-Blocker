# ReelBlock

An Android app that blocks Instagram Reels. The rest of Instagram (feed, Stories, DMs, search, profile) works as normal.

ReelBlock runs as an Android accessibility service that only listens to Instagram. When the Reels viewer appears (from the Reels tab, a post in the feed, Explore or a DM), it presses **Back** for you. If that doesn't work, it switches to the **Home** tab, and as a last resort it leaves Instagram. It has no internet permission and stores nothing except an on/off switch and a counter.

## Install

1. Copy `ReelBlock.apk` to the phone and open it (allow "install unknown apps" if asked).
2. Open **ReelBlock**, tap **Turn on in Accessibility settings**, find ReelBlock and switch it on.
3. On Android 13+ the switch may be greyed out ("Restricted setting"). Go to **Settings → Apps → ReelBlock → ⋮ → Allow restricted settings**, then repeat step 2.

## Build

Needs the Android SDK and Android Studio's JDK.

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## If Instagram changes

Detection relies on Instagram's internal view ids (`clips_viewer*`, `clips_video_container`, and the selected `clips_tab`). If an Instagram update breaks blocking, connect the phone and run `adb logcat -s ReelBlock` to see what is detected, and use `uiautomator dump` while Reels is open to find the new ids. The ids are in `BlockerService.findReels`.
