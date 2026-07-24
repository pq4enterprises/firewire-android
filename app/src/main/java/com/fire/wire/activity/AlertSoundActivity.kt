package com.fire.wire.activity

import android.content.Intent
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fire.wire.R
import com.fire.wire.adapter.Kadapter
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.ActivityAlertSoundBinding
import com.fire.wire.databinding.ItemAlertSoundBinding
import com.fire.wire.model.user.response.UserDetails
import com.fire.wire.prefs
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.AlertSounds
import com.fire.wire.utils.IntentUtils.UPDATE_PROFILE
import com.fire.wire.utils.gone
import com.fire.wire.utils.visible
import com.fire.wire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * ALERT SOUND (design screen 14) — global alert sound picker, reached from the
 * GLOBAL ALERT SOUND card on AREAS & ALERTS.
 *
 * Mirrors iOS NotificationSoundsViewController: tapping a row previews the
 * sound (free) and marks it; SAVE persists the choice, which is premium-gated
 * exactly like iOS (role "basic_user" -> subscription screen). The saved
 * choice is applied to real notifications through AlertSounds' fw_alerts_*
 * notification channels.
 */
@AndroidEntryPoint
class AlertSoundActivity : BaseActivity() {

    private lateinit var binding: ActivityAlertSoundBinding
    private lateinit var vm: FireWireViewModel

    private val sounds = AlertSounds.all.toMutableList()
    private var savedKey = AlertSounds.DEFAULT_KEY
    private var selectedKey = AlertSounds.DEFAULT_KEY
    private var adapter: Kadapter<AlertSounds.Sound, ItemAlertSoundBinding>? = null

    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null

    private var userDetails: UserDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

        savedKey = AlertSounds.current(prefs).key
        selectedKey = savedKey

        initToolbar()
        initApiCall()
        setupList()

        binding.btnSave.setOnClickListener { onSave() }
    }

    private fun initToolbar() {
        binding.toolbar.tvToolbarTitle.text = getString(R.string.alert_sound)
        binding.toolbar.tvToolbarTitle.visible()
        binding.toolbar.ivFeed.gone()
        binding.toolbar.ivMenu.setOnClickListener {
            finish()
        }
    }

    /** Refreshes the cached role silently; the screen works without it. */
    private fun initApiCall() {
        vm.getUserDetails()
        vm.userLiveData.observe(this, Observer { response ->
            if (response.state == ResourceState.SUCCESS) {
                response.data?.data?.let {
                    userDetails = it
                    prefs.userRole = it.role ?: ""
                }
            }
        })
    }

    private fun setupList() {
        adapter = binding.rvSounds.setUpAdapter(
            sounds,
            R.layout.item_alert_sound,
            ItemAlertSoundBinding::inflate,
            { sound, pos, rowBinding ->
                rowBinding.tvSoundName.text = sound.displayName
                rowBinding.ivSelected.visibility =
                    if (sound.key == selectedKey) View.VISIBLE else View.INVISIBLE
                rowBinding.divider.visibility =
                    if (pos == sounds.size - 1) View.GONE else View.VISIBLE
                rowBinding.rowSound.setOnClickListener { onRowTap(sound) }
            }
        )
    }

    /** Tap = preview + select (iOS previews free; only saving is gated). */
    private fun onRowTap(sound: AlertSounds.Sound) {
        selectedKey = sound.key
        adapter?.notifyDataSetChanged()
        preview(sound)
    }

    private fun preview(sound: AlertSounds.Sound) {
        stopPreview()
        if (sound.rawName == null) {
            // DEFAULT row: play the device's default notification sound
            ringtone = RingtoneManager.getRingtone(
                this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone?.play()
        } else {
            val resId = AlertSounds.rawResId(this, sound)
            if (resId != 0) {
                player = MediaPlayer.create(this, resId)
                player?.setOnCompletionListener { stopPreview() }
                player?.start()
            }
        }
    }

    private fun stopPreview() {
        player?.release()
        player = null
        ringtone?.stop()
        ringtone = null
    }

    /**
     * Premium gate, mirroring iOS unlockPremiumFeatureIfValid(): only an
     * explicit "basic_user" role is sent to the subscription screen.
     */
    private fun isBasicUser(): Boolean {
        val role = userDetails?.role ?: prefs.userRole ?: ""
        return role == "basic_user"
    }

    private fun onSave() {
        stopPreview()
        if (selectedKey != savedKey && isBasicUser()) {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra(UPDATE_PROFILE, userDetails ?: UserDetails())
            startActivity(intent)
            return
        }
        AlertSounds.apply(this, prefs, selectedKey)
        savedKey = selectedKey
        showSnack(getString(R.string.settings_updated))
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }

    override fun onDestroy() {
        stopPreview()
        super.onDestroy()
    }
}
