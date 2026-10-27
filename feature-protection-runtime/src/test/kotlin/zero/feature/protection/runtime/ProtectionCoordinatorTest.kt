/*
 * MODULE: feature-protection-runtime tests
 * PURPOSE: Verifies persistence, transitions, reboot handling, redirects, guards, and failure behavior.
 * TAKES: Fake clock sources, an in-memory backend, and a real EventService.
 * GIVES: Pass/fail results for the coordinator.
 * OWNS: No production state.
 * DOES NOT OWN: Android behavior.
 * DEPENDENCIES: feature-protection-runtime, feature-protection, foundation-testing.
 * CONTRACT: Each test asserts one rule.
 * FAILURE BEHAVIOR: Assertion failures identify the broken rule.
 */
package zero.feature.protection.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zero.feature.protection.ProtectionAction
import zero.feature.protection.ProtectionConstants
import zero.feature.protection.ProtectionState
import zero.foundation.clock.BootState
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.events.EventBus
import zero.foundation.events.EventService
import zero.foundation.events.PersistentEventLog
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.persistence.PersistenceException
import zero.foundation.persistence.StoreKey
import zero.foundation.testing.FakeBootIdSource
import zero.foundation.testing.FakeMonotonicSource
import zero.foundation.testing.FakeWallClockSource
import zero.foundation.testing.InMemoryKeyValueBackend

private const val OWN = "com.TheHashimq.zero"
private const val SETTINGS = "com.android.settings"
private const val MINUTE = 60_000L

class ProtectionCoordinatorTest {
    private class Fixture(boot: String? = "boot-1") {
        val mono = FakeMonotonicSource(1_000L)
        val bootIds = FakeBootIdSource(boot)
        val clock = DefaultZeroClock(mono, FakeWallClockSource(), bootIds)
        val backend = InMemoryKeyValueBackend()
        val stores = ModuleStoreFactory(backend)
        val source = ModuleId("protection")
        val store = stores.storeFor(source)
        val events = EventService(
            PersistentEventLog(stores.storeFor(ModuleId("events")), clock),
            EventBus(),
        )

        fun coordinator() = ProtectionCoordinator(clock, store, events, source, OWN, setOf(SETTINGS))

        fun types(): List<String> = events.recent(50).map { it.type.value }.reversed()
    }

    /** A start with a known boot ID enters Countdown and records protection.started. */
    @Test
    fun startAcceptedWithKnownBootRecordsCountdownAndEvent() {
        val f = Fixture()
        val c = f.coordinator()

        assertTrue(c.startCommitment(MINUTE))
        assertEquals(ProtectionState.Countdown(1_000L, MINUTE), c.state())
        assertEquals(listOf("protection.started"), f.types())
    }

    /** A start without a boot ID is rejected and records nothing. */
    @Test
    fun startRejectedWithoutBootIdLeavesArmed() {
        val f = Fixture(boot = null)
        val c = f.coordinator()

        assertFalse(c.startCommitment(MINUTE))
        assertEquals(ProtectionState.Armed, c.state())
        assertEquals(emptyList(), f.types())
    }

    /** A second start during a countdown is rejected and leaves the countdown unchanged. */
    @Test
    fun startRejectedDuringCountdownKeepsState() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)
        val before = c.state()

        assertFalse(c.startCommitment(2 * MINUTE))
        assertEquals(before, c.state())
    }

    /** At the countdown end, the state becomes Grace and records protection.grace-started. */
    @Test
    fun tickAtEndMovesToGraceAndRecordsEvent() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)

        f.mono.setTo(1_000L + MINUTE)

        assertEquals(ProtectionState.Grace(1_000L + MINUTE), c.tick())
        assertEquals(listOf("protection.started", "protection.grace-started"), f.types())
    }

    /** At the grace end, the state returns to Armed and records protection.armed. */
    @Test
    fun tickAfterGraceReturnsToArmedAndRecordsEvent() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)
        f.mono.setTo(1_000L + MINUTE)
        c.tick()

        f.mono.setTo(1_000L + MINUTE + ProtectionConstants.GRACE_MILLIS)

        assertEquals(ProtectionState.Armed, c.tick())
        assertEquals("protection.armed", f.types().last())
    }

    /** Screen off during grace returns to Armed. */
    @Test
    fun screenOffRevokesGrace() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)
        f.mono.setTo(1_000L + MINUTE)
        c.tick()

        assertEquals(ProtectionState.Armed, c.onScreenOff())
    }

    /** A new coordinator over the same store sees the countdown, like a restarted process. */
    @Test
    fun stateSurvivesNewCoordinatorOverSameStore() {
        val f = Fixture()
        f.coordinator().startCommitment(MINUTE)

        assertEquals(ProtectionState.Countdown(1_000L, MINUTE), f.coordinator().state())
    }

    /** A failed write on start throws and leaves the state Armed. */
    @Test
    fun persistFailureOnStartKeepsArmed() {
        val f = Fixture()
        val c = f.coordinator()
        f.backend.failWrites = true

        assertFailsWith<PersistenceException> { c.startCommitment(MINUTE) }
        assertEquals(ProtectionState.Armed, c.state())
    }

    /** A corrupted stored state restores as Armed and records protection.state-corrupt. */
    @Test
    fun corruptStoredStateRestoresArmedAndRecordsEvent() {
        val f = Fixture()
        f.store.putString(StoreKey("state"), "BOGUS")

        val c = f.coordinator()

        assertEquals(ProtectionState.Armed, c.state())
        assertEquals(listOf("protection.state-corrupt"), f.types())
    }

    /** The first boot observation stores the boot ID and does not change the state. */
    @Test
    fun firstObservationStoresBootIdWithoutChangingState() {
        val f = Fixture()
        val c = f.coordinator()

        assertEquals(BootState.FIRST_SEEN, c.reconcileBoot())
        assertEquals("boot-1", f.store.getString(StoreKey("boot-id")))
        assertEquals(ProtectionState.Armed, c.state())
    }

    /** An unreadable boot ID leaves both the state and the stored boot ID alone. */
    @Test
    fun unknownBootIdLeavesStateAndStoredIdAlone() {
        val f = Fixture()
        f.store.putString(StoreKey("boot-id"), "boot-1")
        f.bootIds.bootId = null
        val c = f.coordinator()

        assertEquals(BootState.UNKNOWN, c.reconcileBoot())
        assertEquals("boot-1", f.store.getString(StoreKey("boot-id")))
    }

    /** A reboot during a countdown restarts it from the new time with full duration. */
    @Test
    fun rebootRestartsCountdownFromNowWithFullDuration() {
        val f = Fixture()
        val c = f.coordinator()
        c.reconcileBoot()
        c.startCommitment(MINUTE)
        f.mono.setTo(5_000L)
        f.bootIds.bootId = "boot-2"

        assertEquals(BootState.REBOOTED, c.reconcileBoot())
        assertEquals(ProtectionState.Countdown(5_000L, MINUTE), c.state())
        assertEquals("boot-2", f.store.getString(StoreKey("boot-id")))
        assertTrue(f.types().contains("protection.reboot-restart"))
    }

    /** A protected app during a countdown is redirected and recorded with its package name. */
    @Test
    fun protectedForegroundDuringCountdownRedirectsAndRecords() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)

        assertEquals(ProtectionAction.RedirectHome, c.onForegroundChanged(SETTINGS))

        val blocked = f.events.recent(10).first { it.type.value == "protection.blocked-open" }
        assertEquals(mapOf("package" to SETTINGS), blocked.payload)
    }

    /** An unprotected app and our own package are allowed and record nothing. */
    @Test
    fun unprotectedAndOwnForegroundAreAllowedWithoutEvent() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)

        assertEquals(ProtectionAction.Allow, c.onForegroundChanged("com.example.notes"))
        assertEquals(ProtectionAction.Allow, c.onForegroundChanged(OWN))
        assertEquals(listOf("protection.started"), f.types())
    }

    /** A second redirect 1499 ms after the first is allowed; at 1500 ms it redirects again. */
    @Test
    fun redirectGuardAllowsSecondRedirectWithinGuardOnly() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)
        f.mono.setTo(10_000L)
        assertEquals(ProtectionAction.RedirectHome, c.onForegroundChanged(SETTINGS))

        f.mono.advance(ProtectionConstants.REDIRECT_GUARD_MILLIS - 1)
        assertEquals(ProtectionAction.Allow, c.onForegroundChanged(SETTINGS))

        f.mono.advance(1L)
        assertEquals(ProtectionAction.RedirectHome, c.onForegroundChanged(SETTINGS))
    }

    /** A failed event write does not block the redirect and is counted. */
    @Test
    fun eventFailureDoesNotBlockRedirectAndIsCounted() {
        val f = Fixture()
        val c = f.coordinator()
        c.startCommitment(MINUTE)
        f.backend.failWrites = true

        assertEquals(ProtectionAction.RedirectHome, c.onForegroundChanged(SETTINGS))
        assertEquals(1, c.droppedEventCount)
    }
}
