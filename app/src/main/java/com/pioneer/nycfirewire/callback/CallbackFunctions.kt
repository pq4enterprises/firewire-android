package com.pioneer.nycfirewire.callback

import com.pioneer.nycfirewire.model.incident.response.Incident

interface CallbackFunctions {

    fun filterApply(incidentList: ArrayList<Incident>, toString: String)

    fun updateBottomSheet()
}

