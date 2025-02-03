package com.fire.wire.model.user.response

data class VerifyOtpResponse(
    var message:String?="",
    var code:String?="",
    var data:otpResetToken?=null
)

data class otpResetToken(
    var resetToken:String=""
)
/*
"message": "Verification code has been sent to your email",
"code": "success",
"data": {
    "resetToken": "U2FsdGVkX18yzs5K8JhCIBhydmh3zdAY66cSFzi/u/I="
}*/
