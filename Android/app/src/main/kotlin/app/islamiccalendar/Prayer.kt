package app.islamiccalendar
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Asr appears twice: standard (shadow = 1x) and Hanafi (shadow = 2x). */
enum class Prayer(val key: String) {
    FAJR("fajr"), SUNRISE("sunrise"), DHUHR("dhuhr"), ASR("asr"), ASR_HANAFI("asr2"), MAGHRIB("maghrib"), ISHA("isha")
}

fun prayerName(lang: String, p: Prayer, short: Boolean = false): String =
    tr(lang, if (short && (p == Prayer.ASR || p == Prayer.ASR_HANAFI)) "p_asr_n" else "p_${p.key}")

/** fajr/isha are sun-depression angles in degrees. Methods with ishaMinutes > 0 use a fixed offset after Maghrib. */
data class CalcMethod(val id: String, val nameKey: String, val fajr: Double, val isha: Double, val ishaMinutes: Int = 0)

val CalcMethods = listOf(
    CalcMethod("mwl", "m_mwl", 18.0, 17.0),
    CalcMethod("isna", "m_isna", 15.0, 15.0),
    CalcMethod("egypt", "m_egypt", 19.5, 17.5),
    CalcMethod("makkah", "m_makkah", 18.5, 0.0, ishaMinutes = 90),
    CalcMethod("karachi", "m_karachi", 18.0, 18.0)
)

/** Regional convention as a starting point only; the user can change it. */
fun defaultMethodId(): String = when (Locale.getDefault().country) {
    "SA" -> "makkah"
    "US", "CA" -> "isna"
    "EG" -> "egypt"
    "PK", "IN", "BD", "AF" -> "karachi"
    else -> "mwl"
}

/**
 * Offline astronomical prayer times (same model as PrayTimes.org). Pure Kotlin, no Android dependencies.
 * Times come back in [zone] wall-clock, so DST is handled per date. Polar days with no sunrise/sunset return nulls.
 */
object PrayerCalc {
    private const val RISE_SET = 0.833
    private fun rad(d: Double) = Math.toRadians(d)
    private fun fixAngle(a: Double) = ((a % 360.0) + 360.0) % 360.0
    private fun fixHour(h: Double) = ((h % 24.0) + 24.0) % 24.0

    fun times(date: LocalDate, lat: Double, lng: Double, m: CalcMethod, zone: ZoneId = ZoneId.systemDefault()): Map<Prayer, LocalDateTime?> {
        val jd0 = date.toEpochDay() + 2440587.5 - lng / 360.0
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0

        /** Sun declination (deg) and equation of time (h) at [hours] into the day. */
        fun sun(hours: Double): Pair<Double, Double> {
            val d = jd0 + hours / 24.0 - 2451545.0
            val g = rad(fixAngle(357.529 + 0.98560028 * d))
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = rad(fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))
            val e = rad(23.439 - 0.00000036 * d)
            val ra = fixHour(Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0)
            val decl = Math.toDegrees(asin(sin(e) * sin(l)))
            val eqt = ((q / 15.0 - ra + 12.0) % 24.0 + 24.0) % 24.0 - 12.0
            return decl to eqt
        }
        fun noon(hours: Double) = fixHour(12.0 - sun(hours).second)
        fun angleTime(angle: Double, hours: Double, ccw: Boolean): Double {
            val dec = rad(sun(hours).first); val la = rad(lat)
            val x = (-sin(rad(angle)) - sin(dec) * sin(la)) / (cos(dec) * cos(la))
            if (x < -1.0 || x > 1.0) return Double.NaN
            val dt = Math.toDegrees(acos(x)) / 15.0
            return if (ccw) noon(hours) - dt else noon(hours) + dt
        }
        fun asr(factor: Double, hours: Double): Double {
            val dec = sun(hours).first
            val a = -Math.toDegrees(atan(1.0 / (factor + tan(rad(abs(lat - dec))))))
            return angleTime(a, hours, false)
        }

        // fajr, sunrise, dhuhr, asr, asr2, maghrib(sunset), isha: refine the sun position at each prayer's own time.
        var tm = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 13.0, 18.0, 18.0)
        var r = tm
        repeat(3) {
            r = doubleArrayOf(
                angleTime(m.fajr, tm[0], true),
                angleTime(RISE_SET, tm[1], true),
                noon(tm[2]),
                asr(1.0, tm[3]),
                asr(2.0, tm[4]),
                angleTime(RISE_SET, tm[5], false),
                if (m.ishaMinutes > 0) Double.NaN else angleTime(m.isha, tm[6], false)
            )
            val prev = tm
            tm = DoubleArray(7) { if (r[it].isNaN()) prev[it] else r[it] }
        }

        val sunrise = r[1]; val sunset = r[5]
        if (sunrise.isNaN() || sunset.isNaN()) return Prayer.entries.associateWith { null }
        // High latitudes where the twilight angle is never reached: use the angle-based night portion.
        val night = fixHour(sunrise - sunset)
        val fajr = if (r[0].isNaN()) sunrise - night * m.fajr / 60.0 else r[0]
        val ramadan = try { Hijri.month(Hijri.of(date, 0)) == 9 } catch (e: Exception) { false }
        val isha = when {
            m.ishaMinutes > 0 -> sunset + (m.ishaMinutes + if (ramadan) 30 else 0) / 60.0
            r[6].isNaN() -> sunset + night * m.isha / 60.0
            else -> r[6]
        }

        val shift = tz - lng / 15.0
        val base = date.atStartOfDay()
        fun at(h: Double): LocalDateTime? = if (h.isNaN()) null else base.plusMinutes(Math.round((h + shift) * 60.0))
        return mapOf(
            Prayer.FAJR to at(fajr), Prayer.SUNRISE to at(sunrise), Prayer.DHUHR to at(r[2]),
            Prayer.ASR to at(r[3]), Prayer.ASR_HANAFI to at(r[4]), Prayer.MAGHRIB to at(sunset), Prayer.ISHA to at(isha)
        )
    }
}

object Qibla {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    /** Initial great-circle bearing to the Kaaba in degrees clockwise from true north. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT); val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}

/** Everything needed to compute times. Built from preferences so alarms work without the UI. */
data class PrayerConfig(val lat: Double, val lon: Double, val methodId: String, val hanafi: Boolean, val adj: List<Int>) {
    private val method get() = CalcMethods.firstOrNull { it.id == methodId } ?: CalcMethods[0]
    private fun adjFor(p: Prayer) = adj[when (p) {
        Prayer.FAJR -> 0; Prayer.SUNRISE -> 1; Prayer.DHUHR -> 2; Prayer.ASR, Prayer.ASR_HANAFI -> 3; Prayer.MAGHRIB -> 4; Prayer.ISHA -> 5
    }]

    /** Calculated times plus the user's per-prayer minute adjustments. */
    fun times(date: LocalDate): Map<Prayer, LocalDateTime?> =
        PrayerCalc.times(date, lat, lon, method).mapValues { (p, v) -> v?.plusMinutes(adjFor(p).toLong()) }

    private val asrPrayer get() = if (hanafi) Prayer.ASR_HANAFI else Prayer.ASR
    /** Obligatory prayers in order; the ones used for the countdown and alerts. */
    private val alertPrayers get() = listOf(Prayer.FAJR, Prayer.DHUHR, asrPrayer, Prayer.MAGHRIB, Prayer.ISHA)

    fun next(now: LocalDateTime): Pair<Prayer, LocalDateTime>? {
        for (d in 0L..1L) {
            val t = times(now.toLocalDate().plusDays(d))
            alertPrayers.mapNotNull { p -> t[p]?.let { p to it } }.firstOrNull { it.second.isAfter(now) }?.let { return it }
        }
        return null
    }

    companion object {
        fun parseAdj(s: String?): List<Int> =
            (s ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }.let { if (it.size == 6) it else List(6) { 0 } }

        fun load(p: SharedPreferences): PrayerConfig? {
            val la = p.getString("lat", null)?.toDoubleOrNull() ?: return null
            val lo = p.getString("lon", null)?.toDoubleOrNull() ?: return null
            return PrayerConfig(la, lo, p.getString("pmethod", null) ?: defaultMethodId(), p.getBoolean("hanafi", false), parseAdj(p.getString("padj", null)))
        }
    }
}

/** Schedules one exact alarm for the next prayer; the receiver re-arms the following one. */
object PrayerAlerts {
    private const val CHANNEL = "prayer_alerts"
    private const val REQ = 2001
    private const val NOTIF_BASE = 2000
    /** A notification that arrives this much later than its prayer time is dropped rather than shown misleadingly. */
    const val MAX_LATE_MS = 30 * 60 * 1000L

    private fun pending(ctx: Context, key: String?, at: Long) = PendingIntent.getBroadcast(ctx, REQ,
        Intent(ctx, PrayerReceiver::class.java).putExtra("prayer", key).putExtra("at", at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Exact alarms need the "Alarms & reminders" special access on Android 12+. Without it we fall back to inexact timing. */
    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(ctx: Context, after: LocalDateTime = LocalDateTime.now()) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val p = ctx.getSharedPreferences("s", 0)
        val next = if (p.getBoolean("palerts", false)) PrayerConfig.load(p)?.next(after) else null
        if (next == null) { am.cancel(pending(ctx, null, 0)); return }
        val ms = next.second.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pending(ctx, next.first.key, ms)
        try {
            if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    fun show(ctx: Context, key: String, at: Long) {
        val prayer = Prayer.entries.firstOrNull { it.key == key } ?: return
        val p = ctx.getSharedPreferences("s", 0)
        val lang = p.getString("lang", "en")!!
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, tr(lang, "prayer_alerts"), NotificationManager.IMPORTANCE_HIGH)
            .apply { description = tr(lang, "prayer_alerts_desc") })
        val time = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(localeOf(lang)))
        val hijri = Hijri.text(Hijri.of(LocalDate.now(), p.getInt("adj", 0)), lang)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif_moon)
            .setColor(Color.parseColor("#5563E8"))
            .setContentTitle(tr(lang, "prayer_time_for").replace("{p}", prayerName(lang, prayer, true)))
            .setContentText("$time · $hijri")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_BASE + prayer.ordinal, n)
    }
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val key = i.getStringExtra("prayer")
        val at = i.getLongExtra("at", 0L)
        if (key != null && System.currentTimeMillis() - at <= PrayerAlerts.MAX_LATE_MS) PrayerAlerts.show(ctx, key, at)
        // Look past this prayer so a marginally early fire can never re-arm the same one.
        PrayerAlerts.schedule(ctx, LocalDateTime.now().plusSeconds(30))
    }
}
