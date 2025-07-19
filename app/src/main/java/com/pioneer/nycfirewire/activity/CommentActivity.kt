package com.pioneer.nycfirewire.activity

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityCommentBinding
import com.pioneer.nycfirewire.fragment.CommentsFragment
import com.pioneer.nycfirewire.utils.replaceFragment
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CommentActivity :  BaseActivity() {

    private lateinit var binding: ActivityCommentBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var vm: FireWireViewModel
    private val mTAG= CommentActivity::class.java.canonicalName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomConstraint)
        bottomSheetBehavior.isDraggable=true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

      //  displayFragment(CommentsFragment.newInstance(data as String),true)
    }

}