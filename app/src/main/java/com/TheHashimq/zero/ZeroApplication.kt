/*
 * MODULE: app (composition root)
 * PURPOSE: Builds foundation services, registers modules, freezes navigation, and logs health.
 * TAKES: Android Application lifecycle callbacks.
 * GIVES: The registry, services, protection module, and launch-time logs.
 * OWNS: The registry, services, and protection module references.
 * DOES NOT OWN: Module behavior, UI rendering, or enforcement.
 * DEPENDENCIES: Foundation modules, feature-protection-runtime, and platform adapters.
 * CONTRACT: One registry and one services instance exist per process. Navigation is frozen after startup.
 * FAILURE BEHAVIOR: Module failures are reported by the registry. Health checks contain source exceptions.
 */
package com.TheHashimq.zero

import android.app.Application
import android.util.Log
import com.TheHashimq.zero.platform.FoundationSelfCheck
import com.TheHashimq.zero.platform.clock.AndroidBootIdSource
import com.TheHashimq.zero.platform.clock.AndroidMonotonicSource
import com.TheHashimq.zero.platform.clock.AndroidWallClockSource
import com.TheHashimq.zero.platform.persistence.SharedPreferencesKeyValueBackend
import com.TheHashimq.zero.platform.protection.AndroidAccessibilityEnabledProbe
import zero.feature.protection.runtime.ProtectionAccessibilityHealthSource
import zero.feature.protection.runtime.ProtectionModule
import zero.foundation.clock.DefaultZeroClock
import zero.foundation.contract.ModuleId
import zero.foundation.events.EventBus
import zero.foundation.events.EventService
import zero.foundation.events.PersistentEventLog
import zero.foundation.health.ClockHealthSource
import zero.foundation.health.HealthService
import zero.foundation.navigation.NavigationRegistry
import zero.foundation.persistence.ModuleStoreFactory
import zero.foundation.registry.ModuleRegistry
import zero.foundation.runtime.ZeroServices

/** Application entry point that wires foundation services together. */
class ZeroApplication : Application() {
    /** The process-wide registry. Started once in onCreate. */
    val registry: ModuleRegistry = ModuleRegistry()

    /** Protection module. Its coordinator exists after startup. */
    val protectionModule: ProtectionModule = ProtectionModule(
        ownPackage = "com.TheHashimq.zero",
        protectedPackages = setOf("com.android.settings"),
    )

    /** Services built in onCreate, read by the UI. */
    lateinit var services: ZeroServices
        private set

    /** Builds foundation services, starts protection, freezes navigation, and logs health. */
    override fun onCreate() {
        super.onCreate()

        val clock = DefaultZeroClock(
            AndroidMonotonicSource(),
            AndroidWallClockSource(),
            AndroidBootIdSource(),
        )
        val stores = ModuleStoreFactory(SharedPreferencesKeyValueBackend(this))
        val events = EventService(
            PersistentEventLog(stores.storeFor(ModuleId("events")), clock),
            EventBus(),
        )
        val navigation = NavigationRegistry()
        val accessibilityEnabled = AndroidAccessibilityEnabledProbe(this)
        val health = HealthService(
            listOf(
                ClockHealthSource(clock),
                ProtectionAccessibilityHealthSource(
                    enabled = { accessibilityEnabled.isEnabled() },
                    connected = { protectionModule.isServiceConnected() },
                ),
            ),
        )

        services = ZeroServices(clock, stores, events, health, navigation)
        registry.register(protectionModule)
        registry.startAll(services)
        navigation.freeze()
        FoundationSelfCheck.run(clock, stores)
        logHealth(health)
    }

    private fun logHealth(health: HealthService) {
        val system = health.report()
        Log.i(TAG, "health overall: ${system.overall}")
        system.components.forEach { Log.i(TAG, "health ${it.component}: ${it.state} (${it.detail})") }
    }

    private companion object {
        const val TAG = "ZeroFoundation"
    }
}
