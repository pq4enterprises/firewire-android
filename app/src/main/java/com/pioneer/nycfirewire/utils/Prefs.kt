package com.pioneer.nycfirewire.utils

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
    private val ISAREASELECTED = "area_selected"
    private val IS_DARK_MODE_ON = "is_dark_mode"
    private val LOCALITY_IDS = "locality_ids"
    private val FIRST_NAME = "first_name"
    private val LAST_NAME = "last_name"
    private val EMAIL = "email"
    private val RECREATE = "recreate"
    private val APP_URL = "recreate"
    private val USER_ROLE = "user_role"
    private val COMMENT_COUNT = "comment_count"
    private val FEED_MAIN_POS = "feed_main_pos"
    private val FEED_SUB_POS = "feed_sub_pos"
    private val SOUND_NAME = "sound_name"
    private val ALERT_SOUND = "alert_sound"
    private val SHOW_APP_INTRO = "show_app_intro"
    private val SHOW_HOME_INTRO = "show_home_intro"
    private val SHOW_FEED_INTRO = "show_feed_intro"

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

    var isAreaSelected:Boolean get() = preferences.getBoolean(ISAREASELECTED,false)
        set(value) = preferences.edit().putBoolean(ISAREASELECTED,value).apply()

    var isDarkMode:Boolean get() = preferences.getBoolean(IS_DARK_MODE_ON,false)
        set(value) = preferences.edit().putBoolean(IS_DARK_MODE_ON,value).apply()

    var localityIds: ArrayList<String> get() = preferences.getStringSet(LOCALITY_IDS,null).let { ArrayList(preferences.getStringSet(LOCALITY_IDS,null)!!) }
        set(value) = preferences.edit().putStringSet(LOCALITY_IDS,value?.toSet()).apply()

    var userFirstName:String? get() = preferences.getString(FIRST_NAME,"")
        set(value) = preferences.edit().putString(FIRST_NAME,value).apply()

    var userLastName:String? get() = preferences.getString(LAST_NAME,"")
        set(value) = preferences.edit().putString(LAST_NAME,value).apply()

    var userEmail:String? get() = preferences.getString(EMAIL,"")
        set(value) = preferences.edit().putString(EMAIL,value).apply()

    var isRecreate:Boolean get() = preferences.getBoolean(RECREATE,false)
        set(value) = preferences.edit().putBoolean(RECREATE,value).apply()

    var userRole:String? get() = preferences.getString(USER_ROLE,"")
        set(value) = preferences.edit().putString(USER_ROLE,value).apply()

    var commentCount:String? get() = preferences.getString(COMMENT_COUNT,"")
        set(value) = preferences.edit().putString(COMMENT_COUNT,value).apply()

    var feedMainPosition: Int get() = preferences.getInt(FEED_MAIN_POS,-1)
        set(value) = preferences.edit().putInt(FEED_MAIN_POS,value).apply()

    var feedSubPosition: Int get() = preferences.getInt(FEED_SUB_POS,-1)
        set(value) = preferences.edit().putInt(FEED_SUB_POS,value).apply()

    var soundName:String? get() = preferences.getString(SOUND_NAME,"")
        set(value) = preferences.edit().putString(SOUND_NAME,value).apply()

    /**
     * Stable key of the chosen alert sound (see AlertSounds). Replaces the old
     * display-name-based soundName. Reading it migrates a legacy soundName once,
     * so an existing user keeps the sound they had rather than silently
     * resetting to DEFAULT.
     */
    var alertSound:String? get() {
            val existing = preferences.getString(ALERT_SOUND, null)
            if (!existing.isNullOrEmpty()) return existing
            val legacy = preferences.getString(SOUND_NAME, "")?.trim().orEmpty()
            if (legacy.isEmpty()) return null
            val migrated = when (legacy.uppercase()) {
                "ACTING ENGINE" -> "acting_engine"
                "BATTALION" -> "battalion"
                "DIVISION" -> "division"
                "ENGINE LADDER TICKET" -> "engine_ladder_ticket"
                "ENGINE TICKET" -> "engine_ticket"
                "ENGINE, LADDER, BATTALION" -> "engine_ladder_battalion"
                "LADDER TICKET" -> "ladder_ticket"
                "MDT RING" -> "mdt_ring"
                // legacy label was mangled to "Special FireUnit" by an old rename
                "SPECIAL UNIT", "SPECIAL FIREUNIT" -> "special_unit"
                "STANDBY FOR MESSAGE" -> "standby_for_message"
                "TONES ONLY" -> "tones_only"
                else -> null
            } ?: return null
            preferences.edit().putString(ALERT_SOUND, migrated).apply()
            return migrated
        }
        set(value) = preferences.edit().putString(ALERT_SOUND,value).apply()

    var showAppIntro:Boolean get() = preferences.getBoolean(SHOW_APP_INTRO,false)
        set(value) = preferences.edit().putBoolean(SHOW_APP_INTRO,value).apply()

    var showHomeIntro:Boolean get() = preferences.getBoolean(SHOW_HOME_INTRO,false)
        set(value) = preferences.edit().putBoolean(SHOW_HOME_INTRO,value).apply()

    var showFeedIntro:Boolean get() = preferences.getBoolean(SHOW_FEED_INTRO,false)
        set(value) = preferences.edit().putBoolean(SHOW_FEED_INTRO,value).apply()


}