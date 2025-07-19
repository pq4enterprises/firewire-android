package com.pioneer.nycfirewire.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityPersonalBinding
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.Constants.FROM_START
import com.pioneer.nycfirewire.utils.Constants.NOTIFICATION_AREAS
import com.pioneer.nycfirewire.utils.Constants.PERSONALIZATION
import com.pioneer.nycfirewire.utils.Constants.USER_BASIC_USER
import com.pioneer.nycfirewire.utils.IntentUtils.FROM_ACCOUNT
import com.pioneer.nycfirewire.utils.IntentUtils.OTHER
import com.pioneer.nycfirewire.utils.Prefs
import com.pioneer.nycfirewire.utils.startNewActivity


class PersonalizationActivity: BaseActivity() {

    private lateinit var binding: ActivityPersonalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPersonalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarLayout.tvTitle.text= getString(R.string.personalization)
        clickEvent()
        initData()
    }

  /*  override fun onResume() {
        super.onResume()
        analyticMethod(PERSONALIZATION,"PersonalizationActivity")
    }*/

    private fun initData() {
       var isDarkModeOn = prefs.isDarkMode
        binding.swTheme.isChecked= isDarkModeOn
    }

    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }

        binding.tvNotifications.setOnClickListener {
            val intent= Intent(this, NotificationLocalityActivity::class.java)
            startActivity(intent)
        }

        binding.tvNotificationsSound.setOnClickListener{
            if(prefs.userRole==USER_BASIC_USER){
                moveToPaymentPage()
            }else{
                val intent = Intent(this, NotificationSoundActivity::class.java )
                startActivity(intent)
            }
        }

        binding.tvFeed.setOnClickListener {
            val intent = Intent(this, SelectAreaActivity::class.java )
            intent.putExtra(FROM_START,false)
            startActivity(intent)
        }

     binding.swTheme.setOnClickListener{ view,->
         BackgroundAudioService.stopService(this)
         prefs.feedMainPosition=-1
         prefs.feedSubPosition= -1

         if(prefs.isDarkMode){
             AppCompatDelegate
                 .setDefaultNightMode(
                     AppCompatDelegate
                         .MODE_NIGHT_NO);

             prefs.isDarkMode= false
             binding.swTheme.text = getString(R.string.enable_mode)

         }else{
             AppCompatDelegate
                 .setDefaultNightMode(
                     AppCompatDelegate
                         .MODE_NIGHT_YES);
             prefs.isDarkMode= true

             binding.swTheme.text = getString(R.string.enable_mode)
         }
       //  finish()
       // startNewActivity(MapsActivity::class.java)
     }
    }

    private fun moveToPaymentPage(){
        val intent = Intent(this, MyAccountActivity::class.java)
        intent.putExtra(FROM_ACCOUNT, OTHER)
        startActivity(intent)
    }
}