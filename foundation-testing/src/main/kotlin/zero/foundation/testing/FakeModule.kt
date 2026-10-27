/*
 * MODULE: foundation-testing
 * PURPOSE: Provides a configurable fake implementation of ZeroModule for tests.
 * TAKES: Module identity, optional lifecycle failures, and a shared call log.
 * GIVES: A fake module that records callback calls and its startup context.
 * OWNS: Its configured identity, observed context, and callback log reference.
 * DOES NOT OWN: Registry behavior or test assertions.
 * DEPENDENCIES: foundation-contract.
 * CONTRACT: Records lifecycle calls before throwing configured failures.
 * FAILURE BEHAVIOR: Configured callback errors are rethrown unchanged.
 */
package zero.foundation.testing

import zero.foundation.contract.ModuleContext
import zero.foundation.contract.ModuleId
import zero.foundation.contract.ZeroModule

/** Fake module that records lifecycle calls and can simulate callback failures. */
class FakeModule(
    override val id: ModuleId,
    override val displayName: String = "Fake ${id.value}",
    private val startError: Throwable? = null,
    private val stopError: Throwable? = null,
    private val log: MutableList<String> = mutableListOf(),
) : ZeroModule {
    /** Ordered record of calls, e.g. "start:settings", "stop:settings". */
    val calls: List<String> get() = log.toList()

    /** Context instance most recently received by onStart, if any. */
    var receivedContext: ModuleContext? = null
        private set

    /** Records startup and then throws the configured startup error, if present. */
    override fun onStart(context: ModuleContext) {
        receivedContext = context
        log.add("start:${id.value}")
        startError?.let { throw it }
    }

    /** Records shutdown and then throws the configured shutdown error, if present. */
    override fun onStop() {
        log.add("stop:${id.value}")
        stopError?.let { throw it }
    }
}
