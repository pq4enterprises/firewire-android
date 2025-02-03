package com.fire.wire.model.incident.response

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class FilterData(
    var locality:List<String>?=null,
    var subLocality:List<String>?=null
):Parcelable
