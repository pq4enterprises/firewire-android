package com.fire.wire.model.user.response

data class RefreshTokenResponse(
    val code: String,
    val data: RefreshTokenData,
    val message: String
)