package app.islamiccalendar
import android.Manifest
import android.app.TimePickerDialog
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val content = Content(this); val st = AppState(this)
        setContent {
            val dark = when (st.theme.value) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            val lang = st.lang.value
            // A premium palette saved earlier falls back to Classic once Premium is no longer active.
            val savedPalette = PalettesMap[st.palette.value] ?: Palettes[0]
            val curPalette = if (savedPalette.id in FreePaletteIds || Billing.isPremium) savedPalette else Palettes[0]
            CompositionLocalProvider(
                LocalLang provides lang,
                LocalDark provides dark,
                LocalPalette provides curPalette,
                LocalLayoutDirection provides if (isRtl(lang)) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                AppTheme(dark, curPalette) { if (st.onboarded.value) Root(content, st) else Onboarding(st); PaywallDialog(); UpdateReadyDialog() }
            }
        }
        Billing.init(this)
        Ads.init(this)
        AppUpdate.init(this)
    }

    override fun onResume() {
        super.onResume()
        // Picks up cancellation, renewal, refund or revoke done in Google Play while the app was in the background.
        Billing.refresh()
        AppUpdate.check(this)
    }

    override fun onDestroy() {
        AppUpdate.release()
        super.onDestroy()
    }
}

@Composable fun GlassCard(hero: Boolean = false, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val m = modifier.fillMaxWidth().clip(shape)
    val heroBg = heroBrush
    val accentColor = Accent
    val cs = MaterialTheme.colorScheme
    val dark = LocalDark.current
    // Cards are frosted glass: a soft top-to-bottom surface gradient with a bright-to-faint gradient rim.
    val glass = Brush.verticalGradient(listOf(cs.surface.copy(alpha = if (dark) 0.92f else 0.96f), cs.surface.copy(alpha = if (dark) 0.74f else 0.80f)))
    val rim = Brush.linearGradient(listOf(cs.primary.copy(alpha = if (dark) 0.40f else 0.30f), cs.secondary.copy(alpha = 0.06f)))
    Column(if (hero) m.background(heroBg).drawBehind {
                drawCircle(Color.White.copy(alpha = 0.12f), size.height * 0.9f, Offset(size.width, 0f))
                drawCircle(accentColor.copy(alpha = 0.24f), size.height * 0.55f, Offset(0f, size.height))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), endY = size.height * 0.5f)) }
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), shape).padding(22.dp)
           else m.background(glass).border(BorderStroke(1.dp, rim), shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompositionLocalProvider(LocalContentColor provides if (hero) Color.White else MaterialTheme.colorScheme.onSurface) { content() }
    }
}

@Composable fun SectionLabel(t: String) =
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.tertiary)

@Composable fun PageTitle(t: String, sub: String) = Column(Modifier.padding(bottom = 4.dp)) {
    Text(t, style = MaterialTheme.typography.headlineMedium.copy(brush = accentBrush), fontWeight = FontWeight.ExtraBold)
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }

/** Palette-themed filter chip with high-contrast text in both selected and unselected states. */
@Composable fun ThemedChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FilterChip(
        selected = selected, onClick = onClick, label = label,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = cs.surface.copy(alpha = 0.7f),
            labelColor = cs.onSurface,
            selectedContainerColor = cs.primary,
            selectedLabelColor = cs.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = cs.onSurface.copy(alpha = 0.35f),
            selectedBorderColor = cs.primary
        )
    )
}

@Composable fun VerticalListIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val h = 2.dp.toPx()
        val w = size.width
        val barH = size.height
        val gap = (barH - 3 * h) / 4f
        for (i in 0..2) {
            val y = gap + i * (h + gap)
            drawRoundRect(
                color = tint,
                topLeft = Offset(1.dp.toPx(), y),
                size = androidx.compose.ui.geometry.Size(w - 2.dp.toPx(), h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable fun HorizontalCardsIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cardWidth = w * 0.54f
        val cardHeight = h * 0.78f
        val top = (h - cardHeight) / 2f
        val left = (w - cardWidth) / 2f
        val corner = 2.dp.toPx()

        // Left peek card
        if (left > 2.dp.toPx()) {
            drawRoundRect(
                color = tint.copy(alpha = 0.4f),
                topLeft = Offset(0f, top + 1.5.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(left - 2.dp.toPx(), cardHeight - 3.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )
        }

        // Center card
        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
        )

        // Right peek card
        val rightStart = left + cardWidth + 2.dp.toPx()
        if (w > rightStart) {
            drawRoundRect(
                color = tint.copy(alpha = 0.4f),
                topLeft = Offset(rightStart, top + 1.5.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(w - rightStart, cardHeight - 3.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
            )
        }
    }
}

@Composable fun Root(c: Content, st: AppState) {
    var tab by remember { mutableIntStateOf(0) }
    // Existing pages keep their place; Quran and Dhikr are new pages added before Settings.
    val items = listOf("home" to "🌙", "calendar" to "📅", "prayer" to "🕌", "quran" to "📜", "dhikr" to "📿", "wisdom" to "📖", "settings" to "⚙️")
    Scaffold(containerColor = Color.Transparent, modifier = Modifier.appBackground(),
        bottomBar = { Column { AdSlot()
            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent))))
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                windowInsets = WindowInsets.navigationBars.add(WindowInsets(left = 12.dp, right = 12.dp))) {
            items.forEachIndexed { i, (k, e) ->
                val selected = tab == i
                val source = remember { MutableInteractionSource() }
                Column(Modifier.weight(1f).height(80.dp)
                    .clickable(interactionSource = source, indication = null, role = Role.Tab) { tab = i },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    // Ripple and selection highlight are both clipped to a circle.
                    Box(Modifier.size(40.dp).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent)
                        .indication(source, LocalIndication.current),
                        contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                    Text(t(k), fontSize = 9.sp, maxLines = 1, softWrap = false,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            } } } }) { pad ->
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Box(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp)) {
                when (tab) { 0 -> Home(c, st); 1 -> CalendarScreen(c, st); 2 -> PrayerScreen(st); 3 -> QuranScreen(c, st)
                    4 -> DhikrScreen(c, st); 5 -> Wisdom(c, st); else -> Settings(c, st) }
            }
        }
    }
}

@Composable fun Home(c: Content, st: AppState) {
    val lang = st.lang.value; val loc = localeOf(lang)
    val today = LocalDate.now(); val h = Hijri.of(today, st.adj.intValue); val l = lessonOfDay(c.lessons(lang), today)
    var open by remember { mutableStateOf<Lesson?>(null) }
    open?.let { LessonDialog(it, st) { open = null } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle(t("greeting"), t("greeting_sub"))
        GlassCard(hero = true) {
            Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", loc)), style = MaterialTheme.typography.titleMedium)
            Text(Hijri.text(h, lang), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(t("calc_note"), style = MaterialTheme.typography.bodySmall)
        }
        NextPrayerCard(st)
        GlassCard(modifier = Modifier.clickable { open = l }) {
            SectionLabel("✨ " + t("todays_wisdom"))
            Text(l.takeaway, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(t("practice_today") + l.action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            Text(t("tap_more"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        GlassCard {
            SectionLabel(t("upcoming"))
            upcoming(c.events, today, st.adj.intValue).forEach { (e, d) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(e.name(lang), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(d.format(DateTimeFormatter.ofPattern("d MMM yyyy", loc)) + " · " + t("est"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }
            }
        }
    }
}

private val MIN_GREG_DATE = LocalDate.of(1900, 1, 1)
private val MAX_GREG_DATE = LocalDate.of(2100, 12, 31)

private fun clampDate(d: LocalDate): LocalDate = when {
    d.isBefore(MIN_GREG_DATE) -> MIN_GREG_DATE
    d.isAfter(MAX_GREG_DATE) -> MAX_GREG_DATE
    else -> d
}

@Composable fun CalendarScreen(c: Content, st: AppState) {
    val lang = st.lang.value; val loc = localeOf(lang)
    var anchor by remember { mutableStateOf(clampDate(LocalDate.now())) }
    var hijriMode by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    var viewMode by remember { mutableStateOf("days") } // "days", "months", "years"
    val adj = st.adj.intValue
    val rtl = isRtl(lang)

    val currentGregYear = anchor.year.coerceIn(1900, 2100)
    val currentGregMonth = anchor.monthValue
    val hDate = Hijri.of(anchor, adj)
    val currentHijriYear = Hijri.year(hDate).coerceIn(1318, 1524)
    val currentHijriMonth = Hijri.month(hDate).coerceIn(1, 12)

    val activeYear = if (hijriMode) currentHijriYear else currentGregYear
    val minYear = if (hijriMode) 1318 else 1900
    val maxYear = if (hijriMode) 1524 else 2100

    var yearPageStart by remember(activeYear, minYear) {
        mutableIntStateOf(((activeYear - minYear) / 12) * 12 + minYear)
    }

    val first = if (hijriMode) {
        try {
            Hijri.toGreg(currentHijriYear, currentHijriMonth, 1, adj)
        } catch (e: Exception) {
            anchor.withDayOfMonth(1)
        }
    } else anchor.withDayOfMonth(1)
    val len = if (hijriMode) Hijri.of(first, adj).lengthOfMonth() else first.lengthOfMonth()
    val cells = List(first.dayOfWeek.value % 7) { null } + (0 until len).map { first.plusDays(it.toLong()) }

    val monthLabel = if (hijriMode) hijriMonth(currentHijriMonth, lang)
                     else Month.of(currentGregMonth).getDisplayName(java.time.format.TextStyle.FULL, loc)
    val yearLabel = if (hijriMode) "$currentHijriYear ${ahSuffix(lang)}" else "$currentGregYear"

    // Events falling in the selected year (Gregorian or Hijri), honouring the category chip, in date order.
    val yearEvents = remember(c.events, activeYear, hijriMode, adj, cat) {
        val hijriYears = if (hijriMode) listOf(activeYear) else
            (Hijri.year(Hijri.of(LocalDate.of(activeYear, 1, 1), adj))..Hijri.year(Hijri.of(LocalDate.of(activeYear, 12, 31), adj))).toList()
        hijriYears.flatMap { hy ->
            c.events.filter { cat == null || it.category == cat }.mapNotNull { e ->
                try { Hijri.toGreg(hy, e.month, e.day, adj).let { d -> if (hijriMode || d.year == activeYear) e to d else null } }
                catch (ex: Exception) { null }
            }
        }.sortedBy { it.second }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTitle(t("calendar"), t("calendar_sub"))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemedChip(!hijriMode, { hijriMode = false }, { Text(t("gregorian")) })
                ThemedChip(hijriMode, { hijriMode = true }, { Text(t("hijri")) })
            }
            TextButton({ anchor = clampDate(LocalDate.now()); viewMode = "days" }) {
                Text("📍 " + t("today_btn"), style = MaterialTheme.typography.labelMedium)
            }
        }

        if (viewMode == "days") {
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ThemedChip(cat == null, { cat = null }, { Text(t("all")) }) }
                items(listOf("ramadan", "eid", "hajj", "islamic_new_year", "observance")) { k ->
                    ThemedChip(cat == k, { cat = k }, { Text(t("cat_$k")) })
                }
            }
            
            // Header with interactive Month and Year clickable selectors
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        val prev = if (hijriMode) first.minusDays(1) else anchor.minusMonths(1)
                        anchor = clampDate(prev)
                    },
                    enabled = anchor.isAfter(MIN_GREG_DATE)
                ) { Text(if (rtl) "›" else "‹", fontSize = 20.sp) }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { viewMode = "months" },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "$monthLabel ▾",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        onClick = {
                            yearPageStart = ((activeYear - minYear) / 12) * 12 + minYear
                            viewMode = "years"
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "$yearLabel ▾",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                TextButton(
                    onClick = {
                        val next = if (hijriMode) first.plusDays(len.toLong()) else anchor.plusMonths(1)
                        anchor = clampDate(next)
                    },
                    enabled = anchor.isBefore(MAX_GREG_DATE)
                ) { Text(if (rtl) "‹" else "›", fontSize = 20.sp) }
            }

            Row {
                weekdays(lang).forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (lang == "en") 11.sp else 9.sp)
                    )
                }
            }

            GlassCard {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        (week + List(7 - week.size) { null }).forEach { d ->
                            Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                                if (d != null) {
                                    val h = Hijri.of(d, adj)
                                    val dot = eventsOn(c.events, d, adj).any { cat == null || it.category == cat }
                                    val today = d == LocalDate.now()
                                    val isSelected = picked == d
                                    Column(
                                        Modifier.clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                                    today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { picked = d }
                                            .fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(if (hijriMode) "${Hijri.day(h)}" else "${d.dayOfMonth}")
                                        Text(if (hijriMode) "${d.dayOfMonth}" else "${Hijri.day(h)}", style = MaterialTheme.typography.labelSmall)
                                        Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(if (dot) MaterialTheme.colorScheme.tertiary else Color.Transparent))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Events of the selected year; past ones are dimmed, upcoming ones are full strength.
            GlassCard {
                SectionLabel(t("year_events") + " " + yearLabel)
                if (yearEvents.isEmpty()) Text(t("no_year_events"), style = MaterialTheme.typography.bodyMedium)
                val todayDate = LocalDate.now()
                yearEvents.forEach { (e, d) ->
                    val past = d.isBefore(todayDate)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { picked = d }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(e.name(lang), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).alpha(if (past) 0.45f else 1f))
                        Text(
                            (if (hijriMode) Hijri.text(Hijri.of(d, adj), lang) else d.format(DateTimeFormatter.ofPattern("d MMM yyyy", loc))) + " · " + t("est"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (past) 0.4f else 0.7f)
                        )
                    }
                }
            }
        } else if (viewMode == "months") {
            // Month Picker Grid (12 months)
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            if (hijriMode) {
                                val newY = (currentHijriYear - 1).coerceIn(1318, 1524)
                                anchor = clampDate(try { Hijri.toGreg(newY, currentHijriMonth, 1, adj) } catch (e: Exception) { anchor })
                            } else {
                                anchor = clampDate(anchor.minusYears(1))
                            }
                        },
                        enabled = activeYear > minYear
                    ) { Text(if (rtl) "›" else "‹", fontSize = 20.sp) }

                    Surface(
                        onClick = {
                            yearPageStart = ((activeYear - minYear) / 12) * 12 + minYear
                            viewMode = "years"
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "$yearLabel ▾",
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    TextButton(
                        onClick = {
                            if (hijriMode) {
                                val newY = (currentHijriYear + 1).coerceIn(1318, 1524)
                                anchor = clampDate(try { Hijri.toGreg(newY, currentHijriMonth, 1, adj) } catch (e: Exception) { anchor })
                            } else {
                                anchor = clampDate(anchor.plusYears(1))
                            }
                        },
                        enabled = activeYear < maxYear
                    ) { Text(if (rtl) "‹" else "›", fontSize = 20.sp) }
                }

                Spacer(Modifier.height(4.dp))
                Text(t("select_month"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                (1..12).chunked(3).forEach { rowMonths ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowMonths.forEach { m ->
                            val isCur = if (hijriMode) m == currentHijriMonth else m == currentGregMonth
                            val name = if (hijriMode) hijriMonth(m, lang)
                                       else Month.of(m).getDisplayName(java.time.format.TextStyle.SHORT, loc)
                            Surface(
                                onClick = {
                                    anchor = if (hijriMode) {
                                        clampDate(try { Hijri.toGreg(currentHijriYear, m, 1, adj) } catch (e: Exception) { anchor })
                                    } else {
                                        clampDate(anchor.withMonth(m))
                                    }
                                    viewMode = "days"
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                contentColor = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                                    Text(name, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ viewMode = "days" }) { Text(t("close")) }
                }
            }
        } else {
            // Year Picker Grid (1900 to 2100 with decade pages and quick jumps)
            val pageYears = (yearPageStart until (yearPageStart + 12).coerceAtMost(maxYear + 1)).toList()
            val pageEnd = (yearPageStart + 11).coerceAtMost(maxYear)
            
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            yearPageStart = (yearPageStart - 12).coerceAtLeast(minYear)
                        },
                        enabled = yearPageStart > minYear
                    ) { Text(if (rtl) "›" else "‹", fontSize = 20.sp) }

                    Text(
                        "$yearPageStart – $pageEnd" + if (hijriMode) " ${ahSuffix(lang)}" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            yearPageStart = (yearPageStart + 12).coerceAtMost(maxYear - 11)
                        },
                        enabled = pageEnd < maxYear
                    ) { Text(if (rtl) "‹" else "›", fontSize = 20.sp) }
                }

                Spacer(Modifier.height(4.dp))
                Text(t("select_year"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                // Fast jump decade chips
                LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quickDecades = if (hijriMode) listOf(1318, 1350, 1400, 1448, 1500, 1524)
                                       else listOf(1900, 1930, 1960, 1990, 2026, 2060, 2100)
                    items(quickDecades) { yr ->
                        val active = activeYear == yr
                        ThemedChip(active, {
                            yearPageStart = ((yr - minYear) / 12) * 12 + minYear
                            anchor = if (hijriMode) {
                                clampDate(try { Hijri.toGreg(yr, currentHijriMonth, 1, adj) } catch (e: Exception) { anchor })
                            } else {
                                clampDate(anchor.withYear(yr))
                            }
                            viewMode = "months"
                        }, { Text("$yr") })
                    }
                }

                Spacer(Modifier.height(6.dp))

                pageYears.chunked(3).forEach { rowYears ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowYears.forEach { yr ->
                            val isCur = yr == activeYear
                            Surface(
                                onClick = {
                                    anchor = if (hijriMode) {
                                        clampDate(try { Hijri.toGreg(yr, currentHijriMonth, 1, adj) } catch (e: Exception) { anchor })
                                    } else {
                                        clampDate(anchor.withYear(yr))
                                    }
                                    viewMode = "months" // Drill down to month view
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                contentColor = if (isCur) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("$yr", fontWeight = if (isCur) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ viewMode = "days" }) { Text(t("close")) }
                }
            }
        }
    }
    
    picked?.let { d ->
        AlertDialog(
            onDismissRequest = { picked = null },
            confirmButton = { TextButton({ picked = null }) { Text(t("close")) } },
            title = { Text(d.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", loc))) },
            text = {
                Column {
                    Text(Hijri.text(Hijri.of(d, adj), lang), fontWeight = FontWeight.Bold)
                    Text(t("calc_date_note"), style = MaterialTheme.typography.bodySmall)
                    val ev = eventsOn(c.events, d, adj)
                    if (ev.isEmpty()) Text(t("no_occasions")) else ev.forEach { Text("• ${it.name(lang)}\n${it.description}") }
                    st.prayerCfg?.let { cfg ->
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(t("prayer_times"))
                        PrayerRows(cfg, d, lang)
                    }
                }
            }
        )
    }
}

@Composable fun Wisdom(c: Content, st: AppState) {
    val lang = st.lang.value
    var q by remember { mutableStateOf("") }
    var cat by remember(lang) { mutableStateOf<String?>(null) }
    var type by remember(lang) { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<Lesson?>(null) }
    var isHorizontalCards by remember { mutableStateOf(false) }

    val all = remember(lang) { c.lessons(lang) }
    val shown = all.filter { (cat == null || it.category == cat) && (type == null || it.type == type) &&
        (q.isBlank() || listOf(it.title, it.takeaway, it.body).any { s -> s.contains(q, true) }) }
    open?.let { LessonDialog(it, st) { open = null } }

    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTitle(t("wisdom"), t("wisdom_sub"))
        
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(t("search_lessons")) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )

        // View Layout Toggle (Horizontal Cards vs Vertical List)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${shown.size} ${t("wisdom")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!isHorizontalCards) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isHorizontalCards = false }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalListIcon(
                            tint = if (!isHorizontalCards) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isHorizontalCards) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isHorizontalCards = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalCardsIcon(
                            tint = if (isHorizontalCards) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item { ThemedChip(cat == null, { cat = null }, { Text(t("all")) }) }
            items(all.map { it.category }.distinct()) { k ->
                ThemedChip(cat == k, { cat = k }, { Text(k.replace('_', ' ').replaceFirstChar { it.uppercase() }) })
            }
        }

        // Quote Type Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item { ThemedChip(type == null, { type = null }, { Text(t("any_type")) }) }
            items(all.map { it.type }.distinct()) { k ->
                ThemedChip(type == k, { type = k }, { Text(typeName(k, lang)) })
            }
        }

        if (isHorizontalCards) {
            // Horizontal scrollable cards view
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(shown) { l ->
                    GlassCard(
                        modifier = Modifier
                            .width(300.dp)
                            .height(280.dp)
                            .clickable { open = l }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                SectionLabel(l.category.replace('_', ' '))
                                if (l.id in st.saved) Text("★", color = MaterialTheme.colorScheme.primary)
                            }
                            Text(l.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(l.takeaway, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                            Spacer(Modifier.height(4.dp))
                            Text(t("practice") + l.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${typeName(l.type, lang)} · ${l.minutes} ${t("min_short")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(t("tap_more"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            // Vertical scrollable list view
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(shown) { l ->
                    GlassCard(modifier = Modifier.clickable { open = l }) {
                        SectionLabel(l.category.replace('_', ' '))
                        Text(l.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(l.takeaway, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text(
                            "${typeFull(l.type, lang)} · ${l.minutes} ${t("min_suffix")}" + if (l.id in st.saved) " · ★" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable fun LessonDialog(l: Lesson, st: AppState, close: () -> Unit) {
    val tts = rememberTtsManager()
    val fullText = "${l.title}. ${l.takeaway}. ${l.body}. " + t("practice") + l.action + (l.question?.let { ". " + t("reflect") + it } ?: "")
    DisposableEffect(l.id) {
        onDispose { tts.stop() }
    }
    AlertDialog(onDismissRequest = { tts.stop(); close() }, title = { Text(l.title) },
        confirmButton = { TextButton({ tts.stop(); close() }) { Text(t("done")) } },
        dismissButton = { TextButton({ st.toggleSaved(l.id) }) { Text(if (l.id in st.saved) t("saved") else t("save")) } },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TtsPlayerBar(text = fullText, lang = st.lang.value, utteranceId = "lesson_${l.id}", tts = tts)
            Text(typeFull(l.type, st.lang.value), style = MaterialTheme.typography.labelMedium)
            if (l.machineTranslated) Text("Machine-translated; awaiting human review.", color = MaterialTheme.colorScheme.tertiary)
            Text(l.takeaway, style = MaterialTheme.typography.titleMedium); Text(l.body)
            Text(t("practice") + l.action, fontWeight = FontWeight.SemiBold); l.question?.let { Text(t("reflect") + it) } } })
}

/** Visual mock of the daily-wisdom notification, so users see what they will receive. */
@Composable fun NotificationPreview(lang: String, title: String, text: String, time: String) {
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)), shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(heroBrush), contentAlignment = Alignment.Center) { Text("🌙", fontSize = 11.sp) }
            Text(tr(lang, "notif_channel") + " · " + time, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) }
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        Text(tr(lang, "notif_read").uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

/** About & privacy, shown at the bottom of Settings. Wording must match the hosted privacy policy and the Play Data safety form. */
@Composable private fun AboutCard() {
    val ctx = LocalContext.current
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
    GlassCard {
        Text(t("about_title"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (version.isNotEmpty()) Text("${t("about_version")} $version", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        listOf("about_what" to "about_what_h", "about_content" to "about_content_h",
            "about_privacy" to "about_privacy_h", "about_ads" to "about_ads_h").forEach { (body, head) ->
            Spacer(Modifier.height(4.dp))
            Text(t(head), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(t(body), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Text(t("about_policy"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}


/** Premium status, upgrade entry and subscription management, shown at the top of Settings. */
@Composable private fun PremiumCard() {
    val ctx = LocalContext.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⭐", fontSize = 18.sp)
            Text(t("premium_title"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        if (Billing.isPremium) {
            Text(t("premium_active"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            // Cancelled but still inside the paid period or trial: access continues until it ends.
            if (!Billing.autoRenewing) Text(t("premium_cancelled"), style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ Billing.manage(ctx) }, Modifier.fillMaxWidth()) { Text(t("premium_manage")) }
        } else {
            Text(t("premium_pitch"), style = MaterialTheme.typography.bodySmall)
            if (Billing.pending) Text(t("premium_pending"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            Button({ Billing.paywall = true }, Modifier.fillMaxWidth()) { Text(t("premium_upgrade")) }
            TextButton({ Billing.restore() }, Modifier.fillMaxWidth()) { Text(t("premium_restore")) }
        }
        Billing.messageKey?.let { Text(t(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
    }
}

/** Plan picker. Shows price, billing period, trial and cancellation terms before the purchase button, as Play policy requires. */
@Composable fun PaywallDialog() {
    if (!Billing.paywall) return
    val ctx = LocalContext.current
    val activity = generateSequence(ctx) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
    var selected by remember { mutableStateOf(Billing.PLAN_YEARLY) }
    val plan = Billing.plans.firstOrNull { it.basePlanId == selected } ?: Billing.plans.firstOrNull()
    AlertDialog(
        onDismissRequest = { Billing.paywall = false },
        title = { Text("⭐ " + t("premium_title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(t("premium_benefit_ads")); Text(t("premium_benefit_voices")); Text(t("premium_benefit_themes"))
                if (Billing.plans.isEmpty()) {
                    Text(t("premium_unavailable"), color = MaterialTheme.colorScheme.error)
                } else {
                    Billing.plans.forEach { p ->
                        val yearly = p.basePlanId == Billing.PLAN_YEARLY
                        val trial = p.trialDays
                        ThemedChip(selected == p.basePlanId, { selected = p.basePlanId }, {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(t(if (yearly) "premium_yearly" else "premium_monthly") + " · " + p.price + " " + t(if (yearly) "premium_per_year" else "premium_per_month"),
                                    fontWeight = FontWeight.Bold)
                                if (trial != null) Text(t("premium_trial").replace("{n}", trial.toString()), style = MaterialTheme.typography.bodySmall)
                            }
                        })
                    }
                    if (plan != null) {
                        val trial = plan.trialDays
                        val period = t(if (plan.basePlanId == Billing.PLAN_YEARLY) "premium_per_year" else "premium_per_month")
                        Text(
                            (if (trial != null) t("premium_terms_trial").replace("{n}", trial.toString()) + " " else "") +
                                t("premium_terms").replace("{price}", plan.price).replace("{period}", period),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Billing.messageKey?.let { Text(t(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
                TextButton({ Billing.restore() }) { Text(t("premium_restore")) }
            }
        },
        confirmButton = {
            Button({ if (plan != null && activity != null) Billing.purchase(activity, plan) }, enabled = plan != null) {
                Text(t(if (plan?.trialDays != null) "premium_start_trial" else "premium_subscribe"))
            }
        },
        dismissButton = { TextButton({ Billing.paywall = false }) { Text(t("close")) } }
    )
}

@Composable private fun VoiceSettingsCard(lang: String) {
    val tts = rememberTtsManager()
    val premium = Billing.isPremium
    val chosen = if (premium) tts.selectedVoice[lang].orEmpty() else ""
    val ready = tts.isReady.value
    var options by remember { mutableStateOf(emptyList<TextToSpeechManager.VoiceOption>()) }
    // The engine's voice list can arrive slightly after it reports ready, so retry briefly.
    LaunchedEffect(lang, ready) {
        options = emptyList()
        if (!ready) return@LaunchedEffect
        repeat(20) {
            val found = tts.voiceOptions(lang)
            if (found.isNotEmpty()) { options = found; return@LaunchedEffect }
            kotlinx.coroutines.delay(250)
        }
    }
    val sample = when (lang) {
        "ar" -> "بسم الله الرحمن الرحيم"
        "ur" -> "بسم اللہ الرحمٰن الرحیم"
        else -> "In the name of Allah, the Most Gracious, the Most Merciful."
    }
    GlassCard {
        Text(t("voice"), fontWeight = FontWeight.Bold)
        Text(t("voice_help"), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemedChip(chosen.isEmpty(), { tts.setVoice(lang, "") }, { Text(t("voice_default"), maxLines = 1) })
            options.forEachIndexed { i, o ->
                val icon = when (o.gender) { "male" -> "♂ "; "female" -> "♀ "; else -> "" }
                val name = if (o.key.startsWith("pitch:")) t(if (o.gender == "male") "voice_male" else "voice_female")
                           else "${t("voice_short")} ${i + 1}"
                ThemedChip(chosen == o.key,
                    { if (premium) tts.setVoice(lang, o.key) else Billing.paywall = true },
                    { Text((if (premium) "" else "🔒 ") + icon + name, maxLines = 1) })
            }
        }
        OutlinedButton({ tts.stop(); tts.speak(sample, lang, "NOOR_VOICE_TEST") }, Modifier.fillMaxWidth()) {
            Text("🔊  ${t("voice_test")}")
        }
    }
}

@Composable fun Settings(c: Content, st: AppState) {
    val ctx = LocalContext.current; val lang = st.lang.value
    var denied by remember { mutableStateOf(false) }
    var pendingTest by remember { mutableStateOf(false) }
    fun granted() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied = !ok
        if (ok && pendingTest) Reminder.show(ctx)
        else if (ok) { st.setRemind(true); Reminder.schedule(ctx, st.hour.intValue, st.minute.intValue) }
        pendingTest = false
    }
    val time = LocalTime.of(st.hour.intValue, st.minute.intValue).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(localeOf(lang)))
    val lesson = lessonOfDay(c.lessons(lang))
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle(t("settings"), t("settings_sub"))
        PremiumCard()
        GlassCard { Text(t("language"), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (code, label) -> ThemedChip(lang == code, { st.setLang(code) }, { Text(label) }) } } }
        VoiceSettingsCard(lang)
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎨", fontSize = 18.sp)
                Text(t("theme_and_appearance"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(t("theme_mode"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val themeModes = listOf(
                    Triple("system", "📱", "theme_system"),
                    Triple("light", "☀️", "theme_light"),
                    Triple("dark", "🌙", "theme_dark")
                )
                themeModes.forEach { (k, icon, labelKey) ->
                    val isSelected = st.theme.value == k
                    Surface(
                        onClick = { st.setTheme(k) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(icon, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t(labelKey),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(t("color_palette"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
            ) {
                items(Palettes) { pal ->
                    // Without Premium the app falls back to Classic, so the ring follows the palette actually in use.
                    val inUse = if (st.palette.value in FreePaletteIds || Billing.isPremium) st.palette.value else Palettes[0].id
                    val isSelected = inUse == pal.id
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(pal.previewColors))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable { if (pal.id in FreePaletteIds || Billing.isPremium) st.setPalette(pal.id) else Billing.paywall = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pal.id !in FreePaletteIds && !Billing.isPremium) Text("🔒", fontSize = 16.sp)
                        else if (isSelected) Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("reminder_title"), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(st.remind.value, { on ->
                    if (!on) { st.setRemind(false); Reminder.cancel(ctx) }
                    else if (granted()) { st.setRemind(true); Reminder.schedule(ctx, st.hour.intValue, st.minute.intValue) }
                    else launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) }
            Text(t("reminder_help"), style = MaterialTheme.typography.bodySmall)
            if (denied || (!granted() && Build.VERSION.SDK_INT >= 33)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t("perm_denied"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(
                        onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                                }
                            } else {
                                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", ctx.packageName, null)
                                }
                            }
                            ctx.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t("open_settings"))
                    }
                }
            }
            OutlinedButton({ TimePickerDialog(ctx, { _, h, m -> st.setTime(h, m); if (st.remind.value) Reminder.schedule(ctx, h, m) },
                st.hour.intValue, st.minute.intValue, false).show() }, Modifier.fillMaxWidth()) { Text("⏰  ${t("reminder_time")}: $time") }
            SectionLabel(t("preview"))
            NotificationPreview(lang, "🌙 " + Hijri.text(Hijri.of(LocalDate.now(), st.adj.intValue), lang), lesson.takeaway, time)
            TextButton({ if (granted()) Reminder.show(ctx) else { pendingTest = true; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) } }) { Text(t("send_test")) }
        }
        GlassCard { Text(t("adj_title"), fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ st.setAdj(st.adj.intValue - 1) }) { Text("−") }
                Text("${if (st.adj.intValue > 0) "+" else ""}${st.adj.intValue} ${t("days")}")
                OutlinedButton({ st.setAdj(st.adj.intValue + 1) }) { Text("+") } }
            Text(t("adj_help"), style = MaterialTheme.typography.bodySmall) }
        AboutCard()
    }
}
