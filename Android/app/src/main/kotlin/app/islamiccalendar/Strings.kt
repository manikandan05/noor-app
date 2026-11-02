package app.islamiccalendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/** UI chrome strings only (menus, calendar, buttons, settings). Lesson/quote text is intentionally not translated here. */
val LocalLang = compositionLocalOf { "en" }
val LocalDark = compositionLocalOf { false }

val languages = listOf("en" to "English", "ar" to "العربية", "ur" to "اردو")
fun isRtl(lang: String) = lang == "ar" || lang == "ur"
fun localeOf(lang: String): Locale = Locale.forLanguageTag(lang)

private val arMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
    "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
private val urMonths = listOf("محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
    "رجب", "شعبان", "رمضان", "شوال", "ذوالقعدہ", "ذوالحجہ")
fun hijriMonth(m: Int, lang: String) = when (lang) { "ar" -> arMonths; "ur" -> urMonths; else -> hijriMonths }[m - 1]
fun ahSuffix(lang: String) = when (lang) { "ar" -> "هـ"; "ur" -> "ھ"; else -> "AH" }

/** Sunday-first weekday headers. */
fun weekdays(lang: String) = when (lang) {
    "ar" -> listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
    "ur" -> listOf("اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ")
    else -> listOf("S", "M", "T", "W", "T", "F", "S")
}

private val idx = mapOf("en" to 0, "ar" to 1, "ur" to 2)
private fun s(en: String, ar: String, ur: String) = listOf(en, ar, ur)

private val T: Map<String, List<String>> = mapOf(
    // navigation
    "home" to s("Home", "الرئيسية", "ہوم"),
    "calendar" to s("Calendar", "التقويم", "کیلنڈر"),
    "wisdom" to s("Wisdom", "الحكمة", "حکمت"),
    "settings" to s("Settings", "الإعدادات", "ترتیبات"),
    // home
    "greeting" to s("Assalamu alaikum", "السلام عليكم", "السلام علیکم"),
    "greeting_sub" to s("Your day, in two calendars", "يومك في تقويمين", "آپ کا دن، دو کیلنڈروں میں"),
    "calc_note" to s("Calculated (Umm al-Qura). Local moon sighting may differ.",
        "محسوب (أم القرى). قد يختلف حسب رؤية الهلال المحلية.", "حسابی (ام القریٰ)۔ مقامی رویتِ ہلال سے فرق ہو سکتا ہے۔"),
    "todays_wisdom" to s("Today's wisdom", "حكمة اليوم", "آج کی حکمت"),
    "practice_today" to s("Practice today: ", "طبّق اليوم: ", "آج عمل کریں: "),
    "tap_more" to s("Tap to read more →", "اضغط لقراءة المزيد ←", "مزید پڑھنے کے لیے ٹیپ کریں ←"),
    "upcoming" to s("Upcoming", "القادمة", "آنے والے"),
    "est" to s("est.", "تقديري", "تخمینی"),
    // calendar
    "calendar_sub" to s("Gregorian and Hijri side by side", "الميلادي والهجري جنبًا إلى جنب", "عیسوی اور ہجری ایک ساتھ"),
    "gregorian" to s("Gregorian", "ميلادي", "عیسوی"),
    "hijri" to s("Hijri", "هجري", "ہجری"),
    "all" to s("All", "الكل", "سب"),
    "cat_ramadan" to s("Ramadan", "رمضان", "رمضان"),
    "cat_eid" to s("Eid", "العيد", "عید"),
    "cat_hajj" to s("Hajj", "الحج", "حج"),
    "cat_islamic_new_year" to s("Islamic new year", "رأس السنة الهجرية", "اسلامی نیا سال"),
    "cat_observance" to s("Observance", "مناسبة", "مناسبت"),
    "year_events" to s("Events in", "مناسبات", "مناسبات"),
    "no_year_events" to s("No events this year.", "لا توجد مناسبات هذا العام.", "اس سال کوئی مناسبت نہیں۔"),

    "no_occasions" to s("No occasions on this date.", "لا توجد مناسبات في هذا التاريخ.", "اس تاریخ پر کوئی مناسبت نہیں۔"),
    "calc_date_note" to s("Calculated date; may differ by moon sighting.", "تاريخ محسوب؛ قد يختلف حسب رؤية الهلال.",
        "حسابی تاریخ؛ رویتِ ہلال سے مختلف ہو سکتی ہے۔"),
    "close" to s("Close", "إغلاق", "بند کریں"),
    "select_month" to s("Select Month", "اختر الشهر", "مہینہ منتخب کریں"),
    "select_year" to s("Select Year", "اختر السنة", "سال منتخب کریں"),
    "today_btn" to s("Today", "اليوم", "آج"),
    "cards_view" to s("Horizontal Cards", "بطاقات أفقية", "افقی کارڈز"),
    "list_view" to s("Vertical List", "قائمة عمودية", "عمودی فہرست"),
    // wisdom
    "wisdom_sub" to s("Short reflections for every day", "تأملات قصيرة لكل يوم", "ہر دن کے لیے مختصر غور و فکر"),
    "search_lessons" to s("Search lessons", "ابحث في الدروس", "اسباق تلاش کریں"),
    "any_type" to s("Any type", "أي نوع", "کوئی بھی قسم"),
    "done" to s("Done", "تم", "ہو گیا"),
    "save" to s("Save", "حفظ", "محفوظ کریں"),
    "saved" to s("Saved ★", "تم الحفظ ★", "محفوظ ★"),
    "practice" to s("Practice: ", "التطبيق: ", "عمل: "),
    "reflect" to s("Reflect: ", "تأمّل: ", "غور کریں: "),
    "min_suffix" to s("min", "دقيقة", "منٹ"),
    "min_short" to s("m", "د", "منٹ"),
    // settings
    "settings_sub" to s("Language, theme, reminders and privacy", "اللغة والمظهر والتذكير والخصوصية", "زبان، تھیم، یاد دہانی اور رازداری"),
    "language" to s("Language", "اللغة", "زبان"),
    "theme_and_appearance" to s("Theme & Appearance", "السمة والمظهر", "تھیم اور ظاہری شکل"),
    "theme_mode" to s("Theme Mode", "وضع السمة", "تھیم موڈ"),
    "appearance" to s("Appearance", "المظهر", "ظاہری شکل"),
    "theme_system" to s("System", "النظام", "سسٹم"),
    "theme_light" to s("Light", "فاتح", "روشن"),
    "theme_dark" to s("Dark", "داكن", "گہرا"),
    "color_palette" to s("Color palette", "لوحة الألوان", "رنگوں کا انتخاب"),
    "palette_classic" to s("Classic Oasis", "الواحة الكلاسيكية", "کلاسک نخلستان"),
    "palette_emerald" to s("Emerald Sanctuary", "محراب الزمرد", "زمرد پناہ گاہ"),
    "palette_sapphire" to s("Sapphire Night", "سماء ياقوتية", "نیلم رات"),
    "palette_desert" to s("Desert Rose", "وردة الصحراء", "صحرائی گلاب"),
    "palette_twilight" to s("Twilight Amethyst", "شفق بنفسجي", "شفق یاقوت"),
    "open_settings" to s("Open phone settings", "فتح إعدادات الهاتف", "فون کی سیٹنگز کھولیں"),
    "adj_title" to s("Hijri date adjustment", "تعديل التاريخ الهجري", "ہجری تاریخ میں ردوبدل"),
    "days" to s("day(s)", "يوم", "دن"),
    "adj_help" to s("Shift dates (−2 to +2) to match your local moon-sighting announcement.",
        "قدّم أو أخّر التواريخ (−2 إلى +2) لتوافق إعلان رؤية الهلال في بلدك.",
        "اپنے علاقے کے اعلانِ رویتِ ہلال کے مطابق تاریخیں (−2 سے +2) آگے پیچھے کریں۔"),
    "reminder_title" to s("Daily wisdom reminder", "تذكير الحكمة اليومية", "روزانہ حکمت کی یاد دہانی"),
    "reminder_help" to s("Get today's wisdom as a notification at the time you choose.",
        "احصل على حكمة اليوم كإشعار في الوقت الذي تختاره.", "اپنی پسند کے وقت پر آج کی حکمت نوٹیفکیشن کی صورت میں پائیں۔"),
    "reminder_time" to s("Reminder time", "وقت التذكير", "یاد دہانی کا وقت"),
    "send_test" to s("Send a test notification", "إرسال إشعار تجريبي", "ٹیسٹ نوٹیفکیشن بھیجیں"),
    "perm_denied" to s("Notification permission was denied. Enable it in system settings.",
        "تم رفض إذن الإشعارات. فعّله من إعدادات النظام.", "نوٹیفکیشن کی اجازت مسترد کر دی گئی۔ سسٹم سیٹنگز سے فعال کریں۔"),
    "preview" to s("Preview", "معاينة", "پیش منظر"),
    "ok" to s("OK", "موافق", "ٹھیک ہے"),
    "cancel" to s("Cancel", "إلغاء", "منسوخ"),
    "about_title" to s("About & privacy", "حول التطبيق والخصوصية", "ایپ اور رازداری"),
    "about_text" to s(
        "Content is in draft and not yet reviewed by a qualified religious editor. No account, analytics or audio. A small ad space is reserved above the bottom menu. Saved items stay on this device.",
        "المحتوى مسودة ولم يراجعه محرر ديني مؤهل بعد. لا حساب ولا تحليلات ولا صوت. مساحة إعلانية صغيرة محجوزة فوق القائمة السفلية. العناصر المحفوظة تبقى على جهازك.",
        "مواد ابتدائی مسودہ ہے اور کسی مستند دینی مدیر نے اس کا جائزہ نہیں لیا۔ نہ اکاؤنٹ، نہ اینالیٹکس، نہ آڈیو۔ نچلے مینو کے اوپر اشتہار کے لیے ایک چھوٹی جگہ مختص ہے۔ محفوظ کردہ چیزیں آپ کے آلے پر رہتی ہیں۔"),
    // prayer
    "prayer" to s("Prayer", "الصلاة", "نماز"),
    "prayer_sub" to s("Prayer times and Qibla for your location", "مواقيت الصلاة والقبلة لموقعك", "آپ کے مقام کے لیے نماز کے اوقات اور قبلہ"),
    "prayer_times" to s("Prayer times", "مواقيت الصلاة", "نماز کے اوقات"),
    "qibla" to s("Qibla", "القبلة", "قبلہ"),
    "p_fajr" to s("Fajr", "الفجر", "فجر"),
    "p_sunrise" to s("Sunrise", "الشروق", "طلوعِ آفتاب"),
    "p_dhuhr" to s("Dhuhr", "الظهر", "ظہر"),
    "p_asr" to s("Asr (Standard)", "العصر (جمهور)", "عصر (معیاری)"),
    "p_asr2" to s("Asr (Hanafi)", "العصر (حنفي)", "عصر (حنفی)"),
    "p_asr_n" to s("Asr", "العصر", "عصر"),
    "p_maghrib" to s("Maghrib", "المغرب", "مغرب"),
    "p_isha" to s("Isha", "العشاء", "عشاء"),
    "m_mwl" to s("Muslim World League", "رابطة العالم الإسلامي", "مسلم ورلڈ لیگ"),
    "m_isna" to s("ISNA (North America)", "أمريكا الشمالية (ISNA)", "شمالی امریکا (ISNA)"),
    "m_egypt" to s("Egyptian Authority", "الهيئة المصرية", "مصری اتھارٹی"),
    "m_makkah" to s("Umm al-Qura (Makkah)", "أم القرى (مكة)", "ام القریٰ (مکہ)"),
    "m_karachi" to s("Karachi (Univ. of Islamic Sciences)", "كراتشي", "کراچی"),
    "next_prayer" to s("Next prayer", "الصلاة القادمة", "اگلی نماز"),
    "location_needed" to s("Location needed for accurate times", "الموقع مطلوب لمواقيت دقيقة", "درست اوقات کے لیے مقام درکار ہے"),
    "location_privacy" to s("Approximate location is used once, saved only on this device (rounded to ~1 km) and never sent anywhere.",
        "يُستخدم الموقع التقريبي مرة واحدة ويُحفظ على هذا الجهاز فقط (مقرّب إلى نحو كيلومتر) ولا يُرسل إلى أي جهة.",
        "اندازاً مقام ایک بار استعمال ہوتا ہے، صرف اسی آلے پر (تقریباً 1 کلومیٹر تک) محفوظ ہوتا ہے اور کہیں نہیں بھیجا جاتا۔"),
    "use_location" to s("Use my location", "استخدم موقعي", "میرا مقام استعمال کریں"),
    "locating" to s("Finding location…", "جارٍ تحديد الموقع…", "مقام تلاش ہو رہا ہے…"),
    "update_location" to s("Update", "تحديث", "اپ ڈیٹ"),
    "enter_manually" to s("Enter coordinates manually", "إدخال الإحداثيات يدويًا", "نقاط دستی درج کریں"),
    "latitude" to s("Latitude", "خط العرض", "عرض البلد"),
    "longitude" to s("Longitude", "خط الطول", "طول البلد"),
    "location_failed" to s("Could not get your location. Turn on device location or enter it manually.",
        "تعذّر تحديد موقعك. فعّل الموقع أو أدخله يدويًا.", "آپ کا مقام نہیں مل سکا۔ لوکیشن آن کریں یا دستی درج کریں۔"),
    "location_denied" to s("Location permission was denied. You can enter coordinates manually.",
        "تم رفض إذن الموقع. يمكنك إدخال الإحداثيات يدويًا.", "مقام کی اجازت مسترد ہوئی۔ آپ نقاط دستی درج کر سکتے ہیں۔"),
    "prayer_note" to s("Calculated for your location. Confirm with your local mosque if they differ; use the adjustments below.",
        "محسوبة لموقعك. تحقق من مسجدك المحلي إن اختلفت، واستخدم التعديلات أدناه.",
        "آپ کے مقام کے لیے حسابی۔ مقامی مسجد سے فرق ہو تو نیچے ایڈجسٹمنٹ استعمال کریں۔"),
    "prayer_settings" to s("Prayer settings", "إعدادات الصلاة", "نماز کی ترتیبات"),
    "prayer_alerts" to s("Prayer alerts", "تنبيهات الصلاة", "نماز کے الرٹس"),
    "prayer_alerts_help" to s("Get a notification when each prayer time arrives.", "احصل على إشعار عند دخول وقت كل صلاة.", "ہر نماز کا وقت ہونے پر نوٹیفکیشن پائیں۔"),
    "prayer_alerts_desc" to s("Notification at each prayer time", "إشعار عند كل وقت صلاة", "ہر نماز کے وقت پر نوٹیفکیشن"),
    "prayer_time_for" to s("It's time for {p}", "حان وقت صلاة {p}", "{p} کا وقت ہو گیا"),
    "exact_alarm_help" to s("For on-time alerts, allow \"Alarms & reminders\". Without it, alerts may be a few minutes late.",
        "لتنبيهات في وقتها، اسمح بـ\"المنبهات والتذكيرات\". بدونها قد تتأخر التنبيهات عدة دقائق.",
        "بروقت الرٹس کے لیے \"الارمز اور ریمائنڈرز\" کی اجازت دیں۔ ورنہ الرٹ چند منٹ دیر سے آ سکتے ہیں۔"),
    "allow_exact" to s("Allow exact alarms", "السماح بالمنبهات الدقيقة", "درست الارم کی اجازت دیں"),
    "calc_method" to s("Calculation method", "طريقة الحساب", "حساب کا طریقہ"),
    "asr_for_alerts" to s("Asr for countdown & alerts", "العصر للعد التنازلي والتنبيهات", "الٹی گنتی اور الرٹس کے لیے عصر"),
    "asr_standard" to s("Standard", "جمهور", "معیاری"),
    "asr_hanafi" to s("Hanafi", "حنفي", "حنفی"),
    "adjust_minutes" to s("Adjust minutes (−30 to +30)", "تعديل بالدقائق (−30 إلى +30)", "منٹوں میں ردوبدل (−30 سے +30)"),
    "qibla_direction" to s("Qibla bearing from true north", "اتجاه القبلة من الشمال الحقيقي", "حقیقی شمال سے قبلہ کی سمت"),
    "qibla_turn_right" to s("Turn right {n}°", "در يمينًا {n}°", "دائیں مڑیں {n}°"),
    "qibla_turn_left" to s("Turn left {n}°", "در يسارًا {n}°", "بائیں مڑیں {n}°"),
    "qibla_aligned" to s("You are facing the Qibla", "أنت تواجه القبلة", "آپ قبلہ کی طرف رخ کیے ہوئے ہیں"),
    "qibla_calibrating" to s("Reading compass… move the phone in a figure 8", "جارٍ قراءة البوصلة… حرّك الهاتف على شكل 8",
        "کمپاس پڑھ رہا ہے… فون کو 8 کی شکل میں گھمائیں"),
    "qibla_no_compass" to s("This device has no compass sensor. Use the bearing above with a physical compass.",
        "لا يحتوي هذا الجهاز على حساس بوصلة. استخدم الزاوية أعلاه مع بوصلة فعلية.",
        "اس آلے میں کمپاس سینسر نہیں۔ اوپر دی گئی زاویہ کسی اصل کمپاس کے ساتھ استعمال کریں۔"),
    "qibla_hint" to s("Hold the phone flat and away from metal or magnets for best accuracy.",
        "أمسك الهاتف أفقيًا وبعيدًا عن المعادن والمغناطيس لأفضل دقة.", "بہترین درستی کے لیے فون کو سیدھا اور دھات یا مقناطیس سے دور رکھیں۔"),
    // notification
    "notif_channel" to s("Daily wisdom", "الحكمة اليومية", "روزانہ حکمت"),
    "notif_channel_desc" to s("A daily reminder with today's wisdom", "تذكير يومي بحكمة اليوم", "آج کی حکمت کے ساتھ روزانہ یاد دہانی"),
    "notif_read" to s("Read", "قراءة", "پڑھیں"),
    // onboarding
    "welcome" to s("Welcome", "مرحبًا", "خوش آمدید"),
    "app_title" to s("Islamic Calendar\n& Daily Wisdom", "التقويم الإسلامي\nوالحكمة اليومية", "اسلامی کیلنڈر\nاور روزانہ حکمت"),
    "tagline" to s("Hijri dates, upcoming occasions and a gentle daily reflection. Private, simple and on your device.",
        "تواريخ هجرية، ومناسبات قادمة، وتأمل يومي هادئ. خاص وبسيط وعلى جهازك.",
        "ہجری تاریخیں، آنے والی مناسبتیں اور روزانہ ایک نرم غور و فکر۔ نجی، سادہ اور آپ کے آلے پر۔"),
    "get_started" to s("Get started", "ابدأ", "شروع کریں"),
    "skip" to s("Skip", "تخطي", "چھوڑیں"),
    "next" to s("Next", "التالي", "اگلا"),
    "finish" to s("Done", "تم", "مکمل"),
    "step_x" to s("Step {a} of {b}", "الخطوة {a} من {b}", "مرحلہ {a} از {b}"),
    "tour1_t" to s("Today at a glance", "اليوم في لمحة", "آج ایک نظر میں"),
    "tour1_d" to s("Home shows today's Gregorian and Hijri date, a daily reflection and the next upcoming occasions.",
        "تعرض الرئيسية تاريخ اليوم الميلادي والهجري وتأملًا يوميًا وأقرب المناسبات القادمة.",
        "ہوم پر آج کی عیسوی اور ہجری تاریخ، روزانہ ایک غور و فکر اور آنے والی مناسبتیں نظر آتی ہیں۔"),
    "tour2_t" to s("Two calendars, one view", "تقويمان في عرض واحد", "دو کیلنڈر، ایک نظر"),
    "tour2_d" to s("Switch between Gregorian and Hijri, filter occasions by category and tap any day for details.",
        "بدّل بين الميلادي والهجري، وصنّف المناسبات، واضغط على أي يوم للتفاصيل.",
        "عیسوی اور ہجری کے درمیان بدلیں، مناسبتوں کو زمرے کے مطابق چھانٹیں اور تفصیل کے لیے کسی بھی دن پر ٹیپ کریں۔"),
    "tour3_t" to s("Daily wisdom", "الحكمة اليومية", "روزانہ حکمت"),
    "tour3_d" to s("Browse, search and save short lessons. Saved items are kept on your device.",
        "تصفّح الدروس القصيرة وابحث فيها واحفظها. العناصر المحفوظة تبقى على جهازك.",
        "مختصر اسباق دیکھیں، تلاش کریں اور محفوظ کریں۔ محفوظ کردہ چیزیں آپ کے آلے پر رہتی ہیں۔"),
    "tour4_t" to s("Make it yours", "اجعله لك", "اپنی مرضی کے مطابق بنائیں"),
    "tour4_d" to s("Choose a language, a light or dark theme and a daily reminder, and shift Hijri dates by up to 2 days.",
        "اختر اللغة والمظهر الفاتح أو الداكن وتذكيرًا يوميًا، وقدّم التواريخ الهجرية حتى يومين.",
        "زبان، روشن یا گہری تھیم اور روزانہ یاد دہانی منتخب کریں، اور ہجری تاریخیں 2 دن تک آگے پیچھے کریں۔"),
)

fun tr(lang: String, k: String): String = T[k]?.get(idx[lang] ?: 0) ?: k
@Composable fun t(k: String): String = tr(LocalLang.current, k)
