package hu.merenyimiklos.meterreader.model

import java.time.YearMonth

enum class MeterType(
    val displayName: String,
    val shortName: String,
    val unit: String
) {
    ELECTRICITY("Normál áram", "Normál", "kWh"),
    ELECTRICITY_NIGHT("Éjszakai áram", "Éjszakai", "kWh"),
    GAS("Gáz", "Gáz", "m³"),
    WATER("Hideg víz", "Hideg", "m³"),
    WATER_HOT("Meleg víz", "Meleg", "m³");

    val isElectricity: Boolean
        get() =
            this == ELECTRICITY ||
                this == ELECTRICITY_NIGHT
}

enum class BillingMode(
    val displayName: String
) {
    FLAT_RATE("Átalánydíj"),
    METERED("Mérőállás alapján")
}

typealias GasBillingMode = BillingMode

enum class AppLanguage {
    HUNGARIAN,
    ENGLISH
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class MeterReading(
    val id: String,
    val type: MeterType,
    val value: Double,
    val dateEpochDay: Long,
    val note: String = "",
    val photoUri: String? = null,
    val createdAtMillis: Long =
        System.currentTimeMillis()
)

data class BillingSettings(
    val electricityEnabled: Boolean = true,
    val electricityNightEnabled: Boolean = true,
    val gasEnabled: Boolean = true,
    val waterEnabled: Boolean = false,
    val waterHotEnabled: Boolean = false,
    val electricityUnitPrice: Double = 0.0,
    val electricityMonthlyFixedFee: Double = 0.0,
    val electricityNightUnitPrice: Double = 0.0,
    val electricityNightMonthlyFixedFee: Double = 0.0,
    val electricityMonthlyGoalKwh: Double = 0.0,
    val electricityNightMonthlyGoalKwh: Double = 0.0,
    val gasMonthlyGoalM3: Double = 0.0,
    val waterMonthlyGoalM3: Double = 0.0,
    val waterHotMonthlyGoalM3: Double = 0.0,
    val gasHeatingValueMjPerM3: Double = 34.8,
    val reminderEnabled: Boolean = true,
    val reminderDayOfMonth: Int = 23,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val backupFolderUri: String = "",
    val gasBillingMode: BillingMode =
        BillingMode.FLAT_RATE,
    val gasUnitPrice: Double = 0.0,
    val gasMonthlyFixedFee: Double = 0.0,
    val gasFlatMonthlyPayment: Double = 0.0,
    val waterBillingMode: BillingMode =
        BillingMode.METERED,
    val waterUnitPrice: Double = 0.0,
    val waterMonthlyFixedFee: Double = 0.0,
    val waterFlatMonthlyPayment: Double = 0.0,
    val waterHotBillingMode: BillingMode = BillingMode.METERED,
    val waterHotUnitPrice: Double = 0.0,
    val waterHotMonthlyFixedFee: Double = 0.0,
    val waterHotFlatMonthlyPayment: Double = 0.0,
    val appLanguage: AppLanguage =
        AppLanguage.HUNGARIAN,
    val themeMode: ThemeMode =
        ThemeMode.SYSTEM,
    val sharedHomeFolderUri: String = "",
    val sharedHomeId: String = "",
    val sharedHomeName: String = "",
    val sharedDeviceId: String = "",
    val sharedDeviceName: String = ""
) {
    fun enabledMeterTypes(): List<MeterType> =
        buildList {
            if (electricityEnabled) {
                add(MeterType.ELECTRICITY)
            }
            if (electricityNightEnabled) {
                add(MeterType.ELECTRICITY_NIGHT)
            }
            if (gasEnabled) {
                add(MeterType.GAS)
            }
            if (waterEnabled) {
                add(MeterType.WATER)
            }
            if (waterHotEnabled) {
                add(MeterType.WATER_HOT)
            }
        }

    fun isMeterEnabled(
        type: MeterType
    ): Boolean =
        when (type) {
            MeterType.ELECTRICITY ->
                electricityEnabled
            MeterType.ELECTRICITY_NIGHT ->
                electricityNightEnabled
            MeterType.GAS ->
                gasEnabled
            MeterType.WATER ->
                waterEnabled
            MeterType.WATER_HOT ->
                waterHotEnabled
        }
}

data class MonthlySummary(
    val month: YearMonth,
    val electricityUsage: Double?,
    val electricityNightUsage: Double?,
    val gasUsage: Double?,
    val waterUsage: Double?,
    val electricityCost: Double,
    val electricityNightCost: Double,
    val gasConsumptionCost: Double,
    val gasPayable: Double,
    val waterConsumptionCost: Double,
    val waterPayable: Double,
    val totalEstimatedPayable: Double,
    val waterHotUsage: Double? = null,
    val waterHotConsumptionCost: Double = 0.0,
    val waterHotPayable: Double = 0.0
) {
    val totalElectricityUsage: Double
        get() =
            (electricityUsage ?: 0.0) +
                (electricityNightUsage ?: 0.0)

    val totalElectricityCost: Double
        get() =
            electricityCost +
                electricityNightCost
}

data class OcrResult(
    val detectedValue: Double?,
    val rawText: String
)
