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

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.fire.wire.adapter.Kadapter
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ItemMenuTileBinding
import com.fire.wire.model.link.LinkResponse
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

    companion object {
        private const val GRID_TOTAL_SPANS = 6
        private const val MAX_TILES_PER_ROW = 3
    }

    private lateinit var binding: ActivityNavigationMenuBinding
    private lateinit var vm: FireWireViewModel
    private var userDetails= UserDetails()
    private val gridList = ArrayList<GridItems>()
    private var gridAdapter: Kadapter<GridItems, ItemMenuTileBinding>? = null

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

        // shortcut tiles: fetch server links without blocking the static menu
        vm.getLinks()
        vm.linkLiveData.observe(this, Observer {
            updateShortcutsFromLinks(it)
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
    }

    private fun areasAlertsTile() = GridItems(
        getString(R.string.areas_alerts), R.drawable.fw_ic_bell,
        R.color.fw_red, R.color.fw_red_tint,
        isPersonalization = true
    )

    private fun defaultShortcutTiles() = listOf(
        GridItems(getString(R.string.submit_tip), R.drawable.fw_ic_alert,
            R.color.fw_orange, R.color.fw_orange_tint,
            url = "https://nycfirewire.net/send-a-tip/"),
        GridItems(getString(R.string.fw_chicago_podcast), R.drawable.fw_ic_podcast,
            R.color.fw_text, R.color.fw_surface2,
            url = "https://www.chicagosbraveststories.com"),
        GridItems(getString(R.string.fw_firewire_website), R.drawable.fw_ic_globe,
            R.color.fw_red, R.color.fw_red_tint,
            url = "https://nycfirewire.net/"),
        GridItems(getString(R.string.fw_contact), R.drawable.fw_ic_mail,
            R.color.fw_info, R.color.fw_info_tint,
            url = "https://nycfirewire.net/contact/"),
        areasAlertsTile()
    )

    private fun setupShortcutsGrid() {
        gridList.clear()
        gridList.addAll(defaultShortcutTiles())

        // Balanced grid over a 6-span row: up to 3 tiles per row, smaller rows
        // first — 5 tiles keeps the mockup's 2+3 layout, and any other count
        // (1, 3, 4, 6, 8...) fills clean rows with the same spacing.
        val manager = GridLayoutManager(this, GRID_TOTAL_SPANS)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val count = gridList.size
                if (count <= 0 || position >= count) return GRID_TOTAL_SPANS
                val rows = (count + MAX_TILES_PER_ROW - 1) / MAX_TILES_PER_ROW
                val base = count / rows
                val extra = count % rows
                // first (rows - extra) rows hold `base` tiles, the rest base + 1
                var index = 0
                for (row in 0 until rows) {
                    val tilesInRow = if (row < rows - extra) base else base + 1
                    if (position < index + tilesInRow) return GRID_TOTAL_SPANS / tilesInRow
                    index += tilesInRow
                }
                return GRID_TOTAL_SPANS
            }
        }

        gridAdapter = binding.gvData.setUpAdapter(
            gridList,
            R.layout.item_menu_tile,
            ItemMenuTileBinding::inflate,
            { it,_,bindingItem->
                if (!it.imageUrl.isNullOrEmpty()) {
                    // server-provided icon: untinted image on a neutral circle
                    bindingItem.ivIcon.imageTintList = null
                    bindingItem.ivIcon.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@NavigationMenuActivity, R.color.fw_surface2))
                    Glide.with(this@NavigationMenuActivity)
                        .load(it.imageUrl)
                        .into(bindingItem.ivIcon)
                } else {
                    bindingItem.ivIcon.setImageResource(it.image)
                    bindingItem.ivIcon.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@NavigationMenuActivity, it.iconTint))
                    bindingItem.ivIcon.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@NavigationMenuActivity, it.iconBg))
                }
                bindingItem.tvTitle.text= it.title
                bindingItem.tileRoot.setOnClickListener { _ ->
                    when {
                        it.isPersonalization -> moveToPersonalActivity()
                        !it.url.isNullOrEmpty() -> moveToLink(it.url!!)
                    }
                }
            },{}, manager = manager)

    }

    /**
     * Non-blocking: the static tiles above are shown immediately; when the
     * Link API responds with at least one link, the four external-URL tiles
     * are replaced by the server links (Areas &amp; Alerts always stays, last).
     * On error or an empty list the static tiles remain untouched.
     */
    private fun updateShortcutsFromLinks(response: Resource<LinkResponse>) {
        if (response.state != ResourceState.SUCCESS) return
        val links = response.data?.data?.data.orEmpty()
            .filter { !it.url.isNullOrEmpty() && !it.name.isNullOrEmpty() }
        if (links.isEmpty()) return

        gridList.clear()
        links.sortedBy { it.sort ?: 0 }.forEach { link ->
            gridList.add(
                GridItems(
                    link.name?.uppercase(), R.drawable.fw_ic_globe,
                    R.color.fw_text, R.color.fw_surface2,
                    url = link.url,
                    imageUrl = link.imageUrl
                )
            )
        }
        gridList.add(areasAlertsTile())
        gridAdapter?.notifyDataSetChanged()
    }


    private fun initUi() {
        setupShortcutsGrid()

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
        val intent= Intent(this, AreasAlertsActivity::class.java)
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