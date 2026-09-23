package hu.merenyimiklos.meterreader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.export.XlsxExporter
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.MonthlySummary
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun MeterReaderApp(viewModel: MeterViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val destinations = listOf(
        Destination(ROUTE_HOME, "Áttekintés", Icons.Default.Home),
        Destination(ROUTE_ADD, "Rögzítés", Icons.Default.Add),
        Destination(ROUTE_HISTORY, "Előzmények", Icons.Default.History),
        Destination(ROUTE_SETTINGS, "Beállítások", Icons.Default.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(ROUTE_HOME) { DashboardScreen(viewModel) }
            composable(ROUTE_ADD) {
                AddReadingScreen(
                    viewModel = viewModel,
                    onSaved = {
                        navController.navigate(ROUTE_HOME) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(ROUTE_HISTORY) { HistoryScreen(viewModel) }
            composable(ROUTE_SETTINGS) { SettingsScreen(viewModel) }
        }
    }
}

private data class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun DashboardScreen(viewModel: MeterViewModel) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    val latestElectricity = readings.filter { it.type == MeterType.ELECTRICITY }.maxByOrNull { it.dateEpochDay }
    val latestGas = readings.filter { it.type == MeterType.GAS }.maxByOrNull { it.dateEpochDay }
    val latestSummary = summaries.maxByOrNull { it.month }

    val priceSetupNeeded = settings.electricityUnitPrice <= 0.0 ||
        (settings.gasBillingMode == GasBillingMode.FLAT_RATE && settings.gasFlatMonthlyPayment <= 0.0) ||
        (settings.gasBillingMode == GasBillingMode.METERED && settings.gasUnitPrice <= 0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mérőóra Napló",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Havi áram- és gázfogyasztás egy helyen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (priceSetupNeeded) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("A fizetési becsléshez állítsd be a díjakat.", fontWeight = FontWeight.SemiBold)
                        Text(
                            "A tarifák nincsenek beégetve az alkalmazásba, így a saját szerződésed szerinti értékeket használhatod.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LatestReadingCard(
                    modifier = Modifier.weight(1f),
                    title = "Áram",
                    reading = latestElectricity
                )
                LatestReadingCard(
                    modifier = Modifier.weight(1f),
                    title = "Gáz",
                    reading = latestGas
                )
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Legutóbbi havi becslés", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (latestSummary == null) {
                        Text("Legalább két azonos típusú mérőállás kell a fogyasztás kiszámításához.")
                    } else {
                        SummaryLine("Áram", latestSummary.electricityUsage, "kWh", latestSummary.electricityCost)
                        SummaryLine("Gáz", latestSummary.gasUsage, "m³", latestSummary.gasPayable)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Becsült fizetendő", fontWeight = FontWeight.Bold)
                            Text(formatMoney(latestSummary.totalEstimatedPayable), fontWeight = FontWeight.Bold)
                        }
                        if (settings.gasBillingMode == GasBillingMode.FLAT_RATE) {
                            Text(
                                "A gáz sorban az átalányként megadott havi összeg szerepel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            UsageChart(
                title = "Áramfogyasztás – utolsó 6 hónap",
                summaries = summaries,
                valueSelector = { it.electricityUsage },
                unit = "kWh"
            )
        }

        item {
            UsageChart(
                title = "Gázfogyasztás – utolsó 6 hónap",
                summaries = summaries,
                valueSelector = { it.gasUsage },
                unit = "m³"
            )
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun LatestReadingCard(
    modifier: Modifier,
    title: String,
    reading: MeterReading?
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (reading == null) {
                Text("Nincs adat", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    text = formatDecimal(reading.value) + " " + reading.type.unit,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LocalDate.ofEpochDay(reading.dateEpochDay).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, usage: Double?, unit: String, cost: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(
                usage?.let { formatDecimal(it) + " " + unit } ?: "Nincs számítható fogyasztás",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(formatMoney(cost))
    }
}

@Composable
private fun UsageChart(
    title: String,
    summaries: List<MonthlySummary>,
    valueSelector: (MonthlySummary) -> Double?,
    unit: String
) {
    val data = summaries.takeLast(6).mapNotNull { summary ->
        valueSelector(summary)?.let { summary to it }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            if (data.isEmpty()) {
                Text("Még nincs elég adat a diagramhoz.")
                return@Column
            }

            val maxValue = data.maxOf { it.second }.coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (summary, value) ->
                    val ratio = (value / maxValue).coerceIn(0.0, 1.0)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = formatCompact(value),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height((110 * ratio).roundToInt().coerceAtLeast(4).dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = summary.month.monthValue.toString().padStart(2, '0') + "/" +
                                summary.month.year.toString().takeLast(2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Text(
                text = "Egység: " + unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddReadingScreen(
    viewModel: MeterViewModel,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var meterType by remember { mutableStateOf(MeterType.ELECTRICITY) }
    var valueText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    var photoUriForSavedReading by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var ocrLoading by remember { mutableStateOf(false) }

    fun analyze(uri: Uri, persistPhoto: Boolean) {
        if (persistPhoto) photoUriForSavedReading = uri.toString()
        ocrLoading = true
        scope.launch {
            viewModel.detectReading(uri, meterType)
                .onSuccess { result ->
                    result.detectedValue?.let { valueText = editableNumber(it) }
                    ocrText = result.rawText
                    if (result.detectedValue == null) {
                        snackbarHostState.showSnackbar("Nem találtam biztosan mérőállást. Írd be kézzel.")
                    }
                }
                .onFailure {
                    snackbarHostState.showSnackbar("A képfelismerés nem sikerült: " + (it.message ?: "ismeretlen hiba"))
                }
            ocrLoading = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { analyze(it, true) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { analyze(it, false) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Új mérőállás", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Text("Mérő típusa", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeterType.entries.forEach { type ->
                    FilterChip(
                        selected = meterType == type,
                        onClick = { meterType = type },
                        label = { Text(type.displayName) }
                    )
                }
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mérőállás (" + meterType.unit + ")") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dátum (ÉÉÉÉ-HH-NN)") },
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Megjegyzés") },
                minLines = 2
            )

            Text("Fotós beolvasás", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !ocrLoading,
                    onClick = {
                        runCatching { createPhotoUri(context) }
                            .onSuccess { uri ->
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                            .onFailure {
                                scope.launch { snackbarHostState.showSnackbar("Nem sikerült képfájlt létrehozni.") }
                            }
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Fotózás")
                }

                OutlinedButton(
                    enabled = !ocrLoading,
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Galéria")
                }
            }

            if (ocrLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mérőállás felismerése…")
                }
            }

            if (ocrText.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("OCR által felismert szöveg", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = ocrText,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Mentés előtt ellenőrizd a fenti mérőállást.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val error = viewModel.saveReading(
                        type = meterType,
                        valueText = valueText,
                        dateText = dateText,
                        note = note,
                        photoUri = photoUriForSavedReading
                    )
                    scope.launch {
                        if (error == null) {
                            snackbarHostState.showSnackbar("Mérőállás elmentve.")
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar(error)
                        }
                    }
                }
            ) {
                Text("Mérőállás mentése")
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HistoryScreen(viewModel: MeterViewModel) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val consumptionById = remember(readings) { UsageCalculator.consumptionByReading(readings) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XlsxExporter.MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportXlsx(uri)
                    .onSuccess { snackbarHostState.showSnackbar("Excel export elkészült.") }
                    .onFailure {
                        snackbarHostState.showSnackbar("Export hiba: " + (it.message ?: "ismeretlen hiba"))
                    }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
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
                Text("Előzmények", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = {
                        exportLauncher.launch("meroallasok-" + LocalDate.now() + ".xlsx")
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Excel")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (readings.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text("Még nincs rögzített mérőállás.", Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = readings.sortedWith(
                            compareByDescending<MeterReading> { it.dateEpochDay }
                                .thenByDescending { it.createdAtMillis }
                        ),
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
                                            LocalDate.ofEpochDay(reading.dateEpochDay),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        formatDecimal(reading.value) + " " + reading.type.unit,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (consumption != null) {
                                        Text(
                                            "Fogyasztás az előző mérés óta: " +
                                                formatDecimal(consumption) + " " + reading.type.unit,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    } else {
                                        Text(
                                            "Első mérés – még nincs fogyasztási különbség.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (reading.note.isNotBlank()) {
                                        Text(reading.note, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteReading(reading.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Törlés")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: MeterViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var electricityUnitPrice by remember(settings) { mutableStateOf(editableNumber(settings.electricityUnitPrice)) }
    var electricityFixedFee by remember(settings) { mutableStateOf(editableNumber(settings.electricityMonthlyFixedFee)) }
    var gasMode by remember(settings) { mutableStateOf(settings.gasBillingMode) }
    var gasUnitPrice by remember(settings) { mutableStateOf(editableNumber(settings.gasUnitPrice)) }
    var gasFixedFee by remember(settings) { mutableStateOf(editableNumber(settings.gasMonthlyFixedFee)) }
    var gasFlatPayment by remember(settings) { mutableStateOf(editableNumber(settings.gasFlatMonthlyPayment)) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Beállítások", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Text("Áram", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MoneyField(
                value = electricityUnitPrice,
                onValueChange = { electricityUnitPrice = it },
                label = "Egységár (Ft/kWh)"
            )
            MoneyField(
                value = electricityFixedFee,
                onValueChange = { electricityFixedFee = it },
                label = "Havi fix díj (Ft)"
            )

            HorizontalDivider()

            Text("Gáz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Elszámolás módja")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GasBillingMode.entries.forEach { mode ->
                    FilterChip(
                        selected = gasMode == mode,
                        onClick = { gasMode = mode },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            MoneyField(
                value = gasFlatPayment,
                onValueChange = { gasFlatPayment = it },
                label = "Havi gázátalány (Ft)"
            )
            MoneyField(
                value = gasUnitPrice,
                onValueChange = { gasUnitPrice = it },
                label = if (gasMode == GasBillingMode.FLAT_RATE)
                    "Gáz egységár – opcionális kimutatáshoz (Ft/m³)"
                else
                    "Gáz egységár (Ft/m³)"
            )
            MoneyField(
                value = gasFixedFee,
                onValueChange = { gasFixedFee = it },
                label = "Gáz havi fix díj (Ft)"
            )

            Card(Modifier.fillMaxWidth()) {
                Text(
                    "A becslés tájékoztató jellegű. A tényleges számla függhet sávos tarifától, korrekciós tényezőtől, adóktól és a szolgáltató elszámolásától.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val newSettings = BillingSettings(
                        electricityUnitPrice = parseDecimal(electricityUnitPrice),
                        electricityMonthlyFixedFee = parseDecimal(electricityFixedFee),
                        gasBillingMode = gasMode,
                        gasUnitPrice = parseDecimal(gasUnitPrice),
                        gasMonthlyFixedFee = parseDecimal(gasFixedFee),
                        gasFlatMonthlyPayment = parseDecimal(gasFlatPayment)
                    )
                    viewModel.updateSettings(newSettings)
                    scope.launch { snackbarHostState.showSnackbar("Beállítások elmentve.") }
                }
            ) {
                Text("Mentés")
            }

            Spacer(Modifier.height(12.dp))
        }
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
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

private fun createPhotoUri(context: Context): Uri {
    val directory = File(context.filesDir, "meter_photos").apply { mkdirs() }
    val file = File.createTempFile("meter_", ".jpg", directory)
    return FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
}

private fun parseDecimal(value: String): Double =
    value.trim().replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

private fun editableNumber(value: Double): String {
    if (value == 0.0) return ""
    val long = value.toLong()
    return if (value == long.toDouble()) long.toString() else value.toString()
}

private fun formatDecimal(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("hu", "HU")).apply {
        maximumFractionDigits = 3
        minimumFractionDigits = 0
    }
    return formatter.format(value)
}

private fun formatCompact(value: Double): String =
    if (value >= 1000) (value / 1000.0).let { formatDecimal(it) + "k" } else formatDecimal(value)

private fun formatMoney(value: Double): String {
    val formatter = NumberFormat.getIntegerInstance(Locale("hu", "HU"))
    return formatter.format(value.roundToLong()) + " Ft"
}
