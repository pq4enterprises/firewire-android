package com.fire.wire.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.fire.wire.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object AppUtils {

    fun getImageFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver: ContentResolver = context.contentResolver

        // Get the file name from the URI
        val fileName = getFileName(context, uri) ?: return null

        // Create a temp file to copy the image content
        val tempFile = File(context.cacheDir, fileName)
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(tempFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }

        return tempFile
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

}