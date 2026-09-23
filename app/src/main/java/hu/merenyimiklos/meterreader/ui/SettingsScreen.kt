package hu.merenyimiklos.meterreader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

@Composable
internal fun SettingsScreen(viewModel: MeterViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var electricityUnitPrice by remember(settings) {
        mutableStateOf(editableNumber(settings.electricityUnitPrice))
    }
    var electricityFixedFee by remember(settings) {
        mutableStateOf(editableNumber(settings.electricityMonthlyFixedFee))
    }
    var gasMode by remember(settings) {
        mutableStateOf(settings.gasBillingMode)
    }
    var gasUnitPrice by remember(settings) {
        mutableStateOf(editableNumber(settings.gasUnitPrice))
    }
    var gasFixedFee by remember(settings) {
        mutableStateOf(editableNumber(settings.gasMonthlyFixedFee))
    }
    var gasFlatPayment by remember(settings) {
        mutableStateOf(editableNumber(settings.gasFlatMonthlyPayment))
    }
    var showClearConfirmation by remember {
        mutableStateOf(false)
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportBackup(uri)
                    .onSuccess {
                        snackbarHostState.showSnackbar(
                            "Biztonsági mentés elkészült."
                        )
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar(
                            "Mentési hiba: " +
                                (it.message ?: "ismeretlen hiba")
                        )
                    }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.importBackup(uri)
                    .onSuccess { count ->
                        snackbarHostState.showSnackbar(
                            count.toString() +
                                " mérőállás visszaállítva."
                        )
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar(
                            "Visszaállítási hiba: " +
                                (it.message ?: "ismeretlen hiba")
                        )
                    }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Beállítások",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Áram",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            MoneyField(
                value = electricityUnitPrice,
                onValueChange = {
                    electricityUnitPrice = it
                },
                label = "Egységár (Ft/kWh)"
            )

            MoneyField(
                value = electricityFixedFee,
                onValueChange = {
                    electricityFixedFee = it
                },
                label = "Havi fix díj (Ft)"
            )

            HorizontalDivider()

            Text(
                "Gáz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text("Elszámolás módja")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GasBillingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = gasMode == mode,
                        onClick = {
                            gasMode = mode
                        },
                        label = {
                            Text(mode.displayName)
                        }
                    )
                }
            }

            MoneyField(
                value = gasFlatPayment,
                onValueChange = {
                    gasFlatPayment = it
                },
                label = "Havi gázátalány (Ft)"
            )

            MoneyField(
                value = gasUnitPrice,
                onValueChange = {
                    gasUnitPrice = it
                },
                label = if (
                    gasMode == GasBillingMode.FLAT_RATE
                ) {
                    "Gáz egységár – opcionális kimutatáshoz (Ft/m³)"
                } else {
                    "Gáz egységár (Ft/m³)"
                }
            )

            MoneyField(
                value = gasFixedFee,
                onValueChange = {
                    gasFixedFee = it
                },
                label = "Gáz havi fix díj (Ft)"
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        viewModel.updateSettings(
                            BillingSettings(
                                electricityUnitPrice = parseDecimal(
                                    electricityUnitPrice
                                ),
                                electricityMonthlyFixedFee = parseDecimal(
                                    electricityFixedFee
                                ),
                                gasBillingMode = gasMode,
                                gasUnitPrice = parseDecimal(
                                    gasUnitPrice
                                ),
                                gasMonthlyFixedFee = parseDecimal(
                                    gasFixedFee
                                ),
                                gasFlatMonthlyPayment = parseDecimal(
                                    gasFlatPayment
                                )
                            )
                        )

                        snackbarHostState.showSnackbar(
                            "Beállítások elmentve."
                        )
                    }
                }
            ) {
                Text("Díjbeállítások mentése")
            }

            HorizontalDivider()

            Text(
                "Adatkezelés",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Biztonsági mentés",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        "A JSON mentés tartalmazza az összes mérőállást és díjbeállítást. A fotók nincsenek beágyazva.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            backupLauncher.launch(
                                "meroora-backup-" +
                                    LocalDate.now() +
                                    ".json"
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Backup,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Biztonsági mentés készítése")
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain"
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mentés visszaállítása")
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    showClearConfirmation = true
                }
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Minden adat törlése")
            }

            Card(
                Modifier.fillMaxWidth()
            ) {
                Text(
                    "A költségszámítás becslés. A tényleges számla függhet tarifától, kedvezményes sávtól, korrekciós tényezőtől és egyéb szolgáltatói díjaktól.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirmation = false
            },
            title = {
                Text("Minden adat törlése")
            },
            text = {
                Text(
                    "Ez törli az összes mérőállást és díjbeállítást. Előtte érdemes biztonsági mentést készíteni."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        scope.launch {
                            viewModel.clearAllData()
                                .onSuccess {
                                    snackbarHostState.showSnackbar(
                                        "Minden adat törölve."
                                    )
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar(
                                        "Törlési hiba: " +
                                            (it.message
                                                ?: "ismeretlen hiba")
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
                        showClearConfirmation = false
                    }
                ) {
                    Text("Mégse")
                }
            }
        )
    }
}

@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(label)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true
    )
}
