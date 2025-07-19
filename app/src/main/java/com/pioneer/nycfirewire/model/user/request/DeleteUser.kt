package com.pioneer.nycfirewire.model.user.request

data class DeleteUser(
    var deleted:Boolean=false,
    var reason:String=""
)
