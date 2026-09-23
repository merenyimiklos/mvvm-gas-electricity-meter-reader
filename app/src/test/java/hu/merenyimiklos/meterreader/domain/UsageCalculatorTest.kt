package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UsageCalculatorTest {

    @Test
    fun consumptionUsesDifferenceBetweenConsecutiveReadings() {
        val readings = listOf(
            reading(
                id = "e1",
                type = MeterType.ELECTRICITY,
                value = 1000.0,
                date = "2026-01-01"
            ),
            reading(
                id = "e2",
                type = MeterType.ELECTRICITY,
                value = 1125.5,
                date = "2026-02-01"
            )
        )

        val result = UsageCalculator.consumptionByReading(readings)

        assertEquals(125.5, result["e2"] ?: 0.0, 0.001)
    }

    @Test
    fun flatRateGasUsesConfiguredMonthlyPayment() {
        val readings = listOf(
            reading(
                id = "g1",
                type = MeterType.GAS,
                value = 500.0,
                date = "2026-01-01"
            ),
            reading(
                id = "g2",
                type = MeterType.GAS,
                value = 550.0,
                date = "2026-02-01"
            )
        )

        val settings = BillingSettings(
            gasBillingMode = GasBillingMode.FLAT_RATE,
            gasUnitPrice = 150.0,
            gasFlatMonthlyPayment = 12000.0
        )

        val february = UsageCalculator
            .monthlySummaries(readings, settings)
            .first { it.month.monthValue == 2 }

        assertEquals(50.0, february.gasUsage ?: 0.0, 0.001)
        assertEquals(7500.0, february.gasConsumptionCost, 0.001)
        assertEquals(12000.0, february.gasPayable, 0.001)
    }

    @Test
    fun electricityCostIncludesFixedFee() {
        val readings = listOf(
            reading(
                id = "e1",
                type = MeterType.ELECTRICITY,
                value = 2000.0,
                date = "2026-03-01"
            ),
            reading(
                id = "e2",
                type = MeterType.ELECTRICITY,
                value = 2100.0,
                date = "2026-04-01"
            )
        )

        val settings = BillingSettings(
            electricityUnitPrice = 40.0,
            electricityMonthlyFixedFee = 500.0
        )

        val april = UsageCalculator
            .monthlySummaries(readings, settings)
            .first { it.month.monthValue == 4 }

        assertEquals(4500.0, april.electricityCost, 0.001)
    }

    private fun reading(
        id: String,
        type: MeterType,
        value: Double,
        date: String
    ) = MeterReading(
        id = id,
        type = type,
        value = value,
        dateEpochDay = LocalDate.parse(date).toEpochDay()
    )
}
