package app.islamiccalendar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AppPalette(
    val id: String,
    val nameKey: String,
    val accent: Color,
    val heroColors: List<Color>,
    val previewColors: List<Color>,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightTertiary: Color,
    val lightBgTop: Color,
    val lightBgBottom: Color,
    val lightSurface: Color = Color.White,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color,
    val darkBgTop: Color,
    val darkBgBottom: Color,
    val darkSurface: Color
) {
    val heroBrush: Brush get() = Brush.linearGradient(heroColors)
    fun bgBrush(dark: Boolean): Brush =
        if (dark) Brush.verticalGradient(listOf(darkBgTop, darkBgBottom))
        else Brush.verticalGradient(listOf(lightBgTop, lightBgBottom))
}

val Palettes = listOf(
    AppPalette(
        id = "classic",
        nameKey = "palette_classic",
        accent = Color(0xFFF2C777),
        heroColors = listOf(Color(0xFF4A3FD8), Color(0xFF5563E8), Color(0xFF1FA89B)),
        previewColors = listOf(Color(0xFF4A3FD8), Color(0xFF1FA89B), Color(0xFFF2C777)),
        lightPrimary = Color(0xFF1FA89B),
        lightSecondary = Color(0xFF5563E8),
        lightTertiary = Color(0xFFC9972F),
        lightBgTop = Color(0xFFEFF4FF),
        lightBgBottom = Color(0xFFF3FBF9),
        darkPrimary = Color(0xFF3CCFC0),
        darkSecondary = Color(0xFF7C89FF),
        darkTertiary = Color(0xFFF2C777),
        darkBgTop = Color(0xFF0E1726),
        darkBgBottom = Color(0xFF14233B),
        darkSurface = Color(0xFF16233A)
    ),
    AppPalette(
        id = "emerald",
        nameKey = "palette_emerald",
        accent = Color(0xFFFCD34D),
        heroColors = listOf(Color(0xFF0B533C), Color(0xFF0F8A5F), Color(0xFF25A173)),
        previewColors = listOf(Color(0xFF0B533C), Color(0xFF0F8A5F), Color(0xFFFCD34D)),
        lightPrimary = Color(0xFF0F8A5F),
        lightSecondary = Color(0xFF1B6B53),
        lightTertiary = Color(0xFFD4A017),
        lightBgTop = Color(0xFFEFF9F4),
        lightBgBottom = Color(0xFFF2FAF7),
        darkPrimary = Color(0xFF2DD498),
        darkSecondary = Color(0xFF5EEAD4),
        darkTertiary = Color(0xFFFCD34D),
        darkBgTop = Color(0xFF0A1713),
        darkBgBottom = Color(0xFF12241E),
        darkSurface = Color(0xFF142B23)
    ),
    AppPalette(
        id = "sapphire",
        nameKey = "palette_sapphire",
        accent = Color(0xFFFDE047),
        heroColors = listOf(Color(0xFF1E1B4B), Color(0xFF1D4ED8), Color(0xFF0284C7)),
        previewColors = listOf(Color(0xFF1E1B4B), Color(0xFF1D4ED8), Color(0xFFFDE047)),
        lightPrimary = Color(0xFF1D4ED8),
        lightSecondary = Color(0xFF4338CA),
        lightTertiary = Color(0xFFD97706),
        lightBgTop = Color(0xFFEFF6FF),
        lightBgBottom = Color(0xFFF0FDF4),
        darkPrimary = Color(0xFF60A5FA),
        darkSecondary = Color(0xFF818CF8),
        darkTertiary = Color(0xFFFDE047),
        darkBgTop = Color(0xFF0A1020),
        darkBgBottom = Color(0xFF121B32),
        darkSurface = Color(0xFF14203B)
    ),
    AppPalette(
        id = "desert",
        nameKey = "palette_desert",
        accent = Color(0xFFFDE68A),
        heroColors = listOf(Color(0xFF881337), Color(0xFFC2410C), Color(0xFFD97706)),
        previewColors = listOf(Color(0xFF881337), Color(0xFFC2410C), Color(0xFFFDE68A)),
        lightPrimary = Color(0xFFC2410C),
        lightSecondary = Color(0xFF9A3412),
        lightTertiary = Color(0xFFD97706),
        lightBgTop = Color(0xFFFFF7ED),
        lightBgBottom = Color(0xFFFEF2F2),
        darkPrimary = Color(0xFFFB923C),
        darkSecondary = Color(0xFFF59E0B),
        darkTertiary = Color(0xFFFCD34D),
        darkBgTop = Color(0xFF180F0D),
        darkBgBottom = Color(0xFF261814),
        darkSurface = Color(0xFF2C1C17)
    ),
    AppPalette(
        id = "twilight",
        nameKey = "palette_twilight",
        accent = Color(0xFFFDE047),
        heroColors = listOf(Color(0xFF3B0764), Color(0xFF6B21A8), Color(0xFF9333EA)),
        previewColors = listOf(Color(0xFF3B0764), Color(0xFF7E22CE), Color(0xFFFDE047)),
        lightPrimary = Color(0xFF7E22CE),
        lightSecondary = Color(0xFF6B21A8),
        lightTertiary = Color(0xFFD97706),
        lightBgTop = Color(0xFFFAF5FF),
        lightBgBottom = Color(0xFFF3E8FF),
        darkPrimary = Color(0xFFA855F7),
        darkSecondary = Color(0xFFC084FC),
        darkTertiary = Color(0xFFFBBF24),
        darkBgTop = Color(0xFF130920),
        darkBgBottom = Color(0xFF201133),
        darkSurface = Color(0xFF26153D)
    )
)

val PalettesMap: Map<String, AppPalette> = Palettes.associateBy { it.id }

val LocalPalette = compositionLocalOf { Palettes[0] }

val heroBrush: Brush @Composable get() = LocalPalette.current.heroBrush
val Accent: Color @Composable get() = LocalPalette.current.accent

@Composable fun bgBrush(): Brush = LocalPalette.current.bgBrush(LocalDark.current)

@Composable fun AppTheme(dark: Boolean, palette: AppPalette = LocalPalette.current, content: @Composable () -> Unit) {
    val cs = if (dark) darkColorScheme(
        primary = palette.darkPrimary,
        secondary = palette.darkSecondary,
        tertiary = palette.darkTertiary,
        background = palette.darkBgTop,
        surface = palette.darkSurface,
        onPrimary = palette.darkBgTop,
        onBackground = Color(0xFFE8EEF8),
        onSurface = Color(0xFFE8EEF8)
    ) else lightColorScheme(
        primary = palette.lightPrimary,
        secondary = palette.lightSecondary,
        tertiary = palette.lightTertiary,
        background = palette.lightBgTop,
        surface = palette.lightSurface
    )
    MaterialTheme(
        colorScheme = cs,
        shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content
    )
}
