package hu.merenyimiklos.meterreader.data

import android.content.Context
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.json.JSONArray
import org.json.JSONObject

class LegacyDataMigrator(
    private val context: Context,
    private val meterRepository: MeterRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun migrateIfNeeded() {
        val state = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (state.getBoolean(KEY_COMPLETED, false)) return

        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

        if (meterRepository.count() == 0) {
            parseReadings(legacy.getString(KEY_READINGS, null))?.let { readings ->
                if (readings.isNotEmpty()) meterRepository.replaceAll(readings)
            }
        }

        parseSettings(legacy.getString(KEY_SETTINGS, null))?.let { settings ->
            settingsRepository.update(settings)
        }

        state.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private fun parseReadings(raw: String?): List<MeterReading>? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        MeterReading(
                            id = item.getString("id"),
                            type = MeterType.valueOf(item.getString("type")),
                            value = item.getDouble("value"),
                            dateEpochDay = item.getLong("dateEpochDay"),
                            note = item.optString("note", ""),
                            photoUri = item.optString("photoUri", "").takeIf { it.isNotBlank() },
                            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrNull()
    }

    private fun parseSettings(raw: String?): BillingSettings? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val item = JSONObject(raw)
            BillingSettings(
                electricityUnitPrice = item.optDouble("electricityUnitPrice", 0.0),
                electricityMonthlyFixedFee = item.optDouble("electricityMonthlyFixedFee", 0.0),
                gasBillingMode = runCatching {
                    GasBillingMode.valueOf(
                        item.optString("gasBillingMode", GasBillingMode.FLAT_RATE.name)
                    )
                }.getOrDefault(GasBillingMode.FLAT_RATE),
                gasUnitPrice = item.optDouble("gasUnitPrice", 0.0),
                gasMonthlyFixedFee = item.optDouble("gasMonthlyFixedFee", 0.0),
                gasFlatMonthlyPayment = item.optDouble("gasFlatMonthlyPayment", 0.0)
            )
        }.getOrNull()
    }

    private companion object {
        const val LEGACY_PREFS = "meter_reader_store"
        const val MIGRATION_PREFS = "meter_reader_migration"
        const val KEY_COMPLETED = "room_datastore_migration_completed"
        const val KEY_READINGS = "readings"
        const val KEY_SETTINGS = "settings"
    }
}
