package com.pioneer.nycfirewire.model.incident.request

data class AddCommentRequest(
    var userId:String?="",
    var incidentId:String?="",
    var type:String?="",
    var comment:String?="",
    var img: List<String>?=null,
)



