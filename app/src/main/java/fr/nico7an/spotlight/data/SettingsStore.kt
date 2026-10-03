package fr.nico7an.spotlight.data

import android.content.Context
import fr.nico7an.spotlight.core.Shortcut

/**
 * Réglages de l'app. Les valeurs sont gardées en mémoire car le service d'accessibilité
 * les lit à chaque frappe ; toutes les écritures passent par ce singleton.
 */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    @Volatile
    var shortcut: Shortcut = Shortcut.decode(prefs.getString(KEY_SHORTCUT, null)) ?: Shortcut.META_TAP
        set(value) {
            field = value
            prefs.edit().putString(KEY_SHORTCUT, value.encode()).apply()
        }

    @Volatile
    var customShortcut: Shortcut? = Shortcut.decode(prefs.getString(KEY_CUSTOM, null))
        set(value) {
            field = value
            prefs.edit().putString(KEY_CUSTOM, value?.encode()).apply()
        }

    @Volatile
    var blockSystemAction: Boolean = prefs.getBoolean(KEY_BLOCK_SYSTEM, false)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_BLOCK_SYSTEM, value).apply()
        }

    @Volatile
    var showSuggestions: Boolean = prefs.getBoolean(KEY_SUGGESTIONS, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()
        }

    @Volatile
    var blurBackground: Boolean = prefs.getBoolean(KEY_BLUR, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_BLUR, value).apply()
        }

    companion object {
        private const val KEY_SHORTCUT = "shortcut"
        private const val KEY_CUSTOM = "custom_shortcut"
        private const val KEY_BLOCK_SYSTEM = "block_system_action"
        private const val KEY_SUGGESTIONS = "show_suggestions"
        private const val KEY_BLUR = "blur_background"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
    }
}
