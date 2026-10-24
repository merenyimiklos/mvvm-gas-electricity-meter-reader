package hu.merenyimiklos.meterreader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

@Composable
fun brandScreenBrush(): Brush =
    Brush.verticalGradient(
        colors =
            listOf(
                MaterialTheme
                    .colorScheme
                    .primaryContainer
                    .copy(alpha = 0.24f),
                MaterialTheme
                    .colorScheme
                    .background,
                MaterialTheme
                    .colorScheme
                    .background
            )
    )
