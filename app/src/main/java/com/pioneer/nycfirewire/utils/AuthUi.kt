package com.pioneer.nycfirewire.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.CountDownTimer
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.pioneer.nycfirewire.R

/**
 * Shared presentation helpers for the auth cluster (sign in, register, reset
 * password, verify email). Mirrors iOS FWAuthComponents so both platforms
 * behave the same: inline field errors, inline banners instead of dialogs,
 * resend countdown, font-scale friendly rows.
 */
object AuthUi {

    enum class Banner { ERROR, INFO, SUCCESS }

    /** Shows an inline banner; replaces every dead-end alert on these screens. */
    fun showBanner(view: TextView, message: String, style: Banner) {
        val ctx = view.context
        val (bg, icon, color) = when (style) {
            Banner.ERROR -> Triple(R.drawable.fw_banner_error_bg, R.drawable.fw_ic_alert_circle, R.color.fw_error_text)
            Banner.INFO -> Triple(R.drawable.fw_banner_info_bg, R.drawable.fw_ic_info, R.color.fw_text)
            Banner.SUCCESS -> Triple(R.drawable.fw_banner_success_bg, R.drawable.fw_ic_check_circle, R.color.fw_success_text)
        }
        view.setBackgroundResource(bg)
        view.setTextColor(ContextCompat.getColor(ctx, color))
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(
            view,
            ColorStateList.valueOf(ContextCompat.getColor(ctx, if (style == Banner.INFO) R.color.fw_muted else color))
        )
        view.text = message
        view.visibility = View.VISIBLE
        view.announceForAccessibility(message)
    }

    fun hideBanner(view: TextView) {
        view.visibility = View.GONE
    }

    /** Red outline + message under the field; null clears it. */
    fun setFieldError(input: EditText, errorView: TextView, message: String?) {
        if (message.isNullOrEmpty()) {
            input.setBackgroundResource(R.drawable.fw_input_bg)
            errorView.visibility = View.GONE
        } else {
            input.setBackgroundResource(R.drawable.fw_input_error_bg)
            errorView.text = message
            errorView.visibility = View.VISIBLE
        }
    }

    /** Clears the field's error and the given banners as soon as the user edits. */
    fun clearOnEdit(input: EditText, errorView: TextView, vararg banners: TextView) {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (errorView.visibility == View.VISIBLE) setFieldError(input, errorView, null)
                banners.forEach { if (it.visibility == View.VISIBLE) hideBanner(it) }
            }
        })
    }

    /** Return key on the last field submits the form. */
    fun submitOnDone(input: EditText, action: () -> Unit) {
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                action(); true
            } else false
        }
    }

    /** Auto-submits a 6-digit code field the moment it is complete. */
    fun autoSubmitCode(input: EditText, length: Int = 6, onComplete: () -> Unit) {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val digits = s?.filter { it.isDigit() } ?: ""
                if (digits.length == length) onComplete()
            }
        })
    }

    /** Validation helpers shared by all four screens (same rules as iOS FWAuthValidation). */
    fun emailError(ctx: Context, email: String): String? = when {
        email.isEmpty() -> ctx.getString(R.string.fw_auth_error_email_empty)
        !isValidEmail(email) -> ctx.getString(R.string.fw_auth_error_email_invalid)
        else -> null
    }

    fun passwordError(ctx: Context, password: String, minimum: Int = 8): String? = when {
        password.isEmpty() -> ctx.getString(R.string.fw_auth_error_password_empty)
        password.length < minimum -> ctx.getString(R.string.fw_auth_error_password_short)
        else -> null
    }

    /** Maps transport failures to one plain sentence; server messages pass through. */
    fun friendly(ctx: Context, message: String?): String {
        val raw = message ?: return ctx.getString(R.string.fw_auth_no_connection)
        val lower = raw.lowercase()
        val network = listOf("unable to resolve host", "failed to connect", "timeout", "timed out", "network", "connection", "internet", "socket", "ssl")
            .any { lower.contains(it) }
        return if (network || raw == "null") ctx.getString(R.string.fw_auth_no_connection) else raw
    }

    /**
     * "Don’t have an account? REGISTER" — muted sentence with a red, heavy,
     * uppercase call to action. Replaces the hard-coded Color.RED / black spans.
     */
    fun setLinkSentence(view: TextView, sentence: String, link: String, uppercase: Boolean = true, onClick: () -> Unit) {
        val ctx = view.context
        val start = sentence.indexOf(link, ignoreCase = true)
        val shown = if (uppercase) link.uppercase() else link
        val text = if (start >= 0) sentence.substring(0, start) + shown + sentence.substring(start + link.length) else sentence
        val spannable = SpannableString(text)
        if (start >= 0) {
            val end = start + link.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) { ds.isUnderlineText = false }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.fw_red)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        view.text = spannable
        view.setTextColor(ContextCompat.getColor(ctx, R.color.fw_muted))
        view.movementMethod = LinkMovementMethod.getInstance()
        view.highlightColor = android.graphics.Color.TRANSPARENT
    }

    /** Side-by-side field pairs stack vertically once the font scale gets large. */
    fun stackRowsForLargeText(ctx: Context, vararg rows: LinearLayout) {
        if (ctx.resources.configuration.fontScale < 1.3f) return
        val gap = (14 * ctx.resources.displayMetrics.density).toInt()
        rows.forEach { row ->
            row.orientation = LinearLayout.VERTICAL
            for (i in 0 until row.childCount) {
                val child = row.getChildAt(i)
                val lp = child.layoutParams as LinearLayout.LayoutParams
                lp.width = LinearLayout.LayoutParams.MATCH_PARENT
                lp.weight = 0f
                lp.marginStart = 0
                lp.marginEnd = 0
                lp.topMargin = if (i == 0) 0 else gap
                child.layoutParams = lp
            }
        }
    }

    /** 30-second resend countdown shared by the two code-entry screens. */
    class ResendCountdown(private val timerView: TextView, private val resendView: TextView, private val seconds: Int = 30) {
        private var timer: CountDownTimer? = null

        fun start() {
            timer?.cancel()
            resendView.isEnabled = false
            resendView.alpha = 0.45f
            timer = object : CountDownTimer(seconds * 1000L, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerView.text = timerView.context.getString(R.string.fw_auth_resend_in, millisUntilFinished / 1000)
                }
                override fun onFinish() {
                    timerView.text = timerView.context.getString(R.string.fw_auth_didnt_get)
                    resendView.isEnabled = true
                    resendView.alpha = 1f
                }
            }.start()
        }

        fun showSending() {
            timer?.cancel()
            timerView.text = timerView.context.getString(R.string.fw_auth_sending)
            resendView.isEnabled = false
            resendView.alpha = 0.45f
        }

        fun cancel() {
            timer?.cancel()
        }
    }
}
