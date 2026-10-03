package fr.nico7an.spotlight.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import fr.nico7an.spotlight.core.Diagnostics
import fr.nico7an.spotlight.core.KeyInput
import fr.nico7an.spotlight.core.Modifiers
import fr.nico7an.spotlight.core.Shortcut
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
    private val handler = Handler(Looper.getMainLooper())
    private val detector = TriggerDetector { onTrigger() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsStore.get(this)
        Diagnostics.serviceConnected(true)
        AppRepository.get(this).refreshAsync()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Diagnostics.serviceConnected(false)
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            Diagnostics.keyReceived(describe(event))
        }
        ShortcutRecorder.active?.let { return it.onKey(event) }
        if (!::settings.isInitialized) return false
        return detector.onKey(KeyInput.of(event), settings.shortcut, settings.blockSystemAction)
    }

    private fun onTrigger() {
        Diagnostics.triggered()
        val wasShowing = SearchActivity.isShowing
        SearchActivity.toggle(this)
        if (wasShowing) return
        // HyperOS peut refuser silencieusement l'ouverture d'une activité depuis l'arrière-plan.
        handler.postDelayed({
            if (Diagnostics.lastTriggerUnanswered) {
                Diagnostics.launchBlocked()
                Toast.makeText(
                    this,
                    "Ouverture bloquée par le système : autorisez « Afficher des fenêtres pop-up en arrière-plan » pour Spotlight",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }, LAUNCH_CHECK_DELAY_MS)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private fun describe(event: KeyEvent): String {
        val mods = Modifiers.fromMetaState(event.metaState) and Modifiers.groupOf(event.keyCode).inv()
        val label = Shortcut(event.keyCode, mods).let {
            if (it.isModifierTap && mods != 0) (Modifiers.names(mods) + it.label()).joinToString(" + ") else it.label()
        }
        return "$label  ·  ${KeyEvent.keyCodeToString(event.keyCode)}"
    }

    companion object {
        private const val LAUNCH_CHECK_DELAY_MS = 1_000L

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
