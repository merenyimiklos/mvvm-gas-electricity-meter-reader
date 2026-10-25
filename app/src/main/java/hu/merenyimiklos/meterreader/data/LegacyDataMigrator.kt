package hu.merenyimiklos.meterreader.data

import android.content.Context
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first

class LegacyDataMigrator(
    private val context: Context,
    private val userUid: String,
    private val meterRepository: MeterRepository,
    private val settingsRepository: SettingsRepository
) {
    private val marker = "legacy_reviewed_${UserDataScope.storageId(userUid)}"

    private val state
        get() = context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)

    /** Never assign another person's unowned local data automatically. */
    suspend fun needsReview(): Boolean {
        if (state.getBoolean(marker, false)) return false

        val claimant = state.getString(KEY_CLAIMED_BY, null)
        if (claimant != null && claimant != userUid) return false

        val oldDbExists = context.getDatabasePath("meter_reader.db").exists()
        val oldPreferencesExist =
            context.filesDir.resolve("../datastore/billing_settings.preferences_pb")
                .canonicalFile.exists()
        val oldJson = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        return oldDbExists || oldPreferencesExist ||
            oldJson.contains(KEY_READINGS) || oldJson.contains(KEY_SETTINGS)
    }

    /**
     * Import only after the person explicitly associates the old on-device data
     * with the signed-in account. Never delete the original files.
     */
    suspend fun importApproved(): Int {
        val claimant = state.getString(KEY_CLAIMED_BY, null)
        check(claimant == null || claimant == userUid) {
            "A korábbi adatokat egy másik fiókhoz társították."
        }

        val oldRoom =
            MeterRepository(MeterDatabase.getInstance(context))
        val fromRoom = oldRoom.getAll()
        val legacyPrefs =
            context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val oldReadings =
            fromRoom.ifEmpty {
                parseReadings(legacyPrefs.getString(KEY_READINGS, null))
                    .orEmpty()
            }

        val destinationWasEmpty = meterRepository.count() == 0
        meterRepository.upsertAll(oldReadings)

        if (destinationWasEmpty) {
            val storedSettings = SettingsRepository(context).settings.first()
            val fromJson = parseSettings(legacyPrefs.getString(KEY_SETTINGS, null))
            // Use existing DataStore if present; JSON settings are a fallback
            // for the oldest releases, where there was no DataStore.
            val oldDatastoreExists =
                context.filesDir.resolve("../datastore/billing_settings.preferences_pb")
                    .canonicalFile.exists()
            settingsRepository.update(
                if (oldDatastoreExists) storedSettings else fromJson ?: storedSettings
            )
        }

        state.edit()
            .putString(KEY_CLAIMED_BY, userUid)
            .putBoolean(marker, true)
            .commit()
        return oldReadings.size
    }

    fun dismiss() {
        // Skipping affects only the current session; keep the unowned
        // pre-upgrade data recoverable on the next sign-in.
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
        const val KEY_CLAIMED_BY = "legacy_claimed_by_uid"
        const val KEY_READINGS = "readings"
        const val KEY_SETTINGS = "settings"
    }
}
