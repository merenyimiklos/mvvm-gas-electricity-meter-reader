package hu.merenyimiklos.meterreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hu.merenyimiklos.meterreader.model.AppLanguage
import hu.merenyimiklos.meterreader.model.BillingMode
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.IOException

private val Context.billingSettingsDataStore by preferencesDataStore(
    name = "billing_settings"
)

class SettingsRepository(
    private val context: Context,
    userUid: String? = null
) {
    // Null is reserved for the pre-upgrade unowned settings during explicit import.
    private val dataStore: DataStore<Preferences> =
        if (userUid == null) {
            context.billingSettingsDataStore
        } else {
            scopedDataStore(context, userUid)
        }

    private companion object {
        private val scopedStores = mutableMapOf<String, DataStore<Preferences>>()

        fun scopedDataStore(context: Context, userUid: String): DataStore<Preferences> {
            val name = "billing_settings_${UserDataScope.storageId(userUid)}"
            return synchronized(scopedStores) {
                scopedStores.getOrPut(name) {
                    val application = context.applicationContext
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                        produceFile = { application.preferencesDataStoreFile(name) }
                    )
                }
            }
        }
    }
    private object Keys {
        val electricityEnabled =
            booleanPreferencesKey("electricity_enabled")
        val electricityNightEnabled =
            booleanPreferencesKey("electricity_night_enabled")
        val gasEnabled =
            booleanPreferencesKey("gas_enabled")
        val waterEnabled =
            booleanPreferencesKey("water_enabled")
        val electricityUnitPrice =
            doublePreferencesKey("electricity_unit_price")
        val electricityMonthlyFixedFee =
            doublePreferencesKey("electricity_monthly_fixed_fee")
        val electricityNightUnitPrice =
            doublePreferencesKey("electricity_night_unit_price")
        val electricityNightMonthlyFixedFee =
            doublePreferencesKey("electricity_night_monthly_fixed_fee")
        val electricityMonthlyGoalKwh =
            doublePreferencesKey("electricity_monthly_goal_kwh")
        val electricityNightMonthlyGoalKwh =
            doublePreferencesKey("electricity_night_monthly_goal_kwh")
        val gasMonthlyGoalM3 =
            doublePreferencesKey("gas_monthly_goal_m3")
        val waterMonthlyGoalM3 =
            doublePreferencesKey("water_monthly_goal_m3")
        val gasHeatingValueMjPerM3 =
            doublePreferencesKey("gas_heating_value_mj_per_m3")
        val reminderEnabled =
            booleanPreferencesKey("reminder_enabled")
        val reminderDayOfMonth =
            intPreferencesKey("reminder_day_of_month")
        val reminderHour =
            intPreferencesKey("reminder_hour")
        val reminderMinute =
            intPreferencesKey("reminder_minute")
        val backupFolderUri =
            stringPreferencesKey("backup_folder_uri")
        val gasBillingMode =
            stringPreferencesKey("gas_billing_mode")
        val gasUnitPrice =
            doublePreferencesKey("gas_unit_price")
        val gasMonthlyFixedFee =
            doublePreferencesKey("gas_monthly_fixed_fee")
        val gasFlatMonthlyPayment =
            doublePreferencesKey("gas_flat_monthly_payment")
        val waterBillingMode =
            stringPreferencesKey("water_billing_mode")
        val waterUnitPrice =
            doublePreferencesKey("water_unit_price")
        val waterMonthlyFixedFee =
            doublePreferencesKey("water_monthly_fixed_fee")
        val waterFlatMonthlyPayment =
            doublePreferencesKey("water_flat_monthly_payment")
        val appLanguage =
            stringPreferencesKey("app_language")
        val themeMode =
            stringPreferencesKey("theme_mode")
        val sharedHomeFolderUri =
            stringPreferencesKey("shared_home_folder_uri")
        val sharedHomeId =
            stringPreferencesKey("shared_home_id")
        val sharedHomeName =
            stringPreferencesKey("shared_home_name")
        val sharedDeviceId =
            stringPreferencesKey("shared_device_id")
        val sharedDeviceName =
            stringPreferencesKey("shared_device_name")
    }

    val settings: Flow<BillingSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                BillingSettings(
                    electricityEnabled =
                        preferences[Keys.electricityEnabled] ?: true,
                    electricityNightEnabled =
                        preferences[Keys.electricityNightEnabled] ?: true,
                    gasEnabled =
                        preferences[Keys.gasEnabled] ?: true,
                    waterEnabled =
                        preferences[Keys.waterEnabled] ?: false,
                    electricityUnitPrice =
                        preferences[Keys.electricityUnitPrice] ?: 0.0,
                    electricityMonthlyFixedFee =
                        preferences[Keys.electricityMonthlyFixedFee] ?: 0.0,
                    electricityNightUnitPrice =
                        preferences[Keys.electricityNightUnitPrice] ?: 0.0,
                    electricityNightMonthlyFixedFee =
                        preferences[Keys.electricityNightMonthlyFixedFee] ?: 0.0,
                    electricityMonthlyGoalKwh =
                        preferences[Keys.electricityMonthlyGoalKwh] ?: 0.0,
                    electricityNightMonthlyGoalKwh =
                        preferences[Keys.electricityNightMonthlyGoalKwh] ?: 0.0,
                    gasMonthlyGoalM3 =
                        preferences[Keys.gasMonthlyGoalM3] ?: 0.0,
                    waterMonthlyGoalM3 =
                        preferences[Keys.waterMonthlyGoalM3] ?: 0.0,
                    gasHeatingValueMjPerM3 =
                        preferences[Keys.gasHeatingValueMjPerM3] ?: 34.8,
                    reminderEnabled =
                        preferences[Keys.reminderEnabled] ?: true,
                    reminderDayOfMonth =
                        preferences[Keys.reminderDayOfMonth] ?: 23,
                    reminderHour =
                        preferences[Keys.reminderHour] ?: 18,
                    reminderMinute =
                        preferences[Keys.reminderMinute] ?: 0,
                    backupFolderUri =
                        preferences[Keys.backupFolderUri] ?: "",
                    gasBillingMode =
                        preferences[Keys.gasBillingMode]
                            ?.let { value ->
                                runCatching {
                                    BillingMode.valueOf(value)
                                }.getOrNull()
                            }
                            ?: BillingMode.FLAT_RATE,
                    gasUnitPrice =
                        preferences[Keys.gasUnitPrice] ?: 0.0,
                    gasMonthlyFixedFee =
                        preferences[Keys.gasMonthlyFixedFee] ?: 0.0,
                    gasFlatMonthlyPayment =
                        preferences[Keys.gasFlatMonthlyPayment] ?: 0.0,
                    waterBillingMode =
                        preferences[Keys.waterBillingMode]
                            ?.let { value ->
                                runCatching {
                                    BillingMode.valueOf(value)
                                }.getOrNull()
                            }
                            ?: BillingMode.METERED,
                    waterUnitPrice =
                        preferences[Keys.waterUnitPrice] ?: 0.0,
                    waterMonthlyFixedFee =
                        preferences[Keys.waterMonthlyFixedFee] ?: 0.0,
                    waterFlatMonthlyPayment =
                        preferences[Keys.waterFlatMonthlyPayment] ?: 0.0,
                    appLanguage =
                        preferences[Keys.appLanguage]
                            ?.let { value ->
                                runCatching {
                                    AppLanguage.valueOf(value)
                                }.getOrNull()
                            }
                            ?: AppLanguage.HUNGARIAN,
                    themeMode =
                        preferences[Keys.themeMode]
                            ?.let { value ->
                                runCatching {
                                    ThemeMode.valueOf(value)
                                }.getOrNull()
                            }
                            ?: ThemeMode.SYSTEM,
                    sharedHomeFolderUri =
                        preferences[Keys.sharedHomeFolderUri] ?: "",
                    sharedHomeId =
                        preferences[Keys.sharedHomeId] ?: "",
                    sharedHomeName =
                        preferences[Keys.sharedHomeName] ?: "",
                    sharedDeviceId =
                        preferences[Keys.sharedDeviceId] ?: "",
                    sharedDeviceName =
                        preferences[Keys.sharedDeviceName] ?: ""
                )
            }

    suspend fun update(
        settings: BillingSettings
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.electricityEnabled] =
                settings.electricityEnabled
            preferences[Keys.electricityNightEnabled] =
                settings.electricityNightEnabled
            preferences[Keys.gasEnabled] =
                settings.gasEnabled
            preferences[Keys.waterEnabled] =
                settings.waterEnabled
            preferences[Keys.electricityUnitPrice] =
                settings.electricityUnitPrice
            preferences[Keys.electricityMonthlyFixedFee] =
                settings.electricityMonthlyFixedFee
            preferences[Keys.electricityNightUnitPrice] =
                settings.electricityNightUnitPrice
            preferences[Keys.electricityNightMonthlyFixedFee] =
                settings.electricityNightMonthlyFixedFee
            preferences[Keys.electricityMonthlyGoalKwh] =
                settings.electricityMonthlyGoalKwh
            preferences[Keys.electricityNightMonthlyGoalKwh] =
                settings.electricityNightMonthlyGoalKwh
            preferences[Keys.gasMonthlyGoalM3] =
                settings.gasMonthlyGoalM3
            preferences[Keys.waterMonthlyGoalM3] =
                settings.waterMonthlyGoalM3
            preferences[Keys.gasHeatingValueMjPerM3] =
                settings.gasHeatingValueMjPerM3
            preferences[Keys.reminderEnabled] =
                settings.reminderEnabled
            preferences[Keys.reminderDayOfMonth] =
                settings.reminderDayOfMonth.coerceIn(1, 31)
            preferences[Keys.reminderHour] =
                settings.reminderHour.coerceIn(0, 23)
            preferences[Keys.reminderMinute] =
                settings.reminderMinute.coerceIn(0, 59)
            preferences[Keys.backupFolderUri] =
                settings.backupFolderUri
            preferences[Keys.gasBillingMode] =
                settings.gasBillingMode.name
            preferences[Keys.gasUnitPrice] =
                settings.gasUnitPrice
            preferences[Keys.gasMonthlyFixedFee] =
                settings.gasMonthlyFixedFee
            preferences[Keys.gasFlatMonthlyPayment] =
                settings.gasFlatMonthlyPayment
            preferences[Keys.waterBillingMode] =
                settings.waterBillingMode.name
            preferences[Keys.waterUnitPrice] =
                settings.waterUnitPrice
            preferences[Keys.waterMonthlyFixedFee] =
                settings.waterMonthlyFixedFee
            preferences[Keys.waterFlatMonthlyPayment] =
                settings.waterFlatMonthlyPayment
            preferences[Keys.appLanguage] =
                settings.appLanguage.name
            preferences[Keys.themeMode] =
                settings.themeMode.name
            preferences[Keys.sharedHomeFolderUri] =
                settings.sharedHomeFolderUri
            preferences[Keys.sharedHomeId] =
                settings.sharedHomeId
            preferences[Keys.sharedHomeName] =
                settings.sharedHomeName
            preferences[Keys.sharedDeviceId] =
                settings.sharedDeviceId
            preferences[Keys.sharedDeviceName] =
                settings.sharedDeviceName
        }
    }

    suspend fun reset() {
        dataStore.edit {
            it.clear()
        }
    }
}
