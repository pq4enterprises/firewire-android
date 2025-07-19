package com.pioneer.nycfirewire.model.user.response

data class RefreshTokenData(
    val data: TokenInnerData
)

data class TokenInnerData(
    val _id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val refreshToken: String,
    val role: String,
    val token: String
)
