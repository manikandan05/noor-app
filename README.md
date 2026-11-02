# Noor

Noor is an Islamic calendar, prayer times and daily wisdom app, with an Android version (Jetpack Compose) and an iOS version (SwiftUI).

## Features
- **Home:** today's Hijri date, upcoming Islamic events and a daily reflection.
- **Calendar:** Gregorian and Hijri month, year and multi-decade views, with Islamic events.
- **Prayer:** prayer times for your location, Qibla compass, per-prayer minute adjustments and alerts.
- **Wisdom:** short lessons and reflections you can save.
- **Settings:** language, theme, colour palette, reminders and Hijri date adjustment (−2 to +2 days).
- **Languages:** English, Arabic and Urdu, with right-to-left layout support.

## Repository layout
| Folder | Contents |
|---|---|
| [`Android/`](Android) | Android app (Kotlin, Jetpack Compose, min SDK 26, target SDK 35). |
| [`Ios/`](Ios) | iOS sources and tests (SwiftUI, iOS 17+). Needs an Xcode project, see [`Ios/README.md`](Ios/README.md). |
| [`Android/docs/`](Android/docs) | Privacy policy draft and the [Play Store publishing guide](Android/docs/PUBLISHING.md). |

The lesson, event and translation JSON files are shared in content between both apps:
`Android/app/src/main/assets/` and `Ios/Sources/Resources/`.

## Build the Android app
Requirements: JDK 17, Android SDK (API 35) and Gradle, or Android Studio.

```bash
cd Android
export ANDROID_HOME=$HOME/Library/Android/sdk
gradle assembleDebug                # APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release bundle for Google Play: `gradle bundleRelease`. See [`PUBLISHING.md`](Android/docs/PUBLISHING.md) for signing and store steps.

## Ads
The Android app shows an AdMob banner and asks for consent first (Google UMP). The AdMob App ID and banner unit ID are set in
[`Android/app/build.gradle.kts`](Android/app/build.gradle.kts). Do not click your own live ads. For development, use Google's
test IDs described in [`PUBLISHING.md`](Android/docs/PUBLISHING.md).

## Secrets
Never commit signing keys or passwords. `keystore.properties`, `*.jks` and `*.keystore` are for local use only and must stay out of git.

## Status
Work in progress. Lesson content is draft and Arabic text is machine-translated until a human reviewer approves it. Add verified
references before publishing.
