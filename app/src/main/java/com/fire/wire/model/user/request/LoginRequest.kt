package com.fire.wire.model.user.request

data class LoginRequest(
    val email: String,
    val password: String
)