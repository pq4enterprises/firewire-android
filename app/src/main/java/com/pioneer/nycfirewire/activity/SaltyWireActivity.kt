package com.pioneer.nycfirewire.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.databinding.ActivitySaltyWireBinding
import com.pioneer.nycfirewire.databinding.ItemGridSaltBinding
import com.pioneer.nycfirewire.model.user.response.SaltyData
import com.pioneer.nycfirewire.model.user.response.SaltyWireResponse
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.REGISTRATION
import com.pioneer.nycfirewire.utils.Constants.SALTY_WIRE
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SaltyWireActivity : BaseActivity() {
    private lateinit var binding: ActivitySaltyWireBinding
    private lateinit var vm: FireWireViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySaltyWireBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)

       vm.getSaltyContents()
        vm.saltyLiveData.observe(this, Observer{
            getSaltyContentDetails(it)
        })
        clickEvent()
        binding.toolbar.tvTitle.text= getString(R.string.menu)
    }

    private fun clickEvent() {
        binding.toolbar.tvBack.setOnClickListener{
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        analyticMethod(SALTY_WIRE,"SaltyWireActivity")
    }


    private fun getSaltyContentDetails(response: Resource<SaltyWireResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
              var list= response.data?.data?.data
                setAdapter(ArrayList(list?: emptyList<SaltyData>()))
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                if(response.message==getString(R.string.token_expired)) {
                    startNewActivity(LoginNewActivity::class.java)
                }

            }
            else -> {}
        }
    }

    private fun setAdapter(data: ArrayList<SaltyData>) {

        binding.gvData.setUpAdapter(
            data,
            R.layout.item_grid_salt,
            ItemGridSaltBinding::inflate,
            { it,pos,bindingItem->
                Glide.with(this)
                    .load(it.url)
                    .into(bindingItem.ivLogo)

                bindingItem.tvLogoName.text= it.title
                bindingItem.cvLogo.setOnClickListener { view->
                    moveToLink(it.link)
                }
            },{}, manager = GridLayoutManager(this, 2))

    }
}