# Noor – Ads, Privacy Policy and Play Store Publishing Guide

Package name: `app.islamiccalendar` · App name: **Noor** · Min SDK 26 · Target SDK 35

Follow the parts in order. Parts 1–3 can be done now; Part 5 onwards needs a Google Play developer account.

---

## Part 1 – Host the privacy policy (get a public URL)

AdMob and Google Play both require a **public HTTPS privacy policy URL**. A draft is in [`privacy-policy.html`](privacy-policy.html).

### 1.1 Edit the draft
1. Open `docs/privacy-policy.html`.
2. Replace `YOUR_EMAIL@example.com` with a real contact email.
3. Update the "Last updated" date.
4. If you later add analytics, accounts, a backend or other SDKs, update the policy first.

This draft is a starting point, not legal advice.

### 1.2 Publish it for free with GitHub Pages
1. Create a **public** GitHub repository, for example `noor-privacy`.
2. Upload `privacy-policy.html` and rename it to **`index.html`**.
3. Go to **Settings → Pages**.
4. Under **Build and deployment**, choose **Deploy from a branch**, branch `main`, folder `/ (root)`, then **Save**.
5. Wait about a minute. The URL will be `https://<your-username>.github.io/noor-privacy/`.
6. Open the URL in a browser and check it loads over HTTPS.

Other free options: Netlify Drop (drag the file in), Cloudflare Pages, or Google Sites. The URL must be public, not behind a login, and not a PDF download.

Keep this URL. You will paste it into AdMob (Part 3) and Play Console (Part 6).

---

## Part 2 – AdMob account, app and ad unit

### 2.1 Create the account
1. Go to <https://admob.google.com> and sign in with a Google account.
2. Accept the terms. Choose your country and time zone. **The country cannot be changed later.**

### 2.2 Register the app
1. **Apps → Add app → Android**.
2. "Is the app listed on a supported app store?" → **No** (link it after publishing, see 2.4).
3. App name: **Noor** → **Add app**.
4. Copy the **App ID**. It looks like `ca-app-pub-1234567890123456~1234567890` (contains a **tilde ~**).

### 2.3 Create the banner ad unit
1. **Apps → Noor → Ad units → Add ad unit → Banner**.
2. Name it, for example `Noor_Home_Banner` → **Create ad unit**.
3. Copy the **Ad unit ID**. It looks like `ca-app-pub-1234567890123456/1234567890` (contains a **slash /**).

New ad units can take from a few minutes to about an hour to start serving. "No fill" during that time is normal.

### 2.4 Where the IDs go in the project
Edit [`app/build.gradle.kts`](../app/build.gradle.kts), inside `defaultConfig`:

```kotlin
manifestPlaceholders["admobAppId"] = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"   // App ID (~)
buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ\"")  // Ad unit ID (/)
```

- The App ID goes into the manifest through `${admobAppId}`.
- The Ad unit ID is read in [`Ads.kt`](../app/src/main/kotlin/app/islamiccalendar/Ads.kt).
- `app.islamiccalendar` is the package name (`applicationId`). You do not type it into AdMob while the app is unlisted. You enter it when linking the app to Google Play after publishing (see 7.4).

### 2.5 Do not click your own live ads
**Clicking your own live ads, or repeatedly refreshing them, can get the AdMob account suspended.** The project currently uses your live IDs in debug builds too. For development, either:
- temporarily use Google's test IDs:
  - App ID `ca-app-pub-3940256099942544~3347511713`
  - Banner `ca-app-pub-3940256099942544/6300978111`
- or register your emulator or phone as a test device in AdMob under **Settings → Test devices**.

---

## Part 3 – GDPR consent message and privacy policy URL in AdMob

The app already runs Google's consent flow (UMP SDK) in `Ads.kt`: ads are only requested after consent allows it. You must create the message in AdMob for EEA/UK users to see it.

### 3.1 Add the privacy policy URL to the app
This fixes the warning *"One or more apps selected are affected: missing privacy policy URL"*.
1. AdMob → **Apps → View all apps → Noor → App settings**.
2. Find **Privacy policy URL** (also called "App store details" or "App information").
3. Paste your URL from Part 1 and **Save**.

If you can't find the field, you can enter it while creating the message in 3.2, in the step that asks for the privacy policy.

### 3.2 Create the GDPR message
1. **Privacy & messaging → European regulations → GDPR → Create message**.
2. **Select apps:** Noor → Continue.
3. **Consent options:** choose **Consent or Manage options** (Accept + Manage options).
4. **Ad partners:** keep the default list, or change it under **Manage**.
5. **Customise** the title, text, and button wording if you want.
6. **Privacy policy URL:** paste the URL from Part 1.
7. **Publish.** It can take up to a few hours to become active.

### 3.3 Optional – US states message
**Privacy & messaging → US states → Create message → Noor.** Skip this if you will not serve US users yet.

### 3.4 Optional – let users change their consent later
Google expects users to be able to change their choice. A "Privacy settings" row in the in-app Settings screen that calls `UserMessagingPlatform.showPrivacyOptionsForm()` would do this. It is not implemented yet.

### 3.5 Testing the consent form outside Europe
Outside the EEA the form never shows. To test it, add a debug-only `ConsentDebugSettings` with your device's hashed ID and `DEBUG_GEOGRAPHY_EEA`. The emulator logged this hash earlier: `B3EEABB8EE11C2BE770B684D95219ECB`. Remove it before release.

---

## Part 4 – Block ad categories (recommended)

AdMob → **Blocking controls → Apps → Noor → General categories**. Block categories that do not suit a religious app, for example gambling, dating, alcohol and religion/spirituality ads from other faiths. Also review **Sensitive categories**.

---

## Part 5 – Prepare the release build

### 5.1 Create a signing key (one time)
```bash
keytool -genkeypair -v -keystore noor-release.jks -alias noor \
  -keyalg RSA -keysize 4096 -validity 10000
```
Choose strong passwords and store the keystore and passwords in a **password manager or other secure backup**. If you lose the key you cannot update the app under the same upload key (Play App Signing can reset it, see 6.5).

### 5.2 Never commit secrets
The project root `Android/` should have a `.gitignore` that contains at least:

```
build/
.gradle/
.kotlin/
local.properties
keystore.properties
*.jks
*.keystore
```

A `.gitignore` could not be created automatically in this workspace, so **create it yourself** with the lines above before your first commit. Never commit `*.jks` or `keystore.properties`.

### 5.3 Tell Gradle about the key
Create `Android/keystore.properties` (not committed):

```properties
storeFile=noor-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=noor
keyPassword=YOUR_KEY_PASSWORD
```

[`app/build.gradle.kts`](../app/build.gradle.kts) reads this file. If the file is missing, release builds are produced **unsigned** (fine for checking that the build works, but Play will not accept them).

### 5.4 Check before building
- Real AdMob App ID and Banner ID are set (2.4).
- `versionCode` is higher than the last upload and `versionName` is correct. Increase `versionCode` for **every** upload.
- You tested the app on a real device, including notifications, prayer times and location permission.
- If you used test IDs while developing, switch back to your live IDs.

### 5.5 Build the Android App Bundle (.aab)
Google Play requires `.aab` for new apps, not `.apk`.

```bash
cd Android
export ANDROID_HOME=$HOME/Library/Android/sdk
gradle bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

To test an APK of the release build on a device: `gradle assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk`.

---

## Part 6 – Google Play Console

### 6.1 Developer account
1. Go to <https://play.google.com/console> and sign up.
2. One-time registration fee (currently **US$25**).
3. Complete identity verification. **Personal accounts** need ID verification and a contact phone number and address. **Organisation accounts** need a D-U-N-S number.

### 6.2 Closed-test requirement for new personal accounts
Personal developer accounts created after November 2023 must run a **closed test with at least 12 testers opted in for 14 continuous days** before they can apply for production access. Organisation accounts are exempt. Check the current rule in Play Console's help, because Google changes these requirements.

### 6.3 Create the app
1. **Create app**.
2. App name **Noor**, default language, **App** (not game), **Free**.
3. Accept the declarations.

### 6.4 Fill in the "Set up your app" tasks
| Section | What to enter |
|---|---|
| **Privacy policy** | URL from Part 1. |
| **App access** | All functionality available without login. |
| **Ads** | **Yes, my app contains ads.** |
| **Content rating** | Complete the questionnaire. It is a utility/reference app with no user-generated content, violence or gambling. |
| **Target audience** | Choose adults / 13+. Do not select children, otherwise stricter ads rules (Families policy) apply. |
| **Data safety** | See 6.6. |
| **Government apps / Financial features / Health** | No. |
| **News app** | No. |

### 6.5 Play App Signing
When you upload your first `.aab`, accept **Play App Signing**. Google keeps the app-signing key and your `noor-release.jks` becomes the **upload key**. This lets Google help you reset a lost upload key.

### 6.6 Data safety form
Declare what the app and its SDKs do. Based on the current app:

| Data type | Collected? | Notes |
|---|---|---|
| Approximate location | Processed on device; not sent to you. The AdMob SDK may collect coarse location. | Declare as collected for **advertising**, not shared by you except with the ad provider. Check Google's AdMob guidance for the latest wording. |
| Device or other IDs (advertising ID) | Yes, by AdMob | Purpose: advertising. |
| App activity / diagnostics | Possibly by AdMob | Purpose: advertising, analytics. |
| Personal info (name, email) | No | |

Also state: data is encrypted in transit, and users can request deletion by clearing app data / uninstalling. Review Google's page "Understand how Google's ad SDKs handle data" and answer accordingly. Your answers must match the privacy policy.

### 6.7 Permissions declarations
The app requests:
- `ACCESS_COARSE_LOCATION` (prayer times and Qibla; explain in the declaration).
- `SCHEDULE_EXACT_ALARM` (prayer-time alerts). On Android 14+ this is denied by default for new installs, and Play may ask you to justify it. If prayer alerts are a core function, explain that. Alternatively evaluate `USE_EXACT_ALARM`, which Play restricts to alarm and calendar apps.
- `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `INTERNET`, `AD_ID`.

Answer any permission forms Play Console shows you.

### 6.8 Store listing
Prepare:
- **App name:** Noor (up to 30 characters).
- **Short description** (up to 80 characters), for example: "Islamic calendar, prayer times and daily wisdom."
- **Full description** (up to 4000 characters).
- **App icon:** 512 × 512 PNG, 32-bit, up to 1 MB. Source: `asset/noor_icon_cutout.png`. **Crop the white margin and export on a plain background**; do not upload the transparent cut-out as it is.
- **Feature graphic:** 1024 × 500 PNG or JPG.
- **Phone screenshots:** at least 2, up to 8 (16:9 or 9:16, each side between 320 px and 3840 px). Take them on the emulator.
- Optional: 7-inch and 10-inch tablet screenshots.
- Contact email (public) and optionally website.

If you offer Arabic and Urdu, add translated listings under **Store settings → Manage translations**.

### 6.9 Upload and test
1. **Testing → Closed testing → Create track → Create new release.**
2. Upload `app-release.aab`.
3. Add release notes, for example "First release."
4. Add testers (email list or Google Group) and share the opt-in link.
5. Review and **Start rollout to closed testing**.
6. Keep the test running (see 6.2 if it applies), collect feedback, fix issues.

### 6.10 Release to production
1. **Production → Create new release** (or promote the tested release).
2. Choose the countries and regions.
3. Check **Publishing overview** for outstanding errors.
4. **Send for review.** First reviews often take a few days, sometimes longer.
5. After approval, choose managed publishing if you want to control the go-live time.

---

## Part 7 – After publishing

### 7.1 Check the live app
Install it from Play Store on a real device, check that ads load and the consent form appears for EEA users (use a VPN or the debug setting).

### 7.2 Check AdMob
Use the AdMob dashboard to see requests and impressions. Low fill in the first days is normal.

### 7.3 Payments
AdMob → **Payments**: add your payments profile, tax information and a bank account. Earnings are paid after reaching the payment threshold and passing PIN verification (a PIN is mailed to your address).

### 7.4 Link the app to Google Play
AdMob → **Apps → Noor → App settings → Link to app store**, search for `app.islamiccalendar`, and confirm. Linking unlocks store-based features and better ad matching. The package name must match exactly.

### 7.5 Updates
For each update: increase `versionCode` (and `versionName`), rebuild with `gradle bundleRelease`, upload to a new release, and roll out. Keep your keystore and `keystore.properties` backed up.

---

## Quick checklist

- [ ] Privacy policy edited with a real email, hosted over HTTPS (Part 1)
- [ ] AdMob app + banner unit created; real IDs in `app/build.gradle.kts` (Part 2)
- [ ] Privacy policy URL added in AdMob; GDPR message published (Part 3)
- [ ] Blocking controls set (Part 4)
- [ ] Keystore created, backed up, and `.gitignore` in place (Part 5)
- [ ] `gradle bundleRelease` produces a signed `.aab` (Part 5)
- [ ] Play developer account verified (Part 6.1)
- [ ] App content, Data safety, permissions and store listing complete (Part 6.4–6.8)
- [ ] Closed test run, then production release sent for review (Part 6.9–6.10)
- [ ] AdMob payments profile and app-store link done (Part 7)
