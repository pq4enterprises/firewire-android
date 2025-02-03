package com.fire.wire.callback

import com.fire.wire.model.incident.response.Incident

interface CallbackFunctions {

    fun filterApply(incidentList:ArrayList<Incident>)
}