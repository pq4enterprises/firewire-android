package com.pioneer.nycfirewire.model.user.response

data class FeedsGroupList(
    var feedStateName:String="",
    var feedUrlList:List<Feed>?= null
)


data class FeedsItem(
    var feedName:String="",
    var feedUrl:String=""

)