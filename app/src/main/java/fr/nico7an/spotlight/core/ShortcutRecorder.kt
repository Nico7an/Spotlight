package fr.nico7an.spotlight.core

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Capture d'un nouveau raccourci. Alimentée par le service d'accessibilité quand il est
 * actif (seul moyen de voir la touche Meta avant le système), sinon par la boîte de dialogue.
 */
class ShortcutRecorder {
    var preview by mutableStateOf<String?>(null)
        private set
    var result by mutableStateOf<Shortcut?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var cancelled by mutableStateOf(false)
        private set

    private var pendingTap = 0

    /** Consomme toujours l'événement : pendant l'enregistrement, rien ne doit fuiter. */
    fun onKey(event: KeyEvent): Boolean {
        if (result != null || cancelled) return true
        val group = Modifiers.groupOf(event.keyCode)
        val mods = Modifiers.fromMetaState(event.metaState)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (group != 0) {
                if (event.repeatCount == 0) pendingTap = Modifiers.canonical(event.keyCode)
                preview = Modifiers.names(mods or group).joinToString(" + ") + " + …"
                error = null
            } else {
                pendingTap = 0
                when {
                    event.keyCode == KeyEvent.KEYCODE_ESCAPE && mods == 0 -> cancelled = true
                    (mods and Modifiers.SHIFT.inv()) == 0 && event.isPrintingKey -> {
                        preview = null
                        error = "Ajoutez Ctrl, Alt ou Meta : cette touche sert à écrire."
                    }
                    else -> result = Shortcut(event.keyCode, mods).also { preview = it.label() }
                }
            }
            KeyEvent.ACTION_UP -> {
                val othersHeld = (mods and group.inv()) != 0
                if (group != 0 && !othersHeld && pendingTap == Modifiers.canonical(event.keyCode)) {
                    result = Shortcut(pendingTap, 0).also { preview = it.label() }
                }
            }
        }
        return true
    }

    companion object {
        @Volatile
        var active: ShortcutRecorder? = null
    }
}
