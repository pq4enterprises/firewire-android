package com.pioneer.nycfirewire.activity

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.onesignal.OneSignal
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.Kadapter
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.databinding.ActivityNavigationMenuBinding
import com.pioneer.nycfirewire.databinding.ItemMenuTileBinding
import com.pioneer.nycfirewire.model.link.LinkResponse
import com.pioneer.nycfirewire.model.user.request.DeleteUser
import com.pioneer.nycfirewire.model.user.request.GridItems
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.UserDetails
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.MENU
import com.pioneer.nycfirewire.utils.Constants.USER_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUB_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUPER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.NAVIGATION_MENU
import com.pioneer.nycfirewire.utils.IntentUtils.UPDATE_PROFILE
import com.pioneer.nycfirewire.utils.NetworkUtils.isOnline
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

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

    /** Last server-driven Link tiles (null/empty -> static defaults are shown). */
    private var serverLinkTiles: List<GridItems>? = null

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
        analyticMethod(MENU, "NavigationMenuActivity")

        if(prefs.userImg?.isEmpty() == true) {
            Glide.with(this).load(R.drawable.ic_user_profile_empty).into(binding.profileImage)
        }else{
            Glide.with(this).load(prefs.userImg).into(binding.profileImage)
        }
        if(prefs.userFirstName?.isNotEmpty() == true) {
            binding.tvName.text = prefs.userFirstName?.plus(" ").plus(prefs.userLastName)
            binding.tvEmail.text = prefs.userEmail
        }
    }

    private fun initApiCall() {
        if(isOnline(this)) {
            vm.getUserDetails()
            vm.userLiveData.observe(this, Observer { updateUserDetails(it) })
            vm.deleteUserLiveData.observe(this, Observer { updateDeleteUser(it) })

            // shortcut tiles: fetched without blocking the static menu
            vm.getLinks()
            vm.linkLiveData.observe(this, Observer { updateShortcutsFromLinks(it) })
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
                // Session renewal is silent and owned by TokenAuthenticator. If it could not
                // refresh, it has already cleared the session and routed the user to the login
                // screen with an explanation — so surfacing the raw server string here only
                // stacks a dead-end alert on top of that.
                if (response.message != getString(R.string.token_expired)) {
                    showAlert(response.message)
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
                // Session renewal is silent and owned by TokenAuthenticator. If it could not
                // refresh, it has already cleared the session and routed the user to the login
                // screen with an explanation — so surfacing the raw server string here only
                // stacks a dead-end alert on top of that.
                if (response.message != getString(R.string.token_expired)) {
                    showAlert(response.message)
                }
            }
            else -> {}
        }
    }

    private fun bindProfileDetails(data: UserDetails) {
        binding.tvName.text= data.firstName?.plus(" ").plus(data.lastName)
        binding.tvEmail.text= data.email
        prefs.userRole= data.role
        prefs.userImg= data.img
        if(data.img?.isEmpty() == true) {
            Glide.with(this).load(R.drawable.ic_user_profile_empty).into(binding.profileImage)
        }else{
            Glide.with(this).load(data.img).into(binding.profileImage)
        }

        // the role can arrive after initUi(), so re-evaluate the admin-only POST action
        applyPostVisibility()
    }

    /** POST is admin-only — same roles this lineage already gated it on. */
    private fun applyPostVisibility() {
        if(prefs.userRole==USER_ADMIN || prefs.userRole==USER_SUPER || prefs.userRole==USER_SUB_ADMIN){
            binding.tvPost.visible()
        }else{
            binding.tvPost.gone()
        }
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

    /**
     * Single source of truth for the grid: server Link tiles when available
     * (else the static defaults, which already end with Areas & Alerts), plus
     * the built-in Areas & Alerts tile after any server links.
     */
    private fun composeTiles() {
        gridList.clear()
        val links = serverLinkTiles
        if (links.isNullOrEmpty()) {
            gridList.addAll(defaultShortcutTiles())
        } else {
            gridList.addAll(links)
            gridList.add(areasAlertsTile())
        }
        gridAdapter?.notifyDataSetChanged()
    }

    private fun setupShortcutsGrid() {
        composeTiles()

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
                    Glide.with(this@NavigationMenuActivity).load(it.imageUrl).into(bindingItem.ivIcon)
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
     * Non-blocking: static tiles render immediately; when the Link API returns
     * at least one link the external-URL tiles are replaced by the server links
     * (Areas & Alerts always stays, last). On error or an empty list the static
     * tiles are left untouched.
     */
    private fun updateShortcutsFromLinks(response: Resource<LinkResponse>) {
        if (response.state != ResourceState.SUCCESS) return
        val links = response.data?.data?.data.orEmpty()
            .filter { !it.url.isNullOrEmpty() && !it.name.isNullOrEmpty() }
        if (links.isEmpty()) return

        serverLinkTiles = links.sortedBy { it.sort ?: 0 }.map { link ->
            GridItems(
                link.name?.uppercase(), R.drawable.fw_ic_globe,
                R.color.fw_text, R.color.fw_surface2,
                url = link.url,
                imageUrl = link.imageUrl
            )
        }
        composeTiles()
    }

    private fun initUi() {
        applyPostVisibility()
        setupShortcutsGrid()

        binding.tvClose.setOnClickListener {
            prefs.isRecreate=true
            finish()
        }

        // POST opens the portal's authenticated create-incident URL in an in-app
        // WebView — the URL carries the auth token, so it must never reach the
        // external browser's history.
        binding.tvPost.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }

        binding.cvProfile.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra(UPDATE_PROFILE, userDetails)
            intent.putExtra(FROM_ACCOUNT, NAVIGATION_MENU)
            startActivity(intent)
        }

        binding.tvDelete.setOnClickListener {
            showAlertDialogButtonClicked(getString(R.string.confirm_delete))
        }
    }

    /**
     * Areas & Alerts — the consolidated replacement for the old Personalization
     * hub plus the Notification Locality / Notification City screens.
     */
    private fun moveToPersonalActivity(){
        val intent= Intent(this, AreasAlertsActivity::class.java)
        startActivity(intent)
    }

    private fun deleteOrLogout(){
        // clearSession() removes the access AND refresh tokens and writes the change.
        // The old `prefs.deleteToken` was a no-op property read, so signing out left
        // both live credentials sitting in SharedPreferences.
        prefs.clearSession()
        prefs.userImg= ""
        prefs.userFirstName= ""
        prefs.userLastName= ""
        prefs.userEmail= ""
        prefs.isDarkMode= false
        startNewActivity(LoginNewActivity::class.java)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        OneSignal.logout()
    }

    private fun showAlertDialogButtonClicked(msg: String) {
        val builder = AlertDialog.Builder(this)
        val customLayout = layoutInflater.inflate(R.layout.dialog_custom_alert, null)
        builder.setView(customLayout)
        builder.setTitle(resources.getString(R.string.app_name))
        builder.setMessage(msg)

        val editText: EditText = customLayout.findViewById(R.id.editText)
        val btnCancel: Button = customLayout.findViewById(R.id.btn_cancel)
        val btnOk: Button = customLayout.findViewById(R.id.btn_ok)

        builder.setPositiveButton("") { _: DialogInterface?, _: Int -> }
        builder.setNegativeButton("") { _, _ -> }
        val dialog = builder.create()
        dialog.show()

        btnCancel.setOnClickListener{ dialog.dismiss() }
        btnOk.setOnClickListener{
            if(editText.text.toString().isEmpty()){
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
