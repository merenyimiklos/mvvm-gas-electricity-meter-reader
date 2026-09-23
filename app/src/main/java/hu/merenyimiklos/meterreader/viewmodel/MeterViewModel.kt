package hu.merenyimiklos.meterreader.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.merenyimiklos.meterreader.data.LocalMeterRepository
import hu.merenyimiklos.meterreader.domain.OcrMeterReader
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MeterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalMeterRepository(application)
    private val ocrReader = OcrMeterReader(application)
    private val exporter = XlsxExporter(application.contentResolver)

    val readings = repository.readings
    val settings = repository.settings
    val summaries = combine(readings, settings) { readingsValue, settingsValue ->
        UsageCalculator.monthlySummaries(readingsValue, settingsValue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveReading(type: MeterType, valueText: String, dateText: String, note: String, photoUri: String?): String? {
        val value = valueText.replace(',', '.').toDoubleOrNull() ?: return "Adj meg érvényes mérőállást."
        if (value < 0.0) return "A mérőállás nem lehet negatív."
        val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
            ?: return "A dátum formátuma legyen ÉÉÉÉ-HH-NN."
        val day = date.toEpochDay()
        val typed = readings.value.filter { it.type == type }.sortedBy { it.dateEpochDay }
        val previous = typed.lastOrNull { it.dateEpochDay <= day }
        val next = typed.firstOrNull { it.dateEpochDay > day }
        if (previous != null && value < previous.value) {
            return "Az új mérőállás kisebb a korábbi értéknél (" + previous.value + " " + type.unit + ")."
        }
        if (next != null && value > next.value) {
            return "A megadott érték nagyobb egy későbbi mérőállásnál (" + next.value + " " + type.unit + ")."
        }
        repository.addReading(type, value, day, note, photoUri)
        return null
    }

    fun deleteReading(id: String) = repository.deleteReading(id)
    fun updateSettings(settings: BillingSettings) = repository.updateSettings(settings)

    suspend fun detectReading(uri: Uri, type: MeterType): Result<OcrResult> = withContext(Dispatchers.IO) {
        runCatching { ocrReader.read(uri, type) }
    }

    suspend fun exportXlsx(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { exporter.export(uri, readings.value, settings.value) }
    }
}
