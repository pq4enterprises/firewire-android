package com.pioneer.nycfirewire.model.user.request

data class GridItems(
    var title:String?="",
    var image:Int=0,
    /** color res for the icon tint (design-system accent) */
    var iconTint:Int=0,
    /** color res for the soft circle behind the icon */
    var iconBg:Int=0,
    /** external URL opened in the browser on tap (null for in-app tiles) */
    var url:String?=null,
    /** remote icon loaded with Glide when non-empty; falls back to [image] */
    var imageUrl:String?=null,
    /** in-app Personalization tile flag */
    var isPersonalization:Boolean=false
)
