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
   var comment:String?=""
)

data class UserDetail(
   var _id:String?="" ,
   var firstName:String?="" ,
   var lastName:String?="" ,
   var img:String?="" ,
)





