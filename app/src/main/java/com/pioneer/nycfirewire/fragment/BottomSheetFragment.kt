package com.pioneer.nycfirewire.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.databinding.BottomChoosePhotoBinding
import com.pioneer.nycfirewire.utils.Constants.BOTTOM_SHEET_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.WIRE_FRAGMENT
import java.io.File
import java.io.IOException


class BottomSheetFragment(clickListener: ImageDataListener) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomChoosePhotoBinding
    private val cameraRequestCode = 100
    private val galleryRequestCode = 101
    private var imageUri: Uri? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = BottomChoosePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llGallery.setOnClickListener {
            openGallery()
        }

        binding.llTakePhoto.setOnClickListener {
            if (isCameraPermissionGranted()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        binding.tvCancel.setOnClickListener {
            dismiss() // Dismiss the bottom sheet when the button is clicked
        }

    }

    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, BOTTOM_SHEET_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "SelectionBottomSheet")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }


    private val getCameraImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap: Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                clickListener.getImageData(imageUri?: Uri.EMPTY)
                dismiss()
            }
        }

    private val getGalleryImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                clickListener.getImageData(imageUri?: Uri.EMPTY)
                dismiss()
            }
        }


    private fun isCameraPermissionGranted(): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        )
        return permission == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            cameraRequestCode
        )
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()  // Create a file to store the image
            } catch (ex: IOException) {
                null
            }

            photoFile?.also {
                imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.fire.wire.fileprovider", // Change this to your file provider
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                getCameraImage.launch(intent)
            }
        }
    }

    private fun createImageFile(): File {
        val storageDir: File = requireContext().getExternalFilesDir(null) ?: throw IOException("Failed to get storage directory")
        return File.createTempFile("JPEG_", ".jpg", storageDir)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        getGalleryImage.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            cameraRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

interface ImageDataListener{
    fun getImageData(uri:Uri)
}


