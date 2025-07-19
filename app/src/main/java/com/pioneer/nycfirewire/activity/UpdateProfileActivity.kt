package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.fragment.BottomSheetFragment
import com.pioneer.nycfirewire.fragment.ImageDataListener
import com.pioneer.nycfirewire.model.user.request.ProfileUpdateRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.AppUtils.getImageFileFromUri
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.isValidEmail
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityUpdateProfileBinding
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.utils.Constants.PROFILE_UPDATE
import com.pioneer.nycfirewire.utils.Constants.SELECT_AREA
import com.pioneer.nycfirewire.utils.startNewActivity
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody


@AndroidEntryPoint
class UpdateProfileActivity: BaseActivity(), ImageDataListener {

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

   /* override fun onResume() {
        super.onResume()
        analyticMethod(PROFILE_UPDATE,"UpdateProfileActivity")
    }*/
    private fun initExtra() {
        profileDetails= intent.getParcelableExtra(UPDATE_PROFILE)?: UserDetails()
        bindProfileDetails()

    }

    private fun initApiCall() {
     vm.updateProfileLiveData.observe(this, Observer {
         showUpdateResponse(it)
     })

        vm.imageUploadLiveData.observe(this, Observer {
            updateUploadedImage(it)
        })

        vm.getUserDetails()
        vm.userLiveData.observe(this, Observer {
            updateUserDetails(it)
        })
    }

    private fun updateUserDetails(response: Resource<UserResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    profileDetails= it1.data?: UserDetails()
                    bindProfileDetails()

                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                if(response.message==getString(R.string.token_expired)) {
                    startNewActivity(LoginNewActivity::class.java)
                }

            }
            else -> {}
        }
    }

    private fun bindProfileDetails() {
        prefs.userRole = profileDetails.role
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

    private fun updateUploadedImage(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    showSnack("Image updated successfully!")
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
                if(response.data?.code== Constants.PROFILE_UPDATED) {
                    prefs.userFirstName= binding.etFirstName.text.toString()
                    prefs.userLastName= binding.etLastName.text.toString()
                    prefs.userEmail= binding.etEmailAddress.text.toString()
                   showSnack(response.data.message.toString())
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
        val firstName= binding.etFirstName.text.toString().trim()
        val lastName= binding.etLastName.text.toString().trim()
        val email= binding.etEmailAddress.text.toString().trim()
        val phone= binding.etPhoneNo.text.toString().trim()
        val title= binding.etTitle.text.toString().trim()
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
                img=imageString,
                    role = profileDetails.role
                )

                vm.updateProfileData(postRequest)
            }
        }

    }

    private fun initUi() {
        binding.toolbar.tvTitle.text = getString(R.string.update_profile)
    }

    private fun clickEvent() {
        binding.tvChangePassword.setOnClickListener {
            val intent= Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        binding.btnUpdate.setOnClickListener {
            createRequestFormat()
        }

        binding.toolbar.tvBack.setOnClickListener {
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