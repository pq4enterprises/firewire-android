package com.fire.wire.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.material.snackbar.Snackbar
import java.util.regex.Pattern


const val NAV_FILTER="nav_filter"
const val NAV_WIRE="nav_wire"
const val NAV_NEWS="nav_news"
const val NAV_WIRE_NEWS="nav_wire_news"
const val NAV_WIRE_list="nav_wire_list"
const val NAV_COMMENT_LIST="nav_comment_list"

fun <A:Activity> Activity.startNewActivity(activity: Class<A>){
    Intent(this,activity).also {
      it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

fun <A:Activity> Fragment.startNewActivity(activity: Class<A>){
    Intent(requireContext(),activity).also {
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

fun showToast(context: Context,msg:String){
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}


fun isValidEmail(str: String): Boolean{
    return EMAIL_ADDRESS_PATTERN.matcher(str).matches()
}

val EMAIL_ADDRESS_PATTERN = Pattern.compile(
    "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
)


 fun BitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    // below line is use to generate a drawable.
    val vectorDrawable = ContextCompat.getDrawable(
        context, vectorResId
    )

    // below line is use to set bounds to our vector
    // drawable.
    vectorDrawable!!.setBounds(
        0, 0, vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight
    )

    // below line is use to create a bitmap for our
    // drawable which we have added.
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    // below line is use to add bitmap in our canvas.
    val canvas = Canvas(bitmap)

    // below line is use to draw our
    // vector drawable in canvas.
    vectorDrawable.draw(canvas)

    // after generating our bitmap we are returning our
    // bitmap.
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}