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

val typeLabels = mapOf("quran" to "Qur'an passage", "hadith" to "Hadith",
    "moral_reflection" to "Moral reflection (not a religious text)", "illustrative_story" to "Illustrative story (fictional)")
/** Full label for a contentType (keeps disclaimers); unknown types fall back to a prettified id. */
fun typeFull(k: String) = typeLabels[k] ?: k.replace('_', ' ').replaceFirstChar { it.uppercase() }
/** Short label for chips/cards. */
fun typeName(k: String) = typeFull(k).substringBefore(" (")

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
    private val base = read("lessons").getJSONArray("lessons").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
        Lesson(o.getString("id"), o.getString("contentType"), o.getString("categoryId"), o.getString("title"), o.getString("takeaway"),
            o.getString("body"), o.getString("practicalAction"), o.optString("reflectionQuestion").ifEmpty { null }, o.getInt("readingTimeMinutes")) } }
    private val tr = read("translations").getJSONArray("translations").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
    fun lessons(lang: String): List<Lesson> = if (lang == "en") base else base.map { l ->
        tr.firstOrNull { it.getString("lessonId") == l.id && it.getString("languageCode") == lang }?.let {
            l.copy(title = it.getString("title"), takeaway = it.getString("takeaway"), body = it.getString("body"),
                action = it.getString("practicalAction"), question = it.optString("reflectionQuestion").ifEmpty { null },
                machineTranslated = it.getString("translationStatus") == "machine_translated") } ?: l }
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
