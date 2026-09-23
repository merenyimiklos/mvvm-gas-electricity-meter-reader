package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.MonthlySummary
import java.time.LocalDate
import java.time.YearMonth

object UsageCalculator {
    fun consumptionByReading(readings: List<MeterReading>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        MeterType.entries.forEach { type ->
            val typedReadings = readings.filter { it.type == type }
                .sortedWith(compareBy<MeterReading> { it.dateEpochDay }.thenBy { it.createdAtMillis })
            typedReadings.zipWithNext().forEach { (previous, current) ->
                val difference = current.value - previous.value
                if (difference >= 0.0) result[current.id] = difference
            }
        }
        return result
    }

    fun monthlySummaries(readings: List<MeterReading>, settings: BillingSettings): List<MonthlySummary> {
        if (readings.isEmpty()) return emptyList()
        val usageById = consumptionByReading(readings)
        val usageByMonth = mutableMapOf<YearMonth, MutableMap<MeterType, Double>>()

        readings.forEach { reading ->
            val usage = usageById[reading.id] ?: return@forEach
            val month = YearMonth.from(LocalDate.ofEpochDay(reading.dateEpochDay))
            val values = usageByMonth.getOrPut(month) { mutableMapOf() }
            values[reading.type] = (values[reading.type] ?: 0.0) + usage
        }

        val months = readings.map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }.distinct().sorted()
        return months.map { month ->
            val electricityUsage = usageByMonth[month]?.get(MeterType.ELECTRICITY)
            val gasUsage = usageByMonth[month]?.get(MeterType.GAS)
            val electricityCost = electricityUsage?.let {
                it * settings.electricityUnitPrice + settings.electricityMonthlyFixedFee
            } ?: 0.0
            val gasConsumptionCost = gasUsage?.let {
                it * settings.gasUnitPrice + settings.gasMonthlyFixedFee
            } ?: 0.0
            val gasPayable = when (settings.gasBillingMode) {
                GasBillingMode.FLAT_RATE -> settings.gasFlatMonthlyPayment
                GasBillingMode.METERED -> gasConsumptionCost
            }
            MonthlySummary(
                month = month,
                electricityUsage = electricityUsage,
                gasUsage = gasUsage,
                electricityCost = electricityCost,
                gasConsumptionCost = gasConsumptionCost,
                gasPayable = gasPayable,
                totalEstimatedPayable = electricityCost + gasPayable
            )
        }
    }

    fun estimatedPayableForReading(
        reading: MeterReading,
        consumption: Double?,
        settings: BillingSettings
    ): Pair<Double?, Double?> = when (reading.type) {
        MeterType.ELECTRICITY -> {
            val estimated = consumption?.let {
                it * settings.electricityUnitPrice + settings.electricityMonthlyFixedFee
            }
            estimated to estimated
        }
        MeterType.GAS -> {
            val consumptionCost = consumption?.let {
                it * settings.gasUnitPrice + settings.gasMonthlyFixedFee
            }
            val payable = when (settings.gasBillingMode) {
                GasBillingMode.FLAT_RATE -> settings.gasFlatMonthlyPayment
                GasBillingMode.METERED -> consumptionCost
            }
            consumptionCost to payable
        }
    }
}
