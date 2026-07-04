package com.pioneer.nycfirewire.model.user.response

import com.pioneer.nycfirewire.model.user.response.LoginData

data class LoginResponse(
    val code: String,
    val data: LoginData,
    val error:String?=null,
    val message: String
)