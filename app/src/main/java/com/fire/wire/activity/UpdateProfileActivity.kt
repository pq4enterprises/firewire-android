package com.fire.wire.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.databinding.ActivityUpdateProfileBinding
import com.fire.wire.fragment.BottomSheetFragment
import com.fire.wire.fragment.ImageDataListener
import com.fire.wire.model.user.request.ProfileUpdateRequest
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.model.user.response.UserDetails
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.AppUtils.getImageFileFromUri
import com.fire.wire.utils.Constants
import com.fire.wire.utils.IntentUtils.UPDATE_PROFILE
import com.fire.wire.utils.gone
import com.fire.wire.utils.isValidEmail
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody


@AndroidEntryPoint
class UpdateProfileActivity:BaseActivity(),ImageDataListener {

    private lateinit var binding: ActivityUpdateProfileBinding
    private lateinit var vm: FireWireViewModel
    private var profileDetails= UserDetails()
    var imageString=""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initExtra()
        initUi()
        initApiCall()
        clickEvent()
    }

    private fun initExtra() {
        profileDetails= intent.getParcelableExtra(UPDATE_PROFILE)?: UserDetails()
        binding.etFirstName.setText(profileDetails.firstName)
        binding.etLastName.setText(profileDetails.lastName)
        binding.etEmailAddress.setText(profileDetails.email)
        binding.etPhoneNo.setText(profileDetails.mobile)
        binding.etTitle.setText(profileDetails.title)
        if(!profileDetails.img.isNullOrEmpty()){
            Glide.with(this)
                .load(profileDetails.img)
                .into(binding.ivProfile)
        }

    }

    private fun initApiCall() {
     vm.updateProfileLiveData.observe(this, Observer {
         showUpdateResponse(it)
     })

        vm.imageUploadLiveData.observe(this, Observer {
            updateUploadedImage(it)
        })
    }

    private fun updateUploadedImage(response: Resource<CommonResponse>) {

        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                   // showSnack(response.data.message?:"")
                    imageString= response.data.data?.url?.get(0)?:""
                    prefs.userImg= imageString
                }else{
                    //showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun showUpdateResponse(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                   showSnack(response.message.toString())
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun createRequestFormat(){
        val firstName= binding.etFirstName.text.toString()
        val lastName= binding.etLastName.text.toString()
        val email= binding.etEmailAddress.text.toString()
        val phone= binding.etPhoneNo.text.toString()
        val title= binding.etTitle.text.toString()
        when{
            firstName.isEmpty()-> showSnack(getString(R.string.enter_first_name))
            lastName.isEmpty()-> showSnack(getString(R.string.enter_last_name))
            email.isEmpty()-> showSnack(getString(R.string.enter_email))
            !isValidEmail(email) -> showSnack(getString(R.string.enter_email))
            phone.isEmpty() -> showSnack(getString(R.string.enter_your_phone))
            title.isEmpty() -> showSnack(getString(R.string.enter_your_title))
            else->{
                val postRequest= ProfileUpdateRequest(
                     firstName=firstName,
                lastName=lastName,
                email= email,
                mobile= phone,
                title=title,
                img=imageString
                )

                vm.updateProfileData(postRequest)
            }
        }

    }

    private fun initUi() {
        binding.toolbar.tvToolbarTitle.text = getString(R.string.edit_profile).uppercase()
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
    }

    private fun clickEvent() {
        binding.tvChangePassword.setOnClickListener {
            val intent= Intent(this,ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        binding.btnUpdate.setOnClickListener {
            createRequestFormat()
        }

        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }

        binding.tvChangeImg.setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment(this)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

    }

    override fun getImageData(uri: Uri) {
        binding.ivProfile.setImageURI(uri)
        uploadImageData(uri)
    }
    private fun uploadImageData(uri: Uri) {
        val file = getImageFileFromUri(this, uri)
        if(file!=null){
            val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            vm.uploadImage(part)
        }
    }
}