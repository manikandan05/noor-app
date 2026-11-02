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

## Required software

### Android app
| Software | Version | Notes |
|---|---|---|
| **JDK** | 17 | The build targets Java 17. |
| **Android Studio** | Current stable release | Easiest way to get the SDK, emulator and Gradle. Optional if you use the command line. |
| **Android SDK** | Platform **35**, Build-Tools **35.0.0**, Platform-Tools (`adb`) | Install through Android Studio's SDK Manager. |
| **Gradle** | 9.4.1 (tested) | Needed only for command-line builds. Android Studio can build without a separate install. The repo has no Gradle wrapper. |
| **Emulator or phone** | Android 8.0 (API 26) or newer | Use an image with **Google APIs / Google Play** so AdMob and location work. Tested on an API 36.1 Google Play image. |
| **Internet** | Needed on first build | Gradle downloads dependencies. Ads also need internet at run time, but the rest of the app works offline. |

Plugin versions used: Android Gradle Plugin 8.5.2, Kotlin 2.3.21, Jetpack Compose BOM 2024.09.00, Google Mobile Ads SDK 25.5.0.

### iOS app
| Software | Version | Notes |
|---|---|---|
| **macOS** | A version that supports your Xcode | iOS builds only work on a Mac. |
| **Xcode** | 15 or newer | Needed for iOS 17 and SwiftUI. Command Line Tools alone are not enough. |
| **Apple ID** | Free is enough | Needed to run on a physical device. |

## Run the Android app

### Option A: Android Studio
1. Install JDK 17 and Android Studio, then install SDK Platform 35 and Build-Tools 35.0.0 from **Settings → Languages & Frameworks → Android SDK**.
2. Choose **File → Open** and select the **`Android`** folder (not the repository root).
3. Let Gradle sync finish.
4. Create an emulator in **Device Manager** (API 26 or newer, with Google Play), or connect a phone with USB debugging on.
5. Select the `app` configuration and press **Run**.

### Option B: Command line
```bash
cd Android
export ANDROID_HOME=$HOME/Library/Android/sdk     # point this at your Android SDK
gradle assembleDebug                              # builds app/build/outputs/apk/debug/app-debug.apk
```

Start an emulator (or connect a phone), then install and launch:
```bash
$ANDROID_HOME/emulator/emulator -list-avds                 # list your emulators
$ANDROID_HOME/emulator/emulator -avd <name> &              # start one
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb shell am start -n app.islamiccalendar/.MainActivity
```
The app appears in the launcher as **Noor**.

### First run
- Allow notifications and approximate location when asked. Location is only used on the device to calculate prayer times and the Qibla direction.
- On an emulator, set a location in **Extended controls (⋯) → Location**, otherwise prayer times use no real position.
- The Qibla compass needs a real device sensor; the emulator's virtual sensors are limited.
- Exact prayer-time alarms may need permission in **Settings → Apps → Noor → Alarms & reminders**.
- The ad banner shows only after consent and when an ad loads. It stays hidden if you are offline.

### Troubleshooting
| Problem | Fix |
|---|---|
| `SDK location not found` | Set `ANDROID_HOME`, or create `Android/local.properties` with `sdk.dir=/path/to/Android/sdk`. This file is git-ignored. |
| Wrong Java version | Run `java -version` and make sure it is 17. Set `JAVA_HOME` if needed. |
| App icon or name looks old | Uninstall the app (or use `adb install -r`) and reopen the launcher. |
| No ad appears | New ad units can take up to an hour to serve. Check you are online and have a Google Play emulator image. |

## Run the iOS app
There is no Xcode project in the repo yet, only sources. Follow [`Ios/README.md`](Ios/README.md):
1. In Xcode, create a new **iOS App** named `IslamicCalendarApp` using SwiftUI, and delete the generated `App` and `ContentView` files.
2. Drag `Ios/Sources/*.swift` and `Ios/Sources/Resources/*.json` into the target, ticking **Copy items** and the target.
3. Add `Ios/Tests/CalendarTests.swift` to a unit-test target.
4. Choose a simulator (iOS 17 or newer) and press **Run**. Press **Cmd+U** to run the tests.

The iOS app has not been built or tested in this repository's setup.

## Build a release
```bash
cd Android
gradle bundleRelease      # app/build/outputs/bundle/release/app-release.aab
```
Signing keys, the AdMob and Play Store setup, and the publishing steps are in [`PUBLISHING.md`](Android/docs/PUBLISHING.md).

## Ads
The Android app shows an AdMob banner and asks for consent first (Google UMP). The AdMob App ID and banner unit ID are set in
[`Android/app/build.gradle.kts`](Android/app/build.gradle.kts). Do not click your own live ads. For development, use Google's
test IDs described in [`PUBLISHING.md`](Android/docs/PUBLISHING.md).

## Secrets
Never commit signing keys or passwords. `keystore.properties`, `*.jks` and `*.keystore` are for local use only and must stay out of git.

## Status
Work in progress. Lesson content is draft and Arabic text is machine-translated until a human reviewer approves it. Add verified
references before publishing.
