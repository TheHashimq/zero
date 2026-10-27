/*
 * MODULE: app (platform adapter for foundation-persistence)
 * PURPOSE: Stores foundation values in a dedicated SharedPreferences file.
 * TAKES: Namespaced keys and string values from foundation-persistence.
 * GIVES: A KeyValueBackend backed by SharedPreferences.
 * OWNS: The zero_foundation_store preferences file, used only by this class.
 * DOES NOT OWN: Namespacing, validation, or typed values.
 * DEPENDENCIES: foundation-persistence, Android SDK.
 * CONTRACT: Writes use commit(), so a successful return means the value was persisted to disk.
 * FAILURE BEHAVIOR: A failed commit throws PersistenceException. Reads never throw.
 */
package com.TheHashimq.zero.platform.persistence

import android.content.Context
import zero.foundation.persistence.KeyValueBackend
import zero.foundation.persistence.PersistenceException

/** SharedPreferences implementation of KeyValueBackend. */
class SharedPreferencesKeyValueBackend(context: Context) : KeyValueBackend {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the stored string, or null if [key] is absent. */
    override fun read(key: String): String? = prefs.getString(key, null)

    /** Writes [value] under [key], throwing if SharedPreferences cannot commit it. */
    override fun write(key: String, value: String) {
        if (!prefs.edit().putString(key, value).commit()) {
            throw PersistenceException("commit failed for $key")
        }
    }

    /** Deletes [key], throwing if SharedPreferences cannot commit the removal. */
    override fun delete(key: String) {
        if (!prefs.edit().remove(key).commit()) {
            throw PersistenceException("commit failed for $key")
        }
    }

    private companion object {
        const val PREFS_NAME = "zero_foundation_store"
    }
}
