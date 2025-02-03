package com.fire.wire.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fire.wire.R
import com.fire.wire.databinding.ActivityBaseContentBinding
import com.google.android.material.snackbar.Snackbar
import android.app.Activity
import android.view.inputmethod.InputMethodManager


abstract class BaseActivity :AppCompatActivity() {
    private lateinit var binding: ActivityBaseContentBinding


   protected fun showSnack(msg:String){
        hideKeyboard()
        Snackbar.make(window.decorView.rootView,msg, Snackbar.LENGTH_SHORT).show()
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



}