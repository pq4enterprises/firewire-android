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
    var userId: UserDetail?=null,
    var img:List<String>?=null,
    var comment:String?="",
    var featuredImage: Boolean=false,
    var createdAt:String="",
)

data class UserDetail(
   var _id:String?="" ,
   var firstName:String?="" ,
   var lastName:String?="" ,
   var img:String?="" ,
)






