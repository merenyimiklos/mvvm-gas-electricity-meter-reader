package hu.merenyimiklos.meterreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.model.GasBillingMode
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.MonthlySummary
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import java.time.LocalDate
import java.time.Year
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    viewModel: MeterViewModel
) {
    val readings by
        viewModel.readings
            .collectAsStateWithLifecycle()
    val settings by
        viewModel.settings
            .collectAsStateWithLifecycle()
    val summaries by
        viewModel.summaries
            .collectAsStateWithLifecycle()

    val activeTypes =
        settings.enabledMeterTypes()

    fun latest(
        type: MeterType
    ): MeterReading? =
        readings
            .filter {
                it.type == type
            }
            .maxByOrNull {
                it.dateEpochDay
            }

    val latestSummary =
        summaries.maxByOrNull {
            it.month
        }

    val currentYear =
        Year.now().value

    val yearSummaries =
        summaries.filter {
            it.month.year ==
                currentYear
        }

    val yearCost =
        yearSummaries.sumOf {
            it.totalEstimatedPayable
        }

    val readingCards =
        activeTypes.map {
            type ->
            ReadingCardData(
                type = type,
                trend = readings.filter { it.type == type }.sortedBy { it.dateEpochDay }
                    .zipWithNext { a, b -> (b.value - a.value).coerceAtLeast(0.0).toFloat() }
                    .takeLast(7),
                title =
                    t(type.displayName),
                reading =
                    latest(type),
                icon =
                    when (type) {
                        MeterType.ELECTRICITY ->
                            Icons.Default.Bolt
                        MeterType.ELECTRICITY_NIGHT ->
                            Icons.Default.Bedtime
                        MeterType.GAS ->
                            Icons.Default.LocalFireDepartment
                        MeterType.WATER,
                        MeterType.WATER_HOT ->
                            Icons.Default.WaterDrop
                    }
            )
        }

    val priceSetupNeeded =
        (
            settings.electricityEnabled &&
                settings.electricityUnitPrice <= 0.0
            ) ||
            (
                settings.electricityNightEnabled &&
                    settings.electricityNightUnitPrice <= 0.0
                ) ||
            (
                settings.gasEnabled &&
                    settings.gasBillingMode ==
                    GasBillingMode.FLAT_RATE &&
                    settings.gasFlatMonthlyPayment <= 0.0
                ) ||
            (
                settings.gasEnabled &&
                    settings.gasBillingMode ==
                    GasBillingMode.METERED &&
                    settings.gasUnitPrice <= 0.0
                ) ||
            (
                settings.waterEnabled &&
                    settings.waterBillingMode ==
                    GasBillingMode.FLAT_RATE &&
                    settings.waterFlatMonthlyPayment <= 0.0
                ) ||
            (
                settings.waterEnabled &&
                    settings.waterBillingMode ==
                    GasBillingMode.METERED &&
                    settings.waterUnitPrice <= 0.0
                ) ||
            (
                settings.waterHotEnabled &&
                    settings.waterHotBillingMode == GasBillingMode.FLAT_RATE &&
                    settings.waterHotFlatMonthlyPayment <= 0.0
                ) ||
            (
                settings.waterHotEnabled &&
                    settings.waterHotBillingMode == GasBillingMode.METERED &&
                    settings.waterHotUnitPrice <= 0.0
                )

    Scaffold(
        containerColor =
            MaterialTheme
                .colorScheme
                .background,
        topBar = {
            BrandTopBar(
                title = t("Mérőóra Napló"),
                subtitle =
                    t("Minden mérőállás egy helyen"),
                icon =
                    Icons.Default.Bolt
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .background(
                        brandScreenBrush()
                    )
                    .padding(
                        innerPadding
                    ),
            contentPadding =
                PaddingValues(
                    top = 12.dp,
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            if (priceSetupNeeded) {
                item {
                    ElevatedCard(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = 16.dp
                                )
                                .fillMaxWidth(),
                        colors =
                            CardDefaults
                                .elevatedCardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .tertiaryContainer
                                )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    18.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    4.dp
                                )
                        ) {
                            Text(
                                t("Állítsd be a tarifákat"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                            Text(
                                t("A bekapcsolt mérők költségbecsléséhez add meg a tarifákat."),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }
            }

            if (settings.sharedHomeId.isNotBlank()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = settings.sharedHomeName.ifBlank { t("Közös otthon") },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = t("Megosztott mérőállások"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    t("Aktuális mérőállások"),
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            item {
                if (readingCards.isEmpty()) {
                    Text(
                        t("Nincs bekapcsolt mérő."),
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        readingCards.forEach { reading ->
                            LatestReadingCard(data = reading)
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 16.dp
                            )
                            .fillMaxWidth(),
                    colors =
                        CardDefaults
                            .elevatedCardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                20.dp
                            )
                    ) {
                        Text(
                            t("Legutóbbi havi becslés"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            Modifier.height(
                                12.dp
                            )
                        )

                        if (
                            latestSummary == null
                        ) {
                            Text(
                                t("Legalább két azonos típusú mérőállás kell a fogyasztás kiszámításához.")
                            )
                        } else {
                            if (settings.electricityEnabled) {
                                SummaryLine(
                                    label =
                                        t("Normál áram"),
                                    usage =
                                        latestSummary
                                            .electricityUsage,
                                    unit = "kWh",
                                    cost =
                                        latestSummary
                                            .electricityCost
                                )
                            }

                            if (settings.electricityNightEnabled) {
                                SummaryLine(
                                    label =
                                        t("Éjszakai áram"),
                                    usage =
                                        latestSummary
                                            .electricityNightUsage,
                                    unit = "kWh",
                                    cost =
                                        latestSummary
                                            .electricityNightCost
                                )
                            }

                            if (settings.gasEnabled) {
                                SummaryLine(
                                    label = t("Gáz"),
                                    usage =
                                        latestSummary
                                            .gasUsage,
                                    unit = "m³",
                                    cost =
                                        latestSummary
                                            .gasPayable
                                )
                            }

                            if (settings.waterEnabled) {
                                SummaryLine(
                                    label = t("Hideg víz"),
                                    usage =
                                        latestSummary
                                            .waterUsage,
                                    unit = "m³",
                                    cost =
                                        latestSummary
                                            .waterPayable
                                )
                            }
                            if (settings.waterHotEnabled) {
                                SummaryLine(
                                    label = t("Meleg víz"),
                                    usage =
                                        latestSummary
                                            .waterHotUsage,
                                    unit = "m³",
                                    cost =
                                        latestSummary
                                            .waterHotPayable
                                )
                            }

                            HorizontalDivider(
                                Modifier.padding(
                                    vertical = 12.dp
                                )
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(
                                    t("Becsült fizetendő"),
                                    fontWeight =
                                        FontWeight.Bold
                                )
                                Text(
                                    formatMoney(
                                        latestSummary
                                            .totalEstimatedPayable
                                    ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 16.dp
                            )
                            .fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                20.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {
                        Text(
                            currentYear.toString() +
                                t(" összesítés"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        if (settings.electricityEnabled) {
                            MetricRow(
                                t("Normál áram"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        it.electricityUsage
                                            ?: 0.0
                                    }
                                ) + " kWh"
                            )
                        }

                        if (settings.electricityNightEnabled) {
                            MetricRow(
                                t("Éjszakai áram"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        it.electricityNightUsage
                                            ?: 0.0
                                    }
                                ) + " kWh"
                            )
                        }

                        if (
                            settings.electricityEnabled ||
                            settings.electricityNightEnabled
                        ) {
                            MetricRow(
                                t("Összes áram"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        (
                                            it.electricityUsage
                                                ?: 0.0
                                            ) +
                                            (
                                                it.electricityNightUsage
                                                    ?: 0.0
                                                )
                                    }
                                ) + " kWh"
                            )
                        }

                        if (settings.gasEnabled) {
                            MetricRow(
                                t("Gáz"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        it.gasUsage
                                            ?: 0.0
                                    }
                                ) + " m³"
                            )
                        }

                        if (settings.waterEnabled) {
                            MetricRow(
                                t("Hideg víz"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        it.waterUsage
                                            ?: 0.0
                                    }
                                ) + " m³"
                            )
                        }
                        if (settings.waterHotEnabled) {
                            MetricRow(
                                t("Meleg víz"),
                                formatDecimal(
                                    yearSummaries.sumOf {
                                        it.waterHotUsage
                                            ?: 0.0
                                    }
                                ) + " m³"
                            )
                        }

                        HorizontalDivider(
                            Modifier.padding(
                                vertical = 6.dp
                            )
                        )

                        MetricRow(
                            t("Becsült összköltség"),
                            formatMoney(
                                yearCost
                            ),
                            bold = true
                        )
                    }
                }
            }

            if (settings.electricityEnabled) {
                item {
                    UsageChart(
                        title =
                            t("Normál áram · 6 hónap"),
                        summaries =
                            summaries,
                        valueSelector = {
                            it.electricityUsage
                        },
                        unit = "kWh"
                    )
                }
            }

            if (settings.electricityNightEnabled) {
                item {
                    UsageChart(
                        title =
                            t("Éjszakai áram · 6 hónap"),
                        summaries =
                            summaries,
                        valueSelector = {
                            it.electricityNightUsage
                        },
                        unit = "kWh"
                    )
                }
            }

            if (settings.gasEnabled) {
                item {
                    UsageChart(
                        title =
                            t("Gáz · 6 hónap"),
                        summaries =
                            summaries,
                        valueSelector = {
                            it.gasUsage
                        },
                        unit = "m³"
                    )
                }
            }

            if (settings.waterEnabled) {
                item {
                    UsageChart(
                        title =
                            t("Hideg víz · 6 hónap"),
                        summaries =
                            summaries,
                        valueSelector = {
                            it.waterUsage
                        },
                        unit = "m³"
                    )
                }
            }
            if (settings.waterHotEnabled) {
                item {
                    UsageChart(
                        title =
                            t("Meleg víz · 6 hónap"),
                        summaries =
                            summaries,
                        valueSelector = {
                            it.waterHotUsage
                        },
                        unit = "m³"
                    )
                }
            }
        }
    }
}

private data class ReadingCardData(
    val type: MeterType,
    val title: String,
    val reading: MeterReading?,
    val icon: ImageVector,
    val trend: List<Float>
)

@Composable
private fun LatestReadingCard(data: ReadingCardData) {
    val accent = playMeterColor(data.type)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.075f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlayMeterIcon(type = data.type)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = data.reading?.let {
                        formatDecimal(it.value) + " " + it.type.unit
                    } ?: t("Még nincs mérés"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                data.reading?.let {
                    Text(
                        text = t("Utolsó leolvasás: ") +
                            LocalDate.ofEpochDay(it.dateEpochDay).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (data.trend.isNotEmpty()) {
                PlayMiniBars(color = accent, values = data.trend)
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    usage: Double?,
    unit: String,
    cost: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                label,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                usage?.let {
                    formatDecimal(it) +
                        " " +
                        unit
                } ?: t("Nincs számítható fogyasztás"),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Spacer(
            Modifier.width(12.dp)
        )

        Text(
            formatMoney(cost),
            fontWeight =
                FontWeight.Medium
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )

        Text(
            value,
            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}

@Composable
private fun UsageChart(
    title: String,
    summaries: List<MonthlySummary>,
    valueSelector:
        (MonthlySummary) -> Double?,
    unit: String
) {
    val data = summaries
        .takeLast(6)
        .mapNotNull { summary ->
            valueSelector(summary)
                ?.let {
                    summary to it
                }
        }

    ElevatedCard(
        modifier = Modifier
            .padding(
                horizontal = 16.dp
            )
            .fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                Modifier.height(14.dp)
            )

            if (data.isEmpty()) {
                Text(
                    t("Még nincs elég adat a diagramhoz."),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
                return@Column
            }

            val maxValue =
                data.maxOf {
                    it.second
                }.coerceAtLeast(1.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
                verticalAlignment =
                    Alignment.Bottom
            ) {
                data.forEach {
                    pair ->
                    val summary =
                        pair.first
                    val value =
                        pair.second
                    val ratio =
                        (
                            value /
                                maxValue
                            )
                            .coerceIn(
                                0.0,
                                1.0
                            )

                    Column(
                        modifier =
                            Modifier.weight(1f),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Bottom
                    ) {
                        Text(
                            formatCompact(
                                value
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )

                        Spacer(
                            Modifier.height(
                                5.dp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    0.58f
                                )
                                .height(
                                    (
                                        104 *
                                            ratio
                                        )
                                        .roundToInt()
                                        .coerceAtLeast(
                                            6
                                        )
                                        .dp
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                    RoundedCornerShape(
                                        topStart =
                                            12.dp,
                                        topEnd =
                                            12.dp
                                    )
                                )
                        )

                        Spacer(
                            Modifier.height(
                                5.dp
                            )
                        )

                        Text(
                            summary
                                .month
                                .monthValue
                                .toString()
                                .padStart(
                                    2,
                                    '0'
                                ) +
                                "/" +
                                summary
                                    .month
                                    .year
                                    .toString()
                                    .takeLast(
                                        2
                                    ),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }
                }
            }

            Text(
                t("Egység: ") + unit,
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
