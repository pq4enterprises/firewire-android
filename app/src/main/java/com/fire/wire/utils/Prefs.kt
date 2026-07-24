package com.fire.wire.utils

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val APP_PREF = "firewire"
    private val ISLOGIN = "login"
    private val TOKEN = "token"
    private val REFRESH_TOKEN = "refresh_token"
    private val USER_ID = "user_id"
    private val FILTER_LIST = "filter_list"
    private val USER_IMG = "user_img"
    private val ALERT_SOUND = "alert_sound"
    private val USER_ROLE = "user_role"

    private val preferences: SharedPreferences = context.getSharedPreferences(APP_PREF,Context.MODE_PRIVATE)

    var isLogin:Boolean get() = preferences.getBoolean(ISLOGIN,false)
        set(value) = preferences.edit().putBoolean(ISLOGIN,value).apply()

    var token:String? get() = preferences.getString(TOKEN,null)
        set(value) = preferences.edit().putString(TOKEN,value).apply()
    var deleteToken= preferences.edit().remove(TOKEN)

    var refreshToken:String? get() = preferences.getString(REFRESH_TOKEN,"")
        set(value) = preferences.edit().putString(REFRESH_TOKEN,value).apply()
    var deleteRefreshToken= preferences.edit().remove(REFRESH_TOKEN)

    var userId:String? get() = preferences.getString(USER_ID,null)
        set(value) = preferences.edit().putString(USER_ID,value).apply()

    var filterData:String? get() = preferences.getString(FILTER_LIST,null)
        set(value) = preferences.edit().putString(FILTER_LIST,value).apply()


    var userImg:String? get() = preferences.getString(USER_IMG,"")
        set(value) = preferences.edit().putString(USER_IMG,value).apply()

    // GLOBAL ALERT SOUND key (AlertSounds catalog); null = system default
    var alertSound:String? get() = preferences.getString(ALERT_SOUND,null)
        set(value) = preferences.edit().putString(ALERT_SOUND,value).apply()

    // last known server role, cached for the premium gate on the sound screen
    var userRole:String? get() = preferences.getString(USER_ROLE,"")
        set(value) = preferences.edit().putString(USER_ROLE,value).apply()
}