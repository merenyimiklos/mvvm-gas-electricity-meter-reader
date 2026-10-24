package hu.merenyimiklos.meterreader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.BuildConfig
import hu.merenyimiklos.meterreader.update.UpdateState
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

@Composable
fun MandatoryUpdateGate(
    viewModel: MeterViewModel,
    content: @Composable () -> Unit
) {
    val updateState by
        viewModel.updateState
            .collectAsStateWithLifecycle()

    when (
        val state =
            updateState
    ) {
        UpdateState.Idle,
        UpdateState.Checking ->
            UpdateCheckingScreen()

        is UpdateState.Available ->
            MandatoryUpdateScreen(
                currentVersion =
                    BuildConfig.VERSION_NAME,
                newVersion =
                    state.info.version,
                notes =
                    state.info.notes,
                downloadUrl =
                    state.info.downloadUrl
            )

        UpdateState.UpToDate,
        is UpdateState.Error ->
            content()
    }
}

@Composable
private fun UpdateCheckingScreen() {
    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .surface
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {
                CircularProgressIndicator()
                Text(
                    t("Frissítés ellenőrzése…"),
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )
            }
        }
    }
}

@Composable
private fun MandatoryUpdateScreen(
    currentVersion: String,
    newVersion: String,
    notes: String,
    downloadUrl: String
) {
    val context =
        LocalContext.current

    Surface(
        modifier =
            Modifier.fillMaxSize(),
        color =
            MaterialTheme
                .colorScheme
                .surface
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 42.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Surface(
                modifier =
                    Modifier.size(
                        92.dp
                    ),
                shape =
                    androidx.compose.foundation
                        .shape.CircleShape,
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        Icons.Default
                            .SystemUpdateAlt,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                46.dp
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                Modifier.height(
                    22.dp
                )
            )

            Text(
                t("Frissítés szükséges"),
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                Modifier.height(
                    10.dp
                )
            )

            Text(
                t("Új verzió érhető el, az alkalmazás használatához frissítened kell."),
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                Modifier.height(
                    18.dp
                )
            )

            Text(
                t("Telepített: ") +
                    currentVersion +
                    "\n" +
                    t("Új verzió: ") +
                    newVersion,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign =
                    TextAlign.Center
            )

            if (
                notes.isNotBlank()
            ) {
                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )
                Text(
                    notes.take(
                        700
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    textAlign =
                        TextAlign.Center
                )
            }

            Spacer(
                Modifier.height(
                    26.dp
                )
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    downloadUrl
                                )
                            )
                        )
                    }
                }
            ) {
                Icon(
                    Icons.Default
                        .SystemUpdateAlt,
                    contentDescription =
                        null
                )
                Spacer(
                    Modifier.size(
                        8.dp
                    )
                )
                Text(
                    t("Frissítés most")
                )
            }

            Spacer(
                Modifier.height(
                    12.dp
                )
            )

            Text(
                t("A frissítés után automatikusan visszatérhetsz az alkalmazáshoz."),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}
