package com.pioneer.nycfirewire.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.model.incident.response.UnitColor

/**
 * Server-resolved unit chip colors (per-locality scheme managed in the portal).
 * The server sends CSS hex (#RRGGBB or #RRGGBBAA); Android wants #AARRGGBB.
 * Anything unparseable falls back to the gray "other" chip so a bad value can
 * never crash the detail screen.
 */
object UnitColors {
    fun parseHex(hex: String?): Int? {
        val h = hex?.trim()?.removePrefix("#") ?: return null
        return try {
            when (h.length) {
                6 -> Color.parseColor("#$h")
                8 -> Color.parseColor("#" + h.substring(6, 8) + h.substring(0, 6)) // RRGGBBAA -> AARRGGBB
                else -> null
            }
        } catch (e: IllegalArgumentException) { null }
    }

    fun isDarkMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Pair(background, text) for a chip; gray when the server has no entry for the unit. */
    fun chipColors(context: Context, server: UnitColor?, dark: Boolean): Pair<Int, Int> {
        val grayBg = ContextCompat.getColor(context, R.color.fw_unit_other_bg)
        val grayFg = ContextCompat.getColor(context, R.color.fw_unit_other_fg)
        if (server == null) return Pair(grayBg, grayFg)
        val bg = parseHex(if (dark) server.bgDark ?: server.bg else server.bg) ?: grayBg
        val fg = parseHex(if (dark) server.fgDark ?: server.fg else server.fg) ?: grayFg
        return Pair(bg, fg)
    }
}
