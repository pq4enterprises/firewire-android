package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.model.user.request.GridItems
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.GridLayoutManager

import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.user.request.DeleteUser
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import android.content.DialogInterface
import android.widget.Button

import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityNavigationMenuBinding
import com.pioneer.nycfirewire.databinding.ItemGridSaltBinding
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.MENU
import com.pioneer.nycfirewire.utils.Constants.MY_ACCOUNT
import com.pioneer.nycfirewire.utils.Constants.USER_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.Constants.USER_SUB_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUPER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.NAVIGATION_MENU
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible


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
        clickEvent()
    }

    private fun clickEvent() {
        binding.tvPost.setOnClickListener{
            var intent= Intent(this,WebViewActivity::class.java)
          startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(MENU, "NavigationMenuActivity")

        if(prefs.userImg?.isEmpty() == true) {
            Glide.with(this)
                .load(R.drawable.ic_user_profile_empty)
                .into(binding.profileImage)
        }else{
            Glide.with(this)
                .load(prefs.userImg)
                .into(binding.profileImage)
        }
        if(prefs.userFirstName?.isNotEmpty() == true) {
            binding.tvName.text = prefs.userFirstName?.plus(" ").plus(prefs.userLastName)
            binding.tvEmail.text = prefs.userEmail
        }
    }

    private fun initApiCall() {
        if(isOnline(this)) {
            vm.getUserDetails()
            vm.userLiveData.observe(this, Observer {
                updateUserDetails(it)
            })

            vm.deleteUserLiveData.observe(this, Observer {
                updateDeleteUser(it)
            })
        }else Toast.makeText(this, getString(R.string.check_network_connection), Toast.LENGTH_SHORT).show()
    }

    private fun updateDeleteUser(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS || response.data?.code=="profile_updated") {
                    deleteOrLogout()

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
                    userDetails= it1.data?: UserDetails()
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

    private fun moveToPaymentPage(){
        val intent = Intent(this, MyAccountActivity::class.java)
        intent.putExtra(FROM_ACCOUNT, OTHER)
        startActivity(intent)
    }

    private fun bindProfileDetails(data: UserDetails) {
        binding.tvName.text= data.firstName?.plus(" ").plus(data.lastName)
        binding.tvEmail.text= data.email
        prefs.userImg= data.img
        if(data.img?.isEmpty() == true) {
            Glide.with(this)
                .load(R.drawable.ic_user_profile_empty)
                .into(binding.profileImage)
        }else{
            Glide.with(this)
                .load(data.img)
                .into(binding.profileImage)
        }

        binding.cvSalty.setOnClickListener {
       /*     var intent = Intent(this, SaltyWireActivity::class.java)
            startActivity(intent)*/

            if(prefs.userRole==USER_BASIC_USER){
                moveToPaymentPage()
            }else{
                var intent = Intent(this, SaltyWireActivity::class.java)
                startActivity(intent)
               // moveToLink("https://saltywire.com/")
            }
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
        if(prefs.userRole==USER_ADMIN || prefs.userRole==USER_SUPER || prefs.userRole==USER_SUB_ADMIN){
            binding.tvPost.visible()
        }else{
            binding.tvPost.gone()
        }



        binding.llClose.setOnClickListener {
            prefs.isRecreate=true
            finish()
        }

        binding.cvProfile.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra(UPDATE_PROFILE, userDetails)
            intent.putExtra(FROM_ACCOUNT, NAVIGATION_MENU)
            startActivity(intent)
        }

        binding.tvDelete.setOnClickListener {
            showAlertDialogButtonClicked(getString(R.string.confirm_delete))
           // showConfirm(getString(R.string.confirm_delete))
        }

//        if(prefs.isDarkMode){
//            binding.clView.setBackgroundColor(resources.getColor(R.color.white))
//        }else{
//            binding.clView.background= resources.getDrawable(R.drawable.ic_lite_red_bg)
//        }
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

    private fun deleteOrLogout(){
        prefs.deleteToken
        prefs.isLogin = false
        prefs.userImg= ""
        prefs.userFirstName= ""
        prefs.userLastName= ""
        prefs.userEmail= ""
        prefs.isDarkMode= false
        startNewActivity(LoginNewActivity::class.java)
        AppCompatDelegate
            .setDefaultNightMode(
                AppCompatDelegate
                    .MODE_NIGHT_NO);
        OneSignal.logout();
    }


   private fun showAlertDialogButtonClicked(msg: String) {
        // Create an alert builder
        val builder = AlertDialog.Builder(this)

        // set the custom layout
        val customLayout = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        builder.setView(customLayout)

       builder.setTitle(resources.getString(R.string.app_name))
       builder.setMessage(msg)
       val editText: EditText = customLayout.findViewById(R.id.editText)
       val btnCancel: Button = customLayout.findViewById(R.id.btn_cancel)
       val btnOk: Button = customLayout.findViewById(R.id.btn_ok)

        // add a button
        builder.setPositiveButton("") { dialog: DialogInterface?, which: Int ->
        }
            builder.setNegativeButton("") { dialog, _ ->
            }
        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()

       btnCancel.setOnClickListener{ dialog.dismiss() }

       btnOk.setOnClickListener{
           if(editText.text.toString().isEmpty()){
               //editText.setError(getString(R.string.enter_your_reason))
                showToast(this,"Kindly enter your reason")
           }else{
               val request= DeleteUser(true, editText.text.toString())
               vm.deleteUser(request)
               dialog.dismiss()
           }
       }


   }

    override fun onBackPressed() {
        super.onBackPressed()
        prefs.isRecreate=true
    }
}
