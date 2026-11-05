package app.islamiccalendar

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Centralized Text-to-Speech manager for the NOOR app.
 * Provides a shared speech engine, lifecycle safety, rate controls,
 * and Jetpack Compose reactive state bindings.
 */
class TextToSpeechManager private constructor(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    val isReady = mutableStateOf(false)
    val isSpeaking = mutableStateOf(false)
    val currentUtteranceId = mutableStateOf<String?>(null)
    val speechRate = mutableFloatStateOf(0.95f)

    private val prefs = appContext.getSharedPreferences("s", 0)
    /** Selected voice per app language: "" (engine default), a voice name, or "pitch:male" / "pitch:female". */
    val selectedVoice = mutableStateMapOf<String, String>().apply {
        listOf("en", "ar", "ur").forEach { put(it, prefs.getString("tts_voice_$it", "") ?: "") }
    }

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        android.util.Log.d("NOOR_TTS", "onInit status=$status (SUCCESS=${TextToSpeech.SUCCESS})")
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        android.util.Log.d("NOOR_TTS", "onStart utteranceId=$utteranceId")
                        mainHandler.post {
                            isSpeaking.value = true
                            currentUtteranceId.value = utteranceId
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        android.util.Log.d("NOOR_TTS", "onDone utteranceId=$utteranceId")
                        mainHandler.post {
                            if (currentUtteranceId.value == utteranceId) {
                                isSpeaking.value = false
                                currentUtteranceId.value = null
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        android.util.Log.e("NOOR_TTS", "onError utteranceId=$utteranceId")
                        mainHandler.post {
                            if (currentUtteranceId.value == utteranceId) {
                                isSpeaking.value = false
                                currentUtteranceId.value = null
                            }
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        android.util.Log.e("NOOR_TTS", "onError utteranceId=$utteranceId errorCode=$errorCode")
                        mainHandler.post {
                            if (currentUtteranceId.value == utteranceId) {
                                isSpeaking.value = false
                                currentUtteranceId.value = null
                            }
                        }
                    }
                })
                engine.setSpeechRate(speechRate.floatValue)
                engine.setPitch(1.0f)
            }
            mainHandler.post { isReady.value = true }
        }
    }

    fun localeFor(lang: String): Locale = when (lang) {
        "ar" -> Locale("ar")
        "ur" -> Locale("ur", "PK")
        "ta" -> Locale("ta", "IN")
        else -> Locale.ENGLISH
    }

    fun setVoice(lang: String, key: String) {
        selectedVoice[lang] = key
        prefs.edit().putString("tts_voice_$lang", key).apply()
    }

    /** One selectable voice. [gender] is "male", "female" or null when unknown. */
    class VoiceOption(val key: String, val gender: String?)

    /**
     * Gender per Google TTS voice code (classified by measured pitch of each voice).
     * Android exposes no gender metadata, so this is how male/female voices are identified.
     */
    private val maleCodes = mapOf("en" to setOf("tpd", "iom", "iol"), "ar" to setOf("ard", "are"), "ur" to setOf("urb"))
    private val femaleCodes = mapOf("en" to setOf("tpf", "sfg", "iob", "iog", "tpc"), "ar" to setOf("arz", "arc"), "ur" to setOf("urc", "cfn"))

    /**
     * Up to [MAX_VOICE_OPTIONS] installed voices for [lang], one per voice code (offline preferred),
     * males first. If the language has no installed voices, offers pitch-shifted male/female instead.
     */
    fun voiceOptions(lang: String): List<VoiceOption> {
        val engine = tts ?: return emptyList()
        val voices = try { engine.voices } catch (e: Exception) { null }
        if (voices.isNullOrEmpty()) return emptyList() // engine voices not loaded yet
        val perCode = voices
            .filter { it.locale.language == localeFor(lang).language &&
                !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .groupBy { it.name.substringAfter("-x-", "").substringBefore("-") }
            .filterKeys { it.isNotEmpty() }
            .map { (code, vs) ->
                val v = vs.firstOrNull { !it.isNetworkConnectionRequired } ?: vs.first()
                val g = when (code) {
                    in maleCodes[lang].orEmpty() -> "male"
                    in femaleCodes[lang].orEmpty() -> "female"
                    else -> null
                }
                VoiceOption(v.name, g)
            }
            .sortedWith(compareBy({ it.gender != "male" }, { it.gender != "female" }, { it.key }))
        if (perCode.isEmpty()) return listOf(VoiceOption("pitch:male", "male"), VoiceOption("pitch:female", "female"))
        return perCode.take(MAX_VOICE_OPTIONS - 1)
    }

    fun isLanguageSupported(lang: String): Boolean {
        val engine = tts ?: return false
        val loc = localeFor(lang)
        val res = engine.isLanguageAvailable(loc)
        return res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, lang: String = "en", utteranceId: String = "NOOR_LESSON") {
        android.util.Log.d("NOOR_TTS", "speak() called: lang=$lang utteranceId=$utteranceId isReady=${isReady.value} textLen=${text.length}")
        val engine = tts
        if (engine == null) {
            android.util.Log.e("NOOR_TTS", "engine is null")
            return
        }
        if (!isReady.value || text.isBlank()) {
            android.util.Log.w("NOOR_TTS", "Not ready or text blank")
            return
        }

        // If currently playing this exact item, stop it (toggle behavior)
        if (isSpeaking.value && currentUtteranceId.value == utteranceId) {
            android.util.Log.d("NOOR_TTS", "Currently speaking this item -> stopping")
            stop()
            return
        }

        val loc = localeFor(lang)
        val langResult = engine.setLanguage(loc)
        android.util.Log.d("NOOR_TTS", "setLanguage($loc) returned $langResult")
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            android.util.Log.w("NOOR_TTS", "Language $loc not supported or missing data, falling back to English")
            engine.language = Locale.ENGLISH
        }

        var pitch = 1.0f
        // Voice choice is a Premium feature: without Premium the engine's default voice is always used.
        val chosen = if (Billing.isPremium) selectedVoice[lang].orEmpty() else ""
        when {
            chosen == "pitch:male" -> pitch = 0.75f
            chosen == "pitch:female" -> pitch = 1.25f
            chosen.isNotEmpty() -> try { engine.voices } catch (e: Exception) { null }
                ?.firstOrNull { it.name == chosen }?.let { engine.voice = it }
        }

        engine.setSpeechRate(speechRate.floatValue)
        engine.setPitch(pitch)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }

        val speakResult = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        android.util.Log.d("NOOR_TTS", "engine.speak returned $speakResult (SUCCESS=${TextToSpeech.SUCCESS})")
    }

    fun stop() {
        tts?.stop()
        mainHandler.post {
            isSpeaking.value = false
            currentUtteranceId.value = null
        }
    }

    fun setSpeed(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        speechRate.floatValue = clamped
        tts?.setSpeechRate(clamped)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady.value = false
        isSpeaking.value = false
        currentUtteranceId.value = null
    }

    companion object {
        /** Maximum voice choices shown in Settings, including "Default". */
        const val MAX_VOICE_OPTIONS = 10

        @Volatile
        private var instance: TextToSpeechManager? = null

        fun getInstance(context: Context): TextToSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: TextToSpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/** Provides access to the shared [TextToSpeechManager] in Compose. */
@Composable
fun rememberTtsManager(): TextToSpeechManager {
    val ctx = LocalContext.current
    return remember { TextToSpeechManager.getInstance(ctx) }
}

/**
 * Compact, modern TTS button for Compose cards and dialogs.
 * Displays Play/Stop states and animated indicators.
 */
@Composable
fun TtsPlayButton(
    text: String,
    lang: String = "en",
    utteranceId: String,
    modifier: Modifier = Modifier,
    tts: TextToSpeechManager = rememberTtsManager()
) {
    val isCurrent = tts.currentUtteranceId.value == utteranceId && tts.isSpeaking.value

    OutlinedButton(
        onClick = { tts.speak(text, lang, utteranceId) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (isCurrent) "⏹" else "🔊", fontSize = 14.sp)
            Text(
                text = if (isCurrent) t("stop_reading") else t("read_aloud"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Full-featured TTS Player control bar with speed picker (0.8x, 1.0x, 1.25x)
 * and active reading indicator for Islamic lessons and reflections.
 */
@Composable
fun TtsPlayerBar(
    text: String,
    lang: String = "en",
    utteranceId: String,
    modifier: Modifier = Modifier,
    tts: TextToSpeechManager = rememberTtsManager()
) {
    val isCurrent = tts.currentUtteranceId.value == utteranceId && tts.isSpeaking.value
    val speeds = listOf(0.8f, 1.0f, 1.25f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = { tts.speak(text, lang, utteranceId) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isCurrent) "⏹" else "▶", color = Color.White, fontSize = 14.sp)
                }

                Column {
                    Text(
                        text = if (isCurrent) t("read_aloud") + " (Playing)" else t("read_aloud"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tts.speechRate.floatValue}x " + t("speed"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Speed selector pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                speeds.forEach { spd ->
                    val sel = (tts.speechRate.floatValue == spd)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { tts.setSpeed(spd) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${spd}x",
                            fontSize = 10.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
