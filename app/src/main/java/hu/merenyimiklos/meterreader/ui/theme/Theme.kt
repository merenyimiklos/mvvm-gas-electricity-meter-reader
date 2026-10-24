package hu.merenyimiklos.meterreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.merenyimiklos.meterreader.model.ThemeMode

/**
 * Play Store screenshots are the source of truth for the visual language.
 * Real meter readings and analytics remain provided by the existing ViewModels.
 */
object PlayColors {
    val Navy = Color(0xFF05285D)
    val DeepBlue = Color(0xFF073F91)
    val Blue = Color(0xFF075DE0)
    val Cyan = Color(0xFF12BDE9)
    val Mint = Color(0xFF21D3B0)
    val Ink = Color(0xFF0A204B)
    val Slate = Color(0xFF60779F)
    val Canvas = Color(0xFFF1F7FF)
    val NightCanvas = Color(0xFF06152C)
    val Electricity = Color(0xFFFFB915)
    val NightElectricity = Color(0xFF1469E9)
    val Gas = Color(0xFF0CBF8D)
    val Water = Color(0xFF0DABD5)
    val Success = Color(0xFF0AAF7D)
}

private val LightColors = lightColorScheme(
    primary = PlayColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBFF),
    onPrimaryContainer = PlayColors.Ink,
    secondary = PlayColors.DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEBFF),
    onSecondaryContainer = PlayColors.Ink,
    tertiary = PlayColors.Gas,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBFAF1),
    onTertiaryContainer = Color(0xFF064F41),
    background = PlayColors.Canvas,
    onBackground = PlayColors.Ink,
    surface = Color.White,
    onSurface = PlayColors.Ink,
    surfaceVariant = Color(0xFFEAF2FC),
    onSurfaceVariant = PlayColors.Slate,
    outline = Color(0xFF8299B9),
    outlineVariant = Color(0xFFD8E5F6),
    error = Color(0xFFB32644),
    onError = Color.White,
    errorContainer = Color(0xFFFFDDE7),
    onErrorContainer = Color(0xFF5E0922)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89C6FF),
    onPrimary = Color(0xFF002955),
    primaryContainer = Color(0xFF134B84),
    onPrimaryContainer = Color(0xFFE3F1FF),
    secondary = Color(0xFF82DAF8),
    onSecondary = Color(0xFF00374B),
    secondaryContainer = Color(0xFF134259),
    onSecondaryContainer = Color(0xFFDCF5FF),
    tertiary = Color(0xFF70E8C7),
    onTertiary = Color(0xFF003A2D),
    tertiaryContainer = Color(0xFF125445),
    onTertiaryContainer = Color(0xFFB9FFE9),
    background = PlayColors.NightCanvas,
    onBackground = Color(0xFFF1F7FF),
    surface = Color(0xFF102545),
    onSurface = Color(0xFFF1F7FF),
    surfaceVariant = Color(0xFF193456),
    onSurfaceVariant = Color(0xFFBED0E8),
    outline = Color(0xFF92A8C4),
    outlineVariant = Color(0xFF2D4668),
    error = Color(0xFFFFB0C2),
    onError = Color(0xFF670027),
    errorContainer = Color(0xFF852743),
    onErrorContainer = Color(0xFFFFE0E7)
)

private val PlayShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val PlayTypography = Typography(
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun MeterReaderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PlayTypography,
        shapes = PlayShapes,
        content = content
    )
}
