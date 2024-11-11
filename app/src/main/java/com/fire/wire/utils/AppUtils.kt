package com.fire.wire.utils

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.fire.wire.R

object AppUtils {
    fun setMessageWithClickableLink(context: Context, textView: TextView) {
        val content = context.getString(R.string.account_register)
        val clickableSpan = object : ClickableSpan(){
            override fun onClick(widget: View) {
                showToast(context,"Clicked policy")
            }
        }
        val startIndex = content.indexOf("Terms")
        val endIndex = content.lastIndexOf("Conditions")+"Conditions".length
        val spannableString = SpannableString(content)
        val spanColor= ForegroundColorSpan(Color.WHITE)

        spannableString.setSpan(clickableSpan, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(spanColor, startIndex, endIndex,   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.setText(spannableString)
        textView.setTextColor(Color.WHITE)
        textView.setMovementMethod(LinkMovementMethod.getInstance())
        textView.setHighlightColor(Color.TRANSPARENT)
    }

}