package hu.merenyimiklos.meterreader.ui

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

private enum class ReadingEntryMode(
    val label: String
) {
    QUICK("Gyors havi"),
    SINGLE("Egyedi / fotós")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddReadingScreen(
    viewModel: MeterViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val readings by
        viewModel.readings.collectAsStateWithLifecycle()
    val settings by
        viewModel.settings.collectAsStateWithLifecycle()

    var mode by remember {
        mutableStateOf(ReadingEntryMode.QUICK)
    }
    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Mérőállás rögzítése")
                        Text(
                            "Gyors havi leolvasás vagy egyedi mérés",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ReadingEntryMode.entries) { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = { mode = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }

            item {
                DateSelector(
                    date = selectedDate,
                    onClick = {
                        showDatePicker = true
                    }
                )
            }

            when (mode) {
                ReadingEntryMode.QUICK -> {
                    item {
                        QuickMonthlyReading(
                            viewModel = viewModel,
                            readings = readings,
                            settings = settings,
                            date = selectedDate,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }

                ReadingEntryMode.SINGLE -> {
                    item {
                        SingleReadingEntry(
                            viewModel = viewModel,
                            readings = readings,
                            settings = settings,
                            date = selectedDate,
                            context = context,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis =
            selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    initialMillis
            )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState
                            .selectedDateMillis
                            ?.let { millis ->
                                selectedDate =
                                    LocalDate.ofEpochDay(
                                        millis / 86_400_000L
                                    )
                            }
                        showDatePicker = false
                    }
                ) {
                    Text("Kiválasztás")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Mégse")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}

@Composable
private fun DateSelector(
    date: LocalDate,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text("Mérés dátuma: " + date)
    }
}

@Composable
private fun QuickMonthlyReading(
    viewModel: MeterViewModel,
    readings: List<MeterReading>,
    settings: BillingSettings,
    date: LocalDate,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    var normal by remember(date) {
        mutableStateOf("")
    }
    var night by remember(date) {
        mutableStateOf("")
    }
    var gas by remember(date) {
        mutableStateOf("")
    }
    var note by remember(date) {
        mutableStateOf("")
    }
    var pendingDuplicateTypes by remember {
        mutableStateOf<List<MeterType>>(emptyList())
    }

    fun values(): Map<MeterType, String> =
        mapOf(
            MeterType.ELECTRICITY to normal,
            MeterType.ELECTRICITY_NIGHT to night,
            MeterType.GAS to gas
        )

    fun clear() {
        normal = ""
        night = ""
        gas = ""
        note = ""
    }

    fun save(
        allowDuplicate: Boolean
    ) {
        scope.launch {
            val result =
                viewModel.saveQuickReadings(
                    values = values(),
                    date = date,
                    note = note,
                    allowMonthlyDuplicate =
                        allowDuplicate
                )

            when {
                result.success -> {
                    clear()
                    pendingDuplicateTypes =
                        emptyList()
                    snackbarHostState
                        .showSnackbar(
                            "✓ Mindhárom mérőállás sikeresen rögzítve"
                        )
                }

                result.duplicateTypes
                    .isNotEmpty() -> {
                    pendingDuplicateTypes =
                        result.duplicateTypes
                }

                else -> {
                    snackbarHostState
                        .showSnackbar(
                            result.error
                                ?: "A mentés nem sikerült."
                        )
                }
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceContainerLow
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Gyors havi leolvasás",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    "Írd be egymás után a három óra állását, és egyben elmentjük.",
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

            QuickMeterField(
                type = MeterType.ELECTRICITY,
                value = normal,
                onValueChange = {
                    normal = it
                },
                date = date,
                readings = readings,
                settings = settings
            )

            QuickMeterField(
                type =
                    MeterType.ELECTRICITY_NIGHT,
                value = night,
                onValueChange = {
                    night = it
                },
                date = date,
                readings = readings,
                settings = settings
            )

            QuickMeterField(
                type = MeterType.GAS,
                value = gas,
                onValueChange = {
                    gas = it
                },
                date = date,
                readings = readings,
                settings = settings
            )

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Közös megjegyzés – opcionális"
                    )
                },
                minLines = 2
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        vertical = 14.dp
                    ),
                onClick = {
                    save(false)
                }
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mindhárom mentése"
                )
            }
        }
    }

    if (pendingDuplicateTypes.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                pendingDuplicateTypes =
                    emptyList()
            },
            title = {
                Text(
                    "Ebben a hónapban már van mérés"
                )
            },
            text = {
                Text(
                    "Már található " +
                        pendingDuplicateTypes
                            .joinToString(", ") {
                                it.displayName
                            } +
                        " mérés erre a hónapra. Ha folytatod, új mérésként kerülnek be az értékek."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        save(true)
                    }
                ) {
                    Text("Mentés így is")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDuplicateTypes =
                            emptyList()
                    }
                ) {
                    Text("Mégse")
                }
            }
        )
    }
}

@Composable
private fun QuickMeterField(
    type: MeterType,
    value: String,
    onValueChange: (String) -> Unit,
    date: LocalDate,
    readings: List<MeterReading>,
    settings: BillingSettings
) {
    val previous =
        readings
            .filter {
                it.type == type &&
                    it.dateEpochDay <
                        date.toEpochDay()
            }
            .maxByOrNull {
                it.dateEpochDay
            }

    val current =
        value
            .replace(',', '.')
            .toDoubleOrNull()

    val consumption =
        if (
            current != null &&
            previous != null &&
            current >= previous.value
        ) {
            current - previous.value
        } else {
            null
        }

    val estimated =
        estimateCost(
            type = type,
            consumption = consumption,
            settings = settings
        )

    Column(
        verticalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange =
                onValueChange,
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    type.displayName +
                        " (" +
                        type.unit +
                        ")"
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Decimal
                ),
            singleLine = true
        )

        Text(
            previous?.let {
                "Előző: " +
                    formatDecimal(it.value) +
                    " " +
                    type.unit +
                    " · " +
                    LocalDate.ofEpochDay(
                        it.dateEpochDay
                    )
            } ?: "Még nincs korábbi mérés.",
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        consumption?.let {
            val costText =
                estimated?.let { cost ->
                    " · kb. " +
                        formatMoney(cost)
                }.orEmpty()

            Text(
                "+" +
                    formatDecimal(it) +
                    " " +
                    type.unit +
                    costText,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }

        if (
            current != null &&
            previous != null &&
            current < previous.value
        ) {
            Text(
                "Az érték kisebb a korábbi mérőállásnál.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )
        }
    }
}

@Composable
private fun SingleReadingEntry(
    viewModel: MeterViewModel,
    readings: List<MeterReading>,
    settings: BillingSettings,
    date: LocalDate,
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    var meterType by remember {
        mutableStateOf(
            MeterType.ELECTRICITY
        )
    }
    var valueText by remember {
        mutableStateOf("")
    }
    var note by remember {
        mutableStateOf("")
    }
    var photoUriForSavedReading by remember {
        mutableStateOf<String?>(null)
    }
    var pendingCameraUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var ocrText by remember {
        mutableStateOf("")
    }
    var ocrLoading by remember {
        mutableStateOf(false)
    }

    val previous =
        readings
            .filter {
                it.type == meterType &&
                    it.dateEpochDay <
                        date.toEpochDay()
            }
            .maxByOrNull {
                it.dateEpochDay
            }

    val parsed =
        valueText
            .replace(',', '.')
            .toDoubleOrNull()
    val consumption =
        if (
            parsed != null &&
            previous != null &&
            parsed >= previous.value
        ) {
            parsed - previous.value
        } else {
            null
        }

    fun clearForm() {
        valueText = ""
        note = ""
        photoUriForSavedReading = null
        pendingCameraUri = null
        ocrText = ""
    }

    fun analyze(
        uri: Uri,
        persistPhoto: Boolean
    ) {
        if (persistPhoto) {
            photoUriForSavedReading =
                uri.toString()
        }

        ocrLoading = true

        scope.launch {
            viewModel
                .detectReading(
                    uri,
                    meterType
                )
                .onSuccess { result ->
                    result.detectedValue
                        ?.let {
                            valueText =
                                editableNumber(it)
                        }

                    ocrText =
                        result.rawText

                    if (
                        result.detectedValue ==
                        null
                    ) {
                        snackbarHostState
                            .showSnackbar(
                                "Nem találtam biztosan mérőállást. Írd be kézzel."
                            )
                    }
                }
                .onFailure {
                    snackbarHostState
                        .showSnackbar(
                            "A képfelismerés nem sikerült: " +
                                (
                                    it.message
                                        ?: "ismeretlen hiba"
                                    )
                        )
                }

            ocrLoading = false
        }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .TakePicture()
        ) { success ->
            if (success) {
                pendingCameraUri
                    ?.let {
                        analyze(
                            it,
                            true
                        )
                    }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .GetContent()
        ) { uri ->
            uri?.let {
                analyze(
                    it,
                    false
                )
            }
        }

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Egyedi mérés",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                items(
                    MeterType.entries
                ) { type ->
                    FilterChip(
                        selected =
                            meterType ==
                                type,
                        onClick = {
                            meterType =
                                type
                            ocrText = ""
                        },
                        label = {
                            Text(
                                type.shortName
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = {
                    valueText = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Mérőállás (" +
                            meterType.unit +
                            ")"
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Decimal
                    ),
                singleLine = true
            )

            previous?.let {
                Text(
                    "Előző: " +
                        formatDecimal(
                            it.value
                        ) +
                        " " +
                        meterType.unit +
                        " · " +
                        LocalDate
                            .ofEpochDay(
                                it.dateEpochDay
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            consumption?.let {
                Text(
                    "Fogyasztás: +" +
                        formatDecimal(it) +
                        " " +
                        meterType.unit +
                        estimateCost(
                            meterType,
                            it,
                            settings
                        )?.let { cost ->
                            " · kb. " +
                                formatMoney(
                                    cost
                                )
                        }.orEmpty(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "Megjegyzés"
                    )
                },
                minLines = 2
            )

            Text(
                "Fotós leolvasás",
                fontWeight =
                    FontWeight.SemiBold
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Button(
                    enabled =
                        !ocrLoading,
                    onClick = {
                        runCatching {
                            createPhotoUri(
                                context
                            )
                        }.onSuccess {
                            uri ->
                            pendingCameraUri =
                                uri
                            cameraLauncher
                                .launch(uri)
                        }.onFailure {
                            scope.launch {
                                snackbarHostState
                                    .showSnackbar(
                                        "Nem sikerült képfájlt létrehozni."
                                    )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default
                            .CameraAlt,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(
                            6.dp
                        )
                    )
                    Text("Fotózás")
                }

                OutlinedButton(
                    enabled =
                        !ocrLoading,
                    onClick = {
                        galleryLauncher
                            .launch(
                                "image/*"
                            )
                    }
                ) {
                    Icon(
                        Icons.Default
                            .PhotoLibrary,
                        contentDescription =
                            null
                    )
                    Spacer(
                        Modifier.width(
                            6.dp
                        )
                    )
                    Text("Galéria")
                }
            }

            if (ocrLoading) {
                Row(
                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {
                    CircularProgressIndicator(
                        Modifier.size(
                            22.dp
                        )
                    )
                    Spacer(
                        Modifier.width(
                            10.dp
                        )
                    )
                    Text(
                        "Mérőállás felismerése…"
                    )
                }
            }

            if (ocrText.isNotBlank()) {
                Text(
                    "Felismert szöveg",
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    ocrText,
                    maxLines = 4,
                    overflow =
                        TextOverflow.Ellipsis,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        vertical = 14.dp
                    ),
                onClick = {
                    scope.launch {
                        val error =
                            viewModel
                                .saveReading(
                                    type =
                                        meterType,
                                    valueText =
                                        valueText,
                                    dateText =
                                        date.toString(),
                                    note = note,
                                    photoUri =
                                        photoUriForSavedReading
                                )

                        if (error == null) {
                            clearForm()
                            snackbarHostState
                                .showSnackbar(
                                    "✓ Sikeres rögzítés"
                                )
                        } else {
                            snackbarHostState
                                .showSnackbar(
                                    error
                                )
                        }
                    }
                }
            ) {
                Icon(
                    Icons.Default
                        .CheckCircle,
                    contentDescription =
                        null
                )
                Spacer(
                    Modifier.width(
                        8.dp
                    )
                )
                Text(
                    "Mérőállás mentése"
                )
            }
        }
    }
}

private fun estimateCost(
    type: MeterType,
    consumption: Double?,
    settings: BillingSettings
): Double? {
    val value = consumption ?: return null

    return when (type) {
        MeterType.ELECTRICITY ->
            if (
                settings
                    .electricityUnitPrice >
                0.0
            ) {
                value *
                    settings
                        .electricityUnitPrice +
                    settings
                        .electricityMonthlyFixedFee
            } else {
                null
            }

        MeterType.ELECTRICITY_NIGHT ->
            if (
                settings
                    .electricityNightUnitPrice >
                0.0
            ) {
                value *
                    settings
                        .electricityNightUnitPrice +
                    settings
                        .electricityNightMonthlyFixedFee
            } else {
                null
            }

        MeterType.GAS ->
            when (
                settings.gasBillingMode
            ) {
                GasBillingMode.FLAT_RATE ->
                    settings
                        .gasFlatMonthlyPayment
                        .takeIf {
                            it > 0.0
                        }

                GasBillingMode.METERED ->
                    if (
                        settings
                            .gasUnitPrice >
                        0.0
                    ) {
                        value *
                            settings
                                .gasUnitPrice +
                            settings
                                .gasMonthlyFixedFee
                    } else {
                        null
                    }
            }
    }
}

private fun createPhotoUri(
    context: Context
): Uri {
    val directory =
        File(
            context.filesDir,
            "meter_photos"
        ).apply {
            mkdirs()
        }

    val file =
        File.createTempFile(
            "meter_",
            ".jpg",
            directory
        )

    return FileProvider.getUriForFile(
        context,
        context.packageName +
            ".fileprovider",
        file
    )
}
