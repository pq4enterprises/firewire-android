package com.pioneer.nycfirewire.model.user.response


data class FeedResponse(
  var data:List<Feed>?=null
)

data class Feed(
    var _id:String?="",
    var name:String?="",
    var url:String?="",
    var createdAt:String?="",
    var locality: LocalityFeed?=null,
    var isChecked: Boolean=false
)

data class LocalityFeed(
    var _id:String?="",
    var name:String?="",
    var state:String?="",
    var latitude:String?="",
    var longitude:String?=""
)


