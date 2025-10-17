package com.pioneer.nycfirewire.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.pioneer.nycfirewire.R

object UiUtils {

    fun setReplyText(editText: EditText, username: String, context: Context) {
        val replyTag = "@$username "
        val spannable = SpannableString(replyTag)

        // Apply color span only for @username lastname
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.user_name_bind)), // 👈 choose your color
            0,
            replyTag.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set text in EditText
        editText.setText(spannable)

        // Move cursor to end so user can continue typing
        editText.setSelection(spannable.length)
    }


    fun commentBindColor(comment: String, context: Context, textView: TextView){
// Find the username part
        val usernameRegex = Regex("@\\w+")
        val spannable = SpannableString(comment)

// Apply color span to the username part
        usernameRegex.find(comment)?.let {
            val start = it.range.first
            val end = it.range.last + 1
            val color = ContextCompat.getColor(context, R.color.user_name_bind) // your custom color
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

// Set to TextView
        textView.text = spannable

    }

}