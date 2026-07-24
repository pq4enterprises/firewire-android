package com.fire.wire.model.incident.request

import com.google.gson.annotations.SerializedName

/**
 * Body for POST api/app/incident/activity — same payload iOS builds in
 * APIPayload.addComment: userId/incidentId/type/comment/img, plus optional
 * parentId + mentions when the comment is a reply. Gson omits nulls, so a
 * top-level comment serializes exactly as before.
 */
data class AddCommentRequest(
    var userId:String?="",
    var incidentId:String?="",
    var type:String?="",
    var comment:String?="",
    // wire name is "img" (backend IncidentComment schema + iOS payload);
    // the old "url" key was silently dropped by the API
    @SerializedName("img") var url:String?="",
    var parentId:String?=null,
    var mentions:List<String>?=null,
)
