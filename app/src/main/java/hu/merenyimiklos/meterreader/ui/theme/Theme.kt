package hu.merenyimiklos.meterreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF075E54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F2E6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4B635D),
    secondaryContainer = Color(0xFFCDE8E0),
    tertiary = Color(0xFF4B607C),
    tertiaryContainer = Color(0xFFD3E4FF),
    surface = Color(0xFFF7FAF8),
    surfaceVariant = Color(0xFFDCE5E1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD5CA),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFB8F2E6),
    secondary = Color(0xFFB1CCC4),
    secondaryContainer = Color(0xFF344B45),
    tertiary = Color(0xFFB3C8E8),
    tertiaryContainer = Color(0xFF334863)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MeterReaderTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val colors = when {
        Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S &&
            darkTheme ->
            dynamicDarkColorScheme(context)

        Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)

        darkTheme ->
            DarkColors

        else ->
            LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        content = content
    )
}
