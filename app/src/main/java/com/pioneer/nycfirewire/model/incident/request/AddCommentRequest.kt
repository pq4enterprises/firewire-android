package com.pioneer.nycfirewire.model.incident.request

data class AddCommentRequest(
    var userId:String?="",
    var incidentId:String?="",
    var type:String?="",
    var comment:String?="",
    var img: List<String>?=null,
    var mentions: List<String>?=null,
    var parentId:String?=""
)


data class mainCommentRequest(
    var userId:String?="",
    var incidentId:String?="",
    var type:String?="",
    var comment:String?="",
    var img: List<String>?=null,
    var mentions: List<String>?=null
)



