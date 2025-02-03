package com.fire.wire.model.user.response

data class RegisterResponse(
    val message: String,
    val code: String,
    val error:String,
    val data: LoginData
)

data class RegisterData(var data:String)