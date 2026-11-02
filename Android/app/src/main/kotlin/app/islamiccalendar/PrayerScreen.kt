package app.islamiccalendar
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun fmtTime(t: LocalDateTime?, loc: Locale): String =
    t?.toLocalTime()?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(loc)) ?: "—"

private fun countdown(from: LocalDateTime, to: LocalDateTime): String {
    val s = Duration.between(from, to).seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

/** Ticks once a second so countdowns stay live. */
@Composable fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) { value = LocalDateTime.now(); delay(1000) }
}

/** Next-prayer countdown. Renders nothing until a location is set. */
@Composable fun NextPrayerCard(st: AppState) {
    val cfg = st.prayerCfg ?: return
    val lang = st.lang.value; val loc = localeOf(lang)
    val now by rememberNow()
    val minute = now.withSecond(0).withNano(0)
    val next = remember(cfg, minute) { cfg.next(minute) } ?: return
    GlassCard(hero = true) {
        Text(t("next_prayer").uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(prayerName(lang, next.first, true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(countdown(now, next.second), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(fmtTime(next.second, loc) + " · " + next.second.toLocalDate().format(DateTimeFormatter.ofPattern("EEE d MMM", loc)),
            style = MaterialTheme.typography.bodyMedium)
    }
}

/** One row per prayer for [date]. [highlight] marks the upcoming one. */
@Composable fun PrayerRows(cfg: PrayerConfig, date: LocalDate, lang: String, highlight: Prayer? = null) {
    val loc = localeOf(lang)
    val times = remember(cfg, date) { cfg.times(date) }
    Prayer.entries.forEach { p ->
        val on = p == highlight
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(prayerName(lang, p), fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
            Text(fmtTime(times[p], loc), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestLocation(ctx: Context, onDone: (Location?) -> Unit) {
    val lm = ctx.getSystemService(LocationManager::class.java)
    val enabled = lm.getProviders(true)
    // Coarse permission rejects GPS with a SecurityException, which is caught per call below.
    val usable = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER).filter { it in enabled }
    for (pr in usable) {
        val last = try { lm.getLastKnownLocation(pr) } catch (e: SecurityException) { null }
        if (last != null) { onDone(last); return }
    }
    val pr = usable.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
    if (pr == null) { onDone(null); return }
    try {
        if (Build.VERSION.SDK_INT >= 30) lm.getCurrentLocation(pr, null, ctx.mainExecutor) { onDone(it) }
        else lm.requestSingleUpdate(pr, object : LocationListener {
            override fun onLocationChanged(l: Location) = onDone(l)
            @Deprecated("Required on API < 29") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }, Looper.getMainLooper())
    } catch (e: SecurityException) { onDone(null) }
}

/** Location is read once, kept on this device and rounded to ~1 km. It is never sent anywhere. */
@Composable fun LocationCard(st: AppState) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var manual by remember { mutableStateOf(false) }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    fun fetch() {
        busy = true; error = null
        requestLocation(ctx) { l ->
            busy = false
            if (l != null) { st.setLocation(l.latitude, l.longitude); manual = false } else error = "location_failed"
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) fetch() else error = "location_denied" }
    fun start() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetch()
        else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val has = st.prayerCfg != null
    GlassCard {
        if (has) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📍 ${st.lat.value}, ${st.lon.value}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton({ start() }, enabled = !busy) { Text(t("update_location")) }
            }
        } else {
            Text("📍 " + t("location_needed"), fontWeight = FontWeight.Bold)
            Text(t("location_privacy"), style = MaterialTheme.typography.bodySmall)
            Button({ start() }, enabled = !busy) { Text(if (busy) t("locating") else t("use_location")) }
        }
        error?.let { Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TextButton({ manual = !manual }) { Text(t("enter_manually")) }
        if (manual) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(latText, { latText = it }, Modifier.weight(1f), label = { Text(t("latitude")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(lonText, { lonText = it }, Modifier.weight(1f), label = { Text(t("longitude")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            val la = latText.toDoubleOrNull(); val lo = lonText.toDoubleOrNull()
            val valid = la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0
            Button({ st.setLocation(la!!, lo!!); manual = false }, enabled = valid) { Text(t("save")) }
        }
    }
}

@Composable fun PrayerScreen(st: AppState) {
    val lang = st.lang.value
    var sub by remember { mutableStateOf("times") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle(t("prayer"), t("prayer_sub"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemedChip(sub == "times", { sub = "times" }, { Text(t("prayer_times")) })
            ThemedChip(sub == "qibla", { sub = "qibla" }, { Text("🧭 " + t("qibla")) })
        }
        LocationCard(st)
        val cfg = st.prayerCfg
        if (cfg != null) {
            if (sub == "times") TimesSection(st, cfg, lang) else QiblaSection(cfg)
        }
    }
}

@Composable private fun TimesSection(st: AppState, cfg: PrayerConfig, lang: String) {
    val loc = localeOf(lang)
    var date by remember { mutableStateOf(LocalDate.now()) }
    val now by rememberNow()
    val minute = now.withSecond(0).withNano(0)
    val next = remember(cfg, minute) { cfg.next(minute) }
    val rtl = isRtl(lang)

    NextPrayerCard(st)

    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton({ date = date.minusDays(1) }) { Text(if (rtl) "›" else "‹", fontSize = 20.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", loc)), fontWeight = FontWeight.Bold)
                Text(Hijri.text(Hijri.of(date, st.adj.intValue), lang), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            TextButton({ date = date.plusDays(1) }) { Text(if (rtl) "‹" else "›", fontSize = 20.sp) }
        }
        if (date != LocalDate.now()) TextButton({ date = LocalDate.now() }, Modifier.align(Alignment.CenterHorizontally)) { Text("📍 " + t("today_btn")) }
        PrayerRows(cfg, date, lang, highlight = if (date == now.toLocalDate()) next?.first else null)
        Text(t("prayer_note"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }

    PrayerSettings(st, cfg, lang)
}

@Composable private fun PrayerSettings(st: AppState, cfg: PrayerConfig, lang: String) {
    val ctx = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) st.setPrayerAlerts(true) else denied = true
    }
    GlassCard {
        Text(t("prayer_settings"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t("prayer_alerts"), fontWeight = FontWeight.SemiBold)
                Text(t("prayer_alerts_help"), style = MaterialTheme.typography.bodySmall)
            }
            Switch(st.palerts.value, { on ->
                denied = false
                if (!on) st.setPrayerAlerts(false)
                else if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                    st.setPrayerAlerts(true)
                else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        if (denied) Text(t("perm_denied"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (st.palerts.value && !PrayerAlerts.canExact(ctx)) {
            Text(t("exact_alarm_help"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton({
                ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text(t("allow_exact")) }
        }

        Spacer(Modifier.height(4.dp))
        Text(t("calc_method"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CalcMethods) { m -> ThemedChip(cfg.methodId == m.id, { st.setMethod(m.id) }, { Text(t(m.nameKey)) }) }
        }

        Text(t("asr_for_alerts"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemedChip(!cfg.hanafi, { st.setHanafi(false) }, { Text(t("asr_standard")) })
            ThemedChip(cfg.hanafi, { st.setHanafi(true) }, { Text(t("asr_hanafi")) })
        }

        Text(t("adjust_minutes"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(prayerName(lang, p, true))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ st.setPrayerAdj(i, cfg.adj[i] - 1) }, enabled = cfg.adj[i] > -30) { Text("−", fontSize = 20.sp) }
                    Text(String.format(Locale.ROOT, "%+d", cfg.adj[i]), Modifier.width(40.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    TextButton({ st.setPrayerAdj(i, cfg.adj[i] + 1) }, enabled = cfg.adj[i] < 30) { Text("+", fontSize = 20.sp) }
                }
            }
        }
    }
}

/** Compass heading in degrees from TRUE north (magnetic heading + local declination), smoothed; null until the first reading. */
@Composable private fun rememberHeading(lat: Double, lon: Double): State<Float?> {
    val ctx = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(lat, lon) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val decl = GeomagneticField(lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis()).declination
        val r = FloatArray(9); val o = FloatArray(3)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                SensorManager.getOrientation(r, o)
                val az = ((Math.toDegrees(o[0].toDouble()) + decl + 360.0) % 360.0).toFloat()
                val prev = heading.value
                heading.value = if (prev == null) az else (prev + 0.2f * (((az - prev + 540f) % 360f) - 180f) + 360f) % 360f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return heading
}

@Composable private fun QiblaSection(cfg: PrayerConfig) {
    val ctx = LocalContext.current
    val bearing = remember(cfg.lat, cfg.lon) { Qibla.bearing(cfg.lat, cfg.lon) }
    val hasCompass = remember { ctx.getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null }
    val heading by rememberHeading(cfg.lat, cfg.lon)
    val h = heading
    // Signed shortest turn from where the phone points to the Qibla: positive = turn right.
    val diff = h?.let { ((bearing.toFloat() - it + 540f) % 360f) - 180f }
    val ring = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val prim = MaterialTheme.colorScheme.primary
    val north = MaterialTheme.colorScheme.error

    GlassCard {
        Text(t("qibla_direction"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text("${bearing.roundToInt()}°", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Box(Modifier.size(280.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().rotate(-(h ?: 0f))) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = center; val rad = size.minDimension / 2f - 2.dp.toPx()
                    drawCircle(ring, rad, c, style = Stroke(3.dp.toPx()))
                    for (i in 0 until 72) {
                        val a = Math.toRadians(i * 5.0); val long = i % 9 == 0
                        val len = if (long) 14.dp.toPx() else 7.dp.toPx()
                        drawLine(ring, Offset(c.x + (rad - len) * sin(a).toFloat(), c.y - (rad - len) * cos(a).toFloat()),
                            Offset(c.x + rad * sin(a).toFloat(), c.y - rad * cos(a).toFloat()), 2.dp.toPx())
                    }
                }
                Text("N", Modifier.align(Alignment.TopCenter).padding(top = 18.dp), fontWeight = FontWeight.Bold, color = north)
                Text("E", Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), fontWeight = FontWeight.Bold)
                Text("S", Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), fontWeight = FontWeight.Bold)
                Text("W", Modifier.align(Alignment.CenterStart).padding(start = 18.dp), fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxSize().rotate(bearing.toFloat())) {
                    Text("🕋", Modifier.align(Alignment.TopCenter).padding(top = 44.dp), fontSize = 30.sp)
                }
            }
            // Fixed marker: the direction the top of the phone points.
            Canvas(Modifier.fillMaxSize()) {
                val p = Path().apply {
                    moveTo(center.x - 12.dp.toPx(), 0f); lineTo(center.x + 12.dp.toPx(), 0f); lineTo(center.x, 22.dp.toPx()); close()
                }
                drawPath(p, prim)
            }
        }
        val aligned = diff != null && abs(diff) <= 3f
        Text(
            when {
                !hasCompass -> t("qibla_no_compass")
                diff == null -> t("qibla_calibrating")
                aligned -> "✓ " + t("qibla_aligned")
                diff > 0 -> t("qibla_turn_right").replace("{n}", "${abs(diff).roundToInt()}")
                else -> t("qibla_turn_left").replace("{n}", "${abs(diff).roundToInt()}")
            },
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
            color = if (aligned) prim else MaterialTheme.colorScheme.onSurface
        )
        Text(t("qibla_hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}
