/*
 * MODULE: app (composition root)
 * PURPOSE: Builds foundation services once per process and starts the module registry.
 * TAKES: Android Application lifecycle callbacks.
 * GIVES: A started ModuleRegistry and the self-check run at launch.
 * OWNS: The registry instance and the services it was started with.
 * DOES NOT OWN: Module behavior, UI, or enforcement.
 * DEPENDENCIES: foundation-registry, foundation-runtime, foundation-clock, foundation-persistence, platform adapters.
 * CONTRACT: Exactly one ModuleRegistry exists per process, started once in onCreate.
 * FAILURE BEHAVIOR: Startup callback failures are reported by the registry, not thrown. The self-check catches its own exceptions.
 */
package com.TheHashimq.zero

import android.app.Application
import com.TheHashimq.zero.platform.FoundationSelfCheck
import com.TheHashimq.zero.platform.clock.AndroidBootIdSource
import com.TheHashimq.zero.platform.clock.AndroidMonotonicSource
import com.TheHashimq.zero.platform.clock.AndroidWallClockSource
import com.TheHashimq.zero.platform.persistence.SharedPreferencesKeyValueBackend
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.registry.ModuleRegistry
import zero.foundation.runtime.ZeroServices

/** Application entry point that wires foundation services together. */
class ZeroApplication : Application() {
    /** The process-wide registry. Started once in onCreate. */
    val registry: ModuleRegistry = ModuleRegistry()

    /** Constructs foundation services and starts the registry once when the process launches. */
    override fun onCreate() {
        super.onCreate()

        val clock = DefaultZeroClock(
            AndroidMonotonicSource(),
            AndroidWallClockSource(),
            AndroidBootIdSource(),
        )
        val stores = ModuleStoreFactory(SharedPreferencesKeyValueBackend(this))

        registry.startAll(ZeroServices(clock, stores))
        FoundationSelfCheck.run(clock, stores)
    }
}
