package com.fire.wire.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

fun <A:Activity> Activity.startNewActivity(activity: Class<A>){
    Intent(this,activity).also {
      it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

fun showToast(context: Context,msg:String){
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}