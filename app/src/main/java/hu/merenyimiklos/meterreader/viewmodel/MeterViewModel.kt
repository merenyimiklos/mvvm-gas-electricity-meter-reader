package hu.merenyimiklos.meterreader.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.merenyimiklos.meterreader.backup.BackupManager
import hu.merenyimiklos.meterreader.data.LegacyDataMigrator
import hu.merenyimiklos.meterreader.data.MeterRepository
import hu.merenyimiklos.meterreader.data.SettingsRepository
import hu.merenyimiklos.meterreader.data.db.MeterDatabase
import hu.merenyimiklos.meterreader.domain.OcrMeterReader
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MeterViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MeterDatabase.getInstance(application)
    private val meterRepository = MeterRepository(database)
    private val settingsRepository = SettingsRepository(application)
    private val legacyDataMigrator = LegacyDataMigrator(
        application,
        meterRepository,
        settingsRepository
    )
    private val ocrReader = OcrMeterReader(application)
    private val exporter = XlsxExporter(application.contentResolver)
    private val backupManager = BackupManager(application.contentResolver)

    val readings = meterRepository.readings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillingSettings()
    )

    val summaries = combine(readings, settings) { readingsValue, settingsValue ->
        UsageCalculator.monthlySummaries(readingsValue, settingsValue)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { legacyDataMigrator.migrateIfNeeded() }
        }
    }

    suspend fun saveReading(
        type: MeterType,
        valueText: String,
        dateText: String,
        note: String,
        photoUri: String?
    ): String? {
        val parsed = parseAndValidate(type, valueText, dateText, null)
            ?: return validationMessage

        meterRepository.add(
            type = type,
            value = parsed.value,
            dateEpochDay = parsed.date.toEpochDay(),
            note = note,
            photoUri = photoUri
        )
        return null
    }

    suspend fun updateReading(
        original: MeterReading,
        valueText: String,
        dateText: String,
        note: String
    ): String? {
        val parsed = parseAndValidate(
            original.type,
            valueText,
            dateText,
            original.id
        ) ?: return validationMessage

        meterRepository.update(
            original.copy(
                value = parsed.value,
                dateEpochDay = parsed.date.toEpochDay(),
                note = note
            )
        )
        return null
    }

    fun deleteReading(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            meterRepository.delete(id)
        }
    }

    suspend fun updateSettings(settings: BillingSettings) {
        settingsRepository.update(settings)
    }

    suspend fun detectReading(uri: Uri, type: MeterType): Result<OcrResult> =
        withContext(Dispatchers.IO) {
            runCatching { ocrReader.read(uri, type) }
        }

    suspend fun exportXlsx(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                exporter.export(uri, readings.value, settings.value)
            }
        }

    suspend fun exportBackup(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                backupManager.export(
                    uri = uri,
                    readings = meterRepository.getAll(),
                    settings = settings.value
                )
            }
        }

    suspend fun importBackup(uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = backupManager.import(uri)
                meterRepository.replaceAll(payload.readings)
                settingsRepository.update(payload.settings)
                payload.readings.size
            }
        }

    suspend fun clearAllData(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                meterRepository.clear()
                settingsRepository.reset()
            }
        }

    private var validationMessage: String = ""

    private fun parseAndValidate(
        type: MeterType,
        valueText: String,
        dateText: String,
        excludeId: String?
    ): ParsedReading? {
        val value = valueText.replace(',', '.').toDoubleOrNull()
        if (value == null) {
            validationMessage = "Adj meg érvényes mérőállást."
            return null
        }

        if (value < 0.0) {
            validationMessage = "A mérőállás nem lehet negatív."
            return null
        }

        val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
        if (date == null) {
            validationMessage = "A dátum formátuma legyen ÉÉÉÉ-HH-NN."
            return null
        }

        if (date.isAfter(LocalDate.now().plusDays(1))) {
            validationMessage = "A mérés dátuma nem lehet a jövőben."
            return null
        }

        val day = date.toEpochDay()
        val typed = readings.value
            .filter { it.type == type && it.id != excludeId }
            .sortedBy { it.dateEpochDay }

        val previous = typed.lastOrNull { it.dateEpochDay <= day }
        val next = typed.firstOrNull { it.dateEpochDay > day }

        if (previous != null && value < previous.value) {
            validationMessage =
                "Az új mérőállás kisebb a korábbi értéknél (${previous.value} ${type.unit})."
            return null
        }

        if (next != null && value > next.value) {
            validationMessage =
                "A megadott érték nagyobb egy későbbi mérőállásnál (${next.value} ${type.unit})."
            return null
        }

        validationMessage = ""
        return ParsedReading(value, date)
    }

    private data class ParsedReading(
        val value: Double,
        val date: LocalDate
    )
}
