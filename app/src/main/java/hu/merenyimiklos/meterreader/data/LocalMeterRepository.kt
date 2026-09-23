package hu.merenyimiklos.meterreader.data

import android.content.Context
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalMeterRepository(context: Context) {
    private val preferences = context.getSharedPreferences("meter_reader_store", Context.MODE_PRIVATE)
    private val _readings = MutableStateFlow(loadReadings())
    val readings: StateFlow<List<MeterReading>> = _readings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BillingSettings> = _settings

    @Synchronized
    fun addReading(type: MeterType, value: Double, dateEpochDay: Long, note: String, photoUri: String?) {
        val updated = _readings.value + MeterReading(
            id = UUID.randomUUID().toString(),
            type = type,
            value = value,
            dateEpochDay = dateEpochDay,
            note = note.trim(),
            photoUri = photoUri
        )
        persistReadings(updated)
    }

    @Synchronized
    fun deleteReading(id: String) {
        persistReadings(_readings.value.filterNot { it.id == id })
    }

    @Synchronized
    fun updateSettings(settings: BillingSettings) {
        preferences.edit().putString(KEY_SETTINGS, settingsToJson(settings).toString()).apply()
        _settings.value = settings
    }

    private fun persistReadings(readings: List<MeterReading>) {
        val array = JSONArray()
        readings.sortedBy { it.dateEpochDay }.forEach { reading ->
            array.put(
                JSONObject()
                    .put("id", reading.id)
                    .put("type", reading.type.name)
                    .put("value", reading.value)
                    .put("dateEpochDay", reading.dateEpochDay)
                    .put("note", reading.note)
                    .put("photoUri", reading.photoUri ?: "")
                    .put("createdAtMillis", reading.createdAtMillis)
            )
        }
        preferences.edit().putString(KEY_READINGS, array.toString()).apply()
        _readings.value = readings.sortedBy { it.dateEpochDay }
    }

    private fun loadReadings(): List<MeterReading> {
        val raw = preferences.getString(KEY_READINGS, null) ?: return emptyList()
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
            }.sortedBy { it.dateEpochDay }
        }.getOrDefault(emptyList())
    }

    private fun loadSettings(): BillingSettings {
        val raw = preferences.getString(KEY_SETTINGS, null) ?: return BillingSettings()
        return runCatching {
            val item = JSONObject(raw)
            BillingSettings(
                electricityUnitPrice = item.optDouble("electricityUnitPrice", 0.0),
                electricityMonthlyFixedFee = item.optDouble("electricityMonthlyFixedFee", 0.0),
                gasBillingMode = runCatching {
                    GasBillingMode.valueOf(item.optString("gasBillingMode", GasBillingMode.FLAT_RATE.name))
                }.getOrDefault(GasBillingMode.FLAT_RATE),
                gasUnitPrice = item.optDouble("gasUnitPrice", 0.0),
                gasMonthlyFixedFee = item.optDouble("gasMonthlyFixedFee", 0.0),
                gasFlatMonthlyPayment = item.optDouble("gasFlatMonthlyPayment", 0.0)
            )
        }.getOrDefault(BillingSettings())
    }

    private fun settingsToJson(settings: BillingSettings) = JSONObject()
        .put("electricityUnitPrice", settings.electricityUnitPrice)
        .put("electricityMonthlyFixedFee", settings.electricityMonthlyFixedFee)
        .put("gasBillingMode", settings.gasBillingMode.name)
        .put("gasUnitPrice", settings.gasUnitPrice)
        .put("gasMonthlyFixedFee", settings.gasMonthlyFixedFee)
        .put("gasFlatMonthlyPayment", settings.gasFlatMonthlyPayment)

    private companion object {
        const val KEY_READINGS = "readings"
        const val KEY_SETTINGS = "settings"
    }
}
