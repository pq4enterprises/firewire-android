package com.pioneer.nycfirewire.utils

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.activity.AreasAlertsActivity

/**
 * "You're Premium" prompt shown once per purchase event, i.e. only on the
 * transition from basic_user (or no role yet) to a premium role. Callers pass
 * the role that was stored BEFORE they wrote the new one, so a sticky
 * paymentLiveData re-delivery after rotation, or a /payment re-report on a
 * later launch, finds the role already premium and shows nothing.
 */
object PremiumPrompt {

    fun isPremiumRole(role: String?): Boolean =
        !role.isNullOrEmpty() && role != Constants.USER_BASIC_USER

    /** @return true if the dialog was shown. */
    fun showIfJustUpgraded(activity: Activity, previousRole: String?): Boolean {
        if (isPremiumRole(previousRole)) return false
        if (activity.isFinishing || activity.isDestroyed) return false
        AlertDialog.Builder(activity)
            .setTitle(R.string.premium_prompt_title)
            .setMessage(R.string.premium_prompt_message)
            .setPositiveButton(R.string.premium_prompt_choose) { _, _ ->
                activity.startActivity(Intent(activity, AreasAlertsActivity::class.java))
            }
            .setNegativeButton(R.string.premium_prompt_later, null)
            .show()
        return true
    }
}
