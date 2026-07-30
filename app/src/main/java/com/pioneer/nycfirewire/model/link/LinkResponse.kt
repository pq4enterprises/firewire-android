package com.pioneer.nycfirewire.model.link

/**
 * GET api/app/link?show=true
 * Standard app success wrapper: { message, code, data: { data: [links], pageInfo } }
 * (same nested shape as LocalityResponse)
 */
data class LinkResponse(
    var data: LinkData? = null,
    val code: String? = null,
    val message: String? = null
)

data class LinkData(
    var data: List<LinkItem>? = null
)

data class LinkItem(
    var _id: String? = "",
    var name: String? = "",
    var url: String? = "",
    var imageUrl: String? = "",
    var sort: Int? = 0,
    var createdAt: String? = ""
)
