package com.fire.wire.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.fire.wire.R
import com.fire.wire.databinding.FeedFilterActivityBinding
import com.fire.wire.fragment.CommentsFragment
import com.fire.wire.fragment.FeedFilterFragment
import com.fire.wire.utils.NAV_COMMENT_LIST
import com.fire.wire.utils.replaceFragment
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class FeedFilterActivity : BaseActivity() {

    private lateinit var binding: FeedFilterActivityBinding
    private val mTAG= FeedFilterActivity::class.java.canonicalName

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
        finish()
    }


}