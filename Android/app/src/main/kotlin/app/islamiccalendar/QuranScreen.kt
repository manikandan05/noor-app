package app.islamiccalendar
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

private fun arabicDigits(n: Int) = n.toString().map { '٠' + (it - '0') }.joinToString("")
private fun surahTitle(s: SurahInfo, lang: String) = if (lang == "ar") s.ar else s.en
@Composable private fun fill(key: String, vararg kv: Pair<String, Any>) = kv.fold(t(key)) { acc, (k, v) -> acc.replace("{$k}", "$v") }

/** Colours for one reading theme (screenshots: Modern, Paper, Dark). */
private class ReaderTheme(val id: String, val nameKey: String, val bg: Color, val fg: Color, val sub: Color, val accent: Color, val card: Color)
private val ReaderThemes = listOf(
    ReaderTheme("modern", "qt_modern", Color(0xFFFFFFFF), Color(0xFF14202B), Color(0xFF5B6B7A), Color(0xFF1FA67A), Color(0xFFF1F6F8)),
    ReaderTheme("paper", "qt_paper", Color(0xFFF6ECD3), Color(0xFF3B2A14), Color(0xFF7A6443), Color(0xFF8D6E2F), Color(0xFFEBDDB8)),
    ReaderTheme("dark", "qt_dark", Color(0xFF0E1116), Color(0xFFF1F3F5), Color(0xFFA4ADB8), Color(0xFF2DD498), Color(0xFF1A2029))
)

/** Circular gradient progress ring with arbitrary centre content. */
@Composable fun GradientRing(frac: Float, size: Dp, stroke: Dp = 10.dp, trackColor: Color = LocalContentColor.current.copy(alpha = 0.18f),
                             content: @Composable () -> Unit) {
    val a = MaterialTheme.colorScheme.secondary; val b = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx(); val sz = Size(this.size.width - w, this.size.height - w); val o = Offset(w / 2, w / 2)
            drawArc(trackColor, 0f, 360f, false, o, sz, style = Stroke(w))
            if (frac > 0f) drawArc(Brush.sweepGradient(listOf(a, b, a)), -90f, 360f * frac.coerceAtMost(1f), false, o, sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable fun QuranScreen(c: Content, st: AppState) {
    var open by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    open?.let { (s, a) -> Reader(c, st, s, a) { open = null }; return }

    val lang = st.lang.value
    var tab by remember { mutableStateOf("read") }
    var q by remember { mutableStateOf("") }
    val shown = c.surahs.filter { q.isBlank() || it.en.contains(q.trim(), true) || it.meaning.contains(q.trim(), true) ||
        it.ar.contains(q.trim()) || it.n.toString() == q.trim() }
    fun info(n: Int) = c.surahs.firstOrNull { it.n == n }
    fun parse(k: String) = k.split(":").let { it[0].toInt() to it[1].toInt() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle(t("quran"), t("quran_sub")) }
        item {
            val cur = info(st.qSurah.intValue)
            val goalSecs = st.qGoalMin.intValue * 60
            GlassCard(hero = true, modifier = Modifier.clickable { open = (if (st.qHasRead.value) st.qSurah.intValue else 1) to (if (st.qHasRead.value) st.qAyah.intValue else 1) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GradientRing(st.qReadSecs.intValue / goalSecs.toFloat(), 88.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📖", fontSize = 20.sp)
                            Text("${st.qReadSecs.intValue / 60}m", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(t("last_read").uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        if (st.qHasRead.value && cur != null) {
                            Text("${surahTitle(cur, lang)} ${st.qSurah.intValue}:${st.qAyah.intValue}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text(t("continue_reading"), fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(t("recent_none"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(t("start_reading"), fontWeight = FontWeight.SemiBold)
                        }
                        Text(fill("minutes_read", "n" to st.qReadSecs.intValue / 60) + " · " + t("reading_goal") + " ${st.qGoalMin.intValue}m", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemedChip(tab == "read", { tab = "read" }, { Text(t("tab_read")) })
                ThemedChip(tab == "bookmarks", { tab = "bookmarks" }, { Text("★ " + t("tab_bookmarks")) })
                ThemedChip(tab == "progress", { tab = "progress" }, { Text(t("tab_progress")) })
            }
        }
        when (tab) {
            "read" -> {
                if (st.qRecents.isNotEmpty()) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(t("recents"))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(st.qRecents.toList()) { k ->
                                val (s, a) = parse(k); val si = info(s)
                                if (si != null) Column(
                                    Modifier.width(150.dp).clip(RoundedCornerShape(20.dp)).background(accentBrush).clickable { open = s to a }.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(si.ar, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("${si.en} ${t("ayah")} $a", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text(t("search_surah")) }, singleLine = true, shape = RoundedCornerShape(28.dp))
                }
                item { SectionLabel(t("sura_list")) }
                items(shown, key = { it.n }) { s ->
                    GlassCard(modifier = Modifier.clickable { open = s.n to 1 }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(accentBrush), contentAlignment = Alignment.Center) {
                                Text("${s.n}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(s.en, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${s.meaning} · ${fill("ayahs_n", "n" to s.ayahs)} · ${t(if (s.meccan) "meccan" else "medinan")}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Text(s.ar, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { Text(t("quran_note"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
            }
            "bookmarks" -> {
                val marks = st.qBookmarks.map(::parse).sortedWith(compareBy({ it.first }, { it.second }))
                if (marks.isEmpty()) item { GlassCard { Text(t("no_bookmarks")) } }
                items(marks) { (s, a) ->
                    val si = info(s)
                    if (si != null) GlassCard(modifier = Modifier.clickable { open = s to a }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column { Text("${surahTitle(si, lang)} $s:$a", fontWeight = FontWeight.Bold); Text(si.meaning, style = MaterialTheme.typography.bodySmall) }
                            Text("★", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                        }
                    }
                }
            }
            else -> item {
                GlassCard {
                    SectionLabel(t("reading_goal"))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GradientRing(st.qReadSecs.intValue / (st.qGoalMin.intValue * 60f), 96.dp, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)) {
                            Text("${st.qReadSecs.intValue / 60}/${st.qGoalMin.intValue}", fontWeight = FontWeight.ExtraBold)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(fill("minutes_read", "n" to st.qReadSecs.intValue / 60), fontWeight = FontWeight.Bold)
                            Text(t("goal_minutes"), style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton({ st.setQGoal(st.qGoalMin.intValue - 1) }) { Text("−") }
                                Text("${st.qGoalMin.intValue}", Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                                OutlinedButton({ st.setQGoal(st.qGoalMin.intValue + 1) }) { Text("+") }
                            }
                        }
                    }
                    Text(t("ayat_bookmarked") + ": ${st.qBookmarks.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Reader: one surah at a time. The visible aya is saved as "last read" while scrolling; time spent counts toward today's goal. */
@Composable private fun Reader(c: Content, st: AppState, n: Int, startAyah: Int, close: () -> Unit) {
    BackHandler(onBack = close)
    val lang = st.lang.value
    val info = c.surahs.first { it.n == n }
    val text = remember(n) { c.surah(n) }
    val th = ReaderThemes.firstOrNull { it.id == st.qTheme.value } ?: ReaderThemes[0]
    var showTranslit by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(true) }
    val ls = rememberLazyListState(initialFirstVisibleItemIndex = (startAyah - 1).coerceAtLeast(0))

    LaunchedEffect(n) { snapshotFlow { ls.firstVisibleItemIndex }.drop(1).collect { st.saveQPosition(n, it + 1) } }
    LaunchedEffect(Unit) {
        var acc = 0
        try { while (true) { delay(1000); acc++; if (acc >= 10) { st.addReadSeconds(acc); acc = 0 } } }
        finally { if (acc > 0) st.addReadSeconds(acc) }
    }

    Column(Modifier.fillMaxSize().padding(top = 12.dp, bottom = 8.dp).clip(RoundedCornerShape(28.dp)).background(th.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(close) { Text("‹ " + t("back"), color = th.accent, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(surahTitle(info, lang), color = th.fg, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("${info.meaning} · ${fill("ayahs_n", "n" to info.ayahs)}", color = th.sub, style = MaterialTheme.typography.labelSmall)
                }
                Text("${st.qReadSecs.intValue / 60}/${st.qGoalMin.intValue}m", color = th.sub, style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ReaderThemes.forEach { r ->
                    val sel = r.id == th.id
                    Box(Modifier.clip(RoundedCornerShape(50)).background(r.bg).border(BorderStroke(if (sel) 2.dp else 1.dp, if (sel) th.accent else th.sub.copy(alpha = 0.4f)), RoundedCornerShape(50))
                        .clickable { st.setQTheme(r.id) }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(t(r.nameKey), color = r.fg, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                    }
                }
                Spacer(Modifier.weight(1f))
                listOf(t("show_translit") to showTranslit, t("show_translation") to showTranslation).forEachIndexed { i, (label, on) ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) th.accent else Color.Transparent)
                        .border(BorderStroke(1.dp, th.accent), RoundedCornerShape(50))
                        .clickable { if (i == 0) showTranslit = !showTranslit else showTranslation = !showTranslation }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(label, color = if (on) th.bg else th.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        if (text == null) { Text(t("recent_none"), Modifier.padding(16.dp), color = th.fg); return@Column }
        LazyColumn(state = ls, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(text.arabic.size) { i ->
                val ayah = i + 1
                val marked = "$n:$ayah" in st.qBookmarks
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(th.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("$n:$ayah", color = th.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text(if (marked) "★" else "☆", color = th.accent, fontSize = 22.sp, modifier = Modifier.clickable { st.toggleQBookmark(n, ayah) })
                    }
                    Text(text.arabic[i] + " \uFD3F" + arabicDigits(ayah) + "\uFD3E", Modifier.fillMaxWidth(), color = th.fg, fontSize = 28.sp, lineHeight = 52.sp,
                        textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                    if (showTranslit) Text(text.translit[i], color = th.accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (showTranslation) Text(text.english[i], color = th.fg.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
