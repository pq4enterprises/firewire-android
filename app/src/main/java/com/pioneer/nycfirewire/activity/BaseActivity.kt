package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityBaseContentBinding



abstract class BaseActivity :AppCompatActivity() {
    private lateinit var binding: ActivityBaseContentBinding



   protected fun showSnack(msg:String){
        hideKeyboard()
       val rootView = findViewById<View>(android.R.id.content)
     /*  window.insetsController?.let { controller ->
           controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT)
       }*/

       // Create the Snackbar
       val snackbar = Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT)

       // Optional: Set the anchor view to avoid blocking UI elements like BottomNavigationView or BottomSheet
       val bottomNavigationView = findViewById<View>(R.id.bottom_constraint)  // Replace with your bottom view ID
       if (bottomNavigationView != null) {
           snackbar.setAnchorView(bottomNavigationView)
       }

       // Optional: Adjust Snackbar's bottom margin to avoid overlap with the system navigation gestures
       val snackbarView = snackbar.view
       val params = snackbarView.layoutParams as FrameLayout.LayoutParams
       params.bottomMargin = 100  // Adjust this as necessary for your layout

       snackbarView.layoutParams = params
       snackbar.show()
    }


     fun hideKeyboard() {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
         inputMethodManager.hideSoftInputFromWindow(
             window.decorView.windowToken, 0
         )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityBaseContentBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

     fun showAlert(message: String? = "") {
         if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }

     fun moveToLink(url:String){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }


    fun analyticMethod(screenName: String, className: String){
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, className)
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }


}