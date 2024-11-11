package com.fire.wire.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.fire.wire.databinding.ActivityLoginBinding
import com.fire.wire.databinding.ActivitySplashBinding
import com.fire.wire.prefs
import com.fire.wire.utils.startNewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_TIME_OUT = 1000L
    }

    //private lateinit var binding: ActivitySplashBinding
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //binding = ActivitySplashBinding.inflate(layoutInflater)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }


  /*  override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
           // if(!prefs.isLogin) startNewActivity(LoginActivity::class.java) else startNewActivity(DashboardActivity::class.java)

        }, SPLASH_TIME_OUT)

    }*/


}