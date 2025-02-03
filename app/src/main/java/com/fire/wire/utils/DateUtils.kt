package com.fire.wire.utils

import java.text.SimpleDateFormat
import java.util.*

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




}