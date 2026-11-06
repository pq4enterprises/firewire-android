package com.pioneer.nycfirewire.utils

import com.google.android.gms.maps.GoogleMap
import com.pioneer.nycfirewire.prefs

/**
 * Single home for the "default map type" preference (My Account > Map Type).
 *
 * Server contract (`GET/PUT api/app/user/profile`): `mapType` is one of
 * "standard" | "satellite" | "hybrid" or null. "standard" is shown to the
 * user as STREET and maps to [GoogleMap.MAP_TYPE_NORMAL].
 */
object MapTypePrefs {

    const val STANDARD = "standard"
    const val SATELLITE = "satellite"
    const val HYBRID = "hybrid"

    /** Order used by the picker dialog: STREET / SATELLITE / HYBRID. */
    val VALUES = listOf(STANDARD, SATELLITE, HYBRID)
    val LABELS = listOf("Street", "Satellite", "Hybrid")

    fun isValid(value: String?): Boolean = value != null && value in VALUES

    /** Display label for a server value, or null when nothing has been chosen. */
    fun label(value: String?): String? {
        val idx = VALUES.indexOf(value)
        return if (idx >= 0) LABELS[idx] else null
    }

    /**
     * Mirrors the value from a freshly loaded profile into prefs. Only the three
     * known values are stored; null/unknown clears the local copy so a reset on
     * the server (or another device) wins over a stale local choice.
     */
    fun syncFromServer(value: String?) {
        prefs.mapType = if (isValid(value)) value else null
    }

    /**
     * The GoogleMap constant for the stored preference, or [fallback] when the
     * user has never picked one. Each screen passes its historical default as
     * the fallback so unset users see no change.
     */
    fun googleMapType(fallback: Int): Int = when (prefs.mapType) {
        STANDARD -> GoogleMap.MAP_TYPE_NORMAL
        SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
        HYBRID -> GoogleMap.MAP_TYPE_HYBRID
        else -> fallback
    }

    /** Applies the stored preference (or [fallback]) to [map]. */
    fun apply(map: GoogleMap, fallback: Int) {
        val wanted = googleMapType(fallback)
        if (map.mapType != wanted) map.mapType = wanted
    }
}
