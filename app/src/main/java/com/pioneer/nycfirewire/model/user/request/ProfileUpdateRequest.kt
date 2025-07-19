package com.pioneer.nycfirewire.model.user.request

data class ProfileUpdateRequest(
    var firstName:String?="",
    var lastName:String?="",
    var email:String?="",
    var mobile:String?="",
    var password:String?="",
    var title:String?="",
    var img:String?="",
    var role: String?=""

)

data class LocalityUpdate(
    var locality:ArrayList<String>?=null,
    var subLocality:ArrayList<String>?=null,
    var unit:ArrayList<String>?=null,
)

