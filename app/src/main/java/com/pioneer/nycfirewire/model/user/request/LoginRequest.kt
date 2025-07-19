package com.pioneer.nycfirewire.model.user.request

data class LoginRequest(
    val email: String,
    val password: String
)