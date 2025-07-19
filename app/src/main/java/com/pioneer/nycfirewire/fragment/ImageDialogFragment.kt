package com.pioneer.nycfirewire.fragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.R

class ImageDialogFragment : DialogFragment() {
    companion object {
        fun newInstance(imageUrl: String): ImageDialogFragment {
            val args = Bundle()
            args.putString("image_url", imageUrl)
            val fragment = ImageDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_image_viewer, container, false)
        val imageView = view.findViewById<ImageView>(R.id.fullscreenImage)
        val closeButton = view.findViewById<ImageView>(R.id.closeButton)

        val url = arguments?.getString("image_url")
        Glide.with(this).load(url).into(imageView)
        closeButton.setOnClickListener{
            dismiss()
        }

        view.setOnClickListener { dismiss() } // click anywhere to dismiss

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.black)))
    }
}
