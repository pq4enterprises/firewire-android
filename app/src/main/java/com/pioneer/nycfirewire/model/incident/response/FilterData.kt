package com.pioneer.nycfirewire.model.incident.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class FilterData(
    var locality:List<String>?=null,
    var subLocality:List<String>?=null
):Parcelable
