package hu.merenyimiklos.meterreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actions:
        @Composable RowScope.() -> Unit = {}
) {
    LargeTopAppBar(
        title = {
            Column(
                modifier =
                    Modifier.padding(
                        top = 4.dp
                    )
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(
                                46.dp
                            )
                            .background(
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer,
                                shape =
                                    RoundedCornerShape(
                                        16.dp
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                25.dp
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )

                Text(
                    text = title,
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground
                )

                Text(
                    text = subtitle,
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
        },
        actions = actions,
        colors =
            TopAppBarDefaults
                .largeTopAppBarColors(
                    containerColor =
                        Color.Transparent,
                    scrolledContainerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                )
    )
}
