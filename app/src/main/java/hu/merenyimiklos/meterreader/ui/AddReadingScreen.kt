package hu.merenyimiklos.meterreader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import hu.merenyimiklos.meterreader.model.MeterType
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddReadingScreen(
    viewModel: MeterViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState =
        remember { SnackbarHostState() }

    var meterType by remember {
        mutableStateOf(MeterType.ELECTRICITY)
    }
    var valueText by remember {
        mutableStateOf("")
    }
    var dateText by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var note by remember {
        mutableStateOf("")
    }
    var photoUriForSavedReading by remember {
        mutableStateOf<String?>(null)
    }
    var pendingCameraUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var ocrText by remember {
        mutableStateOf("")
    }
    var ocrLoading by remember {
        mutableStateOf(false)
    }

    fun clearForm() {
        meterType = MeterType.ELECTRICITY
        valueText = ""
        dateText = LocalDate.now().toString()
        note = ""
        photoUriForSavedReading = null
        pendingCameraUri = null
        ocrText = ""
    }

    fun analyze(
        uri: Uri,
        persistPhoto: Boolean
    ) {
        if (persistPhoto) {
            photoUriForSavedReading =
                uri.toString()
        }

        ocrLoading = true

        scope.launch {
            viewModel
                .detectReading(uri, meterType)
                .onSuccess { result ->
                    result.detectedValue?.let {
                        valueText =
                            editableNumber(it)
                    }
                    ocrText = result.rawText

                    if (
                        result.detectedValue == null
                    ) {
                        snackbarHostState
                            .showSnackbar(
                                "Nem találtam biztosan mérőállást. Írd be kézzel."
                            )
                    }
                }
                .onFailure {
                    snackbarHostState.showSnackbar(
                        "A képfelismerés nem sikerült: " +
                            (
                                it.message
                                    ?: "ismeretlen hiba"
                                )
                    )
                }

            ocrLoading = false
        }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                pendingCameraUri?.let {
                    analyze(it, true)
                }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                analyze(it, false)
            }
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Új mérőállás")
                        Text(
                            "Fotózd le vagy add meg kézzel",
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
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.elevatedCardColors(
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
                                12.dp
                            )
                    ) {
                        Text(
                            "Melyik mérőt rögzíted?",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {
                            items(
                                MeterType.entries
                            ) { type ->

                                FilterChip(
                                    selected =
                                        meterType ==
                                            type,
                                    onClick = {
                                        meterType =
                                            type
                                        ocrText = ""
                                    },
                                    label = {
                                        Text(
                                            type.shortName
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(18.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )
                    ) {
                        Text(
                            meterType.displayName,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = valueText,
                            onValueChange = {
                                valueText = it
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "Mérőállás (" +
                                        meterType.unit +
                                        ")"
                                )
                            },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType
                                            .Decimal
                                ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {
                                dateText = it
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "Dátum (ÉÉÉÉ-HH-NN)"
                                )
                            },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = {
                                note = it
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "Megjegyzés"
                                )
                            },
                            minLines = 2
                        )
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.elevatedCardColors(
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
                                12.dp
                            )
                    ) {
                        Text(
                            "Fotós leolvasás",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {
                            Button(
                                enabled =
                                    !ocrLoading,
                                onClick = {
                                    runCatching {
                                        createPhotoUri(
                                            context
                                        )
                                    }.onSuccess {
                                        uri ->
                                        pendingCameraUri =
                                            uri
                                        cameraLauncher
                                            .launch(uri)
                                    }.onFailure {
                                        scope.launch {
                                            snackbarHostState
                                                .showSnackbar(
                                                    "Nem sikerült képfájlt létrehozni."
                                                )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default
                                        .CameraAlt,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        6.dp
                                    )
                                )
                                Text("Fotózás")
                            }

                            OutlinedButton(
                                enabled =
                                    !ocrLoading,
                                onClick = {
                                    galleryLauncher
                                        .launch(
                                            "image/*"
                                        )
                                }
                            ) {
                                Icon(
                                    Icons.Default
                                        .PhotoLibrary,
                                    contentDescription =
                                        null
                                )
                                Spacer(
                                    Modifier.width(
                                        6.dp
                                    )
                                )
                                Text("Galéria")
                            }
                        }

                        if (ocrLoading) {
                            Row(
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    Modifier.size(22.dp)
                                )
                                Spacer(
                                    Modifier.width(
                                        10.dp
                                    )
                                )
                                Text(
                                    "Mérőállás felismerése…"
                                )
                            }
                        }

                        if (ocrText.isNotBlank()) {
                            Text(
                                "Felismert szöveg",
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                            Text(
                                ocrText,
                                maxLines = 4,
                                overflow =
                                    TextOverflow
                                        .Ellipsis,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    contentPadding =
                        PaddingValues(
                            vertical = 14.dp
                        ),
                    onClick = {
                        scope.launch {
                            val error =
                                viewModel
                                    .saveReading(
                                        type =
                                            meterType,
                                        valueText =
                                            valueText,
                                        dateText =
                                            dateText,
                                        note = note,
                                        photoUri =
                                            photoUriForSavedReading
                                    )

                            if (error == null) {
                                clearForm()
                                snackbarHostState
                                    .showSnackbar(
                                        "✓ Sikeres rögzítés"
                                    )
                            } else {
                                snackbarHostState
                                    .showSnackbar(
                                        error
                                    )
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(
                        Modifier.width(8.dp)
                    )
                    Text("Mérőállás mentése")
                }
            }
        }
    }
}

private fun createPhotoUri(
    context: Context
): Uri {
    val directory = File(
        context.filesDir,
        "meter_photos"
    ).apply {
        mkdirs()
    }

    val file = File.createTempFile(
        "meter_",
        ".jpg",
        directory
    )

    return FileProvider.getUriForFile(
        context,
        context.packageName +
            ".fileprovider",
        file
    )
}
