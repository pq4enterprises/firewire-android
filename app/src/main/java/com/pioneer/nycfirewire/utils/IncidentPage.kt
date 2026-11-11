package com.pioneer.nycfirewire.utils

import com.pioneer.nycfirewire.model.incident.response.ProgressionEntry
import java.util.Locale

/**
 * Pure formatting / ordering rules for the incident page (Variation D), kept
 * free of Android types so they are covered by plain JVM unit tests.
 */
object IncidentPage {

    /** The progression card is shown only once an incident has had 2+ titles. */
    const val MIN_PROGRESSION_ROWS = 2

    /**
     * Rows for the INCIDENT PROGRESSION card, newest first. The server sends
     * the title history oldest first; entries without a title are dropped.
     * Returns an empty list when there is nothing to show (fewer than
     * [MIN_PROGRESSION_ROWS] usable entries), so the caller hides the card.
     */
    fun progressionRows(progression: List<ProgressionEntry?>?): List<ProgressionEntry> {
        val usable = progression.orEmpty()
            .filterNotNull()
            .filter { !it.title.isNullOrBlank() }
        if (usable.size < MIN_PROGRESSION_ROWS) return emptyList()
        return usable.asReversed().toList()
    }

    /** "0.4 mi"; "<0.1 mi" for anything that would round to 0.0; null when unknown. */
    fun distanceLabel(distanceMiles: Double?): String? {
        if (distanceMiles == null || distanceMiles.isNaN() || distanceMiles < 0) return null
        if (distanceMiles < 0.05) return "<0.1 mi"
        return String.format(Locale.US, "%.1f mi", distanceMiles)
    }

    /** Firehouse marker snippet: "0.4 mi · 74 Middagh St" (either part may be absent). */
    fun firehouseSnippet(distanceMiles: Double?, address: String?): String {
        val parts = listOfNotNull(
            distanceLabel(distanceMiles),
            address?.trim()?.takeIf { it.isNotEmpty() }
        )
        return parts.joinToString(" · ")
    }
}
