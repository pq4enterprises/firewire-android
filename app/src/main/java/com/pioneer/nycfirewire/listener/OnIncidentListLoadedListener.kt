package com.pioneer.nycfirewire.listener

import com.pioneer.nycfirewire.model.incident.response.Incident

interface OnIncidentListLoadedListener {
    fun onListLoaded(list: List<Incident>)
}