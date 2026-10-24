package hu.merenyimiklos.meterreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hu.merenyimiklos.meterreader.model.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF0A6E67),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC0F3E8),
        onPrimaryContainer = Color(0xFF00201D),

        secondary = Color(0xFF315FA8),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD9E5FF),
        onSecondaryContainer = Color(0xFF001B3F),

        tertiary = Color(0xFF0C8F79),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFA7F2DE),
        onTertiaryContainer = Color(0xFF00201A),

        background = Color(0xFFF6F9FC),
        onBackground = Color(0xFF172026),
        surface = Color(0xFFF8FAFC),
        onSurface = Color(0xFF172026),
        surfaceVariant = Color(0xFFE1E8EC),
        onSurfaceVariant = Color(0xFF445159),

        outline = Color(0xFF748188),
        outlineVariant = Color(0xFFC4CDD2),

        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF67D9CA),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF005048),
        onPrimaryContainer = Color(0xFFB8F3E8),

        secondary = Color(0xFFACC8FF),
        onSecondary = Color(0xFF002F65),
        secondaryContainer = Color(0xFF234979),
        onSecondaryContainer = Color(0xFFD9E5FF),

        tertiary = Color(0xFF58DCC1),
        onTertiary = Color(0xFF00382F),
        tertiaryContainer = Color(0xFF005044),
        onTertiaryContainer = Color(0xFFA7F2DE),

        background = Color(0xFF07131F),
        onBackground = Color(0xFFE5EEF5),
        surface = Color(0xFF0B1827),
        onSurface = Color(0xFFE5EEF5),
        surfaceVariant = Color(0xFF24323E),
        onSurfaceVariant = Color(0xFFC2CCD4),

        outline = Color(0xFF8C9AA4),
        outlineVariant = Color(0xFF3A4854),

        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )

private val AppShapes =
    Shapes(
        extraSmall =
            RoundedCornerShape(10.dp),
        small =
            RoundedCornerShape(14.dp),
        medium =
            RoundedCornerShape(20.dp),
        large =
            RoundedCornerShape(28.dp),
        extraLarge =
            RoundedCornerShape(36.dp)
    )

@Composable
fun MeterReaderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM ->
                isSystemInDarkTheme()

            ThemeMode.LIGHT ->
                false

            ThemeMode.DARK ->
                true
        }

    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColors
            } else {
                LightColors
            },
        shapes = AppShapes,
        content = content
    )
}
