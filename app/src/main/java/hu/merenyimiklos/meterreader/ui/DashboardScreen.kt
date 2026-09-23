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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
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

    val latestNormal = readings
        .filter {
            it.type ==
                MeterType.ELECTRICITY
        }
        .maxByOrNull {
            it.dateEpochDay
        }

    val latestNight = readings
        .filter {
            it.type ==
                MeterType.ELECTRICITY_NIGHT
        }
        .maxByOrNull {
            it.dateEpochDay
        }

    val latestGas = readings
        .filter {
            it.type == MeterType.GAS
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

    val yearNormal =
        yearSummaries.sumOf {
            it.electricityUsage ?: 0.0
        }

    val yearNight =
        yearSummaries.sumOf {
            it.electricityNightUsage
                ?: 0.0
        }

    val yearGas =
        yearSummaries.sumOf {
            it.gasUsage ?: 0.0
        }

    val yearCost =
        yearSummaries.sumOf {
            it.totalEstimatedPayable
        }

    val priceSetupNeeded =
        settings.electricityUnitPrice <= 0.0 ||
            settings
                .electricityNightUnitPrice <= 0.0 ||
            (
                settings.gasBillingMode ==
                    GasBillingMode.FLAT_RATE &&
                    settings
                        .gasFlatMonthlyPayment <= 0.0
                ) ||
            (
                settings.gasBillingMode ==
                    GasBillingMode.METERED &&
                    settings.gasUnitPrice <= 0.0
                )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Mérőóra")
                        Text(
                            "Otthoni energiafigyelő",
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            if (priceSetupNeeded) {
                item {
                    ElevatedCard(
                        modifier = Modifier
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
                                Arrangement
                                    .spacedBy(4.dp)
                        ) {
                            Text(
                                "Állítsd be a tarifákat",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight
                                        .SemiBold
                            )
                            Text(
                                "A normál és éjszakai áram külön egységárral számolható.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Aktuális mérőállások",
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
                LazyRow(
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {
                    items(
                        listOf(
                            ReadingCardData(
                                title =
                                    "Normál áram",
                                reading =
                                    latestNormal,
                                icon =
                                    Icons.Default.Bolt
                            ),
                            ReadingCardData(
                                title =
                                    "Éjszakai áram",
                                reading =
                                    latestNight,
                                icon =
                                    Icons.Default.Bedtime
                            ),
                            ReadingCardData(
                                title = "Gáz",
                                reading =
                                    latestGas,
                                icon =
                                    Icons.Default
                                        .LocalFireDepartment
                            )
                        )
                    ) { item ->
                        LatestReadingCard(
                            data = item
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier
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
                            Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Legutóbbi havi becslés",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        if (
                            latestSummary == null
                        ) {
                            Text(
                                "Legalább két azonos típusú mérőállás kell a fogyasztás kiszámításához."
                            )
                        } else {
                            SummaryLine(
                                label =
                                    "Normál áram",
                                usage =
                                    latestSummary
                                        .electricityUsage,
                                unit = "kWh",
                                cost =
                                    latestSummary
                                        .electricityCost
                            )

                            SummaryLine(
                                label =
                                    "Éjszakai áram",
                                usage =
                                    latestSummary
                                        .electricityNightUsage,
                                unit = "kWh",
                                cost =
                                    latestSummary
                                        .electricityNightCost
                            )

                            SummaryLine(
                                label = "Gáz",
                                usage =
                                    latestSummary
                                        .gasUsage,
                                unit = "m³",
                                cost =
                                    latestSummary
                                        .gasPayable
                            )

                            HorizontalDivider(
                                Modifier.padding(
                                    vertical = 12.dp
                                )
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement
                                        .SpaceBetween
                            ) {
                                Text(
                                    "Becsült fizetendő",
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
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp
                        )
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(20.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {
                        Text(
                            currentYear.toString() +
                                " összesítés",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        MetricRow(
                            "Normál áram",
                            formatDecimal(
                                yearNormal
                            ) + " kWh"
                        )

                        MetricRow(
                            "Éjszakai áram",
                            formatDecimal(
                                yearNight
                            ) + " kWh"
                        )

                        MetricRow(
                            "Összes áram",
                            formatDecimal(
                                yearNormal +
                                    yearNight
                            ) + " kWh"
                        )

                        MetricRow(
                            "Gáz",
                            formatDecimal(
                                yearGas
                            ) + " m³"
                        )

                        HorizontalDivider(
                            Modifier.padding(
                                vertical = 6.dp
                            )
                        )

                        MetricRow(
                            "Becsült összköltség",
                            formatMoney(yearCost),
                            bold = true
                        )
                    }
                }
            }

            item {
                UsageChart(
                    title =
                        "Normál áram · 6 hónap",
                    summaries = summaries,
                    valueSelector = {
                        it.electricityUsage
                    },
                    unit = "kWh"
                )
            }

            item {
                UsageChart(
                    title =
                        "Éjszakai áram · 6 hónap",
                    summaries = summaries,
                    valueSelector = {
                        it.electricityNightUsage
                    },
                    unit = "kWh"
                )
            }

            item {
                UsageChart(
                    title =
                        "Gáz · 6 hónap",
                    summaries = summaries,
                    valueSelector = {
                        it.gasUsage
                    },
                    unit = "m³"
                )
            }
        }
    }
}

private data class ReadingCardData(
    val title: String,
    val reading: MeterReading?,
    val icon: ImageVector
)

@Composable
private fun LatestReadingCard(
    data: ReadingCardData
) {
    ElevatedCard(
        modifier = Modifier.width(210.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Text(
                data.title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            if (data.reading == null) {
                Text(
                    "Még nincs mérés",
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            } else {
                Text(
                    formatDecimal(
                        data.reading.value
                    ) +
                        " " +
                        data.reading.type.unit,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    LocalDate
                        .ofEpochDay(
                            data.reading
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
                } ?: "Nincs számítható fogyasztás",
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
                    "Még nincs elég adat a diagramhoz.",
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
                "Egység: " + unit,
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
