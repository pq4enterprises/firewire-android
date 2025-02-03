package com.fire.wire.model.user.request

data class UpdatePasswordRequest(
   var oldPassword:String?="",
   var newPassword:String?="",
   var confirmPassword:String?=""
)


