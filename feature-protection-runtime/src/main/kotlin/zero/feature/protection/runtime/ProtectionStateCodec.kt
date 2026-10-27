/*
 * MODULE: feature-protection-runtime
 * PURPOSE: Converts protection states to and from the single string stored in the module store.
 * TAKES: ProtectionState values, or stored strings.
 * GIVES: Encoded strings, or decoded states; null for anything malformed.
 * OWNS: The string format: "ARMED", "COUNTDOWN|start|duration", "GRACE|start".
 * DOES NOT OWN: Storage or transitions.
 * DEPENDENCIES: feature-protection.
 * CONTRACT: decode(encode(s)) == s for every valid state. Countdown durations must be positive.
 * FAILURE BEHAVIOR: decode returns null for any malformed input and never throws.
 */
package zero.feature.protection.runtime

import zero.feature.protection.ProtectionState

/** Encodes and decodes protection states for storage. */
internal object ProtectionStateCodec {
    fun encode(state: ProtectionState): String = when (state) {
        ProtectionState.Armed -> "ARMED"
        is ProtectionState.Countdown -> "COUNTDOWN|${state.startedAtMillis}|${state.durationMillis}"
        is ProtectionState.Grace -> "GRACE|${state.startedAtMillis}"
    }

    fun decode(raw: String): ProtectionState? {
        val parts = raw.split("|")
        return when {
            raw == "ARMED" -> ProtectionState.Armed
            parts.size == 3 && parts[0] == "COUNTDOWN" -> {
                val start = parts[1].toLongOrNull() ?: return null
                val duration = parts[2].toLongOrNull() ?: return null
                if (duration <= 0L) null else ProtectionState.Countdown(start, duration)
            }
            parts.size == 2 && parts[0] == "GRACE" ->
                parts[1].toLongOrNull()?.let { ProtectionState.Grace(it) }
            else -> null
        }
    }
}
