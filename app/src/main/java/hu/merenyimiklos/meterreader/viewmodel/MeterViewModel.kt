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
import hu.merenyimiklos.meterreader.export.CsvManager
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.OcrResult
import hu.merenyimiklos.meterreader.reminder.ReadingReminderWorker
import hu.merenyimiklos.meterreader.widget.MeterWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

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
    private val csvManager = CsvManager(application.contentResolver)
    private val backupManager = BackupManager(application.contentResolver)
    private val autoBackupFile = File(
        application.filesDir,
        "backups/auto-backup.json"
    )

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
            runCatching {
                legacyDataMigrator.migrateIfNeeded()
                syncAfterChange()
            }
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

        val duplicateOnDay = readings.value.any {
            it.type == type &&
                it.dateEpochDay == parsed.date.toEpochDay()
        }
        if (duplicateOnDay) {
            return "Erre a napra már van ilyen mérőállás."
        }

        meterRepository.add(
            type = type,
            value = parsed.value,
            dateEpochDay = parsed.date.toEpochDay(),
            note = note,
            photoUri = photoUri
        )
        syncAfterChange()
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

        val duplicateOnDay = readings.value.any {
            it.id != original.id &&
                it.type == original.type &&
                it.dateEpochDay == parsed.date.toEpochDay()
        }
        if (duplicateOnDay) {
            return "Erre a napra már van ilyen mérőállás."
        }

        meterRepository.update(
            original.copy(
                value = parsed.value,
                dateEpochDay = parsed.date.toEpochDay(),
                note = note
            )
        )
        syncAfterChange()
        return null
    }

    fun deleteReading(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            meterRepository.delete(id)
            syncAfterChange()
        }
    }

    fun monthlyDuplicates(
        date: LocalDate
    ): List<MeterType> {
        val month = YearMonth.from(date)
        return readings.value
            .filter {
                YearMonth.from(
                    LocalDate.ofEpochDay(
                        it.dateEpochDay
                    )
                ) == month
            }
            .map { it.type }
            .distinct()
    }

    suspend fun saveQuickReadings(
        values: Map<MeterType, String>,
        date: LocalDate,
        note: String,
        allowMonthlyDuplicate: Boolean = false
    ): QuickSaveResult {
        val missing = MeterType.entries.filter {
            values[it].isNullOrBlank()
        }

        if (missing.isNotEmpty()) {
            return QuickSaveResult(
                error = "Add meg mindhárom mérőállást."
            )
        }

        val duplicates = monthlyDuplicates(date)
        if (
            duplicates.isNotEmpty() &&
            !allowMonthlyDuplicate
        ) {
            return QuickSaveResult(
                duplicateTypes = duplicates
            )
        }

        val parsedValues =
            mutableMapOf<MeterType, ParsedReading>()

        for (type in MeterType.entries) {
            val parsed = parseAndValidate(
                type = type,
                valueText = values[type].orEmpty(),
                dateText = date.toString(),
                excludeId = null
            ) ?: return QuickSaveResult(
                error =
                    type.displayName +
                        ": " +
                        validationMessage
            )

            parsedValues[type] = parsed
        }

        for (type in MeterType.entries) {
            val parsed =
                requireNotNull(parsedValues[type])

            meterRepository.add(
                type = type,
                value = parsed.value,
                dateEpochDay =
                    parsed.date.toEpochDay(),
                note = note,
                photoUri = null
            )
        }

        syncAfterChange()

        return QuickSaveResult(
            savedCount = MeterType.entries.size
        )
    }

    suspend fun updateSettings(settings: BillingSettings) {
        settingsRepository.update(settings)
        syncAfterChange(settings)
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

    suspend fun exportCsv(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                csvManager.export(
                    uri = uri,
                    readings = meterRepository.getAll()
                )
            }
        }

    suspend fun importCsv(uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val imported =
                    csvManager.import(uri)
                meterRepository.upsertAll(
                    imported
                )
                syncAfterChange()
                imported.size
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
                syncAfterChange(payload.settings)
                payload.readings.size
            }
        }

    suspend fun clearAllData(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                meterRepository.clear()
                settingsRepository.reset()
                autoBackupFile.delete()
                ReadingReminderWorker.evaluateAndNotify(
                    getApplication()
                )
                MeterWidgetProvider.updateAll(
                    getApplication()
                )
            }
        }

    private suspend fun syncAfterChange(
        settingsOverride: BillingSettings? = null
    ) {
        withContext(Dispatchers.IO) {
            val currentSettings =
                settingsOverride
                    ?: settingsRepository
                        .settings
                        .first()

            runCatching {
                backupManager.exportToFile(
                    file = autoBackupFile,
                    readings =
                        meterRepository.getAll(),
                    settings =
                        currentSettings
                )
            }

            runCatching {
                ReadingReminderWorker
                    .evaluateAndNotify(
                        getApplication()
                    )
            }

            MeterWidgetProvider.updateAll(
                getApplication()
            )
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

    data class QuickSaveResult(
        val savedCount: Int = 0,
        val duplicateTypes: List<MeterType> = emptyList(),
        val error: String? = null
    ) {
        val success: Boolean
            get() = savedCount > 0 && error == null
    }

    private data class ParsedReading(
        val value: Double,
        val date: LocalDate
    )
}
