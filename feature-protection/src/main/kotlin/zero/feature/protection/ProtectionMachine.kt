/*
 * MODULE: feature-protection
 * PURPOSE: Pure transition function for protection states.
 * TAKES: A current ProtectionState and a ProtectionEvent.
 * GIVES: A Transition with the next state and whether the event was accepted.
 * OWNS: No state.
 * DOES NOT OWN: Clocks, storage, events, or Android behavior.
 * DEPENDENCIES: ProtectionState, ProtectionEvent, ProtectionConstants.
 * CONTRACT: Only Start can be rejected. Tick, ScreenOff, and BootChanged always produce accepted transitions.
 *           A time earlier than the countdown start leaves the state unchanged.
 * FAILURE BEHAVIOR: Start with a non-positive duration throws IllegalArgumentException.
 *                   A rejected Start returns the unchanged state with accepted = false.
 */
package zero.feature.protection

/** Result of applying an event. [accepted] is false only for a rejected Start. */
data class Transition(val state: ProtectionState, val accepted: Boolean)

/** Pure state machine for protection. */
object ProtectionMachine {
    /** Applies [event] to [state] and returns the result. */
    fun reduce(state: ProtectionState, event: ProtectionEvent): Transition = when (event) {
        is ProtectionEvent.Start -> start(state, event)
        is ProtectionEvent.Tick -> Transition(tick(state, event.nowMillis), true)
        is ProtectionEvent.ScreenOff -> Transition(screenOff(state), true)
        is ProtectionEvent.BootChanged -> Transition(bootChanged(state, event.nowMillis), true)
    }

    private fun start(state: ProtectionState, event: ProtectionEvent.Start): Transition {
        require(event.durationMillis > 0) { "durationMillis must be positive, was ${event.durationMillis}" }
        if (state != ProtectionState.Armed) return Transition(state, false)
        if (!event.bootKnown) return Transition(state, false)
        return Transition(ProtectionState.Countdown(event.nowMillis, event.durationMillis), true)
    }

    private fun tick(state: ProtectionState, now: Long): ProtectionState = when (state) {
        ProtectionState.Armed -> state
        is ProtectionState.Countdown -> when {
            now < state.startedAtMillis -> state
            now < state.endsAtMillis -> state
            now < state.endsAtMillis + ProtectionConstants.GRACE_MILLIS ->
                ProtectionState.Grace(state.endsAtMillis)
            else -> ProtectionState.Armed
        }
        is ProtectionState.Grace ->
            if (now < state.endsAtMillis) state else ProtectionState.Armed
    }

    private fun screenOff(state: ProtectionState): ProtectionState =
        if (state is ProtectionState.Grace) ProtectionState.Armed else state

    private fun bootChanged(state: ProtectionState, now: Long): ProtectionState = when (state) {
        ProtectionState.Armed -> state
        is ProtectionState.Countdown -> ProtectionState.Countdown(now, state.durationMillis)
        is ProtectionState.Grace -> ProtectionState.Armed
    }
}
