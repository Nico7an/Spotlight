package fr.nico7an.spotlight.core

import android.view.KeyEvent

/**
 * Raccourci déclencheur.
 *
 * - Si [keyCode] est un modificateur (Meta, Alt…), il s'agit d'un « tap » : la touche
 *   pressée puis relâchée seule.
 * - Sinon c'est une combinaison classique : [modifiers] + [keyCode].
 */
data class Shortcut(val keyCode: Int, val modifiers: Int) {

    val isModifierTap: Boolean get() = Modifiers.groupOf(keyCode) != 0

    fun label(): String =
        if (isModifierTap) KeyNames.of(keyCode)
        else (Modifiers.names(modifiers) + KeyNames.of(keyCode)).joinToString(" + ")

    fun encode(): String = "$keyCode:$modifiers"

    companion object {
        val META_TAP = Shortcut(KeyEvent.KEYCODE_META_LEFT, 0)
        val META_SPACE = Shortcut(KeyEvent.KEYCODE_SPACE, Modifiers.META)
        val ALT_SPACE = Shortcut(KeyEvent.KEYCODE_SPACE, Modifiers.ALT)
        val CTRL_SPACE = Shortcut(KeyEvent.KEYCODE_SPACE, Modifiers.CTRL)

        val PRESETS = listOf(META_TAP, META_SPACE, ALT_SPACE, CTRL_SPACE)

        fun decode(value: String?): Shortcut? {
            val parts = value?.split(':') ?: return null
            if (parts.size != 2) return null
            val key = parts[0].toIntOrNull() ?: return null
            val mods = parts[1].toIntOrNull() ?: return null
            return Shortcut(key, mods)
        }
    }
}
