package com.fire.wire.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.databinding.ActivityNavigationMenuBinding
import com.fire.wire.model.user.request.GridItems
import com.fire.wire.model.user.response.UserDetails
import com.fire.wire.model.user.response.UserResponse
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.IntentUtils.UPDATE_PROFILE
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.GridLayoutManager

import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ItemGridSaltBinding
import com.fire.wire.model.user.request.DeleteUser
import com.fire.wire.model.user.response.CommonResponse
import android.content.DialogInterface

import android.widget.EditText
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.prefs
import com.fire.wire.utils.*


@AndroidEntryPoint
class NavigationMenuActivity: BaseActivity() {

    private lateinit var binding: ActivityNavigationMenuBinding
    private lateinit var vm: FireWireViewModel
    private var userDetails= UserDetails()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        initUi()
        initApiCall()
    }

    override fun onResume() {
        super.onResume()
        if(prefs.userImg?.isNotEmpty() == true)
        Glide.with(this)
            .load(prefs.userImg)
            .into(binding.profileImage)
    }

    private fun initApiCall() {
        vm.getUserDetails()
        vm.userLiveData.observe(this, Observer {
            updateUserDetails(it)
        })

        vm.deleteUserLiveData.observe(this, Observer {
            updateDeleteUser(it)
        })
    }

    private fun updateDeleteUser(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS || response.data?.code=="profile_updated") {
                   startNewActivity(LoginNewActivity::class.java)
                }else{
                    showAlert(response.data?.message.toString())
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

    private fun updateUserDetails(response: Resource<UserResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                response.data?.let { it1 ->
                    userDetails= it1.data?:UserDetails()
                    bindProfileDetails(userDetails)

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

    private fun bindProfileDetails(data: UserDetails) {
        binding.tvName.text= data.firstName
        binding.tvEmail.text= data.email
        binding.cvSalty.setOnClickListener {
            moveToLink("https://saltywire.com/")
        }

        val gridItems = listOf(
            GridItems("Submit\n" +
                    "A Tip", R.drawable.ic_tip_img),
            GridItems("Chicago\n" +
                    "Podcast", R.drawable.ic_user_chicago),
           /* GridItems("Pioneer\n" +
                    "Applications", R.drawable.ic_launcher_foreground),*/
            GridItems("FireWire\n" +
                    "Website", R.drawable.ic_user_firewire),
            GridItems("Contact", R.drawable.ic_user_email),
            GridItems("Personalization", R.drawable.ic_user_personal)
        )

        val gridList= ArrayList(gridItems)

        //binding.gvData.layoutManager = GridLayoutManager(this, 3)
        binding.gvData.setUpAdapter(
            gridList,
            R.layout.item_grid_salt,
            ItemGridSaltBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.ivLogo.setImageResource(it.image)
                bindingItem.tvLogoName.text= it.title
                bindingItem.cvLogo.setOnClickListener {
                    when(pos){
                        0-> moveToLink("https://nycfirewire.net/send-a-tip/")
                        1-> moveToLink("https://www.chicagosbraveststories.com")
                        3-> moveToLink("https://nycfirewire.net/contact/")
                        2-> moveToLink("https://nycfirewire.net/")
                        4-> moveToPersonalActivity()
                    }
                }
            },{}, manager = GridLayoutManager(this, 3))

    }


    private fun initUi() {
        binding.tvClose.setOnClickListener {
            finish()
        }

        binding.cvProfile.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra(UPDATE_PROFILE, userDetails)
            startActivity(intent)
        }

        binding.tvDelete.setOnClickListener {
            showAlertDialogButtonClicked(getString(R.string.confirm_delete))
           // showConfirm(getString(R.string.confirm_delete))
        }
    }

    private fun moveToPersonalActivity(){
        val intent= Intent(this, PersonalizationActivity::class.java)
        startActivity(intent)
    }

   private fun showConfirm(message: String? = "") {
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val request= DeleteUser(true)
               vm.deleteUser(request)
            }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
               dialog.dismiss()
            }
            .show()
    }


   private fun showAlertDialogButtonClicked(msg: String) {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout = layoutInflater.inflate(com.fire.wire.R.layout.dialog_custom_alert, null)
        builder.setView(customLayout)

       builder.setTitle(resources.getString(R.string.app_name))
       builder.setMessage(msg)

        // add a button
        builder.setPositiveButton("OK") { dialog: DialogInterface?, which: Int ->
            // send data from the AlertDialog to the Activity
            val editText: EditText = customLayout.findViewById(R.id.editText)
          if(editText.text.toString().isEmpty()){
              showToast(this,"Kindly enter your reason")
          }else{
              val request= DeleteUser(true, editText.text.toString())
              vm.deleteUser(request)
          }
        }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }
}