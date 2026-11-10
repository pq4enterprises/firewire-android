package com.pioneer.nycfirewire.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/**
 * Asks Google whether a Street View panorama exists near a coordinate WITHOUT
 * creating a panorama (creating one is the billable "Dynamic Street View"
 * event; metadata requests are free and carry no quota charge).
 *
 * Uses the Street View Static API metadata endpoint with the app's own Maps
 * key (manifest meta-data `com.google.android.geo.API_KEY`). The request also
 * carries the `X-Android-Package` / `X-Android-Cert` headers so a key that is
 * restricted to this Android app is accepted.
 *
 * The same [RADIUS_METERS] and outdoor-only source are used by
 * StreetViewActivity, so "available" here means the panorama screen will find
 * the same imagery.
 */
object StreetViewProbe {

    const val RADIUS_METERS = 50

    enum class Result {
        /** A panorama exists near the coordinate. */
        AVAILABLE,
        /** Google answered: no imagery here. */
        NONE,
        /**
         * The key is not allowed to call the metadata endpoint (Street View
         * Static API not enabled on the Cloud project, or not in the key's API
         * restrictions). Coverage is unknown; the panorama SDK itself may
         * still work because it is part of Maps SDK for Android.
         */
        DENIED,
        /** No answer (offline, timeout, quota, unexpected payload). */
        ERROR
    }

    private const val ENDPOINT = "https://maps.googleapis.com/maps/api/streetview/metadata"
    private const val META_KEY = "com.google.android.geo.API_KEY"
    private val STATUS_REGEX = Regex("\"status\"\\s*:\\s*\"([A-Z_]+)\"")

    fun metadataUrl(latitude: Double, longitude: Double, apiKey: String): String =
        String.format(
            Locale.US,
            "%s?location=%.6f,%.6f&radius=%d&source=outdoor&key=%s",
            ENDPOINT, latitude, longitude, RADIUS_METERS, apiKey
        )

    /** The `status` field of a metadata response body, or null when absent. */
    fun parseStatus(body: String?): String? =
        body?.let { STATUS_REGEX.find(it)?.groupValues?.get(1) }

    fun resultForStatus(status: String?): Result = when (status) {
        "OK" -> Result.AVAILABLE
        "ZERO_RESULTS", "NOT_FOUND" -> Result.NONE
        "REQUEST_DENIED" -> Result.DENIED
        else -> Result.ERROR
    }

    /** Blocking network call: run it off the main thread. Never throws. */
    fun check(context: Context, latitude: Double, longitude: Double): Result {
        val key = mapsKey(context) ?: return Result.ERROR
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(metadataUrl(latitude, longitude, key)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Android-Package", context.packageName)
                signingCertSha1(context)?.let { setRequestProperty("X-Android-Cert", it) }
            }
            // The endpoint answers 200 with a status field for every outcome it
            // understands; read the error stream too so a 4xx body is parsed.
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }
            resultForStatus(parseStatus(body))
        } catch (e: Exception) {
            Result.ERROR
        } finally {
            connection?.disconnect()
        }
    }

    private fun mapsKey(context: Context): String? = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString(META_KEY)?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    /** Upper-case hex SHA-1 of the certificate this install is signed with. */
    @Suppress("DEPRECATION")
    private fun signingCertSha1(context: Context): String? = try {
        val pm = context.packageManager
        val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.firstOrNull()?.toByteArray()
        }
        certBytes?.let { bytes ->
            MessageDigest.getInstance("SHA-1").digest(bytes)
                .joinToString("") { String.format(Locale.US, "%02X", it) }
        }
    } catch (e: Exception) {
        null
    }
}
