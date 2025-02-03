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
    val likeCount: String? = ""
) : Parcelable


data class IncidentByIdResponse(
    val message: String? = "",
    val code: String? = "",
    val data: List<IncidentDataByID>? = null
)


