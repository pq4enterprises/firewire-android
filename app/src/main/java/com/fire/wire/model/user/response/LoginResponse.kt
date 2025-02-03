package com.fire.wire.model.user.response

data class LoginResponse(
    val code: String,
    val data: LoginData,
    val message: String
)