package com.pioneer.nycfirewire.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySystemWindowInsets()


        initExtra()
    }

    /**
     * This is the one screen container that does not extend BaseActivity, so it
     * went edge-to-edge via setDecorFitsSystemWindows without ever applying the
     * matching insets. The result was content drawn under the status bar: the
     * comments BACK control sat beneath the system clock and could not be
     * tapped, and the feed filter header had the same overlap. Mirrors
     * BaseActivity.applySystemWindowInsets, including using the larger of the
     * navigation-bar and keyboard bottom inset so the comment composer clears
     * the IME.
     */
    private fun applySystemWindowInsets() {
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, imeInsets.bottom)
            )
            insets
        }
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