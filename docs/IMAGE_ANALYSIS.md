# Workspace Images Analysis Progress

This document tracks the detailed UI/UX and functional analysis of reference images located in the `image/` directory inside the project workspace.

---

## Analysis Summary & Progress

| # | File Name | Screen / Feature | Status |
|---|---|---|---|
| 1 | [`image/unnamed.webp`](image/unnamed.webp) | Prayer Times & Daily Tracker (Today Tab) | ✅ Analyzed |
| 2 | [`image/unnamed (1).webp`](image/unnamed (1).webp) | Quran Reader with Themes & Audio Player | ✅ Analyzed |
| 3 | [`image/unnamed (2).webp`](image/unnamed (2).webp) | Smart Qibla Finder with Compass Skins & Map | ✅ Analyzed |
| 4 | [`image/unnamed (3).webp`](image/unnamed (3).webp) | Quran Hub, Audio Recitations & Offline Downloads | ✅ Analyzed |
| 5 | [`image/unnamed (4).webp`](image/unnamed (4).webp) | Reading Session Tracker, Bookmarks & Quick Resume | ✅ Analyzed |
| 6 | [`image/unnamed (5).jpg`](image/unnamed (5).jpg) | Digital Tasbih / Dhikr Counter & Bead Visualizer | ✅ Analyzed |
| 7 | [`image/unnamed (6).jpg`](image/unnamed (6).jpg) | Ummah Global Community Feed, Chat & Social Interactions | ✅ Analyzed |
| 8 | [`image/unnamed (7).jpg`](image/unnamed (7).jpg) | Duas & Supplications Library (Hisnul Muslim) | ✅ Analyzed |

---

## Detailed Screen-by-Screen Breakdown

### 1. `image/unnamed.webp` — Prayer Times & Daily Tracker (Today Tab)
- **Marketing / Feature Headline**: *"Pray on Time with Accurate Prayer Alerts"*
- **Top Header**:
  - Date display: "Today, 25 August" with Hijri equivalent "1 Rabi' I 1447 AH".
  - Date navigation: Previous (`<`) and Next (`>`) arrows to switch days.
- **Hero / Status Dashboard (2 Cards)**:
  - **Active Prayer Card (Left)**: "Now" badge, current prayer name ("Ashar") with weather/sun icon, large digital time ("15.17"), and next prayer countdown ("Maghrib in 2h 26m").
  - **Daily Prayer Progress Card (Right)**: Circular progress ring with checkmark indicator displaying "2/5 prayed".
- **Prayer Timetable List**:
  - Individual rows for each of the 5 daily prayers (Subuh: 04.40, Dzuhur: 11.58, Ashar: 15.17, Maghrib: 17.56, Isya: 19.05).
  - Status checkmarks: Circle checkmark for completed prayers (Subuh, Dzuhur).
  - Current prayer highlight: Outline border and "Now" badge for Ashar.
  - Upcoming countdown tag: "in 2h 26m" tag next to Maghrib.
  - Audio/Notification controls: Individual speaker / muted bell icons per prayer.
- **Quick Action**:
  - Pill button at bottom: "Mark all as prayed".
- **Bottom Navigation**:
  - 5 tabs: **Today** (Active), **Prayers**, **Quran**, **Videos**, **Ummah**.

---

### 2. `image/unnamed (1).webp` — Quran Reader with Themes & Audio Player (Quran Tab)
- **Marketing / Feature Headline**: *"Recite Daily with Guidance & Translation in Your Language"*
- **Top App Bar / Navigation**:
  - Back arrow (`←`), Surah Selector dropdown ("Al-Baqara ▾"), Reading goal status bar ("Reading goal: 0/5 mins" with circular progress).
- **Themes Modal / Popup Menu**:
  - Top popover showing reading themes:
    - **Modern**: Clean light background with modern Arabic typography.
    - **Paper**: Warm parchment/cream background (Selected with green border).
    - **Dark**: Dark slate/black theme with white text.
- **Verse (Aya) Display**:
  - Verse selector dropdown chip ("Aya 2:4 ▾").
  - Large Arabic script with tajweed colored diacritics / highlights (e.g. green tajweed marks).
  - Verse end ornament with Arabic numeral (`④`).
  - Transliteration text below Arabic (Latin phonetic transliteration).
  - Translation card in English (e.g., *"And who believe in the Revelation sent to thee..."*).
- **Audio Recitation Floating Bar / Player**:
  - Floating bottom player bar in warm parchment tone.
  - Controls: Play/Pause button, Surah/Aya title ("Al-Baqara, Aya 1"), Reciter name ("Saad Al-Ghamdi"), Playback speed toggle ("1x"), Rewind / Fast-forward buttons.

---

### 3. `image/unnamed (2).webp` — Smart Qibla Finder with Compass Skins & Map
- **Marketing / Feature Headline**: *"Smart Qibla Finder. Wherever You Are."*
- **Top App Bar / Navigation**:
  - Back arrow (`<`), Qibla degree bearing title ("Qibla: 292°"), Map view action button (top right).
- **Map View Integration**:
  - Upper section displays an interactive map tracking user's location to Kaaba (Mecca) with a dotted trajectory path and Kaaba badge marker.
- **Main Compass Dial**:
  - Dynamic rotating compass dial with precision tick marks and cardinal points (N, E, S, W).
  - Large emerald-green needle pointer pointing to the precise Qibla direction.
  - Kaaba icon indicator aligned at the top needle position.
  - Radiant green halo/glow effect when phone is aligned facing Mecca.
  - Real-time status feedback text: *"You're now facing Mecca"*.
- **Compass Skin / Theme Selector**:
  - Bottom floating horizontal carousel docking 5 distinct visual compass themes:
    1. **Islamic Green**: Classic green and gold Islamic dial.
    2. **Compass Rose**: Red/black nautical navigational star.
    3. **Minimal Clean**: Modern white dial with green rim & needle (Selected).
    4. **Astrolabe / Antique**: Vintage parchment & brass historical astrolabe style.
    5. **Dark Luxury**: Matte black dial with gold ring and center Kaaba icon.

---

### 4. `image/unnamed (3).webp` — Quran Hub, Audio Recitations & Offline Downloads
- **Marketing / Feature Headline**: *"Let the words of Allah accompany you. Listen wherever you go."*
- **Audio Reciter Selection & Offline Audio System**:
  - Highlights offline listening support (offline wifi indicator badge).
  - Reciter profiles with download counts:
    - *Abdul Rahman As Sudais* (Arabic, 0/114 downloaded).
    - *Mishari Rashid al-Afasy* (Arabic, 0/114 downloaded).
    - *Saad Al-Ghamdi* (Arabic, 2/114 downloaded).
  - Audio playback controls: Previous, Large circular Play, Next, Headphone icon.
- **Top App Bar & Sub-Navigation Tabs**:
  - Top bar with Back arrow (`←`), Title ("Quran"), Bookmark button, Headphone audio launcher.
  - Three main tabs:
    - **Read** (Selected with underline indicator).
    - **Learn**.
    - **My Progress**.
- **Recents Carousel**:
  - Section title: "Recents" with information icon (`ⓘ`).
  - Horizontal scrolling cards containing:
    - Traditional Arabic Surah calligraphy icon.
    - Surah name & Verse ("Al-Baqara Aya 26", "Al-Faatiha Aya 3", etc.).
    - Timestamp ("Just now", "2d", "03/12/25").
- **Sura List Section**:
  - Header: "Sura List" with filter selector ("View: Sura ▾").
  - Clean card-based Surah rows:
    - Surah number (1, 2, ...).
    - Arabic calligraphy badge.
    - English transliteration and meaning ("Al-Faatiha - The Opener", "Al-Baqara - The Cow").

---

### 5. `image/unnamed (4).webp` — Reading Session Tracker, Bookmarks & Quick Resume
- **Marketing / Feature Headline**: *"Track Your last reading session with bookmarks"*
- **Last Read & Daily Reading Session Card**:
  - Floating high-priority card at top of Quran screen:
    - **Circular Daily Reading Ring**: Cyan circular progress displaying time spent (*"12 minutes read"* with open book icon).
    - **Last Read Metadata**: Label "Last Read", target Surah and Verse (*"Al-Baqara 2:26"*).
    - **Call-to-Action**: *"Continue Reading →"* leading directly to the exact ayah.
    - **Pagination Indicator**: Carousel dots for multiple bookmarks / recent reading spots.
- **Extended Sura List**:
  - Highlights additional Sura rows:
    - `3` — `Aal-i-Imraan` (*Family of Imran*).
    - `4` — `An-Nisaa` (*The Women*).

---

### 6. `image/unnamed (5).jpg` — Digital Tasbih / Dhikr Counter & Bead Visualizer
- **Marketing / Feature Headline**: *"Take a moment. Breathe. Remember Allah."*
- **Digital Tasbih / Bead String Counter**:
  - **Progress Display**: Large prominent counter reading *"25 / 33"* (current count in green, target in dark charcoal).
  - **Realistic 3D Bead Visualizer**: Arched string of emerald jade beads that animate/slide dynamically as the user taps to count.
- **Active Dhikr Floating Card**:
  - Translucent rounded card displaying:
    - Arabic text with harakat: `اللَّهُ أَكْبَرُ`
    - Green phonetic transliteration: *Allahu Akbar*
    - English translation: *Allah is The Greatest*
- **Experience Philosophy**:
  - Focus on tranquility, mindfulness, breathing, and distraction-free remembrance (Dhikr / Tasbih).

---

### 7. `image/unnamed (6).jpg` — Ummah Global Community Feed, Chat & Social Interactions (Ummah Tab)
- **Marketing / Feature Headline**: *"Follow, share and connect with Muslims Worldwide."*
- **Community Feed Filters & Topics**:
  - Horizontal chip filter: **All**, **Singapore** (Location-based community tag), **Quran**, **Dua**.
- **Social Post Cards**:
  - **User Profile Header**: Avatar, username (with badge/crown `👑`), timestamp (`2d`), location origin (e.g. `India`), and overflow options menu (`···`).
  - **Multimedia & Text Posts**:
    - Quranic reflections with Arabic verses and translations (e.g., Ayah 2:152).
    - Image attachments (e.g., scenic outdoor prayer rug photo).
    - Community announcements and hashtag competitions (e.g., *#noor14 ummah knowledge kup*).
  - **Engagement Bar**: Counters for Comments (`💬`), Likes (`🤍`), Shares (`➦`), and Views (`👁`).
- **Interactive Chat Floating Elements**:
  - Direct community messaging / greeting bubbles (*"assalamualaikum"*, *"waalaikumsalamm"*).
- **Navigation & Creation Controls**:
  - Floating Scroll-to-Top button (`⌃`).
  - Green floating action button (`+`) to create new posts or share moments with the Ummah.

---

### 8. `image/unnamed (7).jpg` — Duas & Supplications Library (Hisnul Muslim)
- **Marketing / Feature Headline**: *"Start Every Action with a Dua"*
- **Top App Bar & Search**:
  - Back arrow (`<`), Title ("Duas"), Bookmarks icon (top right).
  - Search bar with context query placeholder (*"After wudhu..."*) and search icon (`🔍`).
- **Quick Category Action Dock**:
  - 4 quick navigation buttons with distinctive iconography:
    1. **Bookmarks**: Saved favorite supplications.
    2. **Recents**: Recently recited / viewed supplications.
    3. **Hisnul Muslim**: Complete Fortress of the Muslim index.
    4. **Tasbih**: Direct shortcut to the Tasbih counter.
- **Hisnul Muslim Chapters (Thematic Categories)**:
  - Rich photographic / illustrative cards:
    - **Food & Drink**: Mealtime prayers (*7 Chapters*).
    - **Morning & Evening**: Daily Adhkar (*6 Chapters*).
    - **Travel & Others**: Journey supplications.
- **Essential Duas Cards**:
  - Large feature cards with scenic background photography and accompanying Hadith virtues / explanations:
    - *When leaving the house*: *"The virtue when read is that the angel will say: 'You shall be defended, protected and guided.'"*
    - *Remembrance after Salah*: Hadith guidance on post-prayer Adhkar.

---
