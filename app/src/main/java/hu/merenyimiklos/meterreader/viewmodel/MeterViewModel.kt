package hu.merenyimiklos.meterreader.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.merenyimiklos.meterreader.BuildConfig
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
import hu.merenyimiklos.meterreader.reminder.ReadingReminderScheduler
import hu.merenyimiklos.meterreader.reminder.ReadingReminderWorker
import hu.merenyimiklos.meterreader.shared.SharedHomeAction
import hu.merenyimiklos.meterreader.shared.SharedHomeManager
import hu.merenyimiklos.meterreader.update.AppUpdateChecker
import hu.merenyimiklos.meterreader.update.UpdateState
import hu.merenyimiklos.meterreader.widget.MeterWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

sealed interface SharedHomeSyncState {
    data object Idle : SharedHomeSyncState
    data object Syncing : SharedHomeSyncState

    data class Success(
        val changedCount: Int,
        val syncedAtMillis: Long
    ) : SharedHomeSyncState

    data class Error(
        val message: String
    ) : SharedHomeSyncState
}

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
    private val sharedHomeManager =
        SharedHomeManager(application)
    private val updateChecker = AppUpdateChecker()
    private val _updateState =
        MutableStateFlow<UpdateState>(
            UpdateState.Idle
        )
    val updateState: StateFlow<UpdateState> =
        _updateState.asStateFlow()

    private val _sharedHomeSyncState =
        MutableStateFlow<SharedHomeSyncState>(
            SharedHomeSyncState.Idle
        )
    val sharedHomeSyncState:
        StateFlow<SharedHomeSyncState> =
        _sharedHomeSyncState.asStateFlow()

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
                syncSharedHomeInternal(
                    updateState = false
                )
            }
        }
        checkForUpdates()
    }

    fun checkForUpdates() {
        if (
            _updateState.value ==
            UpdateState.Checking
        ) {
            return
        }

        viewModelScope.launch {
            _updateState.value =
                UpdateState.Checking

            val result =
                withContext(
                    Dispatchers.IO
                ) {
                    updateChecker.check(
                        BuildConfig.VERSION_NAME
                    )
                }

            _updateState.value =
                result.fold(
                    onSuccess = { info ->
                        if (info == null) {
                            UpdateState.UpToDate
                        } else {
                            UpdateState.Available(
                                info
                            )
                        }
                    },
                    onFailure = { error ->
                        UpdateState.Error(
                            error.message
                                ?: "Nem sikerült ellenőrizni a frissítést."
                        )
                    }
                )
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

        val reading =
            meterRepository.add(
                type = type,
                value = parsed.value,
                dateEpochDay = parsed.date.toEpochDay(),
                note = note,
                photoUri = photoUri
            )
        publishSharedUpsert(
            reading
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

        val updated =
            original.copy(
                value = parsed.value,
                dateEpochDay = parsed.date.toEpochDay(),
                note = note
            )
        meterRepository.update(
            updated
        )
        publishSharedUpsert(
            updated
        )
        syncAfterChange()
        return null
    }

    fun deleteReading(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            meterRepository.delete(id)
            publishSharedDelete(
                id
            )
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

        val savedReadings =
            mutableListOf<MeterReading>()

        for (type in MeterType.entries) {
            val parsed =
                requireNotNull(parsedValues[type])

            savedReadings +=
                meterRepository.add(
                    type = type,
                    value = parsed.value,
                    dateEpochDay =
                        parsed.date.toEpochDay(),
                    note = note,
                    photoUri = null
                )
        }

        savedReadings.forEach {
            publishSharedUpsert(it)
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

    suspend fun createSharedHome(
        folderUri: Uri,
        homeName: String,
        deviceName: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(
                    homeName.isNotBlank()
                ) {
                    "Adj nevet a közös otthonnak."
                }
                require(
                    deviceName.isNotBlank()
                ) {
                    "Adj nevet ennek a telefonnak."
                }

                val info =
                    sharedHomeManager
                        .createHome(
                            folderUri,
                            homeName
                        )

                val current =
                    settingsRepository
                        .settings
                        .first()
                val connected =
                    current.copy(
                        sharedHomeFolderUri =
                            folderUri.toString(),
                        sharedHomeId =
                            info.homeId,
                        sharedHomeName =
                            info.name,
                        sharedDeviceId =
                            current
                                .sharedDeviceId
                                .ifBlank {
                                    UUID.randomUUID()
                                        .toString()
                                },
                        sharedDeviceName =
                            deviceName.trim()
                    )

                settingsRepository.update(
                    connected
                )

                meterRepository
                    .getAll()
                    .forEach {
                        reading ->
                        sharedHomeManager
                            .publishUpsert(
                                folderUri =
                                    folderUri,
                                homeId =
                                    connected
                                        .sharedHomeId,
                                deviceId =
                                    connected
                                        .sharedDeviceId,
                                deviceName =
                                    connected
                                        .sharedDeviceName,
                                reading =
                                    reading
                            )
                    }

                syncAfterChange(
                    connected
                )
                syncSharedHomeInternal(
                    updateState = true
                )
            }
        }

    suspend fun joinSharedHome(
        folderUri: Uri,
        deviceName: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(
                    deviceName.isNotBlank()
                ) {
                    "Adj nevet ennek a telefonnak."
                }

                val info =
                    sharedHomeManager
                        .readHome(
                            folderUri
                        )
                val localBefore =
                    meterRepository
                        .getAll()
                val current =
                    settingsRepository
                        .settings
                        .first()
                val connected =
                    current.copy(
                        sharedHomeFolderUri =
                            folderUri.toString(),
                        sharedHomeId =
                            info.homeId,
                        sharedHomeName =
                            info.name,
                        sharedDeviceId =
                            current
                                .sharedDeviceId
                                .ifBlank {
                                    UUID.randomUUID()
                                        .toString()
                                },
                        sharedDeviceName =
                            deviceName.trim()
                    )

                settingsRepository.update(
                    connected
                )

                localBefore.forEach {
                    reading ->
                    sharedHomeManager
                        .publishUpsert(
                            folderUri =
                                folderUri,
                            homeId =
                                connected
                                    .sharedHomeId,
                            deviceId =
                                connected
                                    .sharedDeviceId,
                            deviceName =
                                connected
                                    .sharedDeviceName,
                            reading =
                                reading
                        )
                }

                syncSharedHomeInternal(
                    updateState = true
                )
                syncAfterChange(
                    connected
                )
            }
        }

    suspend fun disconnectSharedHome(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val current =
                    settingsRepository
                        .settings
                        .first()
                val disconnected =
                    current.copy(
                        sharedHomeFolderUri = "",
                        sharedHomeId = "",
                        sharedHomeName = ""
                    )
                settingsRepository.update(
                    disconnected
                )
                _sharedHomeSyncState.value =
                    SharedHomeSyncState.Idle
                syncAfterChange(
                    disconnected
                )
            }
        }

    fun syncSharedHome() {
        viewModelScope.launch(
            Dispatchers.IO
        ) {
            syncSharedHomeInternal(
                updateState = true
            )
        }
    }

    suspend fun syncSharedHomeNow(): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                syncSharedHomeInternal(
                    updateState = true
                )
            }
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
                val current =
                    settingsRepository.settings.first()
                val restoredSettings =
                    payload.settings.copy(
                        backupFolderUri =
                            current.backupFolderUri,
                        sharedHomeFolderUri =
                            current.sharedHomeFolderUri,
                        sharedHomeId =
                            current.sharedHomeId,
                        sharedHomeName =
                            current.sharedHomeName,
                        sharedDeviceId =
                            current.sharedDeviceId,
                        sharedDeviceName =
                            current.sharedDeviceName
                    )
                meterRepository.replaceAll(
                    payload.readings
                )
                settingsRepository.update(
                    restoredSettings
                )
                syncAfterChange(
                    restoredSettings
                )
                payload.readings.size
            }
        }

    suspend fun clearAllData(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                meterRepository.clear()
                settingsRepository.reset()
                autoBackupFile.delete()

                val resetSettings =
                    settingsRepository
                        .settings
                        .first()

                ReadingReminderScheduler.configure(
                    getApplication(),
                    resetSettings
                )
                ReadingReminderWorker.evaluateAndNotify(
                    getApplication()
                )
                MeterWidgetProvider.updateAll(
                    getApplication()
                )
            }
        }

    private suspend fun publishSharedUpsert(
        reading: MeterReading
    ) {
        val current =
            settingsRepository
                .settings
                .first()

        if (!isSharedHomeConnected(current)) {
            return
        }

        runCatching {
            sharedHomeManager
                .publishUpsert(
                    folderUri =
                        Uri.parse(
                            current
                                .sharedHomeFolderUri
                        ),
                    homeId =
                        current.sharedHomeId,
                    deviceId =
                        current.sharedDeviceId,
                    deviceName =
                        current.sharedDeviceName,
                    reading = reading
                )
        }.onFailure {
            error ->
            _sharedHomeSyncState.value =
                SharedHomeSyncState.Error(
                    error.message
                        ?: "A közös szinkron nem sikerült."
                )
        }
    }

    private suspend fun publishSharedDelete(
        readingId: String
    ) {
        val current =
            settingsRepository
                .settings
                .first()

        if (!isSharedHomeConnected(current)) {
            return
        }

        runCatching {
            sharedHomeManager
                .publishDelete(
                    folderUri =
                        Uri.parse(
                            current
                                .sharedHomeFolderUri
                        ),
                    homeId =
                        current.sharedHomeId,
                    deviceId =
                        current.sharedDeviceId,
                    deviceName =
                        current.sharedDeviceName,
                    readingId = readingId
                )
        }.onFailure {
            error ->
            _sharedHomeSyncState.value =
                SharedHomeSyncState.Error(
                    error.message
                        ?: "A közös szinkron nem sikerült."
                )
        }
    }

    private suspend fun syncSharedHomeInternal(
        updateState: Boolean
    ): Int {
        val currentSettings =
            settingsRepository
                .settings
                .first()

        if (
            !isSharedHomeConnected(
                currentSettings
            )
        ) {
            if (updateState) {
                _sharedHomeSyncState.value =
                    SharedHomeSyncState.Idle
            }
            return 0
        }

        if (updateState) {
            _sharedHomeSyncState.value =
                SharedHomeSyncState.Syncing
        }

        return try {
            val changes =
                sharedHomeManager
                    .resolvedChanges(
                        folderUri =
                            Uri.parse(
                                currentSettings
                                    .sharedHomeFolderUri
                            ),
                        expectedHomeId =
                            currentSettings
                                .sharedHomeId
                    )

            val local =
                meterRepository
                    .getAll()
                    .associateBy {
                        it.id
                    }
            val upserts =
                mutableListOf<MeterReading>()
            val deletes =
                mutableListOf<String>()
            var changedCount = 0

            changes.forEach {
                change ->
                when (
                    change.action
                ) {
                    SharedHomeAction.DELETE -> {
                        if (
                            local[
                                change.readingId
                            ] != null
                        ) {
                            deletes +=
                                change.readingId
                            changedCount += 1
                        }
                    }

                    SharedHomeAction.UPSERT -> {
                        val remote =
                            change.reading
                                ?: return@forEach
                        val existing =
                            local[
                                remote.id
                            ]
                        val merged =
                            remote.copy(
                                photoUri =
                                    existing
                                        ?.photoUri
                            )

                        val same =
                            existing
                                ?.copy(
                                    photoUri = null
                                ) ==
                                merged.copy(
                                    photoUri = null
                                )

                        if (!same) {
                            upserts +=
                                merged
                            changedCount += 1
                        }
                    }
                }
            }

            deletes.forEach {
                meterRepository
                    .delete(it)
            }
            meterRepository
                .upsertAll(
                    upserts
                )

            if (changedCount > 0) {
                syncAfterChange(
                    currentSettings
                )
            }

            if (updateState) {
                _sharedHomeSyncState.value =
                    SharedHomeSyncState.Success(
                        changedCount =
                            changedCount,
                        syncedAtMillis =
                            System
                                .currentTimeMillis()
                    )
            }

            changedCount
        } catch (error: Throwable) {
            if (updateState) {
                _sharedHomeSyncState.value =
                    SharedHomeSyncState.Error(
                        error.message
                            ?: "A közös szinkron nem sikerült."
                    )
            }
            if (updateState) {
                throw error
            }
            0
        }
    }

    private fun isSharedHomeConnected(
        settings: BillingSettings
    ): Boolean =
        settings.sharedHomeFolderUri
            .isNotBlank() &&
            settings.sharedHomeId
                .isNotBlank() &&
            settings.sharedDeviceId
                .isNotBlank()

    private suspend fun syncAfterChange(
        settingsOverride: BillingSettings? = null
    ) {
        withContext(Dispatchers.IO) {
            val currentSettings =
                settingsOverride
                    ?: settingsRepository
                        .settings
                        .first()

            val currentReadings =
                meterRepository.getAll()

            runCatching {
                backupManager.exportToFile(
                    file = autoBackupFile,
                    readings =
                        currentReadings,
                    settings =
                        currentSettings
                )
            }

            if (
                currentSettings
                    .backupFolderUri
                    .isNotBlank()
            ) {
                runCatching {
                    val folder =
                        DocumentFile.fromTreeUri(
                            getApplication(),
                            Uri.parse(
                                currentSettings
                                    .backupFolderUri
                            )
                        )
                            ?: error(
                                "A mentési mappa nem érhető el."
                            )

                    val backupFile =
                        folder.findFile(
                            "auto-backup.json"
                        )
                            ?: folder.createFile(
                                "application/json",
                                "auto-backup.json"
                            )
                            ?: error(
                                "Nem sikerült létrehozni az automatikus mentést."
                            )

                    backupManager.export(
                        uri = backupFile.uri,
                        readings =
                            currentReadings,
                        settings =
                            currentSettings
                    )
                }
            }

            ReadingReminderScheduler.configure(
                getApplication(),
                currentSettings
            )

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
