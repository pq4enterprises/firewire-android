package com.fire.wire.model.incident.request

data class AddCommentRequest(
    var userId:String?="",
    var incidentId:String?="",
    var type:String?="",
    var comment:String?="",
    var url:String?="",
)


