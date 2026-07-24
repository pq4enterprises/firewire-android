package com.fire.wire.model.incident.response

data class CommentsResponse(
    var message:String?="",
    var code:String?="",
    var data:DataComment?=null
)

data class DataComment(
    var data:List<Comment>?=null
)

data class Comment(
   var _id:String?="",
   var userId:UserDetail?=null,
   var img:List<String>?=null,
   var comment:String?="",
   // returned by GET api/app/incident/comment/{incidentId} (same payload iOS
   // parses); surfaced by the 2026 design-system comment row
   var createdAt:String?="",
   var replies:List<Comment>?=null
)

data class UserDetail(
   var _id:String?="" ,
   var firstName:String?="" ,
   var lastName:String?="" ,
   var img:String?="" ,
)





