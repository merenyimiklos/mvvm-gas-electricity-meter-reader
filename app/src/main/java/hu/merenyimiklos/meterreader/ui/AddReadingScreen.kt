package hu.merenyimiklos.meterreader.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

@Composable
internal fun AddReadingScreen(
    viewModel: MeterViewModel,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var meterType by remember { mutableStateOf(MeterType.ELECTRICITY) }
    var valueText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var note by remember { mutableStateOf("") }
    var photoUriForSavedReading by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var ocrLoading by remember { mutableStateOf(false) }

    fun analyze(uri: Uri, persistPhoto: Boolean) {
        if (persistPhoto) photoUriForSavedReading = uri.toString()
        ocrLoading = true

        scope.launch {
            viewModel.detectReading(uri, meterType)
                .onSuccess { result ->
                    result.detectedValue?.let { valueText = editableNumber(it) }
                    ocrText = result.rawText

                    if (result.detectedValue == null) {
                        snackbarHostState.showSnackbar(
                            "Nem találtam biztosan mérőállást. Írd be kézzel."
                        )
                    }
                }
                .onFailure {
                    snackbarHostState.showSnackbar(
                        "A képfelismerés nem sikerült: " +
                            (it.message ?: "ismeretlen hiba")
                    )
                }

            ocrLoading = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { analyze(it, true) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { analyze(it, false) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Új mérőállás",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text("Mérő típusa", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeterType.entries.forEach { type ->
                    FilterChip(
                        selected = meterType == type,
                        onClick = {
                            meterType = type
                            ocrText = ""
                        },
                        label = { Text(type.displayName) }
                    )
                }
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mérőállás (" + meterType.unit + ")") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dátum (ÉÉÉÉ-HH-NN)") },
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Megjegyzés") },
                minLines = 2
            )

            Text("Fotós beolvasás", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !ocrLoading,
                    onClick = {
                        runCatching { createPhotoUri(context) }
                            .onSuccess { uri ->
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                            .onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Nem sikerült képfájlt létrehozni."
                                    )
                                }
                            }
                    }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Fotózás")
                }

                OutlinedButton(
                    enabled = !ocrLoading,
                    onClick = {
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Galéria")
                }
            }

            if (ocrLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mérőállás felismerése…")
                }
            }

            if (ocrText.isNotBlank()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "OCR által felismert szöveg",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            ocrText,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Mentés előtt ellenőrizd a mérőállást.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val error = viewModel.saveReading(
                            type = meterType,
                            valueText = valueText,
                            dateText = dateText,
                            note = note,
                            photoUri = photoUriForSavedReading
                        )

                        if (error == null) {
                            snackbarHostState.showSnackbar(
                                "Mérőállás elmentve."
                            )
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar(error)
                        }
                    }
                }
            ) {
                Text("Mérőállás mentése")
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun createPhotoUri(context: Context): Uri {
    val directory = File(
        context.filesDir,
        "meter_photos"
    ).apply { mkdirs() }

    val file = File.createTempFile(
        "meter_",
        ".jpg",
        directory
    )

    return FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
}
