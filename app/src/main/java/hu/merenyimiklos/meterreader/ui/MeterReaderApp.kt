package hu.merenyimiklos.meterreader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_STATISTICS = "statistics"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun MeterReaderApp(viewModel: MeterViewModel) {
    val navController = rememberNavController()
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
            startDestination = ROUTE_HOME,
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
}

private data class Destination(
    val route: String,
    val label: String,
    val icon:
        androidx.compose.ui.graphics.vector.ImageVector
)
