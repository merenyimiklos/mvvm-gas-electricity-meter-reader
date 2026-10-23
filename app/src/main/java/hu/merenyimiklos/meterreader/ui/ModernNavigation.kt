package hu.merenyimiklos.meterreader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.merenyimiklos.meterreader.auth.AuthUiState
import hu.merenyimiklos.meterreader.viewmodel.SharedHomeSyncState

data class ModernDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun ModernBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit
) {
    val left =
        listOf(
            ModernDestination(
                "home",
                t("Áttekintés"),
                Icons.Default.Home
            ),
            ModernDestination(
                "history",
                t("Napló"),
                Icons.Default.ReceiptLong
            )
        )

    val right =
        listOf(
            ModernDestination(
                "statistics",
                t("Statisztika"),
                Icons.Default.BarChart
            )
        )

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 14.dp,
        shape =
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceContainer
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .padding(
                            horizontal = 8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {
                left.forEach {
                    destination ->
                    ModernNavItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected =
                            currentRoute ==
                                destination.route,
                        onClick = {
                            onNavigate(
                                destination.route
                            )
                        }
                    )
                }

                Box(
                    modifier =
                        Modifier.size(
                            72.dp
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Surface(
                        onClick = onAdd,
                        modifier =
                            Modifier.size(
                                62.dp
                            ),
                        shape = CircleShape,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .onPrimary,
                        shadowElevation =
                            10.dp
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                "+",
                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                right.forEach {
                    destination ->
                    ModernNavItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected =
                            currentRoute ==
                                destination.route,
                        onClick = {
                            onNavigate(
                                destination.route
                            )
                        }
                    )
                }

                ModernNavItem(
                    label = t("Menü"),
                    icon =
                        Icons.Default.Menu,
                    selected =
                        currentRoute ==
                            "settings",
                    onClick = onMenu
                )
            }

            Spacer(
                Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                )
            )
        }
    }
}

@Composable
private fun ModernNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by
        animateFloatAsState(
            targetValue =
                if (selected) {
                    1.08f
                } else {
                    1f
                },
            animationSpec =
                tween(180),
            label = "nav-scale"
        )

    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .size(
                    width = 68.dp,
                    height = 62.dp
                )
                .scale(scale),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            if (selected) {
                MaterialTheme
                    .colorScheme
                    .secondaryContainer
            } else {
                MaterialTheme
                    .colorScheme
                    .surfaceContainer
            },
        contentColor =
            if (selected) {
                MaterialTheme
                    .colorScheme
                    .onSecondaryContainer
            } else {
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
            }
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription =
                    label,
                modifier =
                    Modifier.size(
                        if (selected) {
                            25.dp
                        } else {
                            23.dp
                        }
                    )
            )

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(
                    tween(160)
                ),
                exit = fadeOut(
                    tween(120)
                )
            ) {
                Text(
                    label,
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ModernDrawerContent(
    authState: AuthUiState,
    sharedHomeSyncState:
        SharedHomeSyncState,
    onFamily: () -> Unit,
    onSettings: () -> Unit,
    onTutorial: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor =
            MaterialTheme
                .colorScheme
                .surfaceContainerLow
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        24.dp
                    ),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    48.dp
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            (
                                authState.user
                                    ?.displayName
                                    ?.firstOrNull()
                                    ?: "M"
                                )
                                .uppercase(),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .padding(
                                    start = 12.dp
                                )
                                .weight(
                                    1f
                                )
                    ) {
                        Text(
                            authState.user
                                ?.displayName
                                ?.ifBlank {
                                    t("Felhasználó")
                                }
                                ?: t("Felhasználó"),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                        Text(
                            authState.user
                                ?.email
                                ?.ifBlank {
                                    t("Mérőóra Napló")
                                }
                                ?: t("Mérőóra Napló"),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        t("Családi csoport")
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription =
                            null
                    )
                },
                onClick = onFamily,
                colors =
                    NavigationDrawerItemDefaults
                        .colors()
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        when (
                            sharedHomeSyncState
                        ) {
                            SharedHomeSyncState.Syncing ->
                                t("Szinkronizálás…")

                            is SharedHomeSyncState.Error ->
                                t("Szinkronhiba")

                            else ->
                                t("Szinkronizálás most")
                        }
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription =
                            null
                    )
                },
                onClick = onSync
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        t("Útmutató újranézése")
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.School,
                        contentDescription =
                            null
                    )
                },
                onClick = onTutorial
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        t("Beállítások")
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription =
                            null
                    )
                },
                onClick = onSettings
            )

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 8.dp
                    )
            )

            NavigationDrawerItem(
                label = {
                    Text(
                        t("Kijelentkezés")
                    )
                },
                selected = false,
                icon = {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription =
                            null
                    )
                },
                onClick = onSignOut
            )
        }
    }
}
