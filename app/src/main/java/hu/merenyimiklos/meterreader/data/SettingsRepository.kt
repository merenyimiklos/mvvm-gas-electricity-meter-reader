package hu.merenyimiklos.meterreader.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.billingSettingsDataStore by preferencesDataStore(name = "billing_settings")

class SettingsRepository(
    private val context: Context
) {
    private object Keys {
        val electricityUnitPrice = doublePreferencesKey("electricity_unit_price")
        val electricityMonthlyFixedFee = doublePreferencesKey("electricity_monthly_fixed_fee")
        val gasBillingMode = stringPreferencesKey("gas_billing_mode")
        val gasUnitPrice = doublePreferencesKey("gas_unit_price")
        val gasMonthlyFixedFee = doublePreferencesKey("gas_monthly_fixed_fee")
        val gasFlatMonthlyPayment = doublePreferencesKey("gas_flat_monthly_payment")
    }

    val settings: Flow<BillingSettings> = context.billingSettingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            BillingSettings(
                electricityUnitPrice = preferences[Keys.electricityUnitPrice] ?: 0.0,
                electricityMonthlyFixedFee = preferences[Keys.electricityMonthlyFixedFee] ?: 0.0,
                gasBillingMode = preferences[Keys.gasBillingMode]
                    ?.let { value -> runCatching { GasBillingMode.valueOf(value) }.getOrNull() }
                    ?: GasBillingMode.FLAT_RATE,
                gasUnitPrice = preferences[Keys.gasUnitPrice] ?: 0.0,
                gasMonthlyFixedFee = preferences[Keys.gasMonthlyFixedFee] ?: 0.0,
                gasFlatMonthlyPayment = preferences[Keys.gasFlatMonthlyPayment] ?: 0.0
            )
        }

    suspend fun update(settings: BillingSettings) {
        context.billingSettingsDataStore.edit { preferences ->
            preferences[Keys.electricityUnitPrice] = settings.electricityUnitPrice
            preferences[Keys.electricityMonthlyFixedFee] = settings.electricityMonthlyFixedFee
            preferences[Keys.gasBillingMode] = settings.gasBillingMode.name
            preferences[Keys.gasUnitPrice] = settings.gasUnitPrice
            preferences[Keys.gasMonthlyFixedFee] = settings.gasMonthlyFixedFee
            preferences[Keys.gasFlatMonthlyPayment] = settings.gasFlatMonthlyPayment
        }
    }

    suspend fun reset() {
        context.billingSettingsDataStore.edit { it.clear() }
    }
}
