package app.islamiccalendar
import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate

data class Event(val id: String, val names: Map<String, String>, val category: String, val month: Int, val day: Int,
                 val description: String, val tags: List<String>) { fun name(l: String) = names[l] ?: names["en"] ?: id }
data class Lesson(val id: String, val type: String, val category: String, val title: String, val takeaway: String, val body: String,
                  val action: String, val question: String?, val minutes: Int, val machineTranslated: Boolean = false)

data class DuaChapter(val id: String, val icon: String, val names: Map<String, String>) { fun name(l: String) = names[l] ?: names["en"] ?: id }
data class Dua(val id: String, val chapter: String, val titles: Map<String, String>, val arabic: String, val translit: String,
               val meaning: String, val virtue: String?, val source: String) { fun title(l: String) = titles[l] ?: titles["en"] ?: id }
data class SurahInfo(val n: Int, val ar: String, val en: String, val meaning: String, val ayahs: Int, val meccan: Boolean)
class SurahText(val arabic: List<String>, val english: List<String>, val translit: List<String>)

/** Fixed dhikr set for the Tasbih counter: Arabic, transliteration, meaning and default target. */
data class Dhikr(val arabic: String, val translit: String, val meaning: String, val target: Int)
val DhikrList = listOf(
    Dhikr("سُبْحَانَ اللَّهِ", "SubhanAllah", "Glory be to Allah", 33),
    Dhikr("الْحَمْدُ لِلَّهِ", "Alhamdulillah", "All praise is for Allah", 33),
    Dhikr("اللَّهُ أَكْبَرُ", "Allahu Akbar", "Allah is The Greatest", 33),
    Dhikr("لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "There is no god but Allah", 100),
    Dhikr("أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah", "I seek forgiveness from Allah", 100),
    Dhikr("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "Allahumma salli 'ala Muhammad", "O Allah, send blessings upon Muhammad", 100)
)

fun typeFull(k: String, lang: String = "en"): String = when (k) {
    "quran" -> when (lang) { "ar" -> "آية قرآنية"; "ur" -> "قرآنی آیت"; else -> "Qur'an passage" }
    "hadith" -> when (lang) { "ar" -> "حديث شريف"; "ur" -> "حدیث نبوی"; else -> "Hadith" }
    "moral_reflection" -> when (lang) { "ar" -> "تأمل أخلاقي (ليس نصاً دينياً)"; "ur" -> "اخلاقی غور و فکر (غیر مذہبی متن)"; else -> "Moral reflection (not a religious text)" }
    "illustrative_story" -> when (lang) { "ar" -> "قصة للعبرة (تمثيلية)"; "ur" -> "سبق آموز کہانی (تمثیلی)"; else -> "Illustrative story (fictional)" }
    else -> k.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
fun typeName(k: String, lang: String = "en"): String = when (k) {
    "quran" -> when (lang) { "ar" -> "القرآن"; "ur" -> "قرآن"; else -> "Qur'an" }
    "hadith" -> when (lang) { "ar" -> "حديث"; "ur" -> "حدیث"; else -> "Hadith" }
    "moral_reflection" -> when (lang) { "ar" -> "تأمل أخلاقي"; "ur" -> "اخلاقی فکر"; else -> "Moral reflection" }
    "illustrative_story" -> when (lang) { "ar" -> "قصة للعبرة"; "ur" -> "سبق آموز کہانی"; else -> "Illustrative story" }
    else -> typeFull(k, lang).substringBefore(" (")
}

val hijriMonths = listOf("Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani", "Jumada al-awwal", "Jumada al-thani",
    "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah")

/** Umm al-Qura via java.time. Other conventions can be added behind the same functions. */
object Hijri {
    fun of(d: LocalDate, adj: Int): HijrahDate = HijrahDate.from(d.plusDays(adj.toLong()))
    fun toGreg(y: Int, m: Int, day: Int, adj: Int): LocalDate =
        LocalDate.from(HijrahChronology.INSTANCE.date(y, m, day)).minusDays(adj.toLong())
    fun text(h: HijrahDate, lang: String = "en") = "${h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)} " +
        "${hijriMonth(h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR), lang)} ${h.get(java.time.temporal.ChronoField.YEAR)} ${ahSuffix(lang)}"
    fun day(h: HijrahDate) = h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
    fun month(h: HijrahDate) = h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
    fun year(h: HijrahDate) = h.get(java.time.temporal.ChronoField.YEAR)
}

class Content(private val ctx: Context) {
    private fun read(n: String) = JSONObject(ctx.assets.open("$n.json").bufferedReader().use { it.readText() })
    private fun JSONObject.map(k: String) = getJSONObject(k).let { o -> o.keys().asSequence().associateWith { o.getString(it) } }
    val events = read("events").getJSONArray("events").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
        val r = o.getJSONObject("hijriDateRule")
        Event(o.getString("id"), o.map("names"), o.getString("category"), r.getInt("month"), r.getInt("day"),
            o.map("description")["en"] ?: "", o.getJSONArray("traditionTags").let { t -> (0 until t.length()).map(t::getString) }) } }
    private fun parseLessons(n: String) = runCatching {
        read(n).getJSONArray("lessons").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            Lesson(o.getString("id"), o.getString("contentType"), o.getString("categoryId"), o.getString("title"), o.getString("takeaway"),
                o.getString("body"), o.getString("practicalAction"), o.optString("reflectionQuestion").ifEmpty { null }, o.getInt("readingTimeMinutes")) } }
    }.getOrNull()
    val duaChapters: List<DuaChapter> = runCatching {
        read("duas").getJSONArray("chapters").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            DuaChapter(o.getString("id"), o.getString("icon"), o.map("names")) } }
    }.getOrDefault(emptyList())
    val duas: List<Dua> = runCatching {
        read("duas").getJSONArray("duas").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
            Dua(o.getString("id"), o.getString("chapter"), o.map("titles"), o.getString("arabic"), o.getString("translit"),
                o.getString("meaning"), o.optString("virtue").ifEmpty { null }, o.getString("source")) } }
    }.getOrDefault(emptyList())

    /** Quran data is bundled under assets/quran (no network). The index is small; each surah is read on demand. */
    val surahs: List<SurahInfo> = runCatching {
        ctx.assets.open("quran/index.json").bufferedReader().use { JSONObject(it.readText()) }.getJSONArray("surahs").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                SurahInfo(o.getInt("n"), o.getString("ar"), o.getString("en"), o.getString("m"), o.getInt("c"), o.getString("r") == "M") } }
    }.getOrDefault(emptyList())
    private val surahCache = mutableMapOf<Int, SurahText?>()
    fun surah(n: Int): SurahText? = surahCache.getOrPut(n) {
        runCatching {
            val o = ctx.assets.open("quran/s%03d.json".format(n)).bufferedReader().use { JSONObject(it.readText()) }
            fun list(k: String) = o.getJSONArray(k).let { a -> (0 until a.length()).map(a::getString) }
            SurahText(list("a"), list("e"), list("t"))
        }.getOrNull()
    }

    private val base = parseLessons("lessons_en") ?: emptyList()
    private val cache = mutableMapOf<String, List<Lesson>>()
    fun lessons(lang: String): List<Lesson> = cache.getOrPut(lang) {
        if (lang == "en") base else (parseLessons("lessons_$lang") ?: base)
    }
}

class AppState(ctx: Context) {
    private val app = ctx.applicationContext
    private val p = ctx.getSharedPreferences("s", 0)
    // Prayer times: location is stored rounded to ~1 km, only on this device, and never transmitted.
    val lat = mutableStateOf(p.getString("lat", null))
    val lon = mutableStateOf(p.getString("lon", null))
    val pmethod = mutableStateOf(p.getString("pmethod", null) ?: defaultMethodId())
    val hanafi = mutableStateOf(p.getBoolean("hanafi", false))
    val padj = mutableStateOf(PrayerConfig.parseAdj(p.getString("padj", null)))
    val palerts = mutableStateOf(p.getBoolean("palerts", false))
    val prayerCfg: PrayerConfig? get() {
        val la = lat.value?.toDoubleOrNull() ?: return null
        val lo = lon.value?.toDoubleOrNull() ?: return null
        return PrayerConfig(la, lo, pmethod.value, hanafi.value, padj.value)
    }
    fun setLocation(la: Double, lo: Double) {
        lat.value = (Math.round(la * 100) / 100.0).toString(); lon.value = (Math.round(lo * 100) / 100.0).toString()
        p.edit().putString("lat", lat.value).putString("lon", lon.value).apply(); PrayerAlerts.schedule(app)
    }
    fun setMethod(v: String) { pmethod.value = v; p.edit().putString("pmethod", v).apply(); PrayerAlerts.schedule(app) }
    fun setHanafi(v: Boolean) { hanafi.value = v; p.edit().putBoolean("hanafi", v).apply(); PrayerAlerts.schedule(app) }
    fun setPrayerAdj(i: Int, v: Int) {
        padj.value = padj.value.toMutableList().also { it[i] = v.coerceIn(-30, 30) }
        p.edit().putString("padj", padj.value.joinToString(",")).apply(); PrayerAlerts.schedule(app)
    }
    fun setPrayerAlerts(v: Boolean) { palerts.value = v; p.edit().putBoolean("palerts", v).apply(); PrayerAlerts.schedule(app) }
    val adj = mutableIntStateOf(p.getInt("adj", 0))
    val lang = mutableStateOf(p.getString("lang", "en")!!)
    val theme = mutableStateOf(p.getString("theme", "system")!!)
    val palette = mutableStateOf(p.getString("palette", "classic")!!)
    val remind = mutableStateOf(p.getBoolean("remind", false))
    val hour = mutableIntStateOf(p.getInt("hour", 8))
    val minute = mutableIntStateOf(p.getInt("minute", 0))
    fun setTheme(v: String) { theme.value = v; p.edit().putString("theme", v).apply() }
    fun setPalette(v: String) { palette.value = v; p.edit().putString("palette", v).apply() }
    fun setRemind(v: Boolean) { remind.value = v; p.edit().putBoolean("remind", v).apply() }
    fun setTime(h: Int, m: Int) { hour.intValue = h; minute.intValue = m; p.edit().putInt("hour", h).putInt("minute", m).apply() }
    // Daily prayer tracker: keys like "2025-08-25:fajr". Only on this device.
    val prayed = mutableStateListOf<String>().apply { addAll(p.getStringSet("prayed", emptySet())!!) }
    fun prayedOn(d: LocalDate): Set<String> = prayed.filter { it.startsWith("$d:") }.map { it.substringAfter(':') }.toSet()
    private fun savePrayed() { p.edit().putStringSet("prayed", prayed.toSet()).apply() }
    fun togglePrayed(d: LocalDate, key: String) { val k = "$d:$key"; if (!prayed.remove(k)) prayed.add(k); savePrayed() }
    fun markAllPrayed(d: LocalDate, keys: List<String>) { keys.forEach { k -> "$d:$k".let { if (it !in prayed) prayed.add(it) } }; savePrayed() }
    val qiblaSkin = mutableStateOf(p.getString("qibla_skin", "green")!!)
    fun setQiblaSkin(v: String) { qiblaSkin.value = v; p.edit().putString("qibla_skin", v).apply() }

    // Quran: last read position, bookmarks ("surah:ayah"), reader theme, and minutes read today (goal tracked per day).
    val qSurah = mutableIntStateOf(p.getInt("q_surah", 1))
    val qAyah = mutableIntStateOf(p.getInt("q_ayah", 1))
    val qHasRead = mutableStateOf(p.getBoolean("q_has_read", false))
    val qTheme = mutableStateOf(p.getString("q_theme", "modern")!!)
    val qBookmarks = mutableStateListOf<String>().apply { addAll(p.getStringSet("q_bm", emptySet())!!) }
    val qRecents = mutableStateListOf<String>().apply { addAll((p.getString("q_recent", "") ?: "").split(",").filter { it.isNotBlank() }) }
    val qGoalMin = mutableIntStateOf(p.getInt("q_goal", 5))
    val qReadSecs = mutableIntStateOf(if (p.getString("q_read_day", "") == LocalDate.now().toString()) p.getInt("q_read_secs", 0) else 0)
    fun setQTheme(v: String) { qTheme.value = v; p.edit().putString("q_theme", v).apply() }
    fun setQGoal(v: Int) { qGoalMin.intValue = v.coerceIn(1, 120); p.edit().putInt("q_goal", qGoalMin.intValue).apply() }
    fun saveQPosition(s: Int, a: Int) {
        qSurah.intValue = s; qAyah.intValue = a; qHasRead.value = true
        qRecents.remove("$s:$a"); qRecents.add(0, "$s:$a"); while (qRecents.size > 8) qRecents.removeAt(qRecents.size - 1)
        p.edit().putInt("q_surah", s).putInt("q_ayah", a).putBoolean("q_has_read", true).putString("q_recent", qRecents.joinToString(",")).apply()
    }
    fun toggleQBookmark(s: Int, a: Int) { val k = "$s:$a"; if (!qBookmarks.remove(k)) qBookmarks.add(k); p.edit().putStringSet("q_bm", qBookmarks.toSet()).apply() }
    fun addReadSeconds(n: Int) {
        qReadSecs.intValue += n
        p.edit().putString("q_read_day", LocalDate.now().toString()).putInt("q_read_secs", qReadSecs.intValue).apply()
    }

    // Duas and Tasbih.
    val duaSaved = mutableStateListOf<String>().apply { addAll(p.getStringSet("dua_saved", emptySet())!!) }
    val duaRecents = mutableStateListOf<String>().apply { addAll((p.getString("dua_recent", "") ?: "").split(",").filter { it.isNotBlank() }) }
    fun toggleDuaSaved(id: String) { if (!duaSaved.remove(id)) duaSaved.add(id); p.edit().putStringSet("dua_saved", duaSaved.toSet()).apply() }
    fun touchDua(id: String) {
        duaRecents.remove(id); duaRecents.add(0, id); while (duaRecents.size > 8) duaRecents.removeAt(duaRecents.size - 1)
        p.edit().putString("dua_recent", duaRecents.joinToString(",")).apply()
    }
    val tasbihCount = mutableIntStateOf(p.getInt("tasbih_count", 0))
    val tasbihIdx = mutableIntStateOf(p.getInt("tasbih_idx", 0))
    val tasbihTarget = mutableIntStateOf(p.getInt("tasbih_target", 33))
    fun setTasbih(count: Int) { tasbihCount.intValue = count; p.edit().putInt("tasbih_count", count).apply() }
    fun setTasbihDhikr(i: Int) { tasbihIdx.intValue = i; tasbihCount.intValue = 0; p.edit().putInt("tasbih_idx", i).putInt("tasbih_count", 0).apply() }
    fun setTasbihTarget(v: Int) { tasbihTarget.intValue = v; p.edit().putInt("tasbih_target", v).apply() }

    val onboarded = mutableStateOf(p.getBoolean("onboarded", false))
    fun finishOnboarding() { onboarded.value = true; p.edit().putBoolean("onboarded", true).apply() }
    val saved = mutableStateListOf<String>().apply { addAll(p.getStringSet("saved", emptySet())!!) }
    fun setAdj(v: Int) { adj.intValue = v.coerceIn(-2, 2); p.edit().putInt("adj", adj.intValue).apply() }
    fun setLang(v: String) { lang.value = v; p.edit().putString("lang", v).apply() }
    fun toggleSaved(id: String) { if (!saved.remove(id)) saved.add(id); p.edit().putStringSet("saved", saved.toSet()).apply() }
}

fun upcoming(ev: List<Event>, today: LocalDate, adj: Int, n: Int = 3): List<Pair<Event, LocalDate>> {
    val y = Hijri.year(Hijri.of(today, adj))
    return (y..y + 1).flatMap { yy -> ev.map { it to Hijri.toGreg(yy, it.month, it.day, adj) } }
        .filter { !it.second.isBefore(today) }.sortedBy { it.second }.take(n)
}
fun eventsOn(ev: List<Event>, d: LocalDate, adj: Int): List<Event> =
    Hijri.of(d, adj).let { h -> ev.filter { it.month == Hijri.month(h) && it.day == Hijri.day(h) } }
