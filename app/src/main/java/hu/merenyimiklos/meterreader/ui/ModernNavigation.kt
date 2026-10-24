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
import androidx.compose.material.icons.filled.Bolt
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
    val tabs = listOf(
        ModernDestination("home", t("Áttekintés"), Icons.Default.Home),
        ModernDestination("add", t("Leolvasás"), Icons.Default.Bolt),
        ModernDestination("history", t("Napló"), Icons.Default.ReceiptLong),
        ModernDestination("statistics", t("Statisztika"), Icons.Default.BarChart),
        ModernDestination("settings", t("Beállítások"), Icons.Default.Settings)
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(78.dp)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    ModernNavItem(
                        label = tab.label,
                        icon = tab.icon,
                        selected = currentRoute == tab.route,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (tab.route == "add") onAdd() else onNavigate(tab.route)
                        }
                    )
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ModernNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 2.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.height(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                            authState.user
                                ?.displayName
                                ?.firstOrNull()
                                ?.uppercaseChar()
                                ?.toString()
                                ?: "M",
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
