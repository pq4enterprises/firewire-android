package com.fire.wire.model.user.response

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class UserDetails(
    var _id:String?="",
    var firstName:String?="",
    var lastName:String?="",
    var mobile:String?="",
    var email:String?="",
    var role:String?="",
    var title:String?="",
    var img:String?=""
):Parcelable


@Parcelize
data class UserResponse(
    var message:String?="",
    var code:String?="",
    var data: UserDetails?=null
):Parcelable
