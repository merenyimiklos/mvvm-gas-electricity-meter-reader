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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private enum class HistoryFilter(val label: String) {
    ALL("Mind"),
    ELECTRICITY("Áram"),
    GAS("Gáz")
}

@Composable
internal fun HistoryScreen(viewModel: MeterViewModel) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val consumptionById = remember(readings) {
        UsageCalculator.consumptionByReading(readings)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var editingReading by remember { mutableStateOf<MeterReading?>(null) }
    var deletingReading by remember { mutableStateOf<MeterReading?>(null) }

    val filteredReadings = readings
        .filter {
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.ELECTRICITY -> it.type == MeterType.ELECTRICITY
                HistoryFilter.GAS -> it.type == MeterType.GAS
            }
        }
        .sortedWith(
            compareByDescending<MeterReading> { it.dateEpochDay }
                .thenByDescending { it.createdAtMillis }
        )

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XlsxExporter.MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportXlsx(uri)
                    .onSuccess {
                        snackbarHostState.showSnackbar(
                            "Excel export elkészült."
                        )
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar(
                            "Export hiba: " +
                                (it.message ?: "ismeretlen hiba")
                        )
                    }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Előzmények",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        exportLauncher.launch(
                            "meroallasok-" + LocalDate.now() + ".xlsx"
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Excel")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                HistoryFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item.label) }
                    )
                }
            }

            if (filteredReadings.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Nincs megjeleníthető mérőállás.",
                        Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredReadings,
                        key = { it.id }
                    ) { reading ->
                        val consumption = consumptionById[reading.id]

                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        reading.type.displayName + " • " +
                                            LocalDate.ofEpochDay(
                                                reading.dateEpochDay
                                            ),
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        formatDecimal(reading.value) +
                                            " " + reading.type.unit,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        consumption?.let {
                                            "Fogyasztás az előző mérés óta: " +
                                                formatDecimal(it) +
                                                " " + reading.type.unit
                                        } ?: "Első mérés – még nincs fogyasztási különbség.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (reading.note.isNotBlank()) {
                                        HorizontalDivider(
                                            Modifier.padding(vertical = 6.dp)
                                        )
                                        Text(
                                            reading.note,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingReading = reading
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Szerkesztés"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        deletingReading = reading
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Törlés"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    editingReading?.let { reading ->
        EditReadingDialog(
            reading = reading,
            onDismiss = {
                editingReading = null
            },
            onSave = { value, date, note ->
                scope.launch {
                    val error = viewModel.updateReading(
                        original = reading,
                        valueText = value,
                        dateText = date,
                        note = note
                    )

                    if (error == null) {
                        editingReading = null
                        snackbarHostState.showSnackbar(
                            "Mérőállás módosítva."
                        )
                    } else {
                        snackbarHostState.showSnackbar(error)
                    }
                }
            }
        )
    }

    deletingReading?.let { reading ->
        AlertDialog(
            onDismissRequest = {
                deletingReading = null
            },
            title = {
                Text("Mérőállás törlése")
            },
            text = {
                Text(
                    "Biztosan törlöd a " +
                        reading.type.displayName.lowercase() +
                        " " +
                        LocalDate.ofEpochDay(reading.dateEpochDay) +
                        " napi mérését?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReading(reading.id)
                        deletingReading = null
                    }
                ) {
                    Text("Törlés")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingReading = null
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
    onSave: (String, String, String) -> Unit
) {
    var value by remember(reading.id) {
        mutableStateOf(editableNumber(reading.value))
    }
    var date by remember(reading.id) {
        mutableStateOf(
            LocalDate.ofEpochDay(reading.dateEpochDay).toString()
        )
    }
    var note by remember(reading.id) {
        mutableStateOf(reading.note)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                reading.type.displayName +
                    " mérőállás szerkesztése"
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = {
                        Text(
                            "Mérőállás (" +
                                reading.type.unit +
                                ")"
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = {
                        Text("Dátum (ÉÉÉÉ-HH-NN)")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text("Megjegyzés")
                    },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(value, date, note)
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
