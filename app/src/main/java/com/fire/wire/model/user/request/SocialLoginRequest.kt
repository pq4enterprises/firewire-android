package com.fire.wire.model.user.request

data class SocialLoginRequest(
    val token:String="",
    val socialType:String="",
    val role:String="basic_user",
)

