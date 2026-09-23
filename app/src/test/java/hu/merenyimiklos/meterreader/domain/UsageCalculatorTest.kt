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

        val result =
            UsageCalculator.consumptionByReading(readings)

        assertEquals(
            125.5,
            result["e2"] ?: 0.0,
            0.001
        )
    }

    @Test
    fun nightElectricityIsTrackedSeparately() {
        val readings = listOf(
            reading(
                id = "n1",
                type = MeterType.ELECTRICITY_NIGHT,
                value = 300.0,
                date = "2026-01-01"
            ),
            reading(
                id = "n2",
                type = MeterType.ELECTRICITY_NIGHT,
                value = 345.0,
                date = "2026-02-01"
            )
        )

        val settings = BillingSettings(
            electricityNightUnitPrice = 25.0,
            electricityNightMonthlyFixedFee = 100.0
        )

        val february =
            UsageCalculator
                .monthlySummaries(readings, settings)
                .first { it.month.monthValue == 2 }

        assertEquals(
            45.0,
            february.electricityNightUsage ?: 0.0,
            0.001
        )
        assertEquals(
            1225.0,
            february.electricityNightCost,
            0.001
        )
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
            gasBillingMode =
                GasBillingMode.FLAT_RATE,
            gasUnitPrice = 150.0,
            gasFlatMonthlyPayment = 12000.0
        )

        val february =
            UsageCalculator
                .monthlySummaries(readings, settings)
                .first { it.month.monthValue == 2 }

        assertEquals(
            50.0,
            february.gasUsage ?: 0.0,
            0.001
        )
        assertEquals(
            7500.0,
            february.gasConsumptionCost,
            0.001
        )
        assertEquals(
            12000.0,
            february.gasPayable,
            0.001
        )
    }

    @Test
    fun totalIncludesBothElectricityMetersAndGas() {
        val readings = listOf(
            reading("e1", MeterType.ELECTRICITY, 1000.0, "2026-01-01"),
            reading("e2", MeterType.ELECTRICITY, 1100.0, "2026-02-01"),
            reading("n1", MeterType.ELECTRICITY_NIGHT, 200.0, "2026-01-01"),
            reading("n2", MeterType.ELECTRICITY_NIGHT, 240.0, "2026-02-01"),
            reading("g1", MeterType.GAS, 500.0, "2026-01-01"),
            reading("g2", MeterType.GAS, 510.0, "2026-02-01")
        )

        val settings = BillingSettings(
            electricityUnitPrice = 40.0,
            electricityNightUnitPrice = 20.0,
            gasBillingMode = GasBillingMode.METERED,
            gasUnitPrice = 100.0
        )

        val february =
            UsageCalculator
                .monthlySummaries(readings, settings)
                .first { it.month.monthValue == 2 }

        assertEquals(
            5800.0,
            february.totalEstimatedPayable,
            0.001
        )
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
        dateEpochDay =
            LocalDate.parse(date).toEpochDay()
    )
}
