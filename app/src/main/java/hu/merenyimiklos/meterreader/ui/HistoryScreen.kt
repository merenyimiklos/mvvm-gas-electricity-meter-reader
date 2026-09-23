package hu.merenyimiklos.meterreader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class HistoryFilter(
    val label: String,
    val meterType: MeterType?
) {
    ALL("Mind", null),
    ELECTRICITY("Normál", MeterType.ELECTRICITY),
    ELECTRICITY_NIGHT(
        "Éjszakai",
        MeterType.ELECTRICITY_NIGHT
    ),
    GAS("Gáz", MeterType.GAS)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    viewModel: MeterViewModel
) {
    val readings by
        viewModel.readings
            .collectAsStateWithLifecycle()

    val consumptionById =
        remember(readings) {
            UsageCalculator
                .consumptionByReading(
                    readings
                )
        }

    val scope =
        rememberCoroutineScope()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    var filter by remember {
        mutableStateOf(
            HistoryFilter.ALL
        )
    }

    var editingReading by remember {
        mutableStateOf<MeterReading?>(
            null
        )
    }

    var deletingReading by remember {
        mutableStateOf<MeterReading?>(
            null
        )
    }

    val filteredReadings =
        readings
            .filter { reading ->
                filter.meterType == null ||
                    reading.type ==
                        filter.meterType
            }
            .sortedWith(
                compareByDescending<MeterReading> {
                    it.dateEpochDay
                }.thenByDescending {
                    it.createdAtMillis
                }
            )

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .CreateDocument(
                    XlsxExporter.MIME_TYPE
                )
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel
                        .exportXlsx(uri)
                        .onSuccess {
                            snackbarHostState
                                .showSnackbar(
                                    "Excel export elkészült."
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    "Export hiba: " +
                                        (
                                            it.message
                                                ?: "ismeretlen hiba"
                                            )
                                )
                        }
                }
            }
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Mérési napló")
                        Text(
                            readings.size
                                .toString() +
                                " rögzített mérés",
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
                },
                actions = {
                    IconButton(
                        onClick = {
                            exportLauncher
                                .launch(
                                    "meroallasok-" +
                                        LocalDate
                                            .now() +
                                        ".xlsx"
                                )
                        }
                    ) {
                        Icon(
                            Icons.Default
                                .Download,
                            contentDescription =
                                "Excel export"
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
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            item {
                LazyRow(
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    items(
                        HistoryFilter
                            .entries
                    ) { item ->
                        FilterChip(
                            selected =
                                filter == item,
                            onClick = {
                                filter = item
                            },
                            label = {
                                Text(
                                    item.label
                                )
                            }
                        )
                    }
                }
            }

            if (
                filteredReadings
                    .isEmpty()
            ) {
                item {
                    ElevatedCard(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal =
                                        16.dp
                                )
                                .fillMaxWidth(),
                        colors =
                            CardDefaults
                                .elevatedCardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceContainerLow
                                )
                    ) {
                        Text(
                            "Nincs megjeleníthető mérőállás.",
                            modifier =
                                Modifier.padding(
                                    20.dp
                                )
                        )
                    }
                }
            } else {
                items(
                    items =
                        filteredReadings,
                    key = {
                        it.id
                    }
                ) { reading ->
                    val consumption =
                        consumptionById[
                            reading.id
                        ]

                    ElevatedCard(
                        modifier = Modifier
                            .padding(
                                horizontal =
                                    16.dp
                            )
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        18.dp
                                    ),
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f),
                                verticalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            4.dp
                                        )
                            ) {
                                Text(
                                    reading
                                        .type
                                        .displayName,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight
                                            .SemiBold
                                )

                                Text(
                                    formatDecimal(
                                        reading
                                            .value
                                    ) +
                                        " " +
                                        reading
                                            .type
                                            .unit,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .headlineSmall,
                                    fontWeight =
                                        FontWeight
                                            .Bold
                                )

                                Text(
                                    LocalDate
                                        .ofEpochDay(
                                            reading
                                                .dateEpochDay
                                        )
                                        .toString(),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )

                                Text(
                                    consumption
                                        ?.let {
                                            "Fogyasztás az előző mérés óta: " +
                                                formatDecimal(
                                                    it
                                                ) +
                                                " " +
                                                reading
                                                    .type
                                                    .unit
                                        }
                                        ?: "Első mérés – még nincs fogyasztási különbség.",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )

                                if (
                                    reading.note
                                        .isNotBlank()
                                ) {
                                    HorizontalDivider(
                                        Modifier
                                            .padding(
                                                vertical =
                                                    6.dp
                                            )
                                    )

                                    Text(
                                        reading.note,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium
                                    )
                                }
                            }

                            Spacer(
                                Modifier.width(
                                    8.dp
                                )
                            )

                            Column {
                                IconButton(
                                    onClick = {
                                        editingReading =
                                            reading
                                    }
                                ) {
                                    Icon(
                                        Icons.Default
                                            .Edit,
                                        contentDescription =
                                            "Szerkesztés"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        deletingReading =
                                            reading
                                    }
                                ) {
                                    Icon(
                                        Icons.Default
                                            .Delete,
                                        contentDescription =
                                            "Törlés"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingReading?.let {
        reading ->
        EditReadingDialog(
            reading = reading,
            onDismiss = {
                editingReading =
                    null
            },
            onSave = {
                value,
                date,
                note ->
                scope.launch {
                    val error =
                        viewModel
                            .updateReading(
                                original =
                                    reading,
                                valueText =
                                    value,
                                dateText =
                                    date,
                                note =
                                    note
                            )

                    if (error == null) {
                        editingReading =
                            null
                        snackbarHostState
                            .showSnackbar(
                                "Mérőállás módosítva."
                            )
                    } else {
                        snackbarHostState
                            .showSnackbar(
                                error
                            )
                    }
                }
            }
        )
    }

    deletingReading?.let {
        reading ->
        AlertDialog(
            onDismissRequest = {
                deletingReading =
                    null
            },
            title = {
                Text(
                    "Mérőállás törlése"
                )
            },
            text = {
                Text(
                    "Biztosan törlöd a " +
                        reading
                            .type
                            .displayName
                            .lowercase() +
                        " " +
                        LocalDate
                            .ofEpochDay(
                                reading
                                    .dateEpochDay
                            ) +
                        " napi mérését?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel
                            .deleteReading(
                                reading.id
                            )
                        deletingReading =
                            null
                    }
                ) {
                    Text("Törlés")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingReading =
                            null
                    }
                ) {
                    Text("Mégse")
                }
            }
        )
    }
}

@Composable
private fun EditReadingDialog(
    reading: MeterReading,
    onDismiss: () -> Unit,
    onSave:
        (String, String, String) -> Unit
) {
    var value by remember(
        reading.id
    ) {
        mutableStateOf(
            editableNumber(
                reading.value
            )
        )
    }

    var date by remember(
        reading.id
    ) {
        mutableStateOf(
            LocalDate
                .ofEpochDay(
                    reading
                        .dateEpochDay
                )
                .toString()
        )
    }

    var note by remember(
        reading.id
    ) {
        mutableStateOf(
            reading.note
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                reading
                    .type
                    .displayName +
                    " szerkesztése"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                    },
                    label = {
                        Text(
                            "Mérőállás (" +
                                reading
                                    .type
                                    .unit +
                                ")"
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType
                                    .Decimal
                        ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                    },
                    label = {
                        Text(
                            "Dátum (ÉÉÉÉ-HH-NN)"
                        )
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text(
                            "Megjegyzés"
                        )
                    },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        value,
                        date,
                        note
                    )
                }
            ) {
                Text("Mentés")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Mégse")
            }
        }
    )
}
