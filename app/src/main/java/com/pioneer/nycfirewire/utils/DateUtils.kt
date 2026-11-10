package com.pioneer.nycfirewire.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * All user-facing times are 24-hour ("HH:mm"). Patterns are explicit and pinned
 * to Locale.US so the device's 12/24-hour setting and locale cannot flip them.
 * Parse patterns and the payment payload (formatToIso8601) are untouched.
 */
object DateUtils {

    private const val WIRE_DATE_TIME_PATTERN = "dd LLL | HH:mm"   // "07 Jul | 20:31"
    private const val NEWS_DATE_TIME_PATTERN = "dd MMM | HH:mm"   // "08 Oct | 16:20"


    fun getFormattedDateOfFireWire(date:String): String {
        var formatDate=""
        try {

            var spf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            val newDate = spf.parse(date)
            spf = SimpleDateFormat(WIRE_DATE_TIME_PATTERN, Locale.US)
            formatDate = spf.format(newDate!!)
        }catch (e:Exception){
            e.printStackTrace()
        }
       return formatDate
    }


    fun formatDateTime(isoString: String): String {
        // Parse the ISO 8601 date
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
        isoFormat.timeZone = TimeZone.getTimeZone("UTC") // The 'Z' means UTC

        val date: Date = isoFormat.parse(isoString)!!
        // Format the date to "dd LLL | HH:mm" (24-hour)
        val outputFormat = SimpleDateFormat(WIRE_DATE_TIME_PATTERN, Locale.US)
        outputFormat.timeZone = TimeZone.getDefault() // Local timezone (IST, for example)

        return outputFormat.format(date)
    }


    /**
     * "HH:mm" (24-hour, device time zone) for an ISO-8601 UTC instant such as
     * "2026-11-06T18:47:29.943Z" (milliseconds optional). Returns "" when the
     * value is missing or unparseable, so callers can render it unconditionally.
     */
    fun formatTime24(isoString: String?, zone: TimeZone = TimeZone.getDefault()): String {
        val raw = isoString?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        for (pattern in arrayOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                parser.isLenient = false
                val date = parser.parse(raw) ?: continue
                val out = SimpleDateFormat("HH:mm", Locale.US)
                out.timeZone = zone
                return out.format(date)
            } catch (e: Exception) {
                // try the next pattern
            }
        }
        return ""
    }


    fun convertDate(inputDate: String): String? {
        val inputPattern = "EEE, dd MMM yyyy HH:mm:ss Z" // "Mon, 24 Jun 2024 22:14:11 +0000"
        val inputFormatter = SimpleDateFormat(inputPattern, Locale.ENGLISH)

        val date: Date? = try {
            inputFormatter.parse(inputDate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val outputFormatter = SimpleDateFormat(NEWS_DATE_TIME_PATTERN, Locale.US)

        return date?.let { outputFormatter.format(it) }
    }


    fun formatToIso8601(purchaseTime: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(purchaseTime))
    }

    fun getExpiryTime(purchaseTime: Long, validityDays: Int = 30): Long{
        val validityInMillis = validityDays * 24 * 60 * 60 * 1000L // 30 days in milliseconds
        val expiryTime = purchaseTime + validityInMillis
        return expiryTime
    }

    fun getExpiryDate(purchaseTime: Long, validityDays: Int = 30): String {
        val validityInMillis = validityDays * 24 * 60 * 60 * 1000L // 30 days in milliseconds
        val expiryTime = purchaseTime + validityInMillis

        return formatToIso8601(expiryTime)
    }

}