package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsCalculatorTest {

    @Test
    fun changePercentAndThreeMonthAverageAreCalculated() {
        val insights = AnalyticsCalculator.analyze(
            listOf(
                YearMonth.of(2026, 1) to 100.0,
                YearMonth.of(2026, 2) to 120.0,
                YearMonth.of(2026, 3) to 90.0
            )
        )

        assertEquals(90.0, insights.latestValue ?: 0.0, 0.001)
        assertEquals(-25.0, insights.changePercent ?: 0.0, 0.001)
        assertEquals(
            103.333,
            insights.threeMonthAverage ?: 0.0,
            0.01
        )
    }

    @Test
    fun usageAbovePreviousAverageByTwentyFivePercentIsOutlier() {
        val insights = AnalyticsCalculator.analyze(
            listOf(
                YearMonth.of(2026, 1) to 100.0,
                YearMonth.of(2026, 2) to 100.0,
                YearMonth.of(2026, 3) to 100.0,
                YearMonth.of(2026, 4) to 140.0
            )
        )

        assertFalse(insights.points[2].isOutlier)
        assertTrue(insights.points[3].isOutlier)
    }

    @Test
    fun dailyAverageUsesDaysBetweenLatestTwoReadings() {
        val readings = listOf(
            reading(
                id = "a",
                value = 1000.0,
                date = "2026-09-01"
            ),
            reading(
                id = "b",
                value = 1110.0,
                date = "2026-09-11"
            )
        )

        val average = AnalyticsCalculator.latestDailyAverage(
            readings,
            MeterType.ELECTRICITY
        )

        assertEquals(11.0, average ?: 0.0, 0.001)
    }

    private fun reading(
        id: String,
        value: Double,
        date: String
    ) = MeterReading(
        id = id,
        type = MeterType.ELECTRICITY,
        value = value,
        dateEpochDay = LocalDate.parse(date).toEpochDay()
    )
}
