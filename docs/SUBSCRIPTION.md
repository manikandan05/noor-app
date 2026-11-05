# Noor Premium: subscription setup, lifecycle and compliance

Premium is one Google Play subscription (`noor_premium`) with two base plans (`monthly`, `yearly`).
There is **no backend and no account**. Google Play is the source of truth for who is subscribed.

## 1. What Premium unlocks

| Feature | Free | Premium |
|---|---|---|
| Ad banner | Shown | Hidden |
| Reading voice | Engine default only | Full voice list (up to 10, male/female marks) |
| Colour palettes | Classic, Emerald | Classic, Emerald + Sapphire, Desert, Twilight |
| Calendar, prayer, Quran, dhikr, wisdom, reminders | Yes | Yes |

Core religious content stays free. Free palettes are listed in `FreePaletteIds` (`Theme.kt`).

## 2. Play Console setup (you do this once)

1. Upload a signed build to an **internal testing** track. Billing does not work on an `adb install` debug build.
2. **Monetize > Products > Subscriptions > Create subscription**
   - Product ID: `noor_premium` (must match `Billing.PRODUCT_ID`).
   - Base plan `yearly`: billing period 1 year, auto-renewing. Set the price.
   - Base plan `monthly`: billing period 1 month, auto-renewing. Set the price.
   - Suggested pricing: yearly about 8 months of the monthly price. Add regional prices.
   - Base plan IDs must be exactly `yearly` and `monthly` (`Billing.PLAN_YEARLY`, `PLAN_MONTHLY`).
3. **Free trial (optional but recommended):** add an offer to the `yearly` base plan with a *Free trial* phase
   (for example 7 days), eligibility "new customer acquisition". The app finds the trial automatically and shows
   "Start free trial". No code change is needed to change the trial length.
4. **Grace period and account hold:** under the subscription's *Billing > Subscription settings*, enable a grace
   period (suggested 7 days) and account hold (suggested 30 days). The app treats both as "not Premium"
   (Play does not return the purchase while on hold); during grace Play still returns it, so Premium stays on.
5. **Licence testers:** *Settings > License testing*, add tester Gmail accounts. Test purchases renew quickly
   (see section 6).
6. Complete the **Data safety** form (section 5) and add a privacy policy URL that mentions subscriptions.

## 3. Lifecycle: what the app does in every case

The app calls `Billing.refresh()` at launch and on every `onResume`. Only a successful answer from Play
changes the state. A failed call (offline, Play busy) keeps the cached state for up to 7 days
(`OFFLINE_GRACE_MS`).

| Situation | What Play returns | App behaviour |
|---|---|---|
| Never subscribed | No purchase | Free |
| Starts free trial | Purchase `PURCHASED`, auto-renewing | Premium on, immediately |
| Cancels during trial | Purchase still returned, `isAutoRenewing = false` | Premium stays on until the trial ends; Settings shows "Renewal is off..." |
| Trial ends, not cancelled | Purchase continues | Premium on; user is charged by Play |
| Trial ends after cancel | No purchase | Premium off at next refresh (ads, locks return) |
| Monthly/yearly renewal succeeds | Same purchase, new period | Premium stays on, nothing to do |
| Payment fails, in grace period | Purchase still returned | Premium stays on |
| Payment fails, account hold | No purchase | Premium off until payment is fixed |
| User cancels (paid period) | `isAutoRenewing = false` | Premium on until period end, then off |
| Refund or revoke (Play, support, chargeback) | No purchase | Premium off at next refresh |
| Pending payment (cash, bank) | `PENDING` | Not Premium; Settings shows "payment is pending"; unlocks when it completes |
| Purchase not acknowledged | Unacknowledged | App acknowledges on each refresh; Google refunds after 3 days if never acknowledged |
| Reinstall or new phone | Purchase for the Google account | "Restore purchases" or the next launch restores it |
| Offline for days | Call fails | Cached Premium honoured up to 7 days, then Free until Play can be reached |
| Premium palette or voice saved, then subscription lapses | n/a | Palette falls back to Classic, voice to default; saved choice returns if Premium returns |

Edge cases and known limits:
- **Revoke detection is not instant.** With no backend, a refund is noticed at the next app launch/resume,
  not the moment it happens. Adding a server with Play real-time notifications (RTDN) would make it immediate.
- **Offline grace** means a revoked user could keep Premium for up to 7 days if they stay offline.
- **Local state is not tamper-proof.** A rooted user could edit the preferences file. This is acceptable for
  cosmetic features and ads; do not gate anything with real cost behind client-only checks.

## 4. Policy requirements already handled

- Price, billing period, auto-renew and cancellation are shown **before** the purchase button
  (`PaywallDialog`, strings `premium_terms*`). Price is read from Play, never hard-coded.
- Free-trial length and "charged unless you cancel before the trial ends" are shown for trial offers.
- "Manage or cancel subscription" opens Google Play's subscription page for this app.
- "Restore purchases" is available in Settings and in the paywall.
- Cancel is reachable in two taps and never requires contacting the developer.
- Premium never blocks core content, so a lapse cannot lock users out of prayer times or the Quran.

## 5. Data safety and privacy answers

The app does not collect or store payment data. Google Play handles all payment details.

| Data | Collected by the app? | Notes |
|---|---|---|
| Purchase history / subscription status | Processed on device only | Stored as `prem_ok`, `prem_at`, `prem_renew` flags in local preferences; not sent to any server of yours |
| Payment info (card, address) | No | Handled entirely by Google Play |
| Purchase token | Not stored | Held only in memory during acknowledgement |
| Approximate location | Yes, on device only | Used for prayer times (existing behaviour) |
| Advertising ID | Yes (free users) | Used by AdMob for ads; declare under "Device or other IDs" |

Data safety form guidance:
- *Does your app collect or share user data?* Yes (advertising ID shared with AdMob for free users; approximate
  location processed on device).
- *Financial info > Purchase history:* mark only if you later add a backend. With the current on-device design,
  purchases are processed by Google Play; check the current Play guidance when you submit.
- Declare AdMob as a third-party SDK that shares the advertising ID, with the purpose "Advertising".
- Premium users do not load ads, so the advertising ID is not used for them.
- Update the privacy policy to say: subscriptions are billed and managed by Google Play; the app never sees
  payment details; cancel in Google Play > Subscriptions.

In-app text: `about_text` in `Strings.kt` was updated (English, Arabic, Urdu) to match.

## 6. Test plan (internal track, licence-tester account)

Test subscriptions renew on an accelerated schedule (for example a 1-month plan renews every 5 minutes and a
1-year plan every 30 minutes, for a limited number of cycles), so the full lifecycle can be seen in one sitting.

1. Fresh install, no purchase: ads show, voices and palettes locked, Settings shows "Go Premium".
2. Paywall shows both plans with Play prices; yearly selected; trial text appears if the offer exists.
3. Buy yearly with trial: Premium turns on, ad disappears, locks clear, card says "Premium is active".
4. Cancel in Play during the trial: return to the app; card shows "Renewal is off...", Premium still on.
5. Wait for the trial to end: after the next resume, Premium is off.
6. Buy again without cancelling: confirm renewal keeps Premium on across renewals.
7. Refund or revoke the order in Play Console: reopen the app; Premium is off.
8. Choose a premium palette and voice, let Premium lapse: palette shows Classic, voice is default.
9. Reinstall and tap Restore purchases: Premium returns.
10. Airplane mode with Premium: still Premium; after 7 days offline it falls back to Free.
11. Pending payment (use the "slow test card" licence-tester option): "payment is pending", then unlocks.
12. Languages: check the paywall in English, Arabic (right-to-left) and Urdu.

## 7. Files

| File | Role |
|---|---|
| `Billing.kt` | Connection, plans, purchase, restore, acknowledge, entitlement state, offline cache |
| `MainActivity.kt` | `Billing.init`, `onResume` refresh, `PremiumCard`, `PaywallDialog`, locks on voices and palettes |
| `Ads.kt` | `AdSlot()` hidden for Premium |
| `TextToSpeechManager.kt` | Default voice enforced without Premium |
| `Theme.kt` | `FreePaletteIds` |
| `Strings.kt` | `premium_*` strings (English, Arabic, Urdu) and updated privacy text |
