package fr.nico7an.spotlight.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.service.SpotlightAccessibilityService
import fr.nico7an.spotlight.ui.theme.SpotlightTheme

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val settings = SettingsStore.get(this)
        setContent {
            SpotlightTheme {
                SettingsScreen(serviceEnabled = serviceEnabled, settings = settings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = SpotlightAccessibilityService.isEnabled(this)
    }
}
