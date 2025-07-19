package com.pioneer.nycfirewire.model.user.response

import com.pioneer.nycfirewire.model.user.response.LoginData

data class RegisterResponse(
    val message: String,
    val code: String,
    val error:String,
    val data: LoginData
)

data class RegisterData(var data:String)