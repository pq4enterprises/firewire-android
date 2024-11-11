package com.fire.wire.utils

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {

    private val APP_PREF = "firewire"
    private val ISLOGIN = "login"

    private val preferences: SharedPreferences = context.getSharedPreferences(APP_PREF,Context.MODE_PRIVATE)

    var isLogin:Boolean get() = preferences.getBoolean(ISLOGIN,false)
        set(value) = preferences.edit().putBoolean(ISLOGIN,value).apply()
}