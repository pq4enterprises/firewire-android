package com.pioneer.nycfirewire.utils

import com.pioneer.nycfirewire.model.incident.response.ProgressionEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class IncidentPageTest {

    @Test
    fun progression_isHiddenBelowTwoEntries() {
        assertTrue(IncidentPage.progressionRows(null).isEmpty())
        assertTrue(IncidentPage.progressionRows(emptyList()).isEmpty())
        assertTrue(
            IncidentPage.progressionRows(
                listOf(ProgressionEntry(1, "10-75", "2026-11-06T18:47:29.943Z"))
            ).isEmpty()
        )
        // a blank title does not count towards the two
        assertTrue(
            IncidentPage.progressionRows(
                listOf(ProgressionEntry(1, "10-75", "x"), ProgressionEntry(2, "  ", "y"))
            ).isEmpty()
        )
    }

    @Test
    fun progression_isNewestFirst() {
        val rows = IncidentPage.progressionRows(
            listOf(
                ProgressionEntry(1, "10-75", "2026-11-06T18:32:00.000Z"),
                ProgressionEntry(2, "ALL HANDS", "2026-11-06T18:41:00.000Z"),
                ProgressionEntry(3, "2ND ALARM", "2026-11-06T19:02:00.000Z")
            )
        )
        assertEquals(listOf("2ND ALARM", "ALL HANDS", "10-75"), rows.map { it.title })
    }

    @Test
    fun firehouseSnippet_combinesDistanceAndAddress() {
        assertEquals("0.4 mi · 74 Middagh St", IncidentPage.firehouseSnippet(0.42, "74 Middagh St"))
        assertEquals("74 Middagh St", IncidentPage.firehouseSnippet(null, " 74 Middagh St "))
        assertEquals("1.1 mi", IncidentPage.firehouseSnippet(1.06, ""))
        assertEquals("<0.1 mi", IncidentPage.firehouseSnippet(0.01, null))
        assertEquals("", IncidentPage.firehouseSnippet(null, null))
        assertNull(IncidentPage.distanceLabel(-1.0))
    }

    @Test
    fun formatTime24_usesTheGivenZoneAnd24hClock() {
        val newYork = TimeZone.getTimeZone("America/New_York")
        // 18:47 UTC on 6 Nov 2026 is 13:47 EST
        assertEquals("13:47", DateUtils.formatTime24("2026-11-06T18:47:29.943Z", newYork))
        assertEquals("13:47", DateUtils.formatTime24("2026-11-06T18:47:29Z", newYork))
        assertEquals("", DateUtils.formatTime24("not a date", newYork))
        assertEquals("", DateUtils.formatTime24(null, newYork))
    }

    @Test
    fun streetViewProbe_mapsMetadataStatuses() {
        val ok = "{\n \"pano_id\" : \"abc\",\n \"status\" : \"OK\"\n}"
        assertEquals("OK", StreetViewProbe.parseStatus(ok))
        assertEquals(StreetViewProbe.Result.AVAILABLE, StreetViewProbe.resultForStatus("OK"))
        assertEquals(StreetViewProbe.Result.NONE, StreetViewProbe.resultForStatus("ZERO_RESULTS"))
        assertEquals(StreetViewProbe.Result.NONE, StreetViewProbe.resultForStatus("NOT_FOUND"))
        assertEquals(
            StreetViewProbe.Result.DENIED,
            StreetViewProbe.resultForStatus(
                StreetViewProbe.parseStatus("{\"error_message\":\"x\",\"status\":\"REQUEST_DENIED\"}")
            )
        )
        assertEquals(StreetViewProbe.Result.ERROR, StreetViewProbe.resultForStatus(null))
        assertEquals(StreetViewProbe.Result.ERROR, StreetViewProbe.resultForStatus("OVER_QUERY_LIMIT"))
        assertEquals(
            "https://maps.googleapis.com/maps/api/streetview/metadata?location=40.692500,-73.990900&radius=50&source=outdoor&key=K",
            StreetViewProbe.metadataUrl(40.6925, -73.9909, "K")
        )
    }
}
