package fr.nico7an.spotlight.core

import android.view.KeyEvent

/** Masque de modificateurs indépendant du côté (gauche/droite) de la touche. */
object Modifiers {
    const val CTRL = 1
    const val ALT = 2
    const val SHIFT = 4
    const val META = 8

    fun fromMetaState(metaState: Int): Int {
        var mods = 0
        if ((metaState and KeyEvent.META_CTRL_ON) != 0) mods = mods or CTRL
        if ((metaState and KeyEvent.META_ALT_ON) != 0) mods = mods or ALT
        if ((metaState and KeyEvent.META_SHIFT_ON) != 0) mods = mods or SHIFT
        if ((metaState and KeyEvent.META_META_ON) != 0) mods = mods or META
        return mods
    }

    /** Le modificateur porté par [keyCode], ou 0 si ce n'est pas une touche de modification. */
    fun groupOf(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> CTRL
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ALT
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> SHIFT
        KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> META
        else -> 0
    }

    /** Ramène les variantes gauche/droite d'un modificateur à une seule touche. */
    fun canonical(keyCode: Int): Int = when (groupOf(keyCode)) {
        CTRL -> KeyEvent.KEYCODE_CTRL_LEFT
        ALT -> KeyEvent.KEYCODE_ALT_LEFT
        SHIFT -> KeyEvent.KEYCODE_SHIFT_LEFT
        META -> KeyEvent.KEYCODE_META_LEFT
        else -> keyCode
    }

    fun names(mods: Int): List<String> = buildList {
        if ((mods and META) != 0) add("Meta")
        if ((mods and CTRL) != 0) add("Ctrl")
        if ((mods and ALT) != 0) add("Alt")
        if ((mods and SHIFT) != 0) add("Maj")
    }
}

object KeyNames {
    fun of(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> "Meta"
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "Ctrl"
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "Alt"
        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "Maj"
        KeyEvent.KEYCODE_SPACE -> "Espace"
        KeyEvent.KEYCODE_ENTER -> "Entrée"
        KeyEvent.KEYCODE_TAB -> "Tab"
        KeyEvent.KEYCODE_ESCAPE -> "Échap"
        KeyEvent.KEYCODE_DEL -> "Retour arrière"
        KeyEvent.KEYCODE_FORWARD_DEL -> "Suppr"
        KeyEvent.KEYCODE_SEARCH -> "Recherche"
        KeyEvent.KEYCODE_ALL_APPS -> "Toutes les apps"
        else -> {
            val raw = KeyEvent.keyCodeToString(keyCode)
            if (raw == null || !raw.startsWith("KEYCODE_")) {
                "Touche $keyCode"
            } else {
                raw.removePrefix("KEYCODE_").split('_').joinToString(" ") { part ->
                    if (part.length <= 1) part else part.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
        }
    }
}
