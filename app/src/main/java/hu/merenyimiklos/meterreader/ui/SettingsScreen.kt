package hu.merenyimiklos.meterreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.BuildConfig
import hu.merenyimiklos.meterreader.auth.AuthViewModel
import hu.merenyimiklos.meterreader.export.CsvManager
import hu.merenyimiklos.meterreader.model.AppLanguage
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterType
import androidx.compose.foundation.shape.RoundedCornerShape
import hu.merenyimiklos.meterreader.model.ThemeMode
import hu.merenyimiklos.meterreader.update.UpdateState
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import hu.merenyimiklos.meterreader.viewmodel.SharedHomeSyncState
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: MeterViewModel,
    authViewModel: AuthViewModel,
    onOpenMenu: () -> Unit = {}
) {
    val settings by
        viewModel.settings
            .collectAsStateWithLifecycle()
    val updateState by
        viewModel.updateState
            .collectAsStateWithLifecycle()
    val sharedHomeSyncState by
        viewModel.sharedHomeSyncState
            .collectAsStateWithLifecycle()
    val authState by
        authViewModel.state
            .collectAsStateWithLifecycle()
    val deletingAccount by authViewModel.deletingAccount.collectAsStateWithLifecycle()
    val accountDeletionError by authViewModel.deletionError.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState =
        remember { SnackbarHostState() }

    var electricityEnabled by remember(settings) {
        mutableStateOf(
            settings.electricityEnabled
        )
    }
    var nightEnabled by remember(settings) {
        mutableStateOf(
            settings.electricityNightEnabled
        )
    }
    var gasEnabled by remember(settings) {
        mutableStateOf(
            settings.gasEnabled
        )
    }
    var waterEnabled by remember(settings) {
        mutableStateOf(
            settings.waterEnabled
        )
    }
    var waterHotEnabled by remember(settings) {
        mutableStateOf(settings.waterHotEnabled)
    }

    var electricityUnitPrice by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityUnitPrice
            )
        )
    }
    var electricityFixedFee by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityMonthlyFixedFee
            )
        )
    }
    var nightUnitPrice by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityNightUnitPrice
            )
        )
    }
    var nightFixedFee by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityNightMonthlyFixedFee
            )
        )
    }
    var gasMode by remember(settings) {
        mutableStateOf(
            settings.gasBillingMode
        )
    }
    var gasUnitPrice by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.gasUnitPrice
            )
        )
    }
    var gasFixedFee by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.gasMonthlyFixedFee
            )
        )
    }
    var gasFlatPayment by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.gasFlatMonthlyPayment
            )
        )
    }
    var waterMode by remember(settings) {
        mutableStateOf(
            settings.waterBillingMode
        )
    }
    var waterHotMode by remember(settings) {
        mutableStateOf(settings.waterHotBillingMode)
    }
    var waterUnitPrice by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.waterUnitPrice
            )
        )
    }
    var waterFixedFee by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.waterMonthlyFixedFee
            )
        )
    }
    var waterFlatPayment by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.waterFlatMonthlyPayment
            )
        )
    }
    var waterHotFlatPayment by remember(settings) {
        mutableStateOf(editableNumber(settings.waterHotFlatMonthlyPayment))
    }
    var waterHotUnitPrice by remember(settings) {
        mutableStateOf(editableNumber(settings.waterHotUnitPrice))
    }
    var waterHotFixedFee by remember(settings) {
        mutableStateOf(editableNumber(settings.waterHotMonthlyFixedFee))
    }

    var normalGoal by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityMonthlyGoalKwh
            )
        )
    }
    var nightGoal by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.electricityNightMonthlyGoalKwh
            )
        )
    }
    var gasGoal by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.gasMonthlyGoalM3
            )
        )
    }
    var waterGoal by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.waterMonthlyGoalM3
            )
        )
    }
    var waterHotGoal by remember(settings) {
        mutableStateOf(editableNumber(settings.waterHotMonthlyGoalM3))
    }
    var gasHeatingValue by remember(settings) {
        mutableStateOf(
            editableNumber(
                settings.gasHeatingValueMjPerM3
            )
        )
    }
    var reminderEnabled by remember(settings) {
        mutableStateOf(
            settings.reminderEnabled
        )
    }
    var reminderDayText by remember(settings) {
        mutableStateOf(
            settings.reminderDayOfMonth
                .toString()
        )
    }
    var reminderHour by remember(settings) {
        mutableStateOf(
            settings.reminderHour
        )
    }
    var reminderMinute by remember(settings) {
        mutableStateOf(
            settings.reminderMinute
        )
    }
    var showReminderTimePicker by
        remember {
            mutableStateOf(false)
        }
    var backupFolderUri by remember(settings) {
        mutableStateOf(
            settings.backupFolderUri
        )
    }
    var appLanguage by remember(settings) {
        mutableStateOf(
            settings.appLanguage
        )
    }
    var themeMode by remember(settings) {
        mutableStateOf(
            settings.themeMode
        )
    }
    var sharedHomeNameText by remember(
        settings.sharedHomeName
    ) {
        mutableStateOf(
            settings.sharedHomeName
                .ifBlank {
                    "Otthon"
                }
        )
    }
    var sharedDeviceNameText by remember(
        settings.sharedDeviceName
    ) {
        mutableStateOf(
            settings.sharedDeviceName
        )
    }

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }
    var accountDeletionPhrase by remember { mutableStateOf("") }
    var accountDeletionPassword by remember { mutableStateOf("") }

    val backupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel
                        .exportBackup(uri)
                        .onSuccess {
                            snackbarHostState
                                .showSnackbar(
                                    t("Biztonsági mentés elkészült.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("Mentési hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel
                        .importBackup(uri)
                        .onSuccess { count ->
                            snackbarHostState
                                .showSnackbar(
                                    count.toString() +
                                        t(" mérőállás visszaállítva.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("Visszaállítási hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    fun currentSettings() =
        BillingSettings(
            electricityEnabled =
                electricityEnabled,
            electricityNightEnabled =
                nightEnabled,
            gasEnabled =
                gasEnabled,
            waterEnabled =
                waterEnabled,
            waterHotEnabled = waterHotEnabled,
            electricityUnitPrice =
                parseDecimal(
                    electricityUnitPrice
                ),
            electricityMonthlyFixedFee =
                parseDecimal(
                    electricityFixedFee
                ),
            electricityNightUnitPrice =
                parseDecimal(
                    nightUnitPrice
                ),
            electricityNightMonthlyFixedFee =
                parseDecimal(
                    nightFixedFee
                ),
            electricityMonthlyGoalKwh =
                parseDecimal(
                    normalGoal
                ),
            electricityNightMonthlyGoalKwh =
                parseDecimal(
                    nightGoal
                ),
            gasMonthlyGoalM3 =
                parseDecimal(
                    gasGoal
                ),
            waterMonthlyGoalM3 =
                parseDecimal(
                    waterGoal
                ),
            waterHotMonthlyGoalM3 = parseDecimal(waterHotGoal),
            gasHeatingValueMjPerM3 =
                parseDecimal(
                    gasHeatingValue
                ).takeIf {
                    it > 0.0
                } ?: 34.8,
            reminderEnabled =
                reminderEnabled,
            reminderDayOfMonth =
                reminderDayText
                    .toIntOrNull()
                    ?.coerceIn(1, 31)
                    ?: 23,
            reminderHour =
                reminderHour
                    .coerceIn(0, 23),
            reminderMinute =
                reminderMinute
                    .coerceIn(0, 59),
            backupFolderUri =
                backupFolderUri,
            gasBillingMode =
                gasMode,
            gasUnitPrice =
                parseDecimal(
                    gasUnitPrice
                ),
            gasMonthlyFixedFee =
                parseDecimal(
                    gasFixedFee
                ),
            gasFlatMonthlyPayment =
                parseDecimal(
                    gasFlatPayment
                ),
            waterBillingMode =
                waterMode,
            waterUnitPrice =
                parseDecimal(
                    waterUnitPrice
                ),
            waterMonthlyFixedFee =
                parseDecimal(
                    waterFixedFee
                ),
            waterFlatMonthlyPayment =
                parseDecimal(
                    waterFlatPayment
                ),
            waterHotBillingMode = waterHotMode,
            waterHotUnitPrice = parseDecimal(waterHotUnitPrice),
            waterHotMonthlyFixedFee = parseDecimal(waterHotFixedFee),
            waterHotFlatMonthlyPayment = parseDecimal(waterHotFlatPayment),
            appLanguage =
                appLanguage,
            themeMode =
                themeMode,
            sharedHomeFolderUri =
                settings.sharedHomeFolderUri,
            sharedHomeId =
                settings.sharedHomeId,
            sharedHomeName =
                settings.sharedHomeName,
            sharedDeviceId =
                settings.sharedDeviceId,
            sharedDeviceName =
                settings.sharedDeviceName
        )

    val backupFolderLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                }
                backupFolderUri =
                    uri.toString()

                scope.launch {
                    snackbarHostState
                        .showSnackbar(
                            t("Automatikus mentési mappa kiválasztva. Mentsd el a beállításokat.")
                        )
                }
            }
        }

    val createSharedHomeFolderLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                }

                scope.launch {
                    viewModel
                        .createSharedHome(
                            folderUri = uri,
                            homeName =
                                sharedHomeNameText,
                            deviceName =
                                sharedDeviceNameText
                        )
                        .onSuccess {
                            snackbarHostState
                                .showSnackbar(
                                    t("Közös otthon létrehozva.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("Közös otthon hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    val joinSharedHomeFolderLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                }

                scope.launch {
                    viewModel
                        .joinSharedHome(
                            folderUri = uri,
                            deviceName =
                                sharedDeviceNameText
                        )
                        .onSuccess {
                            snackbarHostState
                                .showSnackbar(
                                    t("Csatlakozva a Közös otthonhoz.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("Csatlakozási hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    val csvExportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                CsvManager.MIME_TYPE
            )
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel
                        .exportCsv(uri)
                        .onSuccess {
                            snackbarHostState
                                .showSnackbar(
                                    t("CSV export elkészült.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("CSV export hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    val csvImportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel
                        .importCsv(uri)
                        .onSuccess { count ->
                            snackbarHostState
                                .showSnackbar(
                                    count.toString() +
                                        t(" CSV mérés importálva.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("CSV import hiba: ") +
                                        (
                                            it.message
                                                ?: t("ismeretlen hiba")
                                            )
                                )
                        }
                }
            }
        }

    Scaffold(
        containerColor =
            MaterialTheme
                .colorScheme
                .background,
        topBar = {
            BrandTopBar(
                title =
                    t("Beállítások"),
                subtitle =
                    t("Minden fontos beállítás egy helyen"),
                icon =
                    Icons.Default.Settings,
                actions = {
                    androidx.compose.material3.IconButton(onClick = onOpenMenu) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = t("Menü"),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brandScreenBrush()
                )
                .padding(
                    innerPadding
                ),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Text(
                            t("Általános beállítások"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                        Text(
                            t("Az alkalmazás nyelve és megjelenése"),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Text(
                            t("Nyelv"),
                            fontWeight =
                                FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            FilterChip(
                                selected =
                                    appLanguage ==
                                        AppLanguage.HUNGARIAN,
                                onClick = {
                                    appLanguage =
                                        AppLanguage.HUNGARIAN
                                },
                                label = {
                                    Text(t("Magyar"))
                                }
                            )
                            FilterChip(
                                selected =
                                    appLanguage ==
                                        AppLanguage.ENGLISH,
                                onClick = {
                                    appLanguage =
                                        AppLanguage.ENGLISH
                                },
                                label = {
                                    Text("English")
                                }
                            )
                        }

                        Text(
                            t("Téma"),
                            fontWeight =
                                FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            ThemeMode.entries
                                .forEach { mode ->
                                    val label =
                                        when (mode) {
                                            ThemeMode.SYSTEM ->
                                                t("Rendszer")
                                            ThemeMode.LIGHT ->
                                                t("Világos")
                                            ThemeMode.DARK ->
                                                t("Sötét")
                                        }

                                    FilterChip(
                                        selected =
                                            themeMode ==
                                                mode,
                                        onClick = {
                                            themeMode =
                                                mode
                                        },
                                        label = {
                                            Text(label)
                                        }
                                    )
                                }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.elevatedCardColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceContainerLow
                        )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            t("Díjszabás és mérők"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                        Text(
                            t("Csak azok a mérők jelennek meg az appban, amelyeket itt bekapcsolsz."),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        MeterToggleRow(
                            type = MeterType.ELECTRICITY,
                            title = t("Normál áram"),
                            checked = electricityEnabled,
                            onCheckedChange = {
                                electricityEnabled = it
                            }
                        )
                        MeterToggleRow(
                            type = MeterType.ELECTRICITY_NIGHT,
                            title = t("Éjszakai áram"),
                            checked = nightEnabled,
                            onCheckedChange = {
                                nightEnabled = it
                            }
                        )
                        MeterToggleRow(
                            type = MeterType.GAS,
                            title = t("Gáz"),
                            checked = gasEnabled,
                            onCheckedChange = {
                                gasEnabled = it
                            }
                        )
                        MeterToggleRow(
                            type = MeterType.WATER,
                            title = t("Hideg víz"),
                            checked = waterEnabled,
                            onCheckedChange = {
                                waterEnabled = it
                            }
                        )
                        MeterToggleRow(
                            type = MeterType.WATER_HOT,
                            title = t("Meleg víz"),
                            checked = waterHotEnabled,
                            onCheckedChange = { waterHotEnabled = it }
                        )
                    }
                }
            }

            item {
                val sharedConnected =
                    settings.sharedHomeFolderUri
                        .isNotBlank() &&
                        settings.sharedHomeId
                            .isNotBlank()

                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription =
                                    null
                            )
                            Column {
                                Text(
                                    t("Közös otthon"),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                                Text(
                                    t("Családi mérőállás-szinkron fiók és saját szerver nélkül."),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }

                        if (!sharedConnected) {
                            OutlinedTextField(
                                value =
                                    sharedHomeNameText,
                                onValueChange = {
                                    sharedHomeNameText =
                                        it
                                },
                                modifier =
                                    Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        t("Otthon neve")
                                    )
                                },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value =
                                    sharedDeviceNameText,
                                onValueChange = {
                                    sharedDeviceNameText =
                                        it
                                },
                                modifier =
                                    Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        t("E készülék neve")
                                    )
                                },
                                placeholder = {
                                    Text(
                                        t("pl. Miki telefonja")
                                    )
                                },
                                singleLine = true
                            )

                            Button(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    createSharedHomeFolderLauncher
                                        .launch(null)
                                },
                                enabled =
                                    sharedHomeNameText
                                        .isNotBlank() &&
                                        sharedDeviceNameText
                                            .isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )
                                Text(
                                    t("Új közös otthon létrehozása")
                                )
                            }

                            OutlinedButton(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    joinSharedHomeFolderLauncher
                                        .launch(null)
                                },
                                enabled =
                                    sharedDeviceNameText
                                        .isNotBlank()
                            ) {
                                Text(
                                    t("Csatlakozás meglévőhöz")
                                )
                            }

                            Text(
                                t("A másik telefonon ugyanazt a megosztott Google Drive, OneDrive vagy más dokumentumszolgáltatói mappát kell kiválasztani."),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        } else {
                            AppInfoLine(
                                t("Otthon"),
                                settings
                                    .sharedHomeName
                            )
                            AppInfoLine(
                                t("Készülék"),
                                settings
                                    .sharedDeviceName
                            )

                            val syncText =
                                when (
                                    val state =
                                        sharedHomeSyncState
                                ) {
                                    SharedHomeSyncState.Idle ->
                                        t("Szinkronizálásra kész")

                                    SharedHomeSyncState.Syncing ->
                                        t("Szinkronizálás…")

                                    is SharedHomeSyncState.Success ->
                                        if (
                                            state.changedCount ==
                                            0
                                        ) {
                                            t("Minden naprakész.")
                                        } else {
                                            state.changedCount
                                                .toString() +
                                                t(" módosítás szinkronizálva.")
                                        }

                                    is SharedHomeSyncState.Error ->
                                        t("Szinkronhiba: ") +
                                            state.message
                                }

                            Text(
                                syncText,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    if (
                                        sharedHomeSyncState is
                                        SharedHomeSyncState.Error
                                    ) {
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    }
                            )

                            Button(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    scope.launch {
                                        viewModel
                                            .syncSharedHomeNow()
                                            .onSuccess {
                                                count ->
                                                snackbarHostState
                                                    .showSnackbar(
                                                        if (
                                                            count ==
                                                            0
                                                        ) {
                                                            t("Minden naprakész.")
                                                        } else {
                                                            count
                                                                .toString() +
                                                                t(" módosítás szinkronizálva.")
                                                        }
                                                    )
                                            }
                                            .onFailure {
                                                snackbarHostState
                                                    .showSnackbar(
                                                        t("Szinkronhiba: ") +
                                                            (
                                                                it.message
                                                                    ?: t("ismeretlen hiba")
                                                                )
                                                    )
                                            }
                                    }
                                },
                                enabled =
                                    sharedHomeSyncState !is
                                        SharedHomeSyncState.Syncing
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )
                                Text(
                                    t("Szinkronizálás most")
                                )
                            }

                            OutlinedButton(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    scope.launch {
                                        viewModel
                                            .disconnectSharedHome()
                                            .onSuccess {
                                                snackbarHostState
                                                    .showSnackbar(
                                                        t("Közös otthon leválasztva.")
                                                    )
                                            }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.LinkOff,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )
                                Text(
                                    t("Leválasztás erről az otthonról")
                                )
                            }
                        }
                    }
                }
            }

            item {
                TariffCard(
                    title = t("Normál áram"),
                    subtitle =
                        t("A nappali / normál mérő díjai"),
                    icon = Icons.Default.Bolt,
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {
                    MoneyField(
                        value =
                            electricityUnitPrice,
                        onValueChange = {
                            electricityUnitPrice =
                                it
                        },
                        label =
                            t("Egységár (Ft/kWh)")
                    )
                    MoneyField(
                        value =
                            electricityFixedFee,
                        onValueChange = {
                            electricityFixedFee =
                                it
                        },
                        label =
                            t("Havi fix díj (Ft)")
                    )
                }
            }

            item {
                TariffCard(
                    title = t("Éjszakai áram"),
                    subtitle =
                        t("Külön mérő és külön tarifa"),
                    icon = Icons.Default.Bedtime,
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                ) {
                    MoneyField(
                        value = nightUnitPrice,
                        onValueChange = {
                            nightUnitPrice = it
                        },
                        label =
                            t("Éjszakai egységár (Ft/kWh)")
                    )
                    MoneyField(
                        value = nightFixedFee,
                        onValueChange = {
                            nightFixedFee = it
                        },
                        label =
                            t("Éjszakai havi fix díj (Ft)")
                    )
                }
            }

            item {
                TariffCard(
                    title = t("Gáz"),
                    subtitle =
                        t("Átalány vagy fogyasztás alapján"),
                    icon =
                        Icons.Default
                            .LocalFireDepartment,
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .tertiaryContainer
                ) {
                    Text(
                        t("Elszámolás módja"),
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {
                        GasBillingMode.entries
                            .forEach { mode ->
                                FilterChip(
                                    selected =
                                        gasMode ==
                                            mode,
                                    onClick = {
                                        gasMode =
                                            mode
                                    },
                                    label = {
                                        Text(t(mode.displayName))
                                    }
                                )
                            }
                    }

                    MoneyField(
                        value = gasFlatPayment,
                        onValueChange = {
                            gasFlatPayment = it
                        },
                        label =
                            t("Havi gázátalány (Ft)")
                    )
                    MoneyField(
                        value = gasUnitPrice,
                        onValueChange = {
                            gasUnitPrice = it
                        },
                        label =
                            if (
                                gasMode ==
                                GasBillingMode.FLAT_RATE
                            ) {
                                t("Gáz egységár – kimutatáshoz (Ft/m³)")
                            } else {
                                t("Gáz egységár (Ft/m³)")
                            }
                    )
                    MoneyField(
                        value = gasFixedFee,
                        onValueChange = {
                            gasFixedFee = it
                        },
                        label =
                            t("Gáz havi fix díj (Ft)")
                    )
                    MoneyField(
                        value = gasHeatingValue,
                        onValueChange = {
                            gasHeatingValue = it
                        },
                        label =
                            t("Fűtőérték a rezsikerethez (MJ/m³)")
                    )
                    Text(
                        t("Alapérték: 34,8 MJ/m³. A pontos érték szolgáltatási területenként és időben is eltérhet."),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }

            if (waterEnabled) {
                item {
                    TariffCard(
                        title = t("Hideg víz"),
                        subtitle =
                            t("Átalány vagy fogyasztás alapján"),
                        icon =
                            Icons.Default.WaterDrop,
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    ) {
                        Text(
                            t("Elszámolás módja"),
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            GasBillingMode.entries
                                .forEach { mode ->
                                    FilterChip(
                                        selected =
                                            waterMode ==
                                                mode,
                                        onClick = {
                                            waterMode =
                                                mode
                                        },
                                        label = {
                                            Text(
                                                t(
                                                    mode.displayName
                                                )
                                            )
                                        }
                                    )
                                }
                        }

                        MoneyField(
                            value =
                                waterFlatPayment,
                            onValueChange = {
                                waterFlatPayment =
                                    it
                            },
                            label =
                                t("Havi vízátalány (Ft)")
                        )
                        MoneyField(
                            value =
                                waterUnitPrice,
                            onValueChange = {
                                waterUnitPrice =
                                    it
                            },
                            label =
                                if (
                                    waterMode ==
                                    GasBillingMode.FLAT_RATE
                                ) {
                                    t("Víz egységár – kimutatáshoz (Ft/m³)")
                                } else {
                                    t("Víz egységár (Ft/m³)")
                                }
                        )
                        MoneyField(
                            value =
                                waterFixedFee,
                            onValueChange = {
                                waterFixedFee =
                                    it
                            },
                            label =
                                t("Víz havi fix díj (Ft)")
                        )
                    }
                }
            }

            if (waterHotEnabled) {
                item {
                    TariffCard(
                        title = t("Meleg víz"),
                        subtitle =
                            t("Átalány vagy fogyasztás alapján"),
                        icon =
                            Icons.Default.WaterDrop,
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    ) {
                        Text(
                            t("Elszámolás módja"),
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            GasBillingMode.entries
                                .forEach { mode ->
                                    FilterChip(
                                        selected =
                                            waterHotMode ==
                                                mode,
                                        onClick = {
                                            waterHotMode =
                                                mode
                                        },
                                        label = {
                                            Text(
                                                t(
                                                    mode.displayName
                                                )
                                            )
                                        }
                                    )
                                }
                        }

                        MoneyField(
                            value =
                                waterHotFlatPayment,
                            onValueChange = {
                                waterHotFlatPayment =
                                    it
                            },
                            label =
                                t("Havi melegvíz-átalány (Ft)")
                        )
                        MoneyField(
                            value =
                                waterHotUnitPrice,
                            onValueChange = {
                                waterHotUnitPrice =
                                    it
                            },
                            label =
                                if (
                                    waterHotMode ==
                                    GasBillingMode.FLAT_RATE
                                ) {
                                    t("Melegvíz egységár – kimutatáshoz (Ft/m³)")
                                } else {
                                    t("Melegvíz egységár (Ft/m³)")
                                }
                        )
                        MoneyField(
                            value =
                                waterHotFixedFee,
                            onValueChange = {
                                waterHotFixedFee =
                                    it
                            },
                            label =
                                t("Melegvíz havi fix díj (Ft)")
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription =
                                    null
                            )
                            Column {
                                Text(
                                    t("Havi célok"),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )
                                Text(
                                    t("0 = nincs cél beállítva"),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }

                        MoneyField(
                            value = normalGoal,
                            onValueChange = {
                                normalGoal = it
                            },
                            label =
                                t("Normál áram cél (kWh/hó)")
                        )
                        MoneyField(
                            value = nightGoal,
                            onValueChange = {
                                nightGoal = it
                            },
                            label =
                                t("Éjszakai áram cél (kWh/hó)")
                        )
                        if (gasEnabled) {
                            MoneyField(
                                value = gasGoal,
                                onValueChange = {
                                    gasGoal = it
                                },
                                label =
                                    t("Gáz cél (m³/hó)")
                            )
                        }
                        if (waterEnabled) {
                            MoneyField(
                                value = waterGoal,
                                onValueChange = {
                                    waterGoal = it
                                },
                                label =
                                    t("Hideg víz cél (m³/hó)")
                            )
                        }
                        if (waterHotEnabled) {
                            MoneyField(
                                value = waterHotGoal,
                                onValueChange = { waterHotGoal = it },
                                label = t("Meleg víz cél (m³/hó)")
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default
                                    .NotificationsActive,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    12.dp
                                )
                            )
                            Column(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {
                                Text(
                                    t("Havi leolvasási emlékeztető"),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )
                                Text(
                                    t("A választott naptól minden nap emlékeztet, amíg valamelyik bekapcsolt havi mérés hiányzik."),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                            Switch(
                                checked =
                                    reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled =
                                        it
                                }
                            )
                        }

                        if (reminderEnabled) {
                            OutlinedTextField(
                                value =
                                    reminderDayText,
                                onValueChange = {
                                    value ->
                                    reminderDayText =
                                        value
                                            .filter {
                                                char ->
                                                char.isDigit()
                                            }
                                            .take(2)
                                },
                                modifier =
                                    Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        t("Emlékeztetés kezdőnapja (1–31)")
                                    )
                                },
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType =
                                            KeyboardType.Number
                                    ),
                                singleLine = true
                            )

                            OutlinedButton(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    showReminderTimePicker =
                                        true
                                }
                            ) {
                                Icon(
                                    Icons.Default
                                        .AccessTime,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        8.dp
                                    )
                                )
                                Text(
                                    t("Időpont: ") +
                                        formatReminderTime(
                                            reminderHour,
                                            reminderMinute
                                        )
                                )
                            }

                            Text(
                                t("Beállítva: minden hónap ") +
                                    (
                                        reminderDayText
                                            .toIntOrNull()
                                            ?.coerceIn(
                                                1,
                                                31
                                            )
                                            ?: 23
                                        ) +
                                    t(". napjától naponta kb. ") +
                                    formatReminderTime(
                                        reminderHour,
                                        reminderMinute
                                    ) +
                                    t(". Ha a hónap rövidebb, a hónap utolsó napján indul."),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(
                            vertical = 14.dp
                        ),
                    onClick = {
                        scope.launch {
                            viewModel
                                .updateSettings(
                                    currentSettings()
                                )
                            snackbarHostState
                                .showSnackbar(
                                    t("Beállítások elmentve.")
                                )
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text(
                        t("Beállítások mentése")
                    )
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Text(
                            t("Adatkezelés"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            t("Minden módosítás után automatikus helyi biztonsági mentés készül. Ha választasz saját mappát, az auto-backup.json oda is frissül. A kézi JSON mentés tartalmazza a méréseket, tarifákat, célokat és az emlékeztető beállítását. A fotók nincsenek beágyazva."),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                backupFolderLauncher
                                    .launch(null)
                            }
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null
                            )
                            Spacer(
                                Modifier.width(8.dp)
                            )
                            Text(
                                if (
                                    backupFolderUri
                                        .isBlank()
                                ) {
                                    t("Automatikus mentési mappa kiválasztása")
                                } else {
                                    t("Automatikus mentési mappa módosítása")
                                }
                            )
                        }

                        if (
                            backupFolderUri.isNotBlank()
                        ) {
                            Text(
                                t("Külső automatikus mentés: bekapcsolva"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            TextButton(
                                onClick = {
                                    backupFolderUri = ""
                                }
                            ) {
                                Text(
                                    t("Külső automatikus mentés kikapcsolása")
                                )
                            }
                        }

                        OutlinedButton(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            onClick = {
                                backupLauncher
                                    .launch(
                                        "meroora-backup-" +
                                            LocalDate.now() +
                                            ".json"
                                    )
                            }
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("Biztonsági mentés")
                            )
                        }

                        OutlinedButton(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            onClick = {
                                csvExportLauncher
                                    .launch(
                                        "meroallasok-" +
                                            LocalDate.now() +
                                            ".csv"
                                    )
                            }
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("CSV export")
                            )
                        }

                        OutlinedButton(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            onClick = {
                                csvImportLauncher
                                    .launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/plain",
                                            "text/comma-separated-values"
                                        )
                                    )
                            }
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("CSV import")
                            )
                        }

                        OutlinedButton(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            onClick = {
                                restoreLauncher
                                    .launch(
                                        arrayOf(
                                            "application/json",
                                            "text/plain"
                                        )
                                    )
                            }
                        ) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("Mentés visszaállítása")
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription =
                                    null
                            )
                            Text(
                                t("Fiók"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        val authUser =
                            authState.user

                        if (authUser != null) {
                            AppInfoLine(
                                t("Név"),
                                authUser
                                    .displayName
                                    .ifBlank {
                                        "–"
                                    }
                            )
                            AppInfoLine(
                                t("E-mail"),
                                authUser
                                    .email
                                    .ifBlank {
                                        "–"
                                    }
                            )
                        } else if (
                            authState.guestMode
                        ) {
                            Text(
                                t("Offline fejlesztői mód"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                authViewModel
                                    .replayOnboarding()
                            }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("Útmutató újranézése")
                            )
                        }

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                authViewModel
                                    .signOut()
                            }
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("Kijelentkezés")
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerLow
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription =
                                    null
                            )
                            Text(
                                t("Alkalmazás"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        AppInfoLine(
                            t("Verzió"),
                            BuildConfig.VERSION_NAME
                        )

                        val updateText =
                            when (
                                val state =
                                    updateState
                            ) {
                                UpdateState.Idle ->
                                    t("Még nincs ellenőrizve")

                                UpdateState.Checking ->
                                    t("Frissítés keresése…")

                                UpdateState.UpToDate ->
                                    t("A legfrissebb verzió van telepítve.")

                                is UpdateState.Available ->
                                    t("Új verzió: ") +
                                        state.info.version

                                is UpdateState.Error ->
                                    t("Ellenőrzési hiba: ") +
                                        state.message
                            }

                        Text(
                            updateText,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                if (
                                    updateState is
                                    UpdateState.Available
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                }
                        )

                        OutlinedButton(
                            modifier =
                                Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel
                                    .checkForUpdates()
                            },
                            enabled =
                                updateState !=
                                    UpdateState.Checking
                        ) {
                            Icon(
                                Icons.Default
                                    .SystemUpdateAlt,
                                contentDescription =
                                    null
                            )
                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )
                            Text(
                                t("Frissítések keresése")
                            )
                        }

                        val available =
                            (updateState as?
                                UpdateState.Available)
                                ?.info

                        if (available != null) {
                            Button(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse(
                                                    available
                                                        .downloadUrl
                                                )
                                            )
                                        )
                                    }
                                }
                            ) {
                                Text(
                                    t("Frissítés letöltése")
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {
                        showClearConfirmation =
                            true
                    }
                ) {
                    Icon(
                        Icons.Default
                            .DeleteForever,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text(
                        t("Minden adat törlése")
                    )
                }
            }

            if (authState.user != null) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.30f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Fiók végleges törlése",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                "A Firebase-fiók és az ehhez a fiókhoz tartozó helyi mérőállások, " +
                                    "beállítások és automatikus mentések törlésére szolgál. " +
                                    "A megosztott családi mappa és a korábban kézzel exportált " +
                                    "fájlok nem törlődnek automatikusan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    accountDeletionPhrase = ""
                                    accountDeletionPassword = ""
                                    authViewModel.clearDeletionError()
                                    showDeleteAccountConfirmation = true
                                },
                                enabled = !deletingAccount,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Fiók törlése")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    t("A költség- és rezsikeret-számítás becslés. A tényleges számla függhet a szolgáltatói tarifától, fűtőértéktől, korrekciós tényezőtől és az elszámolási időszaktól."),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }

    if (showReminderTimePicker) {
        val timePickerState =
            rememberTimePickerState(
                initialHour =
                    reminderHour,
                initialMinute =
                    reminderMinute,
                is24Hour = true
            )

        AlertDialog(
            onDismissRequest = {
                showReminderTimePicker =
                    false
            },
            title = {
                Text(
                    t("Emlékeztetés időpontja")
                )
            },
            text = {
                TimePicker(
                    state =
                        timePickerState
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderHour =
                            timePickerState.hour
                        reminderMinute =
                            timePickerState.minute
                        showReminderTimePicker =
                            false
                    }
                ) {
                    Text(t("Beállítás"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReminderTimePicker =
                            false
                    }
                ) {
                    Text(t("Mégse"))
                }
            }
        )
    }

    if (showDeleteAccountConfirmation && authState.user != null) {
        val requiresPassword = authViewModel.deletionRequiresPassword
        val activity = context.findHostActivity()
        AlertDialog(
            onDismissRequest = {
                if (!deletingAccount) {
                    showDeleteAccountConfirmation = false
                    authViewModel.clearDeletionError()
                }
            },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Biztosan törlöd a fiókodat?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ez véglegesen törli a Firebase-fiókodat és az alkalmazásban " +
                            "ehhez a fiókhoz mentett helyi adataidat. " +
                            "A művelet nem vonható vissza."
                    )
                    Text(
                        "Fiók: " + authState.user!!.email,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "A közösen használt mappa és a korábban exportált fájlok " +
                            "megmaradhatnak. Törlés előtt készíts mentést, ha szükséged van rájuk.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = accountDeletionPhrase,
                        onValueChange = { accountDeletionPhrase = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !deletingAccount,
                        label = { Text("Írd be: TÖRLÉS") }
                    )
                    if (requiresPassword) {
                        OutlinedTextField(
                            value = accountDeletionPassword,
                            onValueChange = { accountDeletionPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !deletingAccount,
                            label = { Text("Jelszó a megerősítéshez") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    } else {
                        Text(
                            "A Google-fiókodat még egyszer meg kell erősítened.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!accountDeletionError.isNullOrBlank()) {
                        Text(
                            accountDeletionError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activity?.let {
                            authViewModel.deleteAccount(it, accountDeletionPassword)
                        }
                    },
                    enabled = !deletingAccount &&
                        accountDeletionPhrase.trim() == "TÖRLÉS" &&
                        (!requiresPassword || accountDeletionPassword.isNotBlank()) &&
                        activity != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (deletingAccount) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.width(18.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("Fiók végleges törlése")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        showDeleteAccountConfirmation = false
                        authViewModel.clearDeletionError()
                    }
                ) {
                    Text("Mégse")
                }
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation =
                    false
            },
            title = {
                Text(
                    t("Minden adat törlése")
                )
            },
            text = {
                Text(
                    t("Ez törli az összes mérőállást és beállítást. Előtte érdemes biztonsági mentést készíteni.")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation =
                            false
                        scope.launch {
                            viewModel
                                .clearAllData()
                                .onSuccess {
                                    snackbarHostState
                                        .showSnackbar(
                                            t("Minden adat törölve.")
                                        )
                                }
                                .onFailure {
                                    snackbarHostState
                                        .showSnackbar(
                                            t("Törlési hiba: ") +
                                                (
                                                    it.message
                                                        ?: t("ismeretlen hiba")
                                                    )
                                        )
                                }
                        }
                    }
                ) {
                    Text(t("Törlés"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation =
                            false
                    }
                ) {
                    Text(t("Mégse"))
                }
            }
        )
    }
}

@Composable
private fun TariffCard(
    title: String,
    subtitle: String,
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    containerColor:
        androidx.compose.ui.graphics.Color,
    content:
        @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    containerColor
            )
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                Icon(
                    icon,
                    contentDescription = null
                )
                Column {
                    Text(
                        title,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun MeterToggleRow(
    type: MeterType,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                playMeterColor(type).copy(alpha = 0.09f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayMeterIcon(type = type, size = 42.dp)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange:
        (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange =
            onValueChange,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Decimal
            ),
        singleLine = true
    )
}


@Composable
private fun AppInfoLine(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
        Spacer(
            Modifier.width(16.dp)
        )
        Text(
            value,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

private fun formatReminderTime(
    hour: Int,
    minute: Int
): String =
    hour.coerceIn(0, 23)
        .toString()
        .padStart(2, '0') +
        ":" +
        minute.coerceIn(0, 59)
            .toString()
            .padStart(2, '0')

private fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
