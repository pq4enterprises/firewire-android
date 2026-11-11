package com.pioneer.nycfirewire.model.incident.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class IncidentDataByID(
    val _id: String? = "",
    val address: String? = "",
    val latitude: String? = "",
    val longitude: String? = "",
    val title: String? = "",
    val description: String? = "",
    val updatedAt: String? = "",
    val commentCount: String? = "",
    val likeCount: String? = "",
    val respondingUnits: ArrayList<String>? = null,
    // server-resolved chip colors + legend (absent on older servers)
    val respondingUnitColors: ArrayList<UnitColor>? = null,
    val unitLegend: ArrayList<UnitLegend>? = null,
    val unitColorsConfigured: Boolean? = null,
    val points: List<Points>?=null,
    val field1Value:String?="",
    val field2Value:String?="",
    val field3Value:String?="",
    // signal / alarm level, returned by the by-id endpoint
    val field4Value:String?="",
    val createdAt:String?="",
    val subLocalityName:String?="",
    val featuredImageUrl:String?="",
    var isLiked:Boolean=false,
    // title history, oldest first (absent on older servers / single-title incidents)
    val progression: List<ProgressionEntry>? = null,
) : Parcelable

/** One title change of an incident: `{ "version": 1, "title": "10-75", "at": "<ISO-8601 UTC>" }`. */
@Parcelize
data class ProgressionEntry(
    val version: Int? = null,
    val title: String? = "",
    val at: String? = ""
) : Parcelable


data class IncidentByIdResponse(
    val message: String? = "",
    val code: String? = "",
    val data: List<IncidentDataByID>? = null
)

@Parcelize
data class Points(
    val latitude: String?="",
    val longitude: String?="",
    val name: String?="",
    // used as the marker snippet on the incident map (added with the redesign's
    // firehouse pins); defaulted so existing construction sites are unaffected
    val address: String?="",
    // miles from the incident, computed by the server (points arrive nearest
    // first); absent on older servers
    val distanceMiles: Double?=null
): Parcelable


