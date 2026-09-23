package hu.merenyimiklos.meterreader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.domain.AnalyticsCalculator
import hu.merenyimiklos.meterreader.domain.EnergyLimitStatus
import hu.merenyimiklos.meterreader.domain.HungarianEnergyLimits
import hu.merenyimiklos.meterreader.domain.UsageInsights
import hu.merenyimiklos.meterreader.domain.UsagePoint
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.model.MonthlySummary
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class StatisticsMetric(
    val label: String,
    val unit: String
) {
    NORMAL("Normál", "kWh"),
    NIGHT("Éjszakai", "kWh"),
    TOTAL_ELECTRICITY("Összes áram", "kWh"),
    GAS("Gáz", "m³")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatisticsScreen(
    viewModel: MeterViewModel
) {
    val readings by
        viewModel.readings.collectAsStateWithLifecycle()
    val summaries by
        viewModel.summaries.collectAsStateWithLifecycle()
    val settings by
        viewModel.settings.collectAsStateWithLifecycle()

    var metric by remember {
        mutableStateOf(
            StatisticsMetric.TOTAL_ELECTRICITY
        )
    }

    val values = remember(
        summaries,
        metric
    ) {
        consumptionValues(
            summaries,
            metric
        )
    }

    val dailyAverage = remember(
        readings,
        metric
    ) {
        dailyAverage(
            readings,
            metric
        )
    }

    val insights = remember(
        values,
        dailyAverage
    ) {
        AnalyticsCalculator.analyze(
            values = values,
            dailyAverage = dailyAverage
        )
    }

    val yearOverYearPercent =
        remember(values) {
            yearOverYearChange(
                values
            )
        }

    val costInsights = remember(
        summaries
    ) {
        AnalyticsCalculator.analyze(
            summaries.map {
                it.month to
                    it.totalEstimatedPayable
            }
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Statisztika")
                        Text(
                            "Trendek, keretek és előrejelzés",
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
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    bottom = 28.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {
            item {
                HungarianLimitOverview(
                    readings = readings,
                    settings = settings
                )
            }

            item {
                Text(
                    "Saját fogyasztás",
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
                            8.dp
                        )
                ) {
                    items(
                        StatisticsMetric.entries
                    ) { item ->
                        FilterChip(
                            selected =
                                metric == item,
                            onClick = {
                                metric = item
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

            item {
                AnimatedContent(
                    targetState = metric,
                    label =
                        "statistics_metric"
                ) {
                    MetricOverview(
                        metric = it,
                        insights = insights,
                        settings = settings,
                        yearOverYearPercent =
                            yearOverYearPercent
                    )
                }
            }

            item {
                ConsumptionChartCard(
                    title =
                        metric.label +
                            " fogyasztás",
                    subtitle =
                        "Az utolsó 12 mérési hónap",
                    insights = insights,
                    unit = metric.unit
                )
            }

            val outliers =
                insights.points.filter {
                    it.isOutlier
                }

            if (outliers.isNotEmpty()) {
                item {
                    OutlierCard(
                        latestOutlier =
                            outliers.last(),
                        unit = metric.unit
                    )
                }
            }

            item {
                ExtremesCard(
                    insights = insights,
                    unit = metric.unit
                )
            }

            if (
                metric ==
                StatisticsMetric.GAS
            ) {
                item {
                    GasReferenceCurveCard(
                        summaries =
                            summaries,
                        settings =
                            settings
                    )
                }
            }

            item {
                ConsumptionChartCard(
                    title =
                        "Becsült havi költség",
                    subtitle =
                        "Normál + éjszakai áram + gáz",
                    insights =
                        costInsights,
                    unit = "Ft",
                    money = true
                )
            }

            item {
                Text(
                    "A kiugrás legalább 25%-os eltérést jelent az előző legfeljebb három mérési hónap átlagához képest. A rezsikeret kijelzése tájékoztató becslés; a tényleges számlázást az MVM elszámolási szabályai, a számlázási időszak, gáz esetén pedig a fűtőérték és korrekciós tényező is befolyásolhatja.",
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        ),
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
private fun HungarianLimitOverview(
    readings: List<MeterReading>,
    settings: BillingSettings
) {
    val today = LocalDate.now()

    val normal =
        HungarianEnergyLimits
            .electricityStatus(
                readings,
                MeterType.ELECTRICITY,
                today
            )
    val night =
        HungarianEnergyLimits
            .electricityStatus(
                readings,
                MeterType.ELECTRICITY_NIGHT,
                today
            )
    val gas =
        HungarianEnergyLimits
            .gasStatus(
                readings,
                settings
                    .gasHeatingValueMjPerM3,
                today
            )

    val period =
        HungarianEnergyLimits
            .discountPeriod(today)

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
                            .surfaceContainerLow
                )
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
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
                    "Magyar lakossági rezsikeret",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.SemiBold
                )
                Text(
                    period.first.toString() +
                        " – " +
                        period.second.toString(),
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

            LimitRow(
                title =
                    "Normál áram",
                status = normal,
                unit = "kWh"
            )

            LimitRow(
                title =
                    "Éjszakai / vezérelt",
                status = night,
                unit = "kWh"
            )

            LimitRow(
                title = "Gáz",
                status = gas,
                unit = "m³"
            )

            Text(
                "Áram: 2523 kWh/év mérési pontonként külön. Gáz: 63 645 MJ/év, ami legalább 1729 m³; a havi keret télen magasabb, nyáron alacsonyabb.",
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

@Composable
private fun LimitRow(
    title: String,
    status: EnergyLimitStatus,
    unit: String
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                5.dp
            )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement
                    .SpaceBetween
        ) {
            Text(
                title,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                formatDecimal(
                    status.used
                ) +
                    " / " +
                    formatDecimal(
                        status.yearlyLimit
                    ) +
                    " " +
                    unit
            )
        }

        LinearProgressIndicator(
            progress = {
                status.usageRatio
                    .toFloat()
                    .coerceIn(
                        0f,
                        1f
                    )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        val paceText =
            if (
                status.remainingToDate >=
                0.0
            ) {
                "Az eddig járó keretből még kb. " +
                    formatDecimal(
                        status
                            .remainingToDate
                    ) +
                    " " +
                    unit +
                    " marad."
            } else {
                "Az időarányos keretet kb. " +
                    formatDecimal(
                        -status
                            .remainingToDate
                    ) +
                    " " +
                    unit +
                    "-val meghaladja."
            }

        Text(
            paceText,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                if (
                    status.remainingToDate <
                    0.0
                ) {
                    MaterialTheme
                        .colorScheme
                        .error
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                }
        )

        Text(
            "Éves keretből hátra: " +
                formatDecimal(
                    status
                        .remainingYearly
                ) +
                " " +
                unit +
                status.projectedYearly
                    ?.let {
                        " · jelenlegi tempóval ~" +
                            formatDecimal(it) +
                            " " +
                            unit +
                            "/év"
                    }
                    .orEmpty(),
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@Composable
private fun MetricOverview(
    metric: StatisticsMetric,
    insights: UsageInsights,
    settings: BillingSettings,
    yearOverYearPercent: Double?
) {
    val goal =
        goalForMetric(
            metric,
            settings
        )

    val forecast =
        insights.latestDailyAverage
            ?.let {
                it *
                    YearMonth
                        .now()
                        .lengthOfMonth()
            }

    Column(
        modifier =
            Modifier.padding(
                horizontal = 16.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {
        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
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
                    Modifier.padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {
                Text(
                    "Legutóbbi fogyasztás",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    insights.latestValue
                        ?.let {
                            formatDecimal(it) +
                                " " +
                                metric.unit
                        }
                        ?: "Még nincs elég adat",
                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                ChangeLabel(
                    insights.changePercent
                )

                yearOverYearPercent
                    ?.let { change ->
                        val sign =
                            if (change > 0) {
                                "+"
                            } else {
                                ""
                            }

                        Text(
                            "Előző év azonos hónapjához képest: " +
                                sign +
                                formatDecimal(
                                    change
                                ) +
                                "%",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                if (
                                    change > 0.5
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                }
                        )
                    }

                if (goal > 0.0) {
                    Spacer(
                        Modifier.height(
                            4.dp
                        )
                    )

                    val ratio =
                        (
                            (
                                insights.latestValue
                                    ?: 0.0
                                ) /
                                goal
                            )
                            .toFloat()
                            .coerceIn(
                                0f,
                                1f
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
                            "Havi cél",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                        Text(
                            formatDecimal(
                                insights
                                    .latestValue
                                    ?: 0.0
                            ) +
                                " / " +
                                formatDecimal(
                                    goal
                                ) +
                                " " +
                                metric.unit,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            ratio
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                forecast?.let {
                    Text(
                        "Napi átlag alapján egy teljes hónapra vetítve: ~" +
                            formatDecimal(it) +
                            " " +
                            metric.unit,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    if (
                        goal > 0.0 &&
                        it > goal
                    ) {
                        Text(
                            "A jelenlegi tempó a havi célt várhatóan " +
                                formatDecimal(
                                    it - goal
                                ) +
                                " " +
                                metric.unit +
                                "-val meghaladná.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            fontWeight =
                                FontWeight
                                    .SemiBold
                        )
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            SmallMetricCard(
                modifier =
                    Modifier.weight(1f),
                title =
                    "3 havi átlag",
                value =
                    insights
                        .threeMonthAverage
                        ?.let {
                            formatDecimal(
                                it
                            ) +
                                " " +
                                metric.unit
                        }
                        ?: "–"
            )

            SmallMetricCard(
                modifier =
                    Modifier.weight(1f),
                title = "Napi átlag",
                value =
                    insights
                        .latestDailyAverage
                        ?.let {
                            formatDecimal(
                                it
                            ) +
                                " " +
                                metric.unit +
                                "/nap"
                        }
                        ?: "–"
            )
        }
    }
}

@Composable
private fun ChangeLabel(
    changePercent: Double?
) {
    val icon = when {
        changePercent == null ->
            Icons.Default.TrendingFlat
        changePercent > 0.5 ->
            Icons.Default.TrendingUp
        changePercent < -0.5 ->
            Icons.Default.TrendingDown
        else ->
            Icons.Default.TrendingFlat
    }

    val text =
        changePercent?.let {
            val sign =
                if (it > 0) "+"
                else ""

            sign +
                formatDecimal(it) +
                "% az előző hónaphoz képest"
        } ?: "Az összehasonlításhoz még kell egy korábbi hónap."

    val color = when {
        changePercent == null ->
            MaterialTheme
                .colorScheme
                .onSurfaceVariant
        changePercent > 0.5 ->
            MaterialTheme
                .colorScheme
                .error
        changePercent < -0.5 ->
            MaterialTheme
                .colorScheme
                .primary
        else ->
            MaterialTheme
                .colorScheme
                .onSurfaceVariant
    }

    Row(
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = text,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color = color
        )
    }
}

@Composable
private fun SmallMetricCard(
    modifier: Modifier,
    title: String,
    value: String
) {
    ElevatedCard(
        modifier = modifier,
        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceContainerLow
                )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    4.dp
                )
        ) {
            Text(
                title,
                style =
                    MaterialTheme
                        .typography
                        .labelLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
            Text(
                value,
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

@Composable
private fun ConsumptionChartCard(
    title: String,
    subtitle: String,
    insights: UsageInsights,
    unit: String,
    money: Boolean = false
) {
    var selectedMonth by
        remember(insights.points) {
            mutableStateOf(
                insights.points
                    .lastOrNull()
                    ?.month
            )
        }

    val points =
        insights.points.takeLast(12)

    val selected =
        points.firstOrNull {
            it.month == selectedMonth
        } ?: points.lastOrNull()

    ElevatedCard(
        modifier = Modifier
            .padding(
                horizontal = 16.dp
            )
            .fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(
                    vertical = 18.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 18.dp
                    ),
                verticalAlignment =
                    Alignment
                        .CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
                Column {
                    Text(
                        title,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight
                                .SemiBold
                    )
                    Text(
                        subtitle,
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

            if (points.isEmpty()) {
                Text(
                    "Még nincs elég mérési adat a diagramhoz.",
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
                return@Column
            }

            selected?.let {
                val selectedValue =
                    if (money) {
                        formatMoney(
                            it.value
                        )
                    } else {
                        formatDecimal(
                            it.value
                        ) +
                            " " +
                            unit
                    }

                Row(
                    modifier = Modifier
                        .padding(
                            horizontal =
                                18.dp
                        )
                        .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,
                    verticalAlignment =
                        Alignment
                            .CenterVertically
                ) {
                    Column {
                        Text(
                            formatMonth(
                                it.month
                            ),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                        Text(
                            selectedValue,
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                            fontWeight =
                                FontWeight
                                    .Bold
                        )
                    }

                    if (it.isOutlier) {
                        Text(
                            "KIUGRÁS",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge,
                            fontWeight =
                                FontWeight
                                    .Bold
                        )
                    }
                }
            }

            MonthlyBars(
                points = points,
                selectedMonth =
                    selected?.month,
                onSelect = {
                    selectedMonth = it
                },
                money = money,
                unit = unit
            )
        }
    }
}

@Composable
private fun MonthlyBars(
    points: List<UsagePoint>,
    selectedMonth: YearMonth?,
    onSelect: (YearMonth) -> Unit,
    money: Boolean,
    unit: String
) {
    val maximum =
        points.maxOfOrNull {
            it.value
        }
            ?.coerceAtLeast(1.0)
            ?: 1.0

    LazyRow(
        contentPadding =
            PaddingValues(
                horizontal = 14.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),
        verticalAlignment =
            Alignment.Bottom
    ) {
        items(
            items = points,
            key = {
                it.month.toString()
            }
        ) { point ->
            val ratio =
                (
                    point.value /
                        maximum
                    )
                    .coerceIn(
                        0.0,
                        1.0
                    )
            val selected =
                point.month ==
                    selectedMonth

            val barColor = when {
                point.isOutlier ->
                    MaterialTheme
                        .colorScheme
                        .error
                selected ->
                    MaterialTheme
                        .colorScheme
                        .primary
                else ->
                    MaterialTheme
                        .colorScheme
                        .secondary
            }

            Column(
                modifier =
                    Modifier
                        .width(48.dp)
                        .clickable {
                            onSelect(
                                point.month
                            )
                        },
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally,
                verticalArrangement =
                    Arrangement.Bottom
            ) {
                Text(
                    if (money) {
                        compactMoney(
                            point.value
                        )
                    } else {
                        formatCompact(
                            point.value
                        )
                    },
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    textAlign =
                        TextAlign.Center,
                    maxLines = 1
                )

                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Box(
                    modifier = Modifier
                        .width(
                            if (selected) {
                                30.dp
                            } else {
                                24.dp
                            }
                        )
                        .height(
                            (
                                145 *
                                    ratio
                                )
                                .roundToInt()
                                .coerceAtLeast(
                                    8
                                )
                                .dp
                        )
                        .background(
                            color =
                                barColor,
                            shape =
                                RoundedCornerShape(
                                    topStart =
                                        12.dp,
                                    topEnd =
                                        12.dp,
                                    bottomStart =
                                        if (
                                            selected
                                        ) {
                                            5.dp
                                        } else {
                                            2.dp
                                        },
                                    bottomEnd =
                                        if (
                                            selected
                                        ) {
                                            5.dp
                                        } else {
                                            2.dp
                                        }
                                )
                        )
                )

                Spacer(
                    Modifier.height(
                        6.dp
                    )
                )

                Text(
                    point.month
                        .monthValue
                        .toString()
                        .padStart(
                            2,
                            '0'
                        ) +
                        "/" +
                        point.month
                            .year
                            .toString()
                            .takeLast(2),
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        if (selected) {
                            MaterialTheme
                                .colorScheme
                                .primary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )

                if (point.isOutlier) {
                    Text(
                        "!",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                } else {
                    Spacer(
                        Modifier.height(
                            16.dp
                        )
                    )
                }
            }
        }
    }

    if (!money) {
        Text(
            "Egység: " + unit,
            modifier =
                Modifier.padding(
                    start = 18.dp,
                    top = 2.dp
                ),
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

@Composable
private fun OutlierCard(
    latestOutlier: UsagePoint,
    unit: String
) {
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
                            .errorContainer
                )
    ) {
        Row(
            modifier =
                Modifier.padding(18.dp),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .error
            )
            Column {
                Text(
                    "Szokatlanul magas fogyasztás",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )
                Text(
                    formatMonth(
                        latestOutlier.month
                    ) +
                        ": " +
                        formatDecimal(
                            latestOutlier.value
                        ) +
                        " " +
                        unit +
                        ". Legalább 25%-kal magasabb volt a megelőző időszak átlagánál.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ExtremesCard(
    insights: UsageInsights,
    unit: String
) {
    ElevatedCard(
        modifier = Modifier
            .padding(
                horizontal = 16.dp
            )
            .fillMaxWidth()
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Text(
                "Minimum és maximum",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
                ExtremeValue(
                    modifier =
                        Modifier.weight(1f),
                    label =
                        "Legalacsonyabb",
                    point =
                        insights.minimum,
                    unit = unit
                )
                ExtremeValue(
                    modifier =
                        Modifier.weight(1f),
                    label =
                        "Legmagasabb",
                    point =
                        insights.maximum,
                    unit = unit
                )
            }
        }
    }
}

@Composable
private fun ExtremeValue(
    modifier: Modifier,
    label: String,
    point: UsagePoint?,
    unit: String
) {
    Column(
        modifier = modifier
    ) {
        Text(
            label,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
        Text(
            point?.let {
                formatDecimal(
                    it.value
                ) +
                    " " +
                    unit
            } ?: "–",
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
            fontWeight =
                FontWeight.Bold
        )
        Text(
            point?.let {
                formatMonth(
                    it.month
                )
            } ?: "",
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@Composable
private fun GasReferenceCurveCard(
    summaries: List<MonthlySummary>,
    settings: BillingSettings
) {
    val period =
        HungarianEnergyLimits
            .discountPeriod(
                LocalDate.now()
            )
    val start =
        YearMonth.from(
            period.first
        )

    val months =
        (0L..11L)
            .map {
                start.plusMonths(it)
            }

    val rows =
        months.map { month ->
            val limit =
                HungarianEnergyLimits
                    .gasMonthlyReferenceM3(
                        month.month,
                        settings
                            .gasHeatingValueMjPerM3
                    )
            val actual =
                summaries
                    .firstOrNull {
                        it.month == month
                    }
                    ?.gasUsage

            Triple(
                month,
                limit,
                actual
            )
        }

    val max =
        rows.maxOfOrNull {
            maxOf(
                it.second,
                it.third ?: 0.0
            )
        }
            ?.coerceAtLeast(1.0)
            ?: 1.0

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
                            .secondaryContainer
                )
    ) {
        Column(
            modifier =
                Modifier.padding(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            Text(
                "MVM gáz-keretgörbe",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                "A 63 645 MJ kedvezményes éves keret rögzített szezonális havi megosztása. A m³ érték a beállított fűtőértékkel számított becslés.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            rows.forEach {
                row ->
                val month =
                    row.first
                val limit =
                    row.second
                val actual =
                    row.third

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            3.dp
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement
                                .SpaceBetween
                    ) {
                        Text(
                            month
                                .monthValue
                                .toString()
                                .padStart(
                                    2,
                                    '0'
                                ) +
                                "/" +
                                month.year
                                    .toString()
                                    .takeLast(
                                        2
                                    ),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )

                        Text(
                            "Keret ~" +
                                formatDecimal(
                                    limit
                                ) +
                                " m³" +
                                actual?.let {
                                    " · tény " +
                                        formatDecimal(
                                            it
                                        ) +
                                        " m³"
                                }.orEmpty(),
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    9.dp
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(
                                        (
                                            limit /
                                                max
                                            )
                                            .toFloat()
                                            .coerceIn(
                                                0f,
                                                1f
                                            )
                                    )
                                    .height(
                                        9.dp
                                    )
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                        RoundedCornerShape(
                                            20.dp
                                        )
                                    )
                        )
                    }
                }
            }
        }
    }
}

private fun yearOverYearChange(
    values: List<Pair<YearMonth, Double>>
): Double? {
    val latest =
        values.maxByOrNull {
            it.first
        } ?: return null

    val previousYearMonth =
        latest.first.minusYears(1)

    val previous =
        values.firstOrNull {
            it.first ==
                previousYearMonth
        }?.second ?: return null

    if (previous <= 0.0) {
        return null
    }

    return (
        (
            latest.second -
                previous
            ) /
            previous
        ) * 100.0
}

private fun goalForMetric(
    metric: StatisticsMetric,
    settings: BillingSettings
): Double =
    when (metric) {
        StatisticsMetric.NORMAL ->
            settings
                .electricityMonthlyGoalKwh
        StatisticsMetric.NIGHT ->
            settings
                .electricityNightMonthlyGoalKwh
        StatisticsMetric.TOTAL_ELECTRICITY ->
            settings
                .electricityMonthlyGoalKwh +
                settings
                    .electricityNightMonthlyGoalKwh
        StatisticsMetric.GAS ->
            settings
                .gasMonthlyGoalM3
    }

private fun consumptionValues(
    summaries: List<MonthlySummary>,
    metric: StatisticsMetric
): List<Pair<YearMonth, Double>> =
    summaries.mapNotNull {
        summary ->
        val value =
            when (metric) {
                StatisticsMetric.NORMAL ->
                    summary
                        .electricityUsage

                StatisticsMetric.NIGHT ->
                    summary
                        .electricityNightUsage

                StatisticsMetric
                    .TOTAL_ELECTRICITY ->
                    if (
                        summary
                            .electricityUsage !=
                        null ||
                        summary
                            .electricityNightUsage !=
                        null
                    ) {
                        summary
                            .totalElectricityUsage
                    } else {
                        null
                    }

                StatisticsMetric.GAS ->
                    summary.gasUsage
            }

        value?.let {
            summary.month to it
        }
    }

private fun dailyAverage(
    readings: List<MeterReading>,
    metric: StatisticsMetric
): Double? =
    when (metric) {
        StatisticsMetric.NORMAL ->
            AnalyticsCalculator
                .latestDailyAverage(
                    readings,
                    MeterType.ELECTRICITY
                )

        StatisticsMetric.NIGHT ->
            AnalyticsCalculator
                .latestDailyAverage(
                    readings,
                    MeterType
                        .ELECTRICITY_NIGHT
                )

        StatisticsMetric
            .TOTAL_ELECTRICITY ->
            AnalyticsCalculator
                .latestCombinedElectricityDailyAverage(
                    readings
                )

        StatisticsMetric.GAS ->
            AnalyticsCalculator
                .latestDailyAverage(
                    readings,
                    MeterType.GAS
                )
    }

private fun formatMonth(
    month: YearMonth
): String =
    month.year.toString() +
        ". " +
        month.monthValue
            .toString()
            .padStart(
                2,
                '0'
            )

private fun compactMoney(
    value: Double
): String =
    when {
        abs(value) >=
            1_000_000 ->
            formatDecimal(
                value /
                    1_000_000.0
            ) + "M"

        abs(value) >=
            1000 ->
            formatDecimal(
                value / 1000.0
            ) + "k"

        else ->
            formatDecimal(value)
    }
