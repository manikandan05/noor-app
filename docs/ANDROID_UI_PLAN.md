# Android UI and Functionality Plan

Gap analysis between the app screenshots and the current Android implementation (`Android/`, Kotlin, Jetpack Compose).
Most screens already exist, so this plan covers verification, fixes and polish rather than a rebuild.

## Image sources referenced

| Location | Status |
|---|---|
| `docs/screenshots/home.png` | Viewed |
| `docs/screenshots/home-ar.png` | Viewed |
| `docs/screenshots/calendar.png` | Viewed |
| `docs/screenshots/prayer.png` | Viewed |
| `docs/screenshots/wisdom.png` | Viewed |
| `docs/screenshots/wisdom-ar.png` | Viewed |
| `docs/screenshots/settings.png` | Viewed |
| `docs/screenshots/notification.png` | Viewed |
| `/Users/manikandan/Desktop/image` | **Not read.** It is outside the workspace and the tool blocked access. Enable "Allow outside workspace tool requests" to include it. |
| `.bob/tmp/*.png` (year grid, dark mode, palettes, and others) | Listed only, not opened |

## Screen-by-screen findings

| # | Screenshot | Existing code | Gap or check |
|---|---|---|---|
| 1 | `home.png` | `Home()` in `MainActivity.kt` (Hijri hero card, today's wisdom, upcoming events) | The code also renders `NextPrayerCard`, which the screenshot lacks, so the screenshot is outdated. |
| 2 | `home-ar.png` | RTL via `isRtl()`, localized hero card | Wisdom card text is English under Arabic labels and punctuation is misplaced. Verify against the latest translations commit. |
| 3 | `calendar.png` | `CalendarScreen()` (Gregorian/Hijri toggle, category chips, month and year pickers, events list) | Events list looks low contrast. Week start is hard-coded Sunday-first in `weekdays()`. |
| 4 | `prayer.png` | `PrayerScreen.kt` (times, Qibla, manual coordinates, method, Asr, per-prayer adjustment, alerts) | No functional gap seen. Screenshot is cropped, so Maghrib, Isha and alert rows are not shown. |
| 5 | `wisdom.png` | `Wisdom()` (search, list/cards toggle, category and type filters) | List and cards views use different label keys (`min_suffix` vs `min_short`, `typeFull` vs `typeName`). Category chip label is derived from the id string. |
| 6 | `wisdom-ar.png` | Same code | Category chips and card content appear in English. Verify against the Arabic JSON. |
| 7 | `settings.png` | Language, theme mode, palette, reminder, time picker, preview | No gap seen. |
| 8 | `notification.png` | `Reminder.show()` with a Read action | Confirm the notification icon and dark-mode rendering. |

## Plan

1. **Baseline.** Run `cd Android && gradle installDebug` on an emulator. Walk all five tabs in English, Arabic and Urdu, in light and dark. Record mismatches against the screenshots.
2. **Home.** Confirm the next-prayer card sits between the hero and wisdom cards and the countdown ticks every second. Fix RTL punctuation and mixed-language text.
3. **Calendar.** Raise the events list contrast. Verify the 1900 to 2100 clamp, the Today button and the Hijri grid showing Gregorian numbers. Optional: add a week-start setting in `AppState` and `Settings`.
4. **Prayer.** Test location permission, manual coordinates and the Qibla compass on a real device. Test exact-alarm permission and `PrayerAlerts` rescheduling after boot and timezone change.
5. **Wisdom.** Unify the labels and type names across list and cards. Localize category chips and card labels for Arabic and Urdu using `categoryId` from the JSON. Check the saved star and the lesson dialog.
6. **Settings and notification.** Verify the permission-denied flow, time-picker rescheduling, Hijri adjustment (-2 to +2) propagating to all screens, and the test notification.
7. **Navigation polish (optional).** Replace the emoji nav icons with Material icons for consistent rendering.
8. **Docs and QA.** Update the outdated "Parity gaps" line in `Android/README.md`. Recapture screenshots, including Home with the prayer card. Run `gradle lint assembleDebug`.
