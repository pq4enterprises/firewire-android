package com.fire.wire.model.locality

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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
data class Unit(
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
    var locality:Locality?= null,
    var latitude:String?="",
    var longitude:String?="",
    var createdAt:String?="",
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
    var unit:List<Unit>?=null,
    var isChecked:Boolean=false
):Parcelable



