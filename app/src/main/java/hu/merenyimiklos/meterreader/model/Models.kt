package hu.merenyimiklos.meterreader.model

import java.time.YearMonth

enum class MeterType(val displayName: String, val unit: String) {
    ELECTRICITY("Áram", "kWh"),
    GAS("Gáz", "m³")
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
    val gasBillingMode: GasBillingMode = GasBillingMode.FLAT_RATE,
    val gasUnitPrice: Double = 0.0,
    val gasMonthlyFixedFee: Double = 0.0,
    val gasFlatMonthlyPayment: Double = 0.0
)

data class MonthlySummary(
    val month: YearMonth,
    val electricityUsage: Double?,
    val gasUsage: Double?,
    val electricityCost: Double,
    val gasConsumptionCost: Double,
    val gasPayable: Double,
    val totalEstimatedPayable: Double
)

data class OcrResult(
    val detectedValue: Double?,
    val rawText: String
)
