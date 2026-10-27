/*
 * MODULE: foundation-registry
 * PURPOSE: Registers modules and coordinates their startup and shutdown.
 * TAKES: Explicit module registrations and one startup context.
 * GIVES: Ordered lifecycle reports and current module statuses.
 * OWNS: Registration order, started state, and per-module lifecycle status.
 * DOES NOT OWN: Module discovery, service construction, persistence, or threading.
 * DEPENDENCIES: foundation-contract.
 * CONTRACT: Starts in registration order and stops running modules in reverse order.
 * FAILURE BEHAVIOR: Callback failures are recorded and do not interrupt other modules.
 */
package zero.foundation.registry

import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ModuleStatus
import zero.foundation.contract.ZeroModule

/**
 * Manages explicit module registrations and their lifecycle.
 *
 * Not thread-safe. Call from one thread.
 */
class ModuleRegistry {
    private data class Entry(
        val module: ZeroModule,
        var status: ModuleStatus,
    )

    private val entries = LinkedHashMap<ModuleId, Entry>()
    private var started = false

    /**
     * Adds a module. Registration order is the start order.
     * Throws DuplicateModuleException if the id is already registered.
     * Throws IllegalStateException if startAll has already been called.
     */
    fun register(module: ZeroModule) {
        check(!started) { "Modules cannot be registered after startup has begun" }
        if (entries.containsKey(module.id)) {
            throw DuplicateModuleException(module.id)
        }
        entries[module.id] = Entry(module, ModuleStatus.REGISTERED)
    }

    /**
     * Starts every registered module in registration order, passing [context].
     * A module whose onStart throws is marked FAILED; the others still start.
     * Throws IllegalStateException if called more than once.
     */
    fun startAll(context: ModuleContext): StartReport {
        check(!started) { "Modules have already been started" }
        started = true

        val startedIds = mutableListOf<ModuleId>()
        val failed = linkedMapOf<ModuleId, Throwable>()
        for ((id, entry) in entries) {
            entry.status = ModuleStatus.STARTING
            try {
                entry.module.onStart(context)
                entry.status = ModuleStatus.RUNNING
                startedIds.add(id)
            } catch (failure: Throwable) {
                entry.status = ModuleStatus.FAILED
                failed[id] = failure
            }
        }
        return StartReport(startedIds.toList(), failed.toMap())
    }

    /**
     * Stops every module that is RUNNING, in reverse registration order.
     * Modules that FAILED at start are not stopped.
     * A module whose onStop throws is recorded in the report; the rest still stop.
     * Safe to call when startAll was never called (returns empty report).
     */
    fun stopAll(): StopReport {
        val stoppedIds = mutableListOf<ModuleId>()
        val failed = linkedMapOf<ModuleId, Throwable>()
        for ((id, entry) in entries.entries.toList().asReversed()) {
            if (entry.status != ModuleStatus.RUNNING) continue

            entry.status = ModuleStatus.STOPPING
            try {
                entry.module.onStop()
                entry.status = ModuleStatus.STOPPED
                stoppedIds.add(id)
            } catch (failure: Throwable) {
                entry.status = ModuleStatus.FAILED
                failed[id] = failure
            }
        }
        return StopReport(stoppedIds.toList(), failed.toMap())
    }

    /** Returns the current status, or null if [id] is not registered. */
    fun status(id: ModuleId): ModuleStatus? = entries[id]?.status

    /** Returns all registered ids in registration order. */
    fun registeredIds(): List<ModuleId> = entries.keys.toList()
}

/** Base class for errors raised by the module registry. */
open class ZeroRegistryException(message: String) : RuntimeException(message)

/** Raised when a module id has already been registered. */
class DuplicateModuleException(val id: ModuleId) :
    ZeroRegistryException("Module '$id' is already registered")
