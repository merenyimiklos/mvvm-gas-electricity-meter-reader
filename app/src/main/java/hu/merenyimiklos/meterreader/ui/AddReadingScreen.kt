package hu.merenyimiklos.meterreader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronRight
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
import hu.merenyimiklos.meterreader.domain.UsageCalculator
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
    QUICK(t("Gyors havi")),
    SINGLE(t("Egyedi / fotós"))
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
        containerColor =
            MaterialTheme
                .colorScheme
                .background,
        topBar = {
            BrandTopBar(
                title =
                    t("Új leolvasás"),
                subtitle =
                    t("Fotózd le vagy írd be • OCR és kézi bevitel"),
                icon =
                    Icons.Default.CameraAlt
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brandScreenBrush()
                )
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
                    item {
                        androidx.compose.material3.Surface(
                            onClick = { mode = ReadingEntryMode.SINGLE },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        t("Mérőóra fotózása"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        t("Automatikus leolvasás OCR-rel"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
                    Text(t("Kiválasztás"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text(t("Mégse"))
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
        Text(t("Mérés dátuma: ") + date)
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
    val activeTypes =
        settings.enabledMeterTypes()

    var values by
        remember(
            date,
            activeTypes
        ) {
            mutableStateOf(
                activeTypes.associateWith {
                    ""
                }
            )
        }
    var note by remember(date) {
        mutableStateOf("")
    }
    var pendingDuplicateTypes by remember {
        mutableStateOf<List<MeterType>>(
            emptyList()
        )
    }

    fun clear() {
        values =
            activeTypes.associateWith {
                ""
            }
        note = ""
    }

    fun save(
        allowDuplicate: Boolean
    ) {
        scope.launch {
            val result =
                viewModel.saveQuickReadings(
                    values = values,
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
                            t("✓ A mérőállások sikeresen rögzítve")
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
                                ?: t("A mentés nem sikerült.")
                        )
                }
            }
        }
    }

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
                Modifier.padding(
                    18.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {
                Text(
                    t("Gyors havi leolvasás"),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    t("A bekapcsolt mérők állását egy helyen rögzítheted."),
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

            if (activeTypes.isEmpty()) {
                Text(
                    t("Nincs bekapcsolt mérő. A Beállításokban válassz legalább egyet."),
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            } else {
                activeTypes.forEach {
                    type ->
                    QuickMeterField(
                        type = type,
                        value =
                            values[type]
                                .orEmpty(),
                        onValueChange = {
                            value ->
                            values =
                                values +
                                    (
                                        type to
                                            value
                                        )
                        },
                        date = date,
                        readings = readings,
                        settings = settings
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
                            t("Közös megjegyzés – opcionális")
                        )
                    },
                    minLines = 2
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(
                            vertical =
                                14.dp
                        ),
                    onClick = {
                        save(false)
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
                        t("Összes bekapcsolt mérő mentése")
                    )
                }
            }
        }
    }

    if (
        pendingDuplicateTypes
            .isNotEmpty()
    ) {
        AlertDialog(
            onDismissRequest = {
                pendingDuplicateTypes =
                    emptyList()
            },
            title = {
                Text(
                    t("Ebben a hónapban már van mérés")
                )
            },
            text = {
                Text(
                    t("Már található ") +
                        pendingDuplicateTypes
                            .joinToString(
                                ", "
                            ) {
                                t(
                                    it.displayName
                                )
                            } +
                        t(" mérés erre a hónapra. Ha folytatod, új mérésként kerülnek be az értékek.")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        save(true)
                    }
                ) {
                    Text(
                        t("Mentés így is")
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDuplicateTypes =
                            emptyList()
                    }
                ) {
                    Text(t("Mégse"))
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
        modifier = Modifier
            .fillMaxWidth()
            .background(playMeterColor(type).copy(alpha = 0.105f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayMeterIcon(type = type, size = 44.dp)
            Column {
                Text(
                    t(type.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    type.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange =
                onValueChange,
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    t(type.displayName) +
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
                t("Előző: ") +
                    formatDecimal(it.value) +
                    " " +
                    type.unit +
                    " · " +
                    LocalDate.ofEpochDay(
                        it.dateEpochDay
                    )
            } ?: t("Még nincs korábbi mérés."),
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
                    t(" · kb. ") +
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
                t("Az érték kisebb a korábbi mérőállásnál."),
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

        if (
            isConsumptionAnomaly(
                readings = readings,
                type = type,
                date = date,
                consumption = consumption
            )
        ) {
            Text(
                t("⚠ Szokatlanul nagy ugrás a saját korábbi fogyasztásodhoz képest. Ellenőrizd a beírt mérőállást."),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                fontWeight =
                    FontWeight.SemiBold
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

    val activeTypes =
        settings.enabledMeterTypes()

    var meterType by remember(
        activeTypes
    ) {
        mutableStateOf(
            activeTypes.firstOrNull()
                ?: MeterType.ELECTRICITY
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
                                t("Nem találtam biztosan mérőállást. Írd be kézzel.")
                            )
                    }
                }
                .onFailure {
                    snackbarHostState
                        .showSnackbar(
                            t("A képfelismerés nem sikerült: ") +
                                (
                                    it.message
                                        ?: t("ismeretlen hiba")
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
                t("Egyedi mérés"),
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
                    activeTypes
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
                            Text(t(type.shortName))
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
                        t("Mérőállás (") +
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
                    t("Előző: ") +
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
                    t("Fogyasztás: +") +
                        formatDecimal(it) +
                        " " +
                        meterType.unit +
                        estimateCost(
                            meterType,
                            it,
                            settings
                        )?.let { cost ->
                            t(" · kb. ") +
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

            if (
                isConsumptionAnomaly(
                    readings = readings,
                    type = meterType,
                    date = date,
                    consumption = consumption
                )
            ) {
                Text(
                    t("⚠ Ez a fogyasztás jóval magasabb a saját közelmúltbeli átlagodnál. Érdemes ellenőrizni az értéket."),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
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
                        t("Megjegyzés")
                    )
                },
                minLines = 2
            )

            Text(
                t("Fotós leolvasás"),
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
                                        t("Nem sikerült képfájlt létrehozni.")
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
                    Text(t("Fotózás"))
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
                    Text(t("Galéria"))
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
                        t("Mérőállás felismerése…")
                    )
                }
            }

            if (ocrText.isNotBlank()) {
                Text(
                    t("Felismert szöveg"),
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
                                    t("✓ Sikeres rögzítés")
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
                    t("Mérőállás mentése")
                )
            }
        }
    }
}

private fun isConsumptionAnomaly(
    readings: List<MeterReading>,
    type: MeterType,
    date: LocalDate,
    consumption: Double?
): Boolean {
    val currentConsumption =
        consumption ?: return false

    val usageById =
        UsageCalculator
            .consumptionByReading(
                readings
            )

    val previousUsages =
        readings
            .filter {
                it.type == type &&
                    it.dateEpochDay <
                        date.toEpochDay()
            }
            .sortedByDescending {
                it.dateEpochDay
            }
            .mapNotNull {
                usageById[it.id]
            }
            .take(3)

    if (previousUsages.size < 2) {
        return false
    }

    val average =
        previousUsages.average()

    return average > 0.0 &&
        currentConsumption >
            average * 2.5
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

        MeterType.WATER ->
            when (
                settings.waterBillingMode
            ) {
                GasBillingMode.FLAT_RATE ->
                    settings
                        .waterFlatMonthlyPayment
                        .takeIf {
                            it > 0.0
                        }

                GasBillingMode.METERED ->
                    if (
                        settings.waterUnitPrice >
                        0.0
                    ) {
                        value *
                            settings.waterUnitPrice +
                            settings
                                .waterMonthlyFixedFee
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
