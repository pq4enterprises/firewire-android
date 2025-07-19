package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.databinding.ActivityNotificationLocalityBinding
import com.pioneer.nycfirewire.databinding.ActivityNotificationSoundBinding
import com.pioneer.nycfirewire.databinding.ItemLocalityBinding
import com.pioneer.nycfirewire.databinding.ItemLocalityBinding.inflate
import com.pioneer.nycfirewire.databinding.ItemSoundBinding
import com.pioneer.nycfirewire.model.SoundFile
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_DATA
import com.pioneer.nycfirewire.utils.Constants.LOCALITY_NAME
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class NotificationSoundActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationSoundBinding
    private lateinit var vm: FireWireViewModel
    private var soundList= ArrayList<SoundFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityNotificationSoundBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        binding.toolbarLayout.tvTitle.text= getString(R.string.notification_souns)

        clickEvent()
        initApiCall()

    }

    private fun initApiCall() {
       var soundNames= arrayOf("Acting Engine",
           "Battalion","Division","Engine Ladder Ticket","Engine Ticket",
           "Engine, Ladder, Battalion","Ladder Ticket","MDT Ring","Special FireUnit","Standby For Message","Tones Only")

        soundNames.forEach {

            var soundFile=  if(prefs.soundName?.trim()== it.trim()){
                SoundFile(name = it, true)
            }else SoundFile(name = it, false)

            soundList.add(soundFile)
        }


        setupAdapter()

    }


    fun returnSoundFile(name: String): Int {
        var soundFile= R.raw.music_one

        when(name){
           "Acting Engine" -> R.raw.acting_engine_ticket
           "Battalion" -> R.raw.batalion_ticket
           "Division" -> R.raw.division_ticket
           "Engine Ladder Ticket" -> R.raw.engine_ladder_ticket
           "Engine Ticket" -> R.raw.engine_ticket
           "Engine, Ladder, Battalion" -> R.raw.engine_ladder_batalion_ticket
           "Ladder Ticket" -> R.raw.ladder_ticket
           "MDT Ring" -> R.raw.mdt_ring
           "Special FireUnit" -> R.raw.special_unit
           "Standby For Message" -> R.raw.standby_for_message
           "Tones Only" -> R.raw.tones_only
        }
        return soundFile

    }

  /*  private fun updateLocalityData(response: Resource<LocalityResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    val localityList= response.data.data
                    val list= ArrayList(localityList?.data?:ArrayList())
                    localityListData.clear()
                    localityListData.addAll(list)
                    setupAdapter()

                }else{
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
        }
    }*/

    private fun setupAdapter() {
        if(soundList.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvSound.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvSound.gone()
        }

        binding.rvSound.setUpAdapter(
            soundList,
            R.layout.item_sound,
            ItemSoundBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvSound.text= it.name
                 if(it.isChecked){
                     prefs.soundName= it.name
                     bindingItem.tvSound.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_tick,0)
                 }else   bindingItem.tvSound.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)

                bindingItem.tvSound.setOnClickListener{ view->
                    var localCheck= it.isChecked
                    showSoundOptionsDialog(pos,!localCheck,it.name.toString())

                   /* soundList.forEach {
                        it.isChecked=false
                    }
                    it.isChecked= !localCheck
                    binding.rvSound.adapter?.notifyDataSetChanged()*/
                }
            }
        )
    }



    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }
    }

    // Method to show the dialog
    private fun showSoundOptionsDialog(pos: Int, isChecked: Boolean,name: String) {
        // Create an AlertDialog Builder
        val builder = AlertDialog.Builder(this)

        // Set the title of the dialog
        builder.setTitle("Sound Options")

        // Set the items for the dialog
        builder.setItems(arrayOf("Set Sound", "Preview Sound")) { dialog, which ->
            when (which) {
                0 -> {
                    // Handle "Set Sound" option
                    //Toast.makeText(this, "Set Sound option selected", Toast.LENGTH_SHORT).show()
                    // You can add code here to set the sound (e.g., opening a sound picker or settings)
                    onSettingSound(pos,isChecked)
                }
                1 -> {
                    // Handle "Preview Sound" option
                   // Toast.makeText(this, "Preview Sound option selected", Toast.LENGTH_SHORT).show()
                    // You can add code here to preview the sound (e.g., play sound preview)
                    playSound(name)
                    onSettingSound(pos,isChecked)
                }
            }
        }

        // Set a cancel button to dismiss the dialog
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()  // Simply dismiss the dialog when clicked
        }

        // Create the dialog and show it
        val dialog = builder.create()
        dialog.show()
    }


    fun onSettingSound(pos: Int,isChecked: Boolean){
        soundList.forEach {
            it.isChecked=false
        }

        soundList[pos].isChecked= isChecked
        binding.rvSound.adapter?.notifyDataSetChanged()
    }

    private fun playSound(name: String) {
        // Initialize MediaPlayer to play a sound from raw folder
        val mediaPlayer = MediaPlayer.create(this, returnSoundFile(name)) // sound_file is the name of the file in res/raw without extension

        // Start playing the sound
        mediaPlayer.start()

        // Optionally, set an OnCompletionListener to release the resources once the sound finishes
        mediaPlayer.setOnCompletionListener {
            it.release() // Release the MediaPlayer resources after sound completion
        }
    }

}