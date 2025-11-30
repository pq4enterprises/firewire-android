package com.pioneer.nycfirewire.model.incident.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Incident(
    val _id:String?="",
    val latitude:String?="",
    val longitude:String?="",
    val address:String?="",
    val field1Value: String? = "",
    val field2Value: String? = "",
    val field3Value: String? = "",
    val commentCount:String?="0",
    var likeCount:String?="",
    val createdAt:String?="",
    val featuredImageUrl:String?="",
    var isLiked:Boolean=false,
   // val locality:List<IncidentLocality>?=null,
    val subLocalityDetails:List<IncidentSubLocality>?=null,
):Parcelable

@Parcelize
data class IncidentResponse(
    val code: String?="",
    val data: IncidentData?=null,
    val message: String?="",
    val error: String?="",
):Parcelable


@Parcelize
data class IncidentData(
    val data: List<Incident>?= emptyList(),
    val pageInfo: PageInfo?=null
):Parcelable

@Parcelize
data class PageInfo(
    val offset: String?="",
    val limit: String?="",
    val totalCount: String?="0"
): Parcelable

@Parcelize
data class IncidentLocality(
    val _id:String?="",
    val name:String?="",
    val latitude:String?="" ,
    val longitude:String?=""
):Parcelable

@Parcelize
data class IncidentSubLocality(
    var name:String?="" ,
    val latitude:String?="" ,
    val longitude:String?=""
):Parcelable

//{
    /*"_id": "67484ca4535c211c725cd1c4",
    "locality": [
    {
        "_id": "6731c3be477cc8909d1d0117",
        "name": "USA",
        "state": "USA",
        "latitude": "10",
        "longitude": "10",
        "subscriberOnlyComment": false,
        "deleted": false,
        "createdBy": "672cae5cc9b375ea7e36f233",
        "createdAt": "2024-11-11T08:43:42.065Z",
        "updatedAt": "2024-11-28T14:06:37.583Z",
        "__v": 0,
        "modifiedBy": "672cae5cc9b375ea7e36f233",
        "newsRssFeedURL": "",
        "twitterAccessSecret": "",
        "twitterAccessToken": "",
        "twitterAppKey": "",
        "twitterAppSecret": "",
        "twitterBearerToken": "",
        "twitterPage": "",
        "field1Name": "Title",
        "field2Name": "Description",
        "field3Name": "City/Town",
        "field4Name": "",
        "field5Name": "",
        "facebook": [
        {
            "pageId": "487266014474314",
            "pageName": "Fire wire1",
            "accessToken": "EAAGmzXS1zt8BO05tTlUEoarqZBVVP8gyHI1zutBUB7qJX0d0qhxGsNh6gZBKZBqgOjan1rsYOY0WDG6NPtt5ivT0GBdcJABuh82l53q6X3eCuBZB0StFsLqAL2Yyix58A6IZCISR3rhWaTzNLmKaNbS9zYBo2Mi4enC6J8ZATo23fAWKZBpwpBojFabx0QT2uvq1o3TipSdhsRRlf4AZB2ZAN7Uyx",
            "_id": "674878eda52f4018b811c035"
        },
        {
            "pageId": "490269330833066",
            "pageName": "Firewire",
            "accessToken": "EAAGmzXS1zt8BO7JH2ZBvqRimdE9BQOre0wRBSx4xG3NQr7M4is8bcl0Oy2BF4UoPYjipTZBcN2KUv2eSXyQuZCH52EM84iZCcVR688lcbtPv0k7D2hffEPZBFkmpFY33twVsT3D6o6ZCchvYDyL6QKbYrc0M39T2pa5QOQ9XWjsJVQrWfZA79NDCBFNTI2ZAcpLyi5ZCn8NFU62b3tBOvVXTWZAUnD",
            "_id": "674878eda52f4018b811c036"
        }
        ]
    }
    ],
    "subLocality": [
    {
        "_id": "67343906022e00b03138052a",
        "name": "losangels",
        "locality": "6731c3be477cc8909d1d0117",
        "latitude": "84.38393",
        "longitude": "85.292",
        "deleted": false,
        "createdAt": "2024-11-13T05:28:38.357Z",
        "updatedAt": "2024-11-13T05:28:38.357Z",
        "__v": 0
    }
    ],
    "latitude": "33.942153",
    "longitude": "-118.4036052",
    "address": "1 World Way, Los Angeles, CA 90045, USA",
    "respondingUnits": [
    "E-54"
    ],
    "featured": true,
    "sendPushNotification": true,
    "postFacebook": [
    {
        "pageId": "487266014474314",
        "pageName": "Fire wire1",
        "accessToken": "EAAGmzXS1zt8BO2RgIszevTGG1ZA5HGpnDeVBcQcvpe8F5o8Ds9N3C5oHGAZBvOYnmU1A6xdW4oNXDld2GurdCn56NgZAxKxnXJhNANLwLBVhaesYum1cSmArxIn4krPo9Y9zvuDeyOYcDZBP7fxUv5i2IbIZA3d3QBd2HbiZCeFKp6ZAbtZBbEwH5YmT0DUHoTCZABnY8327FS5WKRPXIF2ZAZCQZCfzZBQZDZD",
        "_id": "6748249a6da824920c10b2a1",
        "checked": true
    }
    ],
    "postTwitter": null,
    "field1Value": "Working Fire",
    "field2Value": "test test ",
    "field3Value": "losangels",
    "field4Value": "",
    "field5Value": "",
    "deleted": false,
    "createdBy": "672cae5cc9b375ea7e36f233",
    "createdAt": "2024-11-28T10:57:40.393Z",
    "updatedAt": "2024-11-28T11:05:51.346Z",
    "__v": 0,
    "commentCount": 2,
    "likeCount": 0
}*/

