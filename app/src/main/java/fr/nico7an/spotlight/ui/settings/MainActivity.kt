package fr.nico7an.spotlight.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.service.SpotlightAccessibilityService
import fr.nico7an.spotlight.ui.theme.SpotlightTheme

class MainActivity : ComponentActivity() {

    private var status by mutableStateOf(SystemStatus())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = SettingsStore.get(this)
        setContent {
            val running by SpotlightAccessibilityService.running.collectAsState()
            SpotlightTheme {
                SettingsScreen(status = status.copy(serviceRunning = running), settings = settings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        status = SystemStatus.read(this)
    }
}
