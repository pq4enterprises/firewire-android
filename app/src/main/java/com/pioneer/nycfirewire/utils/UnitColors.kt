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

    /**
     * Pair(background, text) for a chip; gray when the server has no entry for
     * the unit or its background is unparseable. A missing or malformed text
     * color is computed from the background (black or white by WCAG contrast)
     * instead of falling back to gray text. With no dark background the dark
     * chip is the light chip, so the light `fg` still applies there.
     */
    fun chipColors(context: Context, server: UnitColor?, dark: Boolean): Pair<Int, Int> {
        val grayBg = ContextCompat.getColor(context, R.color.fw_unit_other_bg)
        val grayFg = ContextCompat.getColor(context, R.color.fw_unit_other_fg)
        if (server == null) return Pair(grayBg, grayFg)
        val bgDark = if (dark) parseHex(server.bgDark) else null
        val bg = bgDark ?: parseHex(server.bg) ?: return Pair(grayBg, grayFg)
        val surface = ContextCompat.getColor(context, R.color.fw_surface)
        val fg = when {
            !dark -> parseHex(server.fg)
            bgDark != null -> parseHex(server.fgDark)
            else -> parseHex(server.fgDark) ?: parseHex(server.fg)
        } ?: contrastingText(bg, surface)
        return Pair(bg, fg)
    }

    // ---- WCAG contrast (pure functions, no Android framework calls — unit-testable on the JVM)

    private val BLACK = 0xFF000000.toInt()
    private val WHITE = 0xFFFFFFFF.toInt()

    /** Relative luminance (WCAG 2.x) of an ARGB int; alpha is ignored. */
    fun luminance(argb: Int): Double {
        fun lin(c: Int): Double { val v = c / 255.0; return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4) }
        return 0.2126 * lin((argb shr 16) and 0xFF) + 0.7152 * lin((argb shr 8) and 0xFF) + 0.0722 * lin(argb and 0xFF)
    }

    /** WCAG contrast ratio (1..21) between two opaque colors. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = luminance(a); val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Source-over composite of a translucent ARGB color onto an opaque backdrop. */
    fun compositeOver(argb: Int, backdrop: Int): Int {
        val a = ((argb ushr 24) and 0xFF) / 255.0
        if (a >= 1.0) return argb
        fun mix(shift: Int): Int {
            val c = (argb shr shift) and 0xFF; val b = (backdrop shr shift) and 0xFF
            return Math.round(c * a + b * (1 - a)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /**
     * Black or white, whichever has the higher WCAG contrast on [bg] — the same
     * rule as the server's autoTextColor, so fallback chips match server ones.
     * A translucent [bg] (the SOC tint) is composited over [backdrop] (the card
     * surface) first.
     */
    fun contrastingText(bg: Int, backdrop: Int = WHITE): Int {
        val opaque = compositeOver(bg, backdrop)
        return if (contrastRatio(opaque, BLACK) >= contrastRatio(opaque, WHITE)) BLACK else WHITE
    }
}
