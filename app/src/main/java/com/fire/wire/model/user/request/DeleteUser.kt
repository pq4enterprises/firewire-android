package com.fire.wire.model.user.request

data class DeleteUser(
    var deleted:Boolean=false,
    var reason:String=""
)
