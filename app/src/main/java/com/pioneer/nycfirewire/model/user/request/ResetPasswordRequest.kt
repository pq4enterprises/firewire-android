package com.pioneer.nycfirewire.model.user.request

data class ResetPasswordRequest(
    var resetToken:String="",
    var password:String="",
    var confirmPassword:String="",
)
