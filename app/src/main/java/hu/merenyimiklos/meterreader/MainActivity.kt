package hu.merenyimiklos.meterreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.auth.AuthViewModel
import hu.merenyimiklos.meterreader.data.UserDataScope
import hu.merenyimiklos.meterreader.ui.AppText
import hu.merenyimiklos.meterreader.ui.AuthGate
import hu.merenyimiklos.meterreader.ui.MandatoryUpdateGate
import hu.merenyimiklos.meterreader.ui.MeterReaderApp
import hu.merenyimiklos.meterreader.ui.theme.MeterReaderTheme
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val startOnAdd =
            intent?.action == ACTION_OPEN_ADD

        enableEdgeToEdge()
        setContent {
            val authState by authViewModel.state.collectAsStateWithLifecycle()
            // A new view model (and a new local database) is selected when
            // the Firebase UID changes, including an account switch.
            val userUid = authState.user?.uid ?: UserDataScope.DEBUG_GUEST
            val meterViewModel: MeterViewModel = viewModel(
                key = "meter-session-$userUid"
            )
            val settings by
                meterViewModel.settings.collectAsStateWithLifecycle()
            val hasLegacyData by
                meterViewModel.legacyImportAvailable.collectAsStateWithLifecycle()
            val legacyError by
                meterViewModel.legacyImportError.collectAsStateWithLifecycle()

            LaunchedEffect(settings.appLanguage) {
                AppText.language = settings.appLanguage
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                meterViewModel.checkForUpdates()
                if (authState.authenticated) {
                    meterViewModel.syncSharedHome()
                }
            }

            MeterReaderTheme(themeMode = settings.themeMode) {
                MandatoryUpdateGate(viewModel = meterViewModel) {
                    AuthGate(authViewModel = authViewModel) {
                        MeterReaderApp(
                            viewModel = meterViewModel,
                            authViewModel = authViewModel,
                            startOnAdd = startOnAdd
                        )
                    }
                }

                if (hasLegacyData && authState.user != null) {
                    AlertDialog(
                        onDismissRequest = { meterViewModel.dismissLegacyImport() },
                        title = { Text("Korábbi helyi adatok") },
                        text = {
                            Text(
                                "A telefonon még vannak fiókhoz nem kötött, régebbi mérőállások vagy beállítások. " +
                                    "Csak akkor importáld őket ebbe a fiókba, ha ezek a te adataid. " +
                                    "Az eredeti fájlokat a rendszer nem törli." +
                                    (legacyError?.let { "\\n\\nHiba: $it" } ?: "")
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { meterViewModel.importLegacyData() }) {
                                Text("Hozzárendelés ehhez a fiókhoz")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { meterViewModel.dismissLegacyImport() }) {
                                Text("Most nem")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    companion object {
        const val ACTION_OPEN_ADD =
            "hu.merenyimiklos.meterreader.OPEN_ADD"
    }
}
