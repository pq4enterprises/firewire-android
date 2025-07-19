package com.pioneer.nycfirewire.model.user.response

import com.pioneer.nycfirewire.model.user.response.RefreshTokenData

data class RefreshTokenResponse(
    val code: String,
    val data: TokenInnerData,
    val message: String
)




