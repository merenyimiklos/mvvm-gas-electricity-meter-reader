package hu.merenyimiklos.meterreader.backup

import android.content.ContentResolver
import android.net.Uri
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class BackupPayload(
    val readings: List<MeterReading>,
    val settings: BillingSettings
)

class BackupManager(
    private val contentResolver: ContentResolver
) {
    fun export(
        uri: Uri,
        readings: List<MeterReading>,
        settings: BillingSettings
    ) {
        val json = JSONObject()
            .put("version", 1)
            .put("exportedAt", Instant.now().toString())
            .put("settings", settingsToJson(settings))
            .put("readings", readingsToJson(readings))
            .toString(2)

        val output = requireNotNull(contentResolver.openOutputStream(uri)) {
            "Nem sikerült megnyitni a mentési fájlt."
        }
        output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    fun import(uri: Uri): BackupPayload {
        val input = requireNotNull(contentResolver.openInputStream(uri)) {
            "Nem sikerült megnyitni a biztonsági mentést."
        }
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        require(root.optInt("version", 0) == 1) {
            "Nem támogatott mentési formátum."
        }

        val settings = settingsFromJson(root.getJSONObject("settings"))
        val readingsArray = root.getJSONArray("readings")
        val readings = buildList {
            for (index in 0 until readingsArray.length()) {
                val item = readingsArray.getJSONObject(index)
                add(
                    MeterReading(
                        id = item.getString("id"),
                        type = MeterType.valueOf(item.getString("type")),
                        value = item.getDouble("value"),
                        dateEpochDay = item.getLong("dateEpochDay"),
                        note = item.optString("note", ""),
                        photoUri = null,
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        return BackupPayload(
            readings = readings,
            settings = settings
        )
    }

    private fun readingsToJson(readings: List<MeterReading>): JSONArray {
        val array = JSONArray()
        readings.forEach { reading ->
            array.put(
                JSONObject()
                    .put("id", reading.id)
                    .put("type", reading.type.name)
                    .put("value", reading.value)
                    .put("dateEpochDay", reading.dateEpochDay)
                    .put("note", reading.note)
                    .put("createdAtMillis", reading.createdAtMillis)
            )
        }
        return array
    }

    private fun settingsToJson(settings: BillingSettings): JSONObject = JSONObject()
        .put("electricityUnitPrice", settings.electricityUnitPrice)
        .put("electricityMonthlyFixedFee", settings.electricityMonthlyFixedFee)
        .put("gasBillingMode", settings.gasBillingMode.name)
        .put("gasUnitPrice", settings.gasUnitPrice)
        .put("gasMonthlyFixedFee", settings.gasMonthlyFixedFee)
        .put("gasFlatMonthlyPayment", settings.gasFlatMonthlyPayment)

    private fun settingsFromJson(item: JSONObject): BillingSettings = BillingSettings(
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
}
