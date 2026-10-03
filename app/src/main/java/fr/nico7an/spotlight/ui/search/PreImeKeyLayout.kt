package fr.nico7an.spotlight.ui.search

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * Conteneur qui voit les touches du clavier physique **avant** la méthode de saisie.
 *
 * Avec un clavier physique, l'IME (Gboard, clavier Xiaomi…) reçoit les touches avant l'app et
 * consomme ↑ ↓ et Entrée tant qu'un mot est en cours de composition pour ses suggestions.
 * Les intercepter ici garantit une navigation au clavier fiable quel que soit l'IME.
 */
class PreImeKeyLayout(context: Context) : FrameLayout(context) {

    var onKey: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean =
        onKey?.invoke(event) == true || super.dispatchKeyEventPreIme(event)
}
