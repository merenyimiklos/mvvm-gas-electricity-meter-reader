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
import androidx.core.content.ContextCompat
import hu.merenyimiklos.meterreader.reminder.ReadingReminderScheduler
import hu.merenyimiklos.meterreader.ui.MeterReaderApp
import hu.merenyimiklos.meterreader.ui.theme.MeterReaderTheme
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MeterViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        ReadingReminderScheduler.schedule(this)
        requestNotificationPermissionIfNeeded()

        val startOnAdd =
            intent?.action == ACTION_OPEN_ADD

        enableEdgeToEdge()
        setContent {
            MeterReaderTheme {
                MeterReaderApp(
                    viewModel = viewModel,
                    startOnAdd = startOnAdd
                )
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
