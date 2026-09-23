package hu.merenyimiklos.meterreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
internal fun DashboardScreen(viewModel: MeterViewModel) {
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    val latestElectricity = readings
        .filter { it.type == MeterType.ELECTRICITY }
        .maxByOrNull { it.dateEpochDay }
    val latestGas = readings
        .filter { it.type == MeterType.GAS }
        .maxByOrNull { it.dateEpochDay }
    val latestSummary = summaries.maxByOrNull { it.month }

    val currentYear = Year.now().value
    val yearSummaries = summaries.filter { it.month.year == currentYear }
    val yearElectricity = yearSummaries.sumOf { it.electricityUsage ?: 0.0 }
    val yearGas = yearSummaries.sumOf { it.gasUsage ?: 0.0 }
    val yearCost = yearSummaries.sumOf { it.totalEstimatedPayable }

    val priceSetupNeeded = settings.electricityUnitPrice <= 0.0 ||
        (settings.gasBillingMode == GasBillingMode.FLAT_RATE &&
            settings.gasFlatMonthlyPayment <= 0.0) ||
        (settings.gasBillingMode == GasBillingMode.METERED &&
            settings.gasUnitPrice <= 0.0)

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Mérőóra Napló",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Áram és gáz: mérőállás, fogyasztás és becsült költség",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (priceSetupNeeded) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "A fizetési becsléshez állítsd be a díjakat.",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "A saját szerződésed szerinti egységárakat és a gázátalányt a Beállításokban adhatod meg.",
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
                    Text(
                        "Legutóbbi havi becslés",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (latestSummary == null) {
                        Text(
                            "Legalább két azonos típusú mérőállás kell a fogyasztás kiszámításához."
                        )
                    } else {
                        SummaryLine(
                            "Áram",
                            latestSummary.electricityUsage,
                            "kWh",
                            latestSummary.electricityCost
                        )
                        SummaryLine(
                            "Gáz",
                            latestSummary.gasUsage,
                            "m³",
                            latestSummary.gasPayable
                        )
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Becsült fizetendő", fontWeight = FontWeight.Bold)
                            Text(
                                formatMoney(latestSummary.totalEstimatedPayable),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        currentYear.toString() + " összesítés",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Áram: " + formatDecimal(yearElectricity) + " kWh")
                    Text("Gáz: " + formatDecimal(yearGas) + " m³")
                    Text(
                        "Becsült fizetendő: " + formatMoney(yearCost),
                        fontWeight = FontWeight.Bold
                    )
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
                Text(
                    "Nincs adat",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    formatDecimal(reading.value) + " " + reading.type.unit,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    LocalDate.ofEpochDay(reading.dateEpochDay).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(
                usage?.let { formatDecimal(it) + " " + unit }
                    ?: "Nincs számítható fogyasztás",
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
                            formatCompact(value),
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
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp
                                    )
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            summary.month.monthValue.toString().padStart(2, '0') +
                                "/" + summary.month.year.toString().takeLast(2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                "Egység: " + unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
