package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.Kadapter
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.databinding.ActivityAlertSoundBinding
import com.pioneer.nycfirewire.databinding.ItemAlertSoundBinding
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.AlertSounds
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.IntentUtils
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * GLOBAL ALERT SOUND — re-skinned onto the redesign's layout while keeping this
 * screen's identity (it is still NotificationSoundActivity, still launched from
 * Areas & Alerts).
 *
 * Two things changed beyond the visuals:
 *
 * 1. The choice now DOES something. Previously the picker wrote a display name
 *    to prefs.soundName and nothing ever read it — no channel was configured and
 *    nothing was sent to the server, so incoming pushes always used whatever
 *    sound the OneSignal payload carried. Selections are now persisted by key
 *    via AlertSounds.apply(), which builds the fw_alerts_* notification channel
 *    that NotificationServiceExtension re-targets pushes onto.
 *
 * 2. Saving a non-default sound is premium, mirroring iOS. Previewing stays free.
 *
 * Also fixed on the way through: the old returnSoundFile() built a `when` whose
 * branches were never assigned, so every preview played music_one regardless of
 * the row tapped. AlertSounds resolves the raw resource by name instead.
 */
@AndroidEntryPoint
class NotificationSoundActivity : BaseActivity() {

    private lateinit var binding: ActivityAlertSoundBinding
    private lateinit var vm: FireWireViewModel

    private val sounds = AlertSounds.all.toMutableList()
    private var savedKey = AlertSounds.DEFAULT_KEY
    private var selectedKey = AlertSounds.DEFAULT_KEY
    private var adapter: Kadapter<AlertSounds.Sound, ItemAlertSoundBinding>? = null

    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null

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
        binding.toolbar.ivMenu.setOnClickListener { finish() }
    }

    /** Refreshes the cached role silently; the screen works without it. */
    private fun initApiCall() {
        vm.getUserDetails()
        vm.userLiveData.observe(this, Observer { response ->
            if (response.state == ResourceState.SUCCESS) {
                response.data?.data?.let { prefs.userRole = it.role ?: "" }
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

    /** Tap = select + preview. Previewing is free; only saving is gated. */
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
     * Premium gate, mirroring iOS: only an explicit basic_user is blocked, so a
     * role that has not loaded yet does not lock out a paying user.
     */
    private fun isBasicUser(): Boolean =
        (prefs.userRole ?: "") == Constants.USER_BASIC_USER

    private fun onSave() {
        stopPreview()
        if (selectedKey != savedKey && isBasicUser()) {
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra(IntentUtils.FROM_ACCOUNT, IntentUtils.OTHER)
            startActivity(intent)
            return
        }
        AlertSounds.apply(this, prefs, selectedKey)
        // keep the legacy display name in step so anything still reading it
        // (and the Areas & Alerts summary line) shows the right thing
        prefs.soundName = AlertSounds.byKey(selectedKey).displayName
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
