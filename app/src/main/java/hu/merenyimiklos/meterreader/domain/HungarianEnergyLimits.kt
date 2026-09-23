package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class EnergyLimitStatus(
    val used: Double,
    val yearlyLimit: Double,
    val allowedToDate: Double,
    val remainingYearly: Double,
    val remainingToDate: Double,
    val projectedYearly: Double?,
    val usageRatio: Double
)

object HungarianEnergyLimits {
    const val ELECTRICITY_YEARLY_KWH = 2523.0
    const val GAS_YEARLY_MJ = 63645.0
    const val REFERENCE_GAS_M3 = 1729.0

    private val gasMonthlyMj = mapOf(
        Month.JANUARY to 12365.0,
        Month.FEBRUARY to 10421.0,
        Month.MARCH to 8915.0,
        Month.APRIL to 5145.0,
        Month.MAY to 1827.0,
        Month.JUNE to 635.0,
        Month.JULY to 512.0,
        Month.AUGUST to 565.0,
        Month.SEPTEMBER to 1109.0,
        Month.OCTOBER to 3724.0,
        Month.NOVEMBER to 7490.0,
        Month.DECEMBER to 10937.0
    )

    fun electricityStatus(
        readings: List<MeterReading>,
        type: MeterType,
        asOf: LocalDate = LocalDate.now()
    ): EnergyLimitStatus {
        require(type.isElectricity)

        val period = discountPeriod(asOf)
        val used = consumptionInPeriod(
            readings = readings,
            type = type,
            start = period.first,
            end = asOf
        )

        val totalDays =
            ChronoUnit.DAYS.between(
                period.first,
                period.second.plusDays(1)
            ).toDouble()

        val elapsedDays =
            ChronoUnit.DAYS.between(
                period.first,
                asOf.plusDays(1)
            )
                .coerceAtLeast(1)
                .toDouble()

        val allowedToDate =
            ELECTRICITY_YEARLY_KWH *
                (elapsedDays / totalDays)

        return createStatus(
            used = used,
            yearlyLimit = ELECTRICITY_YEARLY_KWH,
            allowedToDate = allowedToDate,
            elapsedFraction = elapsedDays / totalDays
        )
    }

    fun gasStatus(
        readings: List<MeterReading>,
        heatingValueMjPerM3: Double,
        asOf: LocalDate = LocalDate.now()
    ): EnergyLimitStatus {
        val safeHeatingValue =
            heatingValueMjPerM3
                .takeIf { it > 0.0 }
                ?: 34.8

        val period = discountPeriod(asOf)
        val usedM3 = consumptionInPeriod(
            readings = readings,
            type = MeterType.GAS,
            start = period.first,
            end = asOf
        )
        val usedMj = usedM3 * safeHeatingValue
        val allowedMj =
            gasAllowedMjToDate(
                periodStart = period.first,
                asOf = asOf
            )

        val allowedFraction =
            (allowedMj / GAS_YEARLY_MJ)
                .coerceIn(0.0001, 1.0)

        val statusMj = createStatus(
            used = usedMj,
            yearlyLimit = GAS_YEARLY_MJ,
            allowedToDate = allowedMj,
            elapsedFraction = allowedFraction
        )

        return EnergyLimitStatus(
            used = statusMj.used / safeHeatingValue,
            yearlyLimit = statusMj.yearlyLimit / safeHeatingValue,
            allowedToDate = statusMj.allowedToDate / safeHeatingValue,
            remainingYearly = statusMj.remainingYearly / safeHeatingValue,
            remainingToDate = statusMj.remainingToDate / safeHeatingValue,
            projectedYearly = statusMj.projectedYearly?.div(safeHeatingValue),
            usageRatio = statusMj.usageRatio
        )
    }

    fun gasMonthlyReferenceM3(
        month: Month,
        heatingValueMjPerM3: Double
    ): Double {
        val heatingValue =
            heatingValueMjPerM3
                .takeIf { it > 0.0 }
                ?: 34.8

        return (gasMonthlyMj[month] ?: 0.0) /
            heatingValue
    }

    fun discountPeriod(
        date: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val startYear =
            if (date.monthValue >= 8) {
                date.year
            } else {
                date.year - 1
            }

        return LocalDate.of(startYear, 8, 1) to
            LocalDate.of(startYear + 1, 7, 31)
    }

    private fun consumptionInPeriod(
        readings: List<MeterReading>,
        type: MeterType,
        start: LocalDate,
        end: LocalDate
    ): Double {
        val consumption =
            UsageCalculator.consumptionByReading(readings)

        return readings
            .asSequence()
            .filter { it.type == type }
            .filter {
                val date =
                    LocalDate.ofEpochDay(it.dateEpochDay)
                !date.isBefore(start) &&
                    !date.isAfter(end)
            }
            .sumOf {
                consumption[it.id] ?: 0.0
            }
    }

    private fun gasAllowedMjToDate(
        periodStart: LocalDate,
        asOf: LocalDate
    ): Double {
        var cursor = YearMonth.from(periodStart)
        val end = YearMonth.from(asOf)
        var total = 0.0

        while (!cursor.isAfter(end)) {
            val monthTotal =
                gasMonthlyMj[cursor.month] ?: 0.0

            total +=
                if (cursor == end) {
                    monthTotal *
                        (
                            asOf.dayOfMonth.toDouble() /
                                cursor.lengthOfMonth().toDouble()
                            )
                } else {
                    monthTotal
                }

            cursor = cursor.plusMonths(1)
        }

        return total.coerceAtMost(GAS_YEARLY_MJ)
    }

    private fun createStatus(
        used: Double,
        yearlyLimit: Double,
        allowedToDate: Double,
        elapsedFraction: Double
    ): EnergyLimitStatus {
        val projected =
            if (used > 0.0 && elapsedFraction > 0.0) {
                used / elapsedFraction
            } else {
                null
            }

        return EnergyLimitStatus(
            used = used,
            yearlyLimit = yearlyLimit,
            allowedToDate = allowedToDate,
            remainingYearly =
                (yearlyLimit - used).coerceAtLeast(0.0),
            remainingToDate = allowedToDate - used,
            projectedYearly = projected,
            usageRatio =
                if (yearlyLimit > 0.0) {
                    used / yearlyLimit
                } else {
                    0.0
                }
        )
    }
}
