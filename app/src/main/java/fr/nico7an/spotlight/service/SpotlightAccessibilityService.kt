package fr.nico7an.spotlight.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import fr.nico7an.spotlight.core.KeyInput
import fr.nico7an.spotlight.core.ShortcutRecorder
import fr.nico7an.spotlight.core.TriggerDetector
import fr.nico7an.spotlight.data.AppRepository
import fr.nico7an.spotlight.data.SettingsStore
import fr.nico7an.spotlight.ui.search.SearchActivity

/**
 * Filtre les touches du clavier physique avant le système pour détecter le raccourci
 * d'ouverture. Seul le raccourci est consommé : toutes les autres touches passent.
 */
class SpotlightAccessibilityService : AccessibilityService() {

    private lateinit var settings: SettingsStore
    private val detector = TriggerDetector { SearchActivity.toggle(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsStore.get(this)
        AppRepository.get(this).refreshAsync()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        ShortcutRecorder.active?.let { return it.onKey(event) }
        if (!::settings.isInitialized) return false
        return detector.onKey(KeyInput.of(event), settings.shortcut, settings.blockSystemAction)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val me = ComponentName(context, SpotlightAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
