/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Persists protection state, applies machine and policy decisions, and records events.
 * TAKES: ZeroClock, a ModuleStore, EventService, the source ModuleId, the own package, and the protected packages.
 * GIVES: Start, tick, screen-off, boot reconciliation, and foreground decisions.
 * OWNS: The current ProtectionState, the last redirect time, the service-connected flag, and the dropped-event count.
 * DOES NOT OWN: Performing the redirect or the accessibility connection. The service adapter does that.
 * DEPENDENCIES: feature-protection, foundation-clock, foundation-persistence, foundation-events.
 * CONTRACT: The new state is written to the store before it is applied in memory. A failed write leaves state unchanged.
 *           Protected packages are read at each decision. Start is rejected when none are protected.
 *           A Start is rejected without a known boot ID. A reboot during a countdown restarts it with full duration.
 *           Event recording failures are counted in droppedEventCount and never block protection.
 * FAILURE BEHAVIOR: Store write failures throw PersistenceException from startCommitment, tick, onScreenOff,
 *                   and reconcileBoot. A corrupted stored state restores as Armed and records protection.state-corrupt.
 */
package zero.feature.protection.runtime

import zero.feature.protection.ProtectionAction
import zero.feature.protection.ProtectionEvent
import zero.feature.protection.ProtectionMachine
import zero.feature.protection.ProtectionPolicy
import zero.feature.protection.ProtectionState
import zero.foundation.clock.BootReconciler
import zero.foundation.clock.BootState
import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.events.EventService
import zero.foundation.events.EventType
import zero.foundation.events.ZeroEvent
import zero.foundation.persistence.ModuleStore
import zero.foundation.persistence.PersistenceException
import zero.foundation.persistence.StoreKey

/** Coordinates protection state, persistence, events, and policy decisions. */
class ProtectionCoordinator(
    private val clock: ZeroClock,
    private val store: ModuleStore,
    private val events: EventService,
    private val source: ModuleId,
    private val ownPackage: String,
    private val protectedPackages: () -> Set<String>,
) {
    private var state: ProtectionState = ProtectionState.Armed
    private var lastRedirectMillis: Long? = null
    private var serviceConnected: Boolean = false

    /** Number of events that could not be recorded. Protection still applies when this grows. */
    var droppedEventCount: Int = 0
        private set

    init {
        state = restoreState()
    }

    /** Current protection state. */
    fun state(): ProtectionState = state

    /** Current monotonic time, for display. */
    fun nowMillis(): Long = clock.monotonicMillis()

    /** Whether the accessibility service is connected. */
    fun isServiceConnected(): Boolean = serviceConnected

    /** Called by the service adapter when it connects or disconnects. */
    fun setServiceConnected(connected: Boolean) {
        serviceConnected = connected
    }

    /** Most recent events, newest first. */
    fun recentEvents(limit: Int): List<ZeroEvent> = events.recent(limit)

    /** Starts a commitment. Returns false if the machine rejects it. */
    fun startCommitment(durationMillis: Long): Boolean {
        if (protectedPackages().isEmpty()) return false
        val bootKnown = clock.currentBootId() != null
        val result = ProtectionMachine.reduce(
            state,
            ProtectionEvent.Start(durationMillis, clock.monotonicMillis(), bootKnown),
        )
        if (!result.accepted) return false
        commit(result.state, "protection.started")
        return true
    }

    /** Advances time-based transitions. Returns the state after the tick. */
    fun tick(): ProtectionState {
        val next = ProtectionMachine.reduce(state, ProtectionEvent.Tick(clock.monotonicMillis())).state
        if (next != state) commit(next, eventFor(next))
        return state
    }

    /** Applies a screen-off event. Returns the state after it. */
    fun onScreenOff(): ProtectionState {
        val next = ProtectionMachine.reduce(state, ProtectionEvent.ScreenOff).state
        if (next != state) commit(next, eventFor(next))
        return state
    }

    /**
     * Compares the stored boot ID with the current one.
     * On REBOOTED, applies BootChanged, then stores the new boot ID. The state is written first,
     * so a failed write leaves the old boot ID in place and the next launch detects the reboot again.
     */
    fun reconcileBoot(): BootState {
        val current = clock.currentBootId()
        val stored = store.getString(BOOT_ID)
        val bootState = BootReconciler.reconcile(stored, current)

        if (bootState == BootState.REBOOTED) {
            val before = state
            val next = ProtectionMachine.reduce(
                before,
                ProtectionEvent.BootChanged(clock.monotonicMillis()),
            ).state
            if (next != before) {
                val type = if (before is ProtectionState.Countdown) "protection.reboot-restart" else eventFor(next)
                commit(next, type)
            }
        }

        if (current != null && (bootState == BootState.FIRST_SEEN || bootState == BootState.REBOOTED)) {
            store.putString(BOOT_ID, current)
        }
        return bootState
    }

    /** Decides whether a foreground app should be redirected Home. Records a blocked-open event on redirect. */
    fun onForegroundChanged(packageName: String): ProtectionAction {
        val now = clock.monotonicMillis()
        val action = ProtectionPolicy.decide(
            packageName,
            state,
            protectedPackages(),
            ownPackage,
            lastRedirectMillis,
            now,
        )
        if (action == ProtectionAction.RedirectHome) {
            lastRedirectMillis = now
            recordSafely("protection.blocked-open", mapOf("package" to packageName))
        }
        return action
    }

    private fun commit(next: ProtectionState, eventType: String?) {
        store.putString(STATE, ProtectionStateCodec.encode(next))
        state = next
        if (eventType != null) recordSafely(eventType, emptyMap())
    }

    private fun restoreState(): ProtectionState {
        val raw = store.getString(STATE) ?: return ProtectionState.Armed
        val decoded = ProtectionStateCodec.decode(raw)
        if (decoded != null) return decoded
        recordSafely("protection.state-corrupt", emptyMap())
        return ProtectionState.Armed
    }

    private fun eventFor(next: ProtectionState): String? = when (next) {
        is ProtectionState.Grace -> "protection.grace-started"
        ProtectionState.Armed -> "protection.armed"
        is ProtectionState.Countdown -> null
    }

    private fun recordSafely(type: String, payload: Map<String, String>) {
        try {
            events.record(source, EventType(type), payload)
        } catch (failure: PersistenceException) {
            droppedEventCount++
        }
    }

    private companion object {
        val STATE = StoreKey("state")
        val BOOT_ID = StoreKey("boot-id")
    }
}
