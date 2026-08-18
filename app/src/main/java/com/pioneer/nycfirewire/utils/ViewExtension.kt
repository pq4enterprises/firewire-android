package com.pioneer.nycfirewire.utils

import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

fun View.visible(){
    visibility = View.VISIBLE
}

fun View.gone(){
    visibility = View.GONE
}

fun View.inVisible(){
    visibility = View.INVISIBLE
}


fun AppCompatActivity.replaceFragment(
    fragment: Fragment,
    tag: String?,
    allowStateLoss: Boolean = false,
    @IdRes containerViewId: Int,
    @AnimRes enterAnimation: Int = 0,
    @AnimRes exitAnimation: Int = 0,
    @AnimRes popEnterAnimation: Int = 0,
    @AnimRes popExitAnimation: Int = 0,
    allowBackStack: Boolean = false
) {
    val ft = supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(enterAnimation, exitAnimation, popEnterAnimation, popExitAnimation)
    when {
        allowBackStack -> {
            ft.add(containerViewId, fragment, tag)
            ft.addToBackStack(tag)
        }
        else -> ft.replace(containerViewId, fragment, tag)
    }
    if (!supportFragmentManager.isStateSaved) {
        ft.commit()
    } else if (allowStateLoss) {
        ft.commitAllowingStateLoss()
    }
}


fun SearchView.getQueryTextChangeStateFlow(): StateFlow<String> {

    val query = MutableStateFlow("")

    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return true
        }

        override fun onQueryTextChange(newText: String): Boolean {
            query.value = newText
            return true
        }
    })

    return query

}

inline fun <FRAGMENT : Fragment> FRAGMENT.putArgs(argsBuilder: Bundle.() -> Unit): FRAGMENT =
    this.apply { arguments = Bundle().apply(argsBuilder) }
/**
 * Onboarding tap-target tours are DISABLED in all builds as of 9.9.1.
 *
 * They target views inside RecyclerView rows, which is fragile — a recycled or
 * not-yet-attached view can stall the sequence and trap the user on the screen.
 * This previously read `!BuildConfig.DEBUG`, so the tours were suppressed in
 * every build we QA'd and enabled for every real user, which is backwards.
 *
 * Every tour call site is gated on this flag, so `false` disables them app-wide.
 * The remaining intro plumbing (Prefs flags, the per-screen tour builders) is
 * left in place for a separate cleanup.
 */
val introsEnabled: Boolean get() = false
