package hu.merenyimiklos.meterreader.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private enum class HistoryFilter(
    val label: String,
    val meterType: MeterType?
) {
    ALL(t("Mind"), null),
    ELECTRICITY(t("Normál"), MeterType.ELECTRICITY),
    ELECTRICITY_NIGHT(
        t("Éjszakai"),
        MeterType.ELECTRICITY_NIGHT
    ),
    GAS(t("Gáz"), MeterType.GAS),
    WATER(t("Víz"), MeterType.WATER)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    viewModel: MeterViewModel
) {
    val readings by
        viewModel.readings
            .collectAsStateWithLifecycle()
    val settings by
        viewModel.settings
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

    var previewPhotoUri by remember {
        mutableStateOf<String?>(null)
    }

    var detailReading by remember {
        mutableStateOf<MeterReading?>(null)
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
                                    t("Excel export elkészült.")
                                )
                        }
                        .onFailure {
                            snackbarHostState
                                .showSnackbar(
                                    t("Export hiba: ") +
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
                    t("Mérési napló"),
                subtitle =
                    readings.size
                        .toString() +
                        t(" rögzített mérés"),
                icon =
                    Icons.Default.ReceiptLong,
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
                            Icons.Default.Download,
                            contentDescription =
                                t("Excel export"),
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary
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
                            t("Nincs megjeleníthető mérőállás."),
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
                            .clickable {
                                detailReading =
                                    reading
                            }
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
                                        .displayName
                                        .let(::t),
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
                                            t("Fogyasztás az előző mérés óta: ") +
                                                formatDecimal(
                                                    it
                                                ) +
                                                " " +
                                                reading
                                                    .type
                                                    .unit
                                        }
                                        ?: t("Első mérés – még nincs fogyasztási különbség."),
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
                                if (
                                    reading.photoUri != null
                                ) {
                                    IconButton(
                                        onClick = {
                                            previewPhotoUri =
                                                reading.photoUri
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription =
                                                t("Mérőfotó")
                                        )
                                    }
                                }

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
                                            t("Szerkesztés")
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
                                            t("Törlés")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailReading?.let { reading ->
        ReadingDetailDialog(
            reading = reading,
            readings = readings,
            settings = settings,
            consumption =
                consumptionById[
                    reading.id
                ],
            onDismiss = {
                detailReading = null
            }
        )
    }

    previewPhotoUri?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                previewPhotoUri = null
            },
            title = {
                Text(t("Eredeti mérőfotó"))
            },
            text = {
                PhotoPreview(uri)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        previewPhotoUri = null
                    }
                ) {
                    Text(t("Bezárás"))
                }
            }
        )
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
                                t("Mérőállás módosítva.")
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
                    t("Mérőállás törlése")
                )
            },
            text = {
                Text(
                    t("Biztosan törlöd a ") +
                        reading
                            .type
                            .displayName
                            .let(::t)
                            .lowercase() +
                        " " +
                        LocalDate
                            .ofEpochDay(
                                reading
                                    .dateEpochDay
                            ) +
                        t(" napi mérését?")
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
                    Text(t("Törlés"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingReading =
                            null
                    }
                ) {
                    Text(t("Mégse"))
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
                    t(" szerkesztése")
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
                            t("Mérőállás (") +
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
                            t("Dátum (ÉÉÉÉ-HH-NN)")
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
                            t("Megjegyzés")
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
                Text(t("Mentés"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(t("Mégse"))
            }
        }
    )
}


@Composable
private fun PhotoPreview(
    uriString: String
) {
    val context = LocalContext.current

    val bitmap by produceState<
        android.graphics.Bitmap?
    >(
        initialValue = null,
        uriString
    ) {
        value = withContext(
            Dispatchers.IO
        ) {
            runCatching {
                context.contentResolver
                    .openInputStream(
                        Uri.parse(
                            uriString
                        )
                    )
                    ?.use {
                        BitmapFactory
                            .decodeStream(it)
                    }
            }.getOrNull()
        }
    }

    if (bitmap == null) {
        Text(
            t("A fotó már nem érhető el.")
        )
    } else {
        Image(
            bitmap =
                bitmap!!.asImageBitmap(),
            contentDescription =
                t("Mérőfotó"),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    max = 520.dp
                ),
            contentScale =
                ContentScale.Fit
        )
    }
}


@Composable
private fun ReadingDetailDialog(
    reading: MeterReading,
    readings: List<MeterReading>,
    settings: BillingSettings,
    consumption: Double?,
    onDismiss: () -> Unit
) {
    val previous =
        readings
            .filter {
                it.type ==
                    reading.type &&
                    it.dateEpochDay <
                        reading.dateEpochDay
            }
            .maxByOrNull {
                it.dateEpochDay
            }

    val days =
        previous?.let {
            reading.dateEpochDay -
                it.dateEpochDay
        }

    val dailyAverage =
        if (
            consumption != null &&
            days != null &&
            days > 0
        ) {
            consumption /
                days.toDouble()
        } else {
            null
        }

    val costs =
        UsageCalculator
            .estimatedPayableForReading(
                reading = reading,
                consumption =
                    consumption,
                settings = settings
            )

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                reading.type.displayName
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                DetailLine(
                    t("Dátum"),
                    LocalDate
                        .ofEpochDay(
                            reading.dateEpochDay
                        )
                        .toString()
                )
                DetailLine(
                    t("Mérőállás"),
                    formatDecimal(
                        reading.value
                    ) +
                        " " +
                        reading.type.unit
                )

                previous?.let {
                    DetailLine(
                        t("Előző állás"),
                        formatDecimal(
                            it.value
                        ) +
                            " " +
                            reading.type.unit
                    )
                }

                consumption?.let {
                    DetailLine(
                        t("Fogyasztás"),
                        formatDecimal(it) +
                            " " +
                            reading.type.unit
                    )
                }

                days?.takeIf {
                    it > 0
                }?.let {
                    DetailLine(
                        t("Eltelt idő"),
                        it.toString() +
                            t(" nap")
                    )
                }

                dailyAverage?.let {
                    DetailLine(
                        t("Napi átlag"),
                        formatDecimal(it) +
                            " " +
                            reading.type.unit +
                            t("/nap")
                    )
                }

                costs.second?.let {
                    DetailLine(
                        t("Becsült fizetendő"),
                        formatMoney(it)
                    )
                }

                if (
                    reading.note.isNotBlank()
                ) {
                    DetailLine(
                        t("Megjegyzés"),
                        reading.note
                    )
                }

                DetailLine(
                    t("Fotó"),
                    if (
                        reading.photoUri !=
                        null
                    ) {
                        t("Elérhető a kép ikonnal")
                    } else {
                        t("Nincs")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(t("Bezárás"))
            }
        }
    )
}

@Composable
private fun DetailLine(
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
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
        Spacer(
            Modifier.width(12.dp)
        )
        Text(
            value,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}
