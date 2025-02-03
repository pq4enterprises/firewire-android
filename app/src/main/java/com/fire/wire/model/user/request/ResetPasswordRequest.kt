package com.fire.wire.model.user.request

data class ResetPasswordRequest(
    var resetToken:String="",
    var password:String=""
)
