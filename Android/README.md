# Islamic Calendar & Daily Wisdom — Android (Jetpack Compose)
Open this folder in Android Studio (Koala+), let Gradle sync, run on API 26+. JSON content in app/src/main/assets is shared with the iOS app.
Parity gaps vs iOS: welcome screen, daily/event reminders, week-start and theme settings, extra Hijri methods (java.time provides Umm al-Qura only).

## Running on the Android emulator (command line)

SDK location: `~/Library/Android/sdk` (`ANDROID_HOME`). Add the tools to your `PATH` (e.g. in `~/.zshrc`):

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools
```

1. List available virtual devices (AVDs):
   ```bash
   emulator -list-avds
   ```
   Currently available: `Medium_Phone_API_36.1`, `Small_Phone`.

2. Start an emulator (keep this terminal open, or add `&` to background it):
   ```bash
   emulator -avd Medium_Phone_API_36.1
   ```
   Without the PATH setup, use the full path: `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1`.
   Optional: `-no-snapshot-load` for a cold boot, `-wipe-data` to reset the device.

3. Confirm the emulator is booted:
   ```bash
   adb devices
   ```

4. Build and install the app (run from the `Android` folder; the project has no Gradle wrapper, so use the installed `gradle`):
   ```bash
   cd Android
   gradle installDebug
   ```

5. Launch the app:
   ```bash
   adb shell am start -n app.islamiccalendar/.MainActivity
   ```

Alternatively, in Android Studio use **Tools > Device Manager**, press the play button next to an AVD, then **Run**.
