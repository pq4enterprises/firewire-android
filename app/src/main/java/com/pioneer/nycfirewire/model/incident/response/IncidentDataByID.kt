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
    val points: List<Points>?=null,
    val field1Value:String?="",
    val field2Value:String?="",
    val field3Value:String?="",
    val createdAt:String?="",
    val subLocalityName:String?="",
    val featuredImageUrl:String?="",
    var isLiked:Boolean=false,
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
    val name: String?=""
): Parcelable


