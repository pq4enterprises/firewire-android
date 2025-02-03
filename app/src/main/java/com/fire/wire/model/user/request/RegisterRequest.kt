package com.fire.wire.model.user.request

data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val mobile: String,
    val password: String,
    val title: String
)