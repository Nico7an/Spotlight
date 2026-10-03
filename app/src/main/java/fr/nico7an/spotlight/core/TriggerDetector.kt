package fr.nico7an.spotlight.core

import android.view.KeyEvent

/** Sous-ensemble d'un [KeyEvent], pour pouvoir tester la détection sans Android. */
data class KeyInput(
    val action: Int,
    val keyCode: Int,
    val metaState: Int = 0,
    val repeatCount: Int = 0,
    val eventTime: Long = 0L,
) {
    companion object {
        fun of(event: KeyEvent) =
            KeyInput(event.action, event.keyCode, event.metaState, event.repeatCount, event.eventTime)
    }
}

/**
 * Machine à états qui reconnaît le raccourci dans le flux de touches.
 * [onKey] renvoie `true` quand l'événement doit être consommé (non transmis au système).
 */
class TriggerDetector(private val onTrigger: () -> Unit) {

    private var tapArmed = false
    private var tapDownTime = 0L

    /** Relâchements de touches à avaler car leur appui a déclenché Spotlight. */
    private val swallowedUps = HashSet<Int>()

    fun onKey(event: KeyInput, shortcut: Shortcut, blockSystemAction: Boolean): Boolean {
        if (event.action == KeyEvent.ACTION_UP && swallowedUps.remove(Modifiers.canonical(event.keyCode))) {
            return true
        }
        return if (shortcut.isModifierTap) onTapKey(event, shortcut, blockSystemAction) else onComboKey(event, shortcut)
    }

    private fun onTapKey(event: KeyInput, shortcut: Shortcut, blockSystemAction: Boolean): Boolean {
        val group = Modifiers.groupOf(shortcut.keyCode)
        val isTarget = Modifiers.groupOf(event.keyCode) == group
        if (!isTarget) {
            // N'importe quelle autre touche transforme l'appui en combinaison (ex. Meta + Tab).
            if (event.action == KeyEvent.ACTION_DOWN) tapArmed = false
            return false
        }
        val otherModifiers = Modifiers.fromMetaState(event.metaState) and group.inv()
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    tapArmed = otherModifiers == 0
                    tapDownTime = event.eventTime
                }
                blockSystemAction
            }
            KeyEvent.ACTION_UP -> {
                val fire = tapArmed && otherModifiers == 0 && event.eventTime - tapDownTime <= TAP_TIMEOUT_MS
                tapArmed = false
                if (fire) onTrigger()
                fire || blockSystemAction
            }
            else -> false
        }
    }

    private fun onComboKey(event: KeyInput, shortcut: Shortcut): Boolean {
        if (event.keyCode != shortcut.keyCode) return false
        if (Modifiers.fromMetaState(event.metaState) != shortcut.modifiers) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount == 0) {
            onTrigger()
            swallowedUps += event.keyCode
            // Sinon le système verrait « Meta relâchée seule » et lancerait sa propre action.
            if ((shortcut.modifiers and Modifiers.META) != 0) swallowedUps += KeyEvent.KEYCODE_META_LEFT
        }
        return true
    }

    companion object {
        const val TAP_TIMEOUT_MS = 800L
    }
}
