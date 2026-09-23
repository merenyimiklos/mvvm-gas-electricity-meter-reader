package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import java.time.LocalDate
import java.time.YearMonth

data class UsagePoint(
    val month: YearMonth,
    val value: Double,
    val isOutlier: Boolean
)

data class UsageInsights(
    val points: List<UsagePoint>,
    val latestValue: Double?,
    val previousValue: Double?,
    val changePercent: Double?,
    val threeMonthAverage: Double?,
    val minimum: UsagePoint?,
    val maximum: UsagePoint?,
    val latestDailyAverage: Double?
)

object AnalyticsCalculator {
    fun analyze(
        values: List<Pair<YearMonth, Double>>,
        dailyAverage: Double? = null
    ): UsageInsights {
        val ordered = values
            .filter { it.second >= 0.0 }
            .sortedBy { it.first }

        val points = ordered.mapIndexed { index, item ->
            val previousWindow = ordered
                .take(index)
                .takeLast(3)
                .map { it.second }
                .filter { it > 0.0 }

            val baseline = previousWindow
                .takeIf { it.isNotEmpty() }
                ?.average()

            UsagePoint(
                month = item.first,
                value = item.second,
                isOutlier = baseline != null &&
                    baseline > 0.0 &&
                    item.second > baseline * 1.25
            )
        }

        val latest = points.lastOrNull()
        val previous = points.dropLast(1).lastOrNull()
        val changePercent = if (
            latest != null &&
            previous != null &&
            previous.value > 0.0
        ) {
            ((latest.value - previous.value) / previous.value) * 100.0
        } else {
            null
        }

        return UsageInsights(
            points = points,
            latestValue = latest?.value,
            previousValue = previous?.value,
            changePercent = changePercent,
            threeMonthAverage = points
                .takeLast(3)
                .map { it.value }
                .takeIf { it.isNotEmpty() }
                ?.average(),
            minimum = points.minByOrNull { it.value },
            maximum = points.maxByOrNull { it.value },
            latestDailyAverage = dailyAverage
        )
    }

    fun latestDailyAverage(
        readings: List<MeterReading>,
        type: MeterType
    ): Double? {
        val typed = readings
            .filter { it.type == type }
            .sortedWith(
                compareBy<MeterReading> { it.dateEpochDay }
                    .thenBy { it.createdAtMillis }
            )

        if (typed.size < 2) return null

        val previous = typed[typed.lastIndex - 1]
        val latest = typed.last()
        val days = latest.dateEpochDay - previous.dateEpochDay
        val consumption = latest.value - previous.value

        if (days <= 0L || consumption < 0.0) return null

        return consumption / days.toDouble()
    }

    fun latestCombinedElectricityDailyAverage(
        readings: List<MeterReading>
    ): Double? {
        val normal = latestDailyAverage(
            readings,
            MeterType.ELECTRICITY
        )
        val night = latestDailyAverage(
            readings,
            MeterType.ELECTRICITY_NIGHT
        )

        if (normal == null && night == null) return null
        return (normal ?: 0.0) + (night ?: 0.0)
    }
}
