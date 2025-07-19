package com.pioneer.nycfirewire.model.locality

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalityResponse(
    var data: LocalityData?=null,
    val code: String,
    val message: String
):Parcelable

@Parcelize
data class LocalityData(
    val data:List<Locality>?=null
):Parcelable


@Parcelize
data class FireUnit(
var _id:String? ="",
var unitName:String?="",
var isChecked:Boolean=false
):Parcelable


@Parcelize
data class Query(
    val search: String,
    val locality: List<String>,
    val subLocality: List<String>
):Parcelable

@Parcelize
data class SubLocality(
    var _id:String?="",
    var name:String?="",
    var locality: Locality?= null,
    var latitude:String?="",
    var longitude:String?="",
    var createdAt:String?="",
    var isChecked:Boolean=false

):Parcelable


@Parcelize
data class IncidentType(
    var _id:String?="",
    var optionName:String?="",
    var isChecked:Boolean=false
):Parcelable

@Parcelize
data class Locality(
    var _id:String?="",
    var name:String?="",
    var state:String?="",
    var latitude:String?="",
    var longitude:String?="",
    var subLocality:List<SubLocality>?=null,
    var unit:List<FireUnit>?=null,
    var incidentType:List<IncidentType>?=null,
    var isChecked:Boolean=false,
    var isSelectAll: Boolean=false
):Parcelable



