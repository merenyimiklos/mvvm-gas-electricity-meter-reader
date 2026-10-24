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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.auth.AuthViewModel
import hu.merenyimiklos.meterreader.ui.AppText
import hu.merenyimiklos.meterreader.ui.AuthGate
import hu.merenyimiklos.meterreader.ui.MandatoryUpdateGate
import hu.merenyimiklos.meterreader.ui.MeterReaderApp
import hu.merenyimiklos.meterreader.ui.theme.MeterReaderTheme
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MeterViewModel by viewModels()
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
            val settings by
                viewModel.settings.collectAsStateWithLifecycle()

            LaunchedEffect(settings.appLanguage) {
                AppText.language =
                    settings.appLanguage
            }

            MeterReaderTheme(
                themeMode = settings.themeMode
            ) {
                MandatoryUpdateGate(
                    viewModel = viewModel
                ) {
                    AuthGate(
                        authViewModel =
                            authViewModel
                    ) {
                        MeterReaderApp(
                            viewModel = viewModel,
                            authViewModel =
                                authViewModel,
                            startOnAdd = startOnAdd
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkForUpdates()
        viewModel.syncSharedHome()
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
