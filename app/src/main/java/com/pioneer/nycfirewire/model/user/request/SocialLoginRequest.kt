package com.pioneer.nycfirewire.model.user.request

data class SocialLoginRequest(
    //val token:String="",
    val socialType:String="",
    val role:String="basic_user",
    val firstName:String="",
    val lastName:String="",
    val email:String="",
)



/*{
    "socialType" : "google",
    "role" : "basic_user",
    "firstName": "saran",
    "lastName" : "p",
    "email": "sarans@yopmail.com"
}*/
