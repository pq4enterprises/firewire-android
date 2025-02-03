package com.fire.wire.model.user.response

data class LoginData(
    val _id: String?="",
    val active: Boolean=false,
    val email: String?="",
    val firstName: String?="",
    val lastName: String?="",
    val mobile: String?="",
    val refreshToken: String?="",
    val role: String?="",
    val token: String?="",
    val verified: Boolean=false
)
