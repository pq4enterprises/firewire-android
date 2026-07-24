package com.fire.wire.model.user.request

data class ProfileUpdateRequest(
    var firstName:String?="",
    var lastName:String?="",
    var email:String?="",
    var mobile:String?="",
    var password:String?="",
    var title:String?="",
    var img:String?=""

)

