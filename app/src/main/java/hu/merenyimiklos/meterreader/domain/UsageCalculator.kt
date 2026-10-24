package hu.merenyimiklos.meterreader.domain

import hu.merenyimiklos.meterreader.model.BillingMode
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.MonthlySummary
import java.time.LocalDate
import java.time.YearMonth

object UsageCalculator {
    fun consumptionByReading(
        readings: List<MeterReading>
    ): Map<String, Double> {
        val result =
            mutableMapOf<String, Double>()

        MeterType.entries.forEach { type ->
            val typedReadings =
                readings
                    .filter {
                        it.type == type
                    }
                    .sortedWith(
                        compareBy<MeterReading> {
                            it.dateEpochDay
                        }.thenBy {
                            it.createdAtMillis
                        }
                    )

            typedReadings.zipWithNext()
                .forEach { pair ->
                    val previous =
                        pair.first
                    val current =
                        pair.second
                    val difference =
                        current.value -
                            previous.value

                    if (difference >= 0.0) {
                        result[current.id] =
                            difference
                    }
                }
        }

        return result
    }

    fun monthlySummaries(
        readings: List<MeterReading>,
        settings: BillingSettings
    ): List<MonthlySummary> {
        if (readings.isEmpty()) {
            return emptyList()
        }

        val usageById =
            consumptionByReading(readings)
        val usageByMonth =
            mutableMapOf<
                YearMonth,
                MutableMap<MeterType, Double>
            >()

        readings.forEach { reading ->
            val usage =
                usageById[reading.id]
                    ?: return@forEach
            val month =
                YearMonth.from(
                    LocalDate.ofEpochDay(
                        reading.dateEpochDay
                    )
                )
            val values =
                usageByMonth.getOrPut(
                    month
                ) {
                    mutableMapOf()
                }

            values[reading.type] =
                (values[reading.type] ?: 0.0) +
                    usage
        }

        val months =
            readings
                .map {
                    YearMonth.from(
                        LocalDate.ofEpochDay(
                            it.dateEpochDay
                        )
                    )
                }
                .distinct()
                .sorted()

        return months.map { month ->
            val normalUsage =
                usageByMonth[month]
                    ?.get(
                        MeterType.ELECTRICITY
                    )
            val nightUsage =
                usageByMonth[month]
                    ?.get(
                        MeterType.ELECTRICITY_NIGHT
                    )
            val gasUsage =
                usageByMonth[month]
                    ?.get(
                        MeterType.GAS
                    )
            val waterUsage =
                usageByMonth[month]
                    ?.get(
                        MeterType.WATER
                    )

            val normalCost =
                if (
                    settings.electricityEnabled
                ) {
                    normalUsage?.let {
                        it *
                            settings
                                .electricityUnitPrice +
                            settings
                                .electricityMonthlyFixedFee
                    } ?: 0.0
                } else {
                    0.0
                }

            val nightCost =
                if (
                    settings
                        .electricityNightEnabled
                ) {
                    nightUsage?.let {
                        it *
                            settings
                                .electricityNightUnitPrice +
                            settings
                                .electricityNightMonthlyFixedFee
                    } ?: 0.0
                } else {
                    0.0
                }

            val gasConsumptionCost =
                if (settings.gasEnabled) {
                    gasUsage?.let {
                        it *
                            settings.gasUnitPrice +
                            settings
                                .gasMonthlyFixedFee
                    } ?: 0.0
                } else {
                    0.0
                }

            val gasPayable =
                if (settings.gasEnabled) {
                    when (
                        settings
                            .gasBillingMode
                    ) {
                        BillingMode.FLAT_RATE ->
                            settings
                                .gasFlatMonthlyPayment

                        BillingMode.METERED ->
                            gasConsumptionCost
                    }
                } else {
                    0.0
                }

            val waterConsumptionCost =
                if (settings.waterEnabled) {
                    waterUsage?.let {
                        it *
                            settings.waterUnitPrice +
                            settings
                                .waterMonthlyFixedFee
                    } ?: 0.0
                } else {
                    0.0
                }

            val waterPayable =
                if (
                    settings.waterEnabled
                ) {
                    when (
                        settings
                            .waterBillingMode
                    ) {
                        BillingMode.FLAT_RATE ->
                            settings
                                .waterFlatMonthlyPayment

                        BillingMode.METERED ->
                            waterConsumptionCost
                    }
                } else {
                    0.0
                }

            MonthlySummary(
                month = month,
                electricityUsage =
                    normalUsage,
                electricityNightUsage =
                    nightUsage,
                gasUsage = gasUsage,
                waterUsage = waterUsage,
                electricityCost =
                    normalCost,
                electricityNightCost =
                    nightCost,
                gasConsumptionCost =
                    gasConsumptionCost,
                gasPayable =
                    gasPayable,
                waterConsumptionCost =
                    waterConsumptionCost,
                waterPayable =
                    waterPayable,
                totalEstimatedPayable =
                    normalCost +
                        nightCost +
                        gasPayable +
                        waterPayable
            )
        }
    }

    fun estimatedPayableForReading(
        reading: MeterReading,
        consumption: Double?,
        settings: BillingSettings
    ): Pair<Double?, Double?> =
        when (reading.type) {
            MeterType.ELECTRICITY -> {
                val estimated =
                    consumption?.let {
                        it *
                            settings
                                .electricityUnitPrice +
                            settings
                                .electricityMonthlyFixedFee
                    }
                estimated to estimated
            }

            MeterType.ELECTRICITY_NIGHT -> {
                val estimated =
                    consumption?.let {
                        it *
                            settings
                                .electricityNightUnitPrice +
                            settings
                                .electricityNightMonthlyFixedFee
                    }
                estimated to estimated
            }

            MeterType.GAS -> {
                val consumptionCost =
                    consumption?.let {
                        it *
                            settings
                                .gasUnitPrice +
                            settings
                                .gasMonthlyFixedFee
                    }
                val payable =
                    when (
                        settings
                            .gasBillingMode
                    ) {
                        BillingMode.FLAT_RATE ->
                            settings
                                .gasFlatMonthlyPayment

                        BillingMode.METERED ->
                            consumptionCost
                    }
                consumptionCost to payable
            }

            MeterType.WATER -> {
                val consumptionCost =
                    consumption?.let {
                        it *
                            settings
                                .waterUnitPrice +
                            settings
                                .waterMonthlyFixedFee
                    }
                val payable =
                    when (
                        settings
                            .waterBillingMode
                    ) {
                        BillingMode.FLAT_RATE ->
                            settings
                                .waterFlatMonthlyPayment

                        BillingMode.METERED ->
                            consumptionCost
                    }
                consumptionCost to payable
            }
        }
}
