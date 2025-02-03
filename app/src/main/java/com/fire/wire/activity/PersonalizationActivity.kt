package com.fire.wire.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fire.wire.R
import com.fire.wire.databinding.ActivityPersonalBinding

class PersonalizationActivity:AppCompatActivity() {

    private lateinit var binding: ActivityPersonalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPersonalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarLayout.tvTitle.text= getString(R.string.personalization)
        clickEvent()
    }

    private fun clickEvent() {
        binding.toolbarLayout.tvBack.setOnClickListener {
            finish()
        }

        binding.tvNotifications.setOnClickListener {
            val intent= Intent(this, NotificationLocalityActivity::class.java)
            startActivity(intent)
        }

        binding.tvFeed.setOnClickListener {
            val intent = Intent(this,FeedsActivity::class.java )
            startActivity(intent)
        }
    }

}