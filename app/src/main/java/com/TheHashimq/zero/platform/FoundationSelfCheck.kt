/*
 * MODULE: app (temporary startup diagnostics)
 * PURPOSE: Logs clock and store behavior at launch so the foundation can be verified on a device.
 * TAKES: A ZeroClock and a ModuleStoreFactory.
 * GIVES: Logcat lines under tag ZeroFoundation.
 * OWNS: Two keys in the "foundation" store namespace: launch-count and last-boot-id.
 * DOES NOT OWN: Any user-facing behavior. Removed in Phase 6 when real health checks exist.
 * DEPENDENCIES: foundation-clock, foundation-persistence, Android logging.
 * CONTRACT: Never throws. Stores the boot ID only after FIRST_SEEN or REBOOTED, as BootReconciler requires.
 * FAILURE BEHAVIOR: Any exception is logged as an error under the same tag and swallowed.
 */
package com.TheHashimq.zero.platform

import android.util.Log
import zero.foundation.clock.BootReconciler
import zero.foundation.clock.BootState
import zero.foundation.clock.ZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.persistence.StoreKey

/** Temporary launch-time diagnostics for foundation services. */
object FoundationSelfCheck {
    private const val TAG = "ZeroFoundation"
    private val OWNER = ModuleId("foundation")
    private val LAUNCH_COUNT = StoreKey("launch-count")
    private val LAST_BOOT_ID = StoreKey("last-boot-id")

    /** Runs the checks, logs results, and contains ordinary failures. */
    fun run(clock: ZeroClock, stores: ModuleStoreFactory) {
        try {
            val store = stores.storeFor(OWNER)

            val launches = (store.getLong(LAUNCH_COUNT) ?: 0L) + 1L
            store.putLong(LAUNCH_COUNT, launches)
            Log.i(TAG, "launch count: $launches")

            val currentBoot = clock.currentBootId()
            Log.i(TAG, "boot id readable: ${currentBoot != null}")

            val state = BootReconciler.reconcile(store.getString(LAST_BOOT_ID), currentBoot)
            Log.i(TAG, "boot state: $state")
            if (currentBoot != null && (state == BootState.FIRST_SEEN || state == BootState.REBOOTED)) {
                store.putString(LAST_BOOT_ID, currentBoot)
            }

            Log.i(TAG, "monotonic ms: ${clock.monotonicMillis()}")
        } catch (failure: Exception) {
            Log.e(TAG, "self-check failed", failure)
        }
    }
}
