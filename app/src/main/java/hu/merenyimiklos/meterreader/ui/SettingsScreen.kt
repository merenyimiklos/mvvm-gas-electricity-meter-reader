package hu.merenyimiklos.meterreader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: MeterViewModel
) {
    val settings by
        viewModel.settings
            .collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState =
        remember { SnackbarHostState() }

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

    var showClearConfirmation by remember {
        mutableStateOf(false)
    }

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
                                    "Biztonsági mentés elkészült."
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    "Mentési hiba: " +
                                        (
                                            it.message
                                                ?: "ismeretlen hiba"
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
                                        " mérőállás visszaállítva."
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    "Visszaállítási hiba: " +
                                        (
                                            it.message
                                                ?: "ismeretlen hiba"
                                            )
                                )
                        }
                }
            }
        }

    fun currentSettings() =
        BillingSettings(
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
            gasHeatingValueMjPerM3 =
                parseDecimal(
                    gasHeatingValue
                ).takeIf {
                    it > 0.0
                } ?: 34.8,
            reminderEnabled =
                reminderEnabled,
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
                )
        )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Beállítások")
                        Text(
                            "Tarifák, célok és emlékeztetők",
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
                .padding(
                    innerPadding
                ),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            item {
                TariffCard(
                    title = "Normál áram",
                    subtitle =
                        "A nappali / normál mérő díjai",
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
                            "Egységár (Ft/kWh)"
                    )
                    MoneyField(
                        value =
                            electricityFixedFee,
                        onValueChange = {
                            electricityFixedFee =
                                it
                        },
                        label =
                            "Havi fix díj (Ft)"
                    )
                }
            }

            item {
                TariffCard(
                    title = "Éjszakai áram",
                    subtitle =
                        "Külön mérő és külön tarifa",
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
                            "Éjszakai egységár (Ft/kWh)"
                    )
                    MoneyField(
                        value = nightFixedFee,
                        onValueChange = {
                            nightFixedFee = it
                        },
                        label =
                            "Éjszakai havi fix díj (Ft)"
                    )
                }
            }

            item {
                TariffCard(
                    title = "Gáz",
                    subtitle =
                        "Átalány vagy fogyasztás alapján",
                    icon =
                        Icons.Default
                            .LocalFireDepartment,
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .tertiaryContainer
                ) {
                    Text(
                        "Elszámolás módja",
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
                                        Text(
                                            mode.displayName
                                        )
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
                            "Havi gázátalány (Ft)"
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
                                "Gáz egységár – kimutatáshoz (Ft/m³)"
                            } else {
                                "Gáz egységár (Ft/m³)"
                            }
                    )
                    MoneyField(
                        value = gasFixedFee,
                        onValueChange = {
                            gasFixedFee = it
                        },
                        label =
                            "Gáz havi fix díj (Ft)"
                    )
                    MoneyField(
                        value = gasHeatingValue,
                        onValueChange = {
                            gasHeatingValue = it
                        },
                        label =
                            "Fűtőérték a rezsikerethez (MJ/m³)"
                    )
                    Text(
                        "Alapérték: 34,8 MJ/m³. A pontos érték szolgáltatási területenként és időben is eltérhet.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
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
                                    "Havi célok",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )
                                Text(
                                    "0 = nincs cél beállítva",
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
                                "Normál áram cél (kWh/hó)"
                        )
                        MoneyField(
                            value = nightGoal,
                            onValueChange = {
                                nightGoal = it
                            },
                            label =
                                "Éjszakai áram cél (kWh/hó)"
                        )
                        MoneyField(
                            value = gasGoal,
                            onValueChange = {
                                gasGoal = it
                            },
                            label =
                                "Gáz cél (m³/hó)"
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
                    Row(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
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
                            Modifier.width(12.dp)
                        )
                        Column(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {
                            Text(
                                "Havi leolvasási emlékeztető",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            Text(
                                "Minden hónap 23-ától naponta kb. 18:00-kor jelez, amíg a normál, éjszakai és gázmérőből nincs e havi rögzítés.",
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
                                    "Beállítások elmentve."
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
                        "Beállítások mentése"
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
                            "Adatkezelés",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            "A JSON mentés tartalmazza a méréseket, tarifákat, célokat és az emlékeztető beállítását. A fotók nincsenek beágyazva.",
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
                                "Biztonsági mentés"
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
                                "Mentés visszaállítása"
                            )
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
                        "Minden adat törlése"
                    )
                }
            }

            item {
                Text(
                    "A költség- és rezsikeret-számítás becslés. A tényleges számla függhet a szolgáltatói tarifától, fűtőértéktől, korrekciós tényezőtől és az elszámolási időszaktól.",
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

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation =
                    false
            },
            title = {
                Text(
                    "Minden adat törlése"
                )
            },
            text = {
                Text(
                    "Ez törli az összes mérőállást és beállítást. Előtte érdemes biztonsági mentést készíteni."
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
                                            "Minden adat törölve."
                                        )
                                }
                                .onFailure {
                                    snackbarHostState
                                        .showSnackbar(
                                            "Törlési hiba: " +
                                                (
                                                    it.message
                                                        ?: "ismeretlen hiba"
                                                    )
                                        )
                                }
                        }
                    }
                ) {
                    Text("Törlés")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation =
                            false
                    }
                ) {
                    Text("Mégse")
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
