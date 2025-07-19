package com.pioneer.nycfirewire.model.user.response

data class SaltyWireResponse(
    var message:String?="",
    var code:String?="",
    var data: SaltyContent?=null
)

data class SaltyData(
    var _id: String?="",
    var title: String?="",
    var link: String="",
    var url: String?="",
    var type: String?="",
    var createdAt: String?=""
)



data class SaltyContent(
    var data:List<SaltyData>
)