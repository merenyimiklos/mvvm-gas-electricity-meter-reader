package hu.merenyimiklos.meterreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import hu.merenyimiklos.meterreader.ui.MeterReaderApp
import hu.merenyimiklos.meterreader.ui.theme.MeterReaderTheme
import hu.merenyimiklos.meterreader.viewmodel.MeterViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeterReaderTheme {
                MeterReaderApp(viewModel)
            }
        }
    }
}
