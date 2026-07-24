package com.fire.wire.model.incident.response

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


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
    val field4Value: String? = "",
    val respondingUnits: List<String?>? = null,
    val subLocalityName: String? = "",
    val points: List<IncidentPoint>? = null
) : Parcelable

/** Nearby firehouse / point of interest attached to an incident
 *  (iOS parity: `Point` in IncidentDetailResponseModel.swift). */
@Parcelize
data class IncidentPoint(
    val _id: String? = "",
    val latitude: String? = "",
    val longitude: String? = "",
    val address: String? = "",
    val name: String? = ""
) : Parcelable


data class IncidentByIdResponse(
    val message: String? = "",
    val code: String? = "",
    val data: List<IncidentDataByID>? = null
)


