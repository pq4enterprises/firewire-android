package com.pioneer.nycfirewire.model.incident.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class CommentsResponse(
    var message:String?="",
    var code:String?="",
    var data: DataComment?=null
)

data class DataComment(
    var data:List<Comment>?=null,
    val pageInfo: PageInfo?=null
)

data class Comment(
    var _id:String?="",
    var incidentId:String?="",
    var userId: UserDetail?=null,
    var img:List<String>?=null,
    var comment:String?="",
    var featuredImage: Boolean=false,
    var createdAt:String="",
    var replies: List<Replies>?=null
)

data class UserDetail(
   var _id:String?="" ,
   var firstName:String?="" ,
   var lastName:String?="" ,
   var email:String?="" ,
   var img:String?="" ,
)

data class UserDetailReply(
    var _id:String?="" ,
    var firstName:String?="" ,
    var lastName:String?="" ,
    var email:String?="" ,
    var img:String?="" ,
)


data class Replies(
    var _id:String?="",
    var incidentId:String?="",
    var userId: UserDetailReply?=null,
    var parentId: String?="",
    var img:List<String>?=null,
    var comment:String?="",
    var featuredImage: Boolean=false,
    var createdAt:String="",
)






