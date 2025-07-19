package com.pioneer.nycfirewire.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pioneer.nycfirewire.fragment.CommentsFragment
import com.pioneer.nycfirewire.fragment.FeedFilterFragment
import com.pioneer.nycfirewire.utils.NAV_COMMENT_LIST
import com.pioneer.nycfirewire.utils.replaceFragment
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.FeedFilterActivityBinding
import com.pioneer.nycfirewire.prefs
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FeedFilterOrCommentsActivity : AppCompatActivity() {

    private lateinit var binding: FeedFilterActivityBinding
    private val mTAG= FeedFilterOrCommentsActivity::class.java.canonicalName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= FeedFilterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initExtra()
    }

    private fun displayFragment(fragment: Fragment, flag:Boolean){
        replaceFragment(
            fragment,
            mTAG,
            allowStateLoss = true,
            containerViewId = R.id.fl_main,
            allowBackStack = flag
        )
    }

    private fun initExtra() {
        if(intent!=null){
            if(intent.hasExtra(NAV_COMMENT_LIST)){
                val data= intent.getStringExtra(NAV_COMMENT_LIST)
                displayFragment(CommentsFragment.newInstance(data as String),true)
            }else{
                displayFragment(FeedFilterFragment.newInstance(),false)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        prefs.isRecreate= true
        finish()
    }




}