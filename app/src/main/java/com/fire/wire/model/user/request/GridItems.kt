package com.fire.wire.model.user.request

data class GridItems(
    var title:String?="",
    var image:Int=0,
    /** color res for the icon tint (design-system accent) */
    var iconTint:Int=0,
    /** color res for the soft circle behind the icon */
    var iconBg:Int=0
)
