package com.pioneer.nycfirewire.model.user.response

data class CommonResponse(
    var message:String?="",
    var code:String?="",
    var data: UrlData?=null
)

data class UrlData(
    var url:List<String>
)

