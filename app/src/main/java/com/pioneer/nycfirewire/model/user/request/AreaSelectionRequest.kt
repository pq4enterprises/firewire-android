package com.pioneer.nycfirewire.model.user.request

/**
 * Request rows for the Areas & Alerts screen. Both endpoints take the FULL
 * selection set as a JSON array; the server diffs against what is stored and
 * inserts/deletes rows accordingly (see firewire-api userProfile controller).
 */

// POST api/app/user/area — one row per sub-locality in the user's wire feed
data class UserAreaItem(
    val userId: String,
    val localityId: String,
    val subLocalityId: String
)

// POST api/app/user/notification — one row per alert target.
// type mirrors iOS: "locality", "subLocality", "unit", "incidentType".
data class UserNotificationItem(
    val userId: String,
    val notificationId: String,
    val type: String
)
