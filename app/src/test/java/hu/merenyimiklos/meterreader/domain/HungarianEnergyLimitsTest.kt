package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HungarianEnergyLimitsTest {

    @Test
    fun electricityHasSeparate2523KwhAnnualLimit() {
        val status =
            HungarianEnergyLimits
                .electricityStatus(
                    readings = emptyList(),
                    type =
                        MeterType.ELECTRICITY,
                    asOf =
                        LocalDate.of(
                            2026,
                            8,
                            1
                        )
                )

        assertEquals(
            2523.0,
            status.yearlyLimit,
            0.001
        )
        assertTrue(
            status.allowedToDate >
                6.0
        )
        assertTrue(
            status.allowedToDate <
                8.0
        )
    }

    @Test
    fun gasJanuaryReferenceIsMuchHigherThanJuly() {
        val january =
            HungarianEnergyLimits
                .gasMonthlyReferenceM3(
                    java.time.Month.JANUARY,
                    34.8
                )
        val july =
            HungarianEnergyLimits
                .gasMonthlyReferenceM3(
                    java.time.Month.JULY,
                    34.8
                )

        assertTrue(
            january > july * 20
        )
    }

    @Test
    fun usedElectricityIsCalculatedFromReadingDifferences() {
        val readings =
            listOf(
                reading(
                    "a",
                    1000.0,
                    "2026-08-01"
                ),
                reading(
                    "b",
                    1120.0,
                    "2026-09-01"
                )
            )

        val status =
            HungarianEnergyLimits
                .electricityStatus(
                    readings =
                        readings,
                    type =
                        MeterType.ELECTRICITY,
                    asOf =
                        LocalDate.of(
                            2026,
                            9,
                            23
                        )
                )

        assertEquals(
            120.0,
            status.used,
            0.001
        )
    }

    private fun reading(
        id: String,
        value: Double,
        date: String
    ) = MeterReading(
        id = id,
        type =
            MeterType.ELECTRICITY,
        value = value,
        dateEpochDay =
            LocalDate.parse(
                date
            ).toEpochDay()
    )
}
