/*
 * MODULE: foundation-clock tests
 * PURPOSE: Verifies every BootReconciler outcome and the report-once sequence.
 * TAKES: Stored and current boot ID combinations.
 * GIVES: Pass/fail results for reboot classification.
 * OWNS: No production state.
 * DOES NOT OWN: Platform boot-ID reading.
 * DEPENDENCIES: foundation-clock, foundation-testing.
 * CONTRACT: Each combination maps to exactly one BootState.
 * FAILURE BEHAVIOR: Assertion failures identify the incorrect classification.
 */
package zero.foundation.clock

import kotlin.test.Test
import kotlin.test.assertEquals
import zero.foundation.testing.FakeBootIdSource

/** Tests boot-state classification and the caller's store-after-report sequence. */
class BootReconcilerTest {
    /** No stored ID and a current ID gives FIRST_SEEN. */
    @Test
    fun firstSeenWhenNothingStored() {
        assertEquals(BootState.FIRST_SEEN, BootReconciler.reconcile(null, "boot-1"))
    }

    /** Equal stored and current IDs give SAME_BOOT. */
    @Test
    fun sameBootWhenIdsMatch() {
        assertEquals(BootState.SAME_BOOT, BootReconciler.reconcile("boot-1", "boot-1"))
    }

    /** Different IDs give REBOOTED. */
    @Test
    fun rebootWhenIdsDiffer() {
        assertEquals(BootState.REBOOTED, BootReconciler.reconcile("boot-1", "boot-2"))
    }

    /** A missing current ID gives UNKNOWN even when an ID was stored. */
    @Test
    fun unknownWhenCurrentMissing() {
        assertEquals(BootState.UNKNOWN, BootReconciler.reconcile("boot-1", null))
    }

    /** Missing current and stored IDs give UNKNOWN. */
    @Test
    fun unknownWhenBothMissing() {
        assertEquals(BootState.UNKNOWN, BootReconciler.reconcile(null, null))
    }

    /** Reboot is reported once, after the caller records the new ID. */
    @Test
    fun rebootIsReportedOnceAfterRecording() {
        val ids = FakeBootIdSource("boot-1")
        var stored: String? = null

        assertEquals(BootState.FIRST_SEEN, BootReconciler.reconcile(stored, ids.currentBootId()))
        stored = ids.currentBootId()

        assertEquals(BootState.SAME_BOOT, BootReconciler.reconcile(stored, ids.currentBootId()))

        ids.bootId = "boot-2"
        assertEquals(BootState.REBOOTED, BootReconciler.reconcile(stored, ids.currentBootId()))
        stored = ids.currentBootId()

        assertEquals(BootState.SAME_BOOT, BootReconciler.reconcile(stored, ids.currentBootId()))
    }
}
