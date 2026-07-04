package com.pioneer.nycfirewire.model.user.request

data class VerifyEmailOtpRequest(
    var email:String="",
    var otp:String=""
)