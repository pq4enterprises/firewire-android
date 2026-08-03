package com.pioneer.nycfirewire.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.activity.FeedsActivity
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.locality.Locality
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.BottomSheetFeedFilterBinding
import com.pioneer.nycfirewire.databinding.ItemSubFilterItemBinding
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.COMMENTS_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.FEED_FILTER_FRAGMENT
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class FeedFilterFragment : Fragment() {

    private lateinit var binding: BottomSheetFeedFilterBinding
    private var callback: ReplaceCallback? = null
    private lateinit var vm: FireWireViewModel
    private var localityListData = ArrayList<Locality>()

    companion object {
        fun newInstance() = FeedFilterFragment().putArgs { }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? ReplaceCallback
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetFeedFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        clickEvent()
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getLocalityList(Constants.TYPE_AREA)
        vm.localityLiveData.observe(viewLifecycleOwner) {
            updateFilterData(it)
        }

    }

    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, FEED_FILTER_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "FeedFilterFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    private fun updateFilterData(response: Resource<LocalityResponse>?) {
        when (response?.state) {
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if (response.data?.code == Constants.CODE_SUCCESS) {
                    val localityList = response.data.data
                    val list = ArrayList(localityList?.data ?: ArrayList())
                    localityListData.clear()
                    localityListData.addAll(list)
                    setupAdapter()

                } else {
                    showAlert(response.data?.message.toString())
                }


            }
            ResourceState.ERROR -> {
                binding.progress.gone()

            }
            else -> {}
        }
    }

    private fun setupAdapter() {
        // The saved selection arrives on the response itself: the server sets
        // isChecked on every locality and subLocality from the caller's stored
        // UserLocality rows. The old checkSelectedData() cross-referenced a local
        // JSON cache in prefs.filterData instead — but the code that populated that
        // cache (saveData/getData) was commented out, so it looped over a
        // permanently empty list and did nothing at all.
        if (localityListData.isNotEmpty()) {
            binding.tvNoData.gone()
            binding.rvSubFilter.visible()
        } else {
            binding.tvNoData.visible()
            binding.rvSubFilter.gone()
        }


        binding.rvSubFilter.setUpAdapter(
            localityListData,
            R.layout.item_sub_filter_item,
            ItemSubFilterItemBinding::inflate,
            { it, pos, bindingItem ->

                bindingItem.cbCountry.isChecked = it.isChecked
                bindingItem.tvSubCountry.text = it.name
                bindingItem.llCb.setOnClickListener { view ->
                    it.isChecked = !it.isChecked
                    binding.rvSubFilter.adapter?.notifyItemChanged(pos)
                }

            }
        )
    }

    private fun clickEvent() {
        binding.tvSelectAll.setOnClickListener {
            // was localityListData.map { it.isChecked }, which reads the flag
            // and discards the result — the control never did anything
            localityListData.forEach { it.isChecked = true }
            binding.rvSubFilter.adapter?.notifyDataSetChanged()
        }

        binding.tvDone.setOnClickListener {

            val localityId = ArrayList<String>()
            localityId.addAll(localityListData.filter { it.isChecked }.map { it._id ?: "" })

            val intent = Intent(requireContext(), FeedsActivity::class.java )
            intent.putStringArrayListExtra("LocalityId",localityId)
            startActivity(intent)
            activity?.finish()
        }
    }



    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }

}