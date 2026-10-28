/*
 * MODULE: foundation-events
 * PURPOSE: Converts events to and from the single-string form stored in the backend.
 * TAKES: ZeroEvent values or stored strings.
 * GIVES: Encoded strings or decoded events; null for malformed input.
 * OWNS: The format version "v1".
 * DOES NOT OWN: Storage.
 * DEPENDENCIES: ZeroEvent, EventType, foundation-contract ModuleId.
 * CONTRACT: decode(encode(event)) equals the original event, with delimiters percent-escaped.
 * FAILURE BEHAVIOR: decode returns null for wrong versions, field counts, or unparseable fields and never throws.
 */
package zero.foundation.events

import zero.foundation.contract.ModuleId

/** Encodes events as "v1|seq|mono|wall|source|type|k=v,k=v" with escaping. */
internal object EventCodec {
    private const val VERSION = "v1"
    private const val FIELD_COUNT = 7

    /** Encodes [event] into the versioned persistent event representation. */
    fun encode(event: ZeroEvent): String {
        val payload = event.payload.entries.joinToString(",") { (key, value) ->
            "${escape(key)}=${escape(value)}"
        }
        return listOf(
            VERSION,
            event.sequence.toString(),
            event.monotonicMillis.toString(),
            event.wallMillis.toString(),
            event.source.value,
            event.type.value,
            payload,
        ).joinToString("|")
    }

    /** Decodes [raw], returning null if it is malformed or has an unsupported version. */
    fun decode(raw: String): ZeroEvent? {
        val parts = raw.split("|")
        if (parts.size != FIELD_COUNT || parts[0] != VERSION) return null
        return try {
            ZeroEvent(
                sequence = parts[1].toLong(),
                monotonicMillis = parts[2].toLong(),
                wallMillis = parts[3].toLong(),
                source = ModuleId(parts[4]),
                type = EventType(parts[5]),
                payload = decodePayload(parts[6]) ?: return null,
            )
        } catch (failure: IllegalArgumentException) {
            null
        }
    }

    private fun decodePayload(raw: String): Map<String, String>? {
        if (raw.isEmpty()) return emptyMap()
        val result = linkedMapOf<String, String>()
        for (pair in raw.split(",")) {
            val keyValue = pair.split("=")
            if (keyValue.size != 2) return null
            result[unescape(keyValue[0])] = unescape(keyValue[1])
        }
        return result
    }

    private fun escape(text: String): String =
        text.replace("%", "%25").replace("|", "%7C").replace(",", "%2C").replace("=", "%3D")

    private fun unescape(text: String): String =
        text.replace("%3D", "=").replace("%2C", ",").replace("%7C", "|").replace("%25", "%")
}
