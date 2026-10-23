package hu.merenyimiklos.meterreader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.BuildConfig
import hu.merenyimiklos.meterreader.update.UpdateState
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_STATISTICS = "statistics"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun MeterReaderApp(
    viewModel: MeterViewModel,
    startOnAdd: Boolean = false
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val updateState by
        viewModel.updateState
            .collectAsStateWithLifecycle()
    var dismissedUpdateVersion by
        rememberSaveable {
            mutableStateOf<String?>(
                null
            )
        }
    val currentRoute =
        navController.currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route

    val destinations = listOf(
        Destination(
            ROUTE_HOME,
            "Áttekintés",
            Icons.Default.Home
        ),
        Destination(
            ROUTE_ADD,
            "Rögzítés",
            Icons.Default.AddCircle
        ),
        Destination(
            ROUTE_HISTORY,
            "Napló",
            Icons.Default.ReceiptLong
        ),
        Destination(
            ROUTE_STATISTICS,
            "Statisztika",
            Icons.Default.BarChart
        ),
        Destination(
            ROUTE_SETTINGS,
            "Beállítások",
            Icons.Default.Settings
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor =
            MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainer
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected =
                            currentRoute == destination.route,
                        onClick = {
                            if (
                                currentRoute !=
                                destination.route
                            ) {
                                navController.navigate(
                                    destination.route
                                ) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(ROUTE_HOME) {
                                        saveState = true
                                    }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription =
                                    destination.label
                            )
                        },
                        label = {
                            Text(destination.label)
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                indicatorColor =
                                    MaterialTheme
                                        .colorScheme
                                        .secondaryContainer
                            )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination =
                if (startOnAdd) ROUTE_ADD
                else ROUTE_HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(ROUTE_HOME) {
                DashboardScreen(viewModel)
            }

            composable(ROUTE_ADD) {
                AddReadingScreen(viewModel)
            }

            composable(ROUTE_HISTORY) {
                HistoryScreen(viewModel)
            }

            composable(ROUTE_STATISTICS) {
                StatisticsScreen(viewModel)
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen(viewModel)
            }
        }
    }

    val availableUpdate =
        (updateState as?
            UpdateState.Available)
            ?.info

    if (
        availableUpdate != null &&
        dismissedUpdateVersion !=
        availableUpdate.version
    ) {
        AlertDialog(
            onDismissRequest = {
                dismissedUpdateVersion =
                    availableUpdate.version
            },
            title = {
                Text(
                    "Új verzió érhető el"
                )
            },
            text = {
                Text(
                    buildString {
                        append(
                            "Telepített: "
                        )
                        append(
                            BuildConfig.VERSION_NAME
                        )
                        append(
                            "\nÚj verzió: "
                        )
                        append(
                            availableUpdate.version
                        )

                        if (
                            availableUpdate.notes
                                .isNotBlank()
                        ) {
                            append(
                                "\n\nÚjdonságok:\n"
                            )
                            append(
                                availableUpdate.notes
                                    .take(900)
                            )
                        }

                        append(
                            "\n\nA letöltés után az Android megerősítést kér az APK frissítéséhez."
                        )
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        availableUpdate
                                            .downloadUrl
                                    )
                                )
                            )
                        }
                    }
                ) {
                    Text(
                        "Frissítés letöltése"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dismissedUpdateVersion =
                            availableUpdate.version
                    }
                ) {
                    Text("Később")
                }
            }
        )
    }
}

private data class Destination(
    val route: String,
    val label: String,
    val icon:
        androidx.compose.ui.graphics.vector.ImageVector
)
