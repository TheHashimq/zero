/*
 * MODULE: app (platform adapter for foundation-clock)
 * PURPOSE: Supplies real Android time and boot-ID values to foundation-clock.
 * TAKES: Android SystemClock, System.currentTimeMillis, and /proc boot_id.
 * GIVES: MonotonicSource, WallClockSource, and BootIdSource implementations.
 * OWNS: No state.
 * DOES NOT OWN: Clock policy or reboot interpretation.
 * DEPENDENCIES: foundation-clock, Android SDK.
 * CONTRACT: Boot ID is null when the kernel file cannot be read.
 * FAILURE BEHAVIOR: Read failures and empty content return null instead of throwing.
 */
package com.TheHashimq.zero.platform.clock

import android.os.SystemClock
import java.io.File
import java.io.IOException
import zero.foundation.clock.BootIdSource
import zero.foundation.clock.MonotonicSource
import zero.foundation.clock.WallClockSource

/** Monotonic time from SystemClock.elapsedRealtime, which counts since boot. */
class AndroidMonotonicSource : MonotonicSource {
    override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
}

/** Wall-clock time from System.currentTimeMillis. */
class AndroidWallClockSource : WallClockSource {
    override fun epochMillis(): Long = System.currentTimeMillis()
}

/** Boot ID read from the kernel. May be unavailable on some devices. */
class AndroidBootIdSource : BootIdSource {
    override fun currentBootId(): String? = try {
        File(BOOT_ID_PATH).readText().trim().ifEmpty { null }
    } catch (failure: IOException) {
        null
    } catch (failure: SecurityException) {
        null
    }

    private companion object {
        const val BOOT_ID_PATH = "/proc/sys/kernel/random/boot_id"
    }
}
