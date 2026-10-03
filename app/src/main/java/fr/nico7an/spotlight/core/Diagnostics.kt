package fr.nico7an.spotlight.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ce que le service voit réellement, affiché dans les réglages pour comprendre un échec. */
data class DiagnosticsState(
    val serviceConnected: Boolean = false,
    val lastKey: String? = null,
    val lastKeyAt: Long = 0L,
    val triggeredAt: Long = 0L,
    val openedAt: Long = 0L,
    val launchBlocked: Boolean = false,
)

object Diagnostics {
    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    fun serviceConnected(connected: Boolean) = _state.update { it.copy(serviceConnected = connected) }

    fun keyReceived(description: String) =
        _state.update { it.copy(lastKey = description, lastKeyAt = System.currentTimeMillis()) }

    fun triggered() =
        _state.update { it.copy(triggeredAt = System.currentTimeMillis(), launchBlocked = false) }

    fun launchBlocked() = _state.update { it.copy(launchBlocked = true) }

    fun searchOpened() =
        _state.update { it.copy(openedAt = System.currentTimeMillis(), launchBlocked = false) }

    /** Le dernier déclenchement n'a été suivi d'aucune ouverture de la fenêtre. */
    val lastTriggerUnanswered: Boolean
        get() = _state.value.let { it.triggeredAt > 0 && it.openedAt < it.triggeredAt }
}
