package hu.merenyimiklos.meterreader.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

internal fun parseDecimal(value: String): Double =
    value.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

internal fun editableNumber(value: Double): String {
    if (value == 0.0) return ""
    val long = value.toLong()
    return if (value == long.toDouble()) long.toString() else value.toString()
}

internal fun formatDecimal(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("hu", "HU")).apply {
        maximumFractionDigits = 3
        minimumFractionDigits = 0
    }
    return formatter.format(value)
}

internal fun formatCompact(value: Double): String =
    if (value >= 1000) formatDecimal(value / 1000.0) + "k" else formatDecimal(value)

internal fun formatMoney(value: Double): String {
    val formatter = NumberFormat.getIntegerInstance(Locale("hu", "HU"))
    return formatter.format(value.roundToLong()) + " Ft"
}
