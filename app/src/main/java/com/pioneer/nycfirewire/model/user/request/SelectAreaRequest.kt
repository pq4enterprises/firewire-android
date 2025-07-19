package com.pioneer.nycfirewire.model.user.request


data class PostAreaData(
   var localityId: String="" ,
   var subLocalityId: String="" ,
   var userId: String=""
)


data class NotificationAreaData(
    var userId: String="" ,
    var notificationId: String="" ,
    var type: String=""
)


