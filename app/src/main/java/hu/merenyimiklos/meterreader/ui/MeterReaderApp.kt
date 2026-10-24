package hu.merenyimiklos.meterreader.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.auth.AuthViewModel
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_STATISTICS = "statistics"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun MeterReaderApp(
    viewModel: MeterViewModel,
    authViewModel: AuthViewModel,
    startOnAdd: Boolean = false
) {
    val navController =
        rememberNavController()
    val drawerState =
        rememberDrawerState(
            DrawerValue.Closed
        )
    val scope =
        rememberCoroutineScope()

    val authState by
        authViewModel.state
            .collectAsStateWithLifecycle()
    val sharedHomeSyncState by
        viewModel.sharedHomeSyncState
            .collectAsStateWithLifecycle()

    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route

    fun navigateRoot(
        route: String
    ) {
        if (
            currentRoute ==
            route
        ) {
            return
        }

        navController.navigate(
            route
        ) {
            launchSingleTop = true
            restoreState = true
            popUpTo(
                ROUTE_HOME
            ) {
                saveState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState =
            drawerState,
        drawerContent = {
            ModernDrawerContent(
                authState =
                    authState,
                sharedHomeSyncState =
                    sharedHomeSyncState,
                onFamily = {
                    navigateRoot(
                        ROUTE_SETTINGS
                    )
                    scope.launch {
                        drawerState.close()
                    }
                },
                onSettings = {
                    navigateRoot(
                        ROUTE_SETTINGS
                    )
                    scope.launch {
                        drawerState.close()
                    }
                },
                onTutorial = {
                    scope.launch {
                        drawerState.close()
                    }
                    authViewModel
                        .replayOnboarding()
                },
                onSync = {
                    viewModel
                        .syncSharedHome()
                    scope.launch {
                        drawerState.close()
                    }
                },
                onSignOut = {
                    scope.launch {
                        drawerState.close()
                    }
                    authViewModel
                        .signOut()
                }
            )
        }
    ) {
        Scaffold(
            modifier =
                Modifier.fillMaxSize(),
            containerColor =
                MaterialTheme
                    .colorScheme
                    .surface,
            bottomBar = {
                ModernBottomBar(
                    currentRoute =
                        currentRoute,
                    onNavigate = {
                        route ->
                        navigateRoot(
                            route
                        )
                    },
                    onAdd = {
                        navigateRoot(
                            ROUTE_ADD
                        )
                    },
                    onMenu = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        ) {
            padding ->
            NavHost(
                navController =
                    navController,
                startDestination =
                    if (startOnAdd) {
                        ROUTE_ADD
                    } else {
                        ROUTE_HOME
                    },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            padding
                        ),
                enterTransition = {
                    fadeIn(
                        animationSpec =
                            tween(220)
                    ) +
                        slideInHorizontally(
                            animationSpec =
                                tween(220),
                            initialOffsetX = {
                                it / 12
                            }
                        )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec =
                            tween(140)
                    ) +
                        slideOutHorizontally(
                            animationSpec =
                                tween(180),
                            targetOffsetX = {
                                -it / 14
                            }
                        )
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec =
                            tween(180)
                    ) +
                        slideInHorizontally(
                            animationSpec =
                                tween(200),
                            initialOffsetX = {
                                -it / 14
                            }
                        )
                },
                popExitTransition = {
                    fadeOut(
                        animationSpec =
                            tween(130)
                    ) +
                        slideOutHorizontally(
                            animationSpec =
                                tween(180),
                            targetOffsetX = {
                                it / 14
                            }
                        )
                }
            ) {
                composable(
                    ROUTE_HOME
                ) {
                    DashboardScreen(
                        viewModel
                    )
                }

                composable(
                    ROUTE_ADD
                ) {
                    AddReadingScreen(
                        viewModel
                    )
                }

                composable(
                    ROUTE_HISTORY
                ) {
                    HistoryScreen(
                        viewModel
                    )
                }

                composable(
                    ROUTE_STATISTICS
                ) {
                    StatisticsScreen(
                        viewModel
                    )
                }

                composable(
                    ROUTE_SETTINGS
                ) {
                    SettingsScreen(
                        viewModel =
                            viewModel,
                        authViewModel =
                            authViewModel,
                        onOpenMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }
}
