package com.pioneer.nycfirewire.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {


    fun getFormattedDateOfFireWire(date:String): String {
        var formatDate=""
        try {

            var spf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
            val newDate = spf.parse(date)
            spf = SimpleDateFormat("dd LLL | hh:mm aa", Locale.ENGLISH)
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
        // Format the date to "dd LLL | hh:mm aa"
        val outputFormat = SimpleDateFormat("dd LLL | hh:mm aa", Locale.ENGLISH)
        outputFormat.timeZone = TimeZone.getDefault() // Local timezone (IST, for example)

        return outputFormat.format(date)
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

        val outputPattern = "dd MMM | hh:mm a" // "08 Oct | 04:20 PM"
        val outputFormatter = SimpleDateFormat(outputPattern, Locale.ENGLISH)

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