package hu.merenyimiklos.meterreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.ui.theme.PlayColors

fun playMeterColor(type: MeterType): Color = when (type) {
    MeterType.ELECTRICITY -> PlayColors.Electricity
    MeterType.ELECTRICITY_NIGHT -> PlayColors.NightElectricity
    MeterType.GAS -> PlayColors.Gas
    MeterType.WATER -> PlayColors.Water
    MeterType.WATER_HOT -> PlayColors.Gas
}

fun playMeterIcon(type: MeterType): ImageVector = when (type) {
    MeterType.ELECTRICITY -> Icons.Default.Bolt
    MeterType.ELECTRICITY_NIGHT -> Icons.Default.Bedtime
    MeterType.GAS -> Icons.Default.LocalFireDepartment
    MeterType.WATER, MeterType.WATER_HOT -> Icons.Default.WaterDrop
}

/** Branded, recognisable meter icon shared across dashboard, input and history. */
@Composable
fun PlayMeterIcon(
    type: MeterType,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    val color = playMeterColor(type)
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(color.copy(alpha = 0.91f), color)
                ),
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = playMeterIcon(type),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
fun PlaySectionHeading(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlayMiniBars(
    color: Color,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val max = values.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Box(
                modifier = Modifier
                    .size(width = 7.dp, height = (9 + 24 * (value / max)).dp)
                    .background(
                        color.copy(alpha = if (index == values.lastIndex) 1f else 0.45f),
                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                    )
            )
        }
    }
}
