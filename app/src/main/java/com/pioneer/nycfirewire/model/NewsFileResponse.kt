package com.pioneer.nycfirewire.model

import org.simpleframework.xml.*


@Root(name = "rss", strict = false)
class TrendingSearchResponseWrapper @JvmOverloads constructor(
    @field: Element(name = "channel")
    var channel: TrendingSearchResponse? = null
)

@Root(name = "channel", strict = false)
class TrendingSearchResponse @JvmOverloads constructor(
    @field: ElementList(inline = true)
    var itemList: List<TrendingSearchItem>? = null
)


@Root(name = "item", strict = false)
class TrendingSearchItem(
    @field: Element(name = "title")
    var title: String = "",
    @field: Element(name = "link")
    var link: String = "" ,
    @field: Element(name = "pubDate")
    var pubDate: String = "",
    @field: Element(name = "description", required = false)
    var description: String = "",

    @field: Element(name = "encoded", required = false)
    @Namespace(prefix = "content", reference = "http://purl.org/rss/1.0/modules/content/")
    var content: String?=null,
)


