package com.pioneer.nycfirewire.model.user.response

import com.pioneer.nycfirewire.model.user.response.LoginData

data class RegisterResponse(
    val message: String,
    val code: String,
    val error:String?=null,
    val data: RegisterData
)


data class RegisterData(
    val email: String,
    val emailVerified: Boolean
)