package app.islamiccalendar
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


/** New page: a Duas library (Hisnul Muslim style chapters) and a calm Tasbih counter. */
@Composable fun DhikrScreen(c: Content, st: AppState) {
    var tab by remember { mutableStateOf("duas") }
    Column(Modifier.fillMaxSize().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTitle(t("dhikr"), t("dhikr_sub"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemedChip(tab == "duas", { tab = "duas" }, { Text("🤲 " + t("tab_duas")) })
            ThemedChip(tab == "tasbih", { tab = "tasbih" }, { Text("📿 " + t("tab_tasbih")) })
        }
        if (tab == "duas") DuasSection(c, st) { tab = "tasbih" } else TasbihSection(st)
    }
}

@Composable private fun DuasSection(c: Content, st: AppState, openTasbih: () -> Unit) {
    val lang = st.lang.value
    var q by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf<String?>(null) }
    var quick by remember { mutableStateOf<String?>(null) } // "bookmarks" | "recents"
    var open by remember { mutableStateOf<Dua?>(null) }
    open?.let { DuaDialog(it, st, lang) { open = null } }

    val shown = c.duas.filter { d ->
        (chapter == null || d.chapter == chapter) &&
        (quick != "bookmarks" || d.id in st.duaSaved) &&
        (quick != "recents" || d.id in st.duaRecents) &&
        (q.isBlank() || listOf(d.title(lang), d.title("en"), d.meaning, d.translit, d.arabic).any { it.contains(q.trim(), true) })
    }.let { l -> if (quick == "recents") l.sortedBy { st.duaRecents.indexOf(it.id) } else l }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text(t("search_duas")) }, singleLine = true, shape = RoundedCornerShape(28.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Triple("bookmarks", "★", "quick_bookmarks"), Triple("recents", "🕘", "quick_recents"),
                    Triple("hisn", "🛡️", "quick_hisn"), Triple("tasbih", "📿", "quick_tasbih")).forEach { (k, icon, label) ->
                    val sel = quick == k || (k == "hisn" && quick == null && chapter == null)
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) accentBrush else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))))
                        .clickable { when (k) { "tasbih" -> openTasbih(); "hisn" -> { quick = null; chapter = null }; else -> quick = if (quick == k) null else k } }
                        .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(icon, fontSize = 22.sp)
                        Text(t(label), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            SectionLabel(t("chapters"))
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { ThemedChip(chapter == null, { chapter = null }, { Text(t("all")) }) }
                items(c.duaChapters) { ch ->
                    ThemedChip(chapter == ch.id, { chapter = if (chapter == ch.id) null else ch.id }, { Text("${ch.icon} ${ch.name(lang)}") })
                }
            }
        }
        if (shown.isEmpty()) item { GlassCard { Text(t("no_duas")) } }
        items(shown, key = { it.id }) { d ->
            val ch = c.duaChapters.firstOrNull { it.id == d.chapter }
            GlassCard(modifier = Modifier.clickable { st.touchDua(d.id); open = d }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("${ch?.icon ?: ""} ${ch?.name(lang) ?: ""}")
                    if (d.id in st.duaSaved) Text("★", color = MaterialTheme.colorScheme.primary)
                }
                Text(d.title(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(d.arabic, Modifier.fillMaxWidth(), fontSize = 20.sp, lineHeight = 36.sp, textAlign = TextAlign.Center, maxLines = 2, color = MaterialTheme.colorScheme.primary)
                d.virtue?.let { Text("✦ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
            }
        }
    }
}

@Composable private fun DuaDialog(d: Dua, st: AppState, lang: String, close: () -> Unit) {
    val tts = rememberTtsManager()
    DisposableEffect(d.id) {
        onDispose { tts.stop() }
    }
    AlertDialog(onDismissRequest = { tts.stop(); close() }, title = { Text(d.title(lang)) },
        confirmButton = { TextButton({ tts.stop(); close() }) { Text(t("done")) } },
        dismissButton = { TextButton({ st.toggleDuaSaved(d.id) }) { Text(if (d.id in st.duaSaved) t("saved") else t("save")) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TtsPlayButton(text = d.arabic, lang = "ar", utteranceId = "dua_${d.id}", tts = tts)
                }
                Text(d.arabic, Modifier.fillMaxWidth(), fontSize = 26.sp, lineHeight = 46.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                Text(d.translit, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Text(d.meaning)
                d.virtue?.let { Text(t("virtue") + ": " + it, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold) }
                Text(t("source") + ": " + d.source, style = MaterialTheme.typography.bodySmall)
            }
        })
}

/** Tasbih: an arched string of beads that slides along as the user taps. Count and target persist across launches. */
@Composable private fun TasbihSection(st: AppState) {
    val haptic = LocalHapticFeedback.current
    val dhikr = DhikrList[st.tasbihIdx.intValue.coerceIn(0, DhikrList.lastIndex)]
    val target = st.tasbihTarget.intValue
    val count = st.tasbihCount.intValue
    var rounds by remember { mutableIntStateOf(0) }
    val done = count >= target
    val bead1 = MaterialTheme.colorScheme.primary; val bead2 = MaterialTheme.colorScheme.secondary

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t("tasbih_title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DhikrList.size) { i -> ThemedChip(st.tasbihIdx.intValue == i, { st.setTasbihDhikr(i); st.setTasbihTarget(DhikrList[i].target) }, { Text(DhikrList[i].translit) }) }
        }
        GlassCard(hero = true, modifier = Modifier.clickable {
            if (done) { rounds++; st.setTasbih(1) } else st.setTasbih(count + 1)
            haptic.performHapticFeedback(if (count + 1 >= target) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$count", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(" / $target", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
                }
                // Bead arc: 'target' capped at 33 visible beads; beads up to the current count are lit.
                val visible = target.coerceAtMost(33)
                val lit = if (target <= 33) count else ((count.toFloat() / target) * visible).toInt()
                Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                    val cx = size.width / 2; val r = size.width * 0.46f; val cy = size.height + r * 0.55f
                    for (i in 0 until visible) {
                        val a = PI * (1.18 + 0.64 * (i / (visible - 1f).coerceAtLeast(1f)))   // sweep across the top of a circle
                        val p = Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
                        val on = i < lit
                        val rad = 9.dp.toPx()
                        drawCircle(if (on) Brush.radialGradient(listOf(Color.White, bead1), p - Offset(rad * 0.3f, rad * 0.3f), rad * 1.6f)
                                   else Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.18f)), p, rad * 1.4f), rad, p)
                    }
                }
                Text(t("tap_to_count"), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            }
        }
        GlassCard {
            Text(dhikr.arabic, Modifier.fillMaxWidth(), fontSize = 34.sp, lineHeight = 52.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
            Text(dhikr.translit, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(dhikr.meaning, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (done) Text(t("completed"), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            if (rounds > 0) Text(t("total_today") + ": $rounds", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
        GlassCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("target"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(listOf(33, 99, 100, 313, 1000)) { n ->
                        ThemedChip(target == n, { st.setTasbihTarget(n) }, { Text("$n") })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ st.setTasbih(0); rounds = 0 }, Modifier.fillMaxWidth()) { Text("↺ " + t("reset")) }
        }
    }
}
