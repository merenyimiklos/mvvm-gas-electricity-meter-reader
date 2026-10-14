package hu.merenyimiklos.meterreader.model

import java.time.YearMonth

enum class MeterType(
    val displayName: String,
    val shortName: String,
    val unit: String
) {
    ELECTRICITY("Normál áram", "Normál", "kWh"),
    ELECTRICITY_NIGHT("Éjszakai áram", "Éjszakai", "kWh"),
    GAS("Gáz", "Gáz", "m³");

    val isElectricity: Boolean
        get() = this == ELECTRICITY || this == ELECTRICITY_NIGHT
}

enum class GasBillingMode(val displayName: String) {
    FLAT_RATE("Átalánydíj"),
    METERED("Mérőállás alapján")
}

data class MeterReading(
    val id: String,
    val type: MeterType,
    val value: Double,
    val dateEpochDay: Long,
    val note: String = "",
    val photoUri: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class BillingSettings(
    val electricityUnitPrice: Double = 0.0,
    val electricityMonthlyFixedFee: Double = 0.0,
    val electricityNightUnitPrice: Double = 0.0,
    val electricityNightMonthlyFixedFee: Double = 0.0,
    val electricityMonthlyGoalKwh: Double = 0.0,
    val electricityNightMonthlyGoalKwh: Double = 0.0,
    val gasMonthlyGoalM3: Double = 0.0,
    val gasHeatingValueMjPerM3: Double = 34.8,
    val reminderEnabled: Boolean = true,
    val backupFolderUri: String = "",
    val gasBillingMode: GasBillingMode = GasBillingMode.FLAT_RATE,
    val gasUnitPrice: Double = 0.0,
    val gasMonthlyFixedFee: Double = 0.0,
    val gasFlatMonthlyPayment: Double = 0.0
)

data class MonthlySummary(
    val month: YearMonth,
    val electricityUsage: Double?,
    val electricityNightUsage: Double?,
    val gasUsage: Double?,
    val electricityCost: Double,
    val electricityNightCost: Double,
    val gasConsumptionCost: Double,
    val gasPayable: Double,
    val totalEstimatedPayable: Double
) {
    val totalElectricityUsage: Double
        get() = (electricityUsage ?: 0.0) + (electricityNightUsage ?: 0.0)

    val totalElectricityCost: Double
        get() = electricityCost + electricityNightCost
}

data class OcrResult(
    val detectedValue: Double?,
    val rawText: String
)
