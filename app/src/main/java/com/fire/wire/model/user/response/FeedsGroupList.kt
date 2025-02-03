package com.fire.wire.model.user.response

data class FeedsGroupList(
    var feedStateName:String="",
    var feedUrlList:List<FeedsItem>?= null
)


data class FeedsItem(
    var feedName:String="",
    var feedUrl:String="",
)