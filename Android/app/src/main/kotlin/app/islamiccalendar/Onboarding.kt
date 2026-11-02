package app.islamiccalendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private const val STEPS = 4
private val tourEmoji = listOf("🌙", "📅", "📖", "⚙️")

/** Welcome screen followed by a quick tour with a progress bar. Both can be skipped. */
@Composable fun Onboarding(st: AppState) {
    val ctx = LocalContext.current
    var started by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            st.setRemind(true)
            Reminder.schedule(ctx, st.hour.intValue, st.minute.intValue)
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT < 33) {
            st.setRemind(true)
            Reminder.schedule(ctx, st.hour.intValue, st.minute.intValue)
        }
    }

    val heroBg = heroBrush
    val accentColor = Accent
    Box(Modifier.fillMaxSize().background(heroBg).drawBehind {
        drawCircle(Color.White.copy(alpha = 0.08f), size.width * 0.6f, Offset(size.width, 0f))
        drawCircle(accentColor.copy(alpha = 0.22f), size.width * 0.45f, Offset(0f, size.height))
    }.systemBarsPadding().padding(24.dp)) {
        if (!started) Welcome(
            onStart = {
                requestNotificationPermission()
                started = true
            },
            onSkip = {
                requestNotificationPermission()
                st.finishOnboarding()
            }
        )
        else Tour(
            onBackToWelcome = { started = false },
            onDone = {
                requestNotificationPermission()
                st.finishOnboarding()
            }
        )
    }
}

@Composable private fun Welcome(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onSkip) { Text(t("skip"), color = Color.White) }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(132.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text("🌙", fontSize = 64.sp)
        }
        Spacer(Modifier.height(28.dp))
        Text(t("welcome"), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
        Text(t("app_title"), color = Color.White, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(t("tagline"),
            color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        PrimaryAction(t("get_started"), onStart)
    }
}

@Composable private fun Tour(onBackToWelcome: () -> Unit, onDone: () -> Unit) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    val last = i == STEPS - 1
    val progress by animateFloatAsState((i + 1f) / STEPS, label = "tourProgress")
    BackHandler { if (i > 0) i-- else onBackToWelcome() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t("step_x").replace("{a}", "${i + 1}").replace("{b}", "$STEPS"), color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            TextButton(onDone) { Text(t("skip"), color = Color.White) }
        }
        LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Accent, trackColor = Color.White.copy(alpha = 0.25f), drawStopIndicator = {})
        Crossfade(i, Modifier.weight(1f).fillMaxWidth(), label = "tourStep") { idx ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(120.dp).clip(RoundedCornerShape(36.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Text(tourEmoji[idx], fontSize = 56.sp)
                }
                Spacer(Modifier.height(28.dp))
                Text(t("tour${idx + 1}_t"), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(t("tour${idx + 1}_d"), color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            (0 until STEPS).forEach { d ->
                Box(Modifier.padding(4.dp).height(8.dp).width(if (d == i) 24.dp else 8.dp).clip(CircleShape)
                    .background(if (d == i) Accent else Color.White.copy(alpha = 0.4f)))
            }
        }
        PrimaryAction(if (last) t("finish") else t("next")) { if (last) onDone() else i++ }
    }
}

@Composable private fun PrimaryAction(label: String, onClick: () -> Unit) =
    Button(onClick, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF1B1B1B))) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
