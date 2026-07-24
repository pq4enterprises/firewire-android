package com.fire.wire.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.adapter.setUpAdapter
import com.fire.wire.databinding.*
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.Comment
import com.fire.wire.model.incident.response.CommentsResponse
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.prefs
import com.fire.wire.resource.Resource
import com.fire.wire.resource.ResourceState
import com.fire.wire.utils.*
import com.fire.wire.utils.Constants.INCIDENT_ID
import com.fire.wire.viewModel.FireWireViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody


@AndroidEntryPoint
class CommentsFragment: Fragment() ,ImageDataListener {
    private lateinit var binding: BottomSheetCommentBinding
    private var callBack: ReplaceCallback?=null
    private lateinit var vm:FireWireViewModel
    var incidentId=""
    var imageString=""

    companion object{
        fun newInstance(incidentId: String) = CommentsFragment().putArgs {
            putString(INCIDENT_ID,incidentId)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callBack= context as? ReplaceCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initExtra()
        initViewModel()
        clickEvents()

    }

    private fun updateCommentsData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code=="comment_added") {
                   showSnackbar(binding.cvComment,response.data.message.toString())
                    binding.etComment.setText("")
                    binding.ivPhoto.setImageResource(R.drawable.fw_ic_camera)
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun updateUploadedImage(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                if(response.data?.code== Constants.CODE_SUCCESS) {
                    imageString= response.data.data?.url?.get(0) ?: ""
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun clickEvents() {
        // design screen 10 · COMMENTS: in-screen BACK affordance mirrors system back
        binding.tvBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.ivPhoto.setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment(this)
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }
        binding.ivSend.setOnClickListener {
            val type="comment"
            val comment= binding.etComment.text.toString()

            when{
                comment.isEmpty()-> showSnackbar(it,getString(R.string.enter_comment))
                else->{
                    val commentRequest= AddCommentRequest(
                        prefs.userId.toString(), incidentId,type,comment,imageString)
                    vm.postComment(commentRequest)
                }
            }

        }
    }

    private fun initExtra() {
        incidentId= arguments?.getString(INCIDENT_ID)?:""
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getCommentsList(incidentId)
        vm.commentsLiveData.observe(viewLifecycleOwner, Observer {
             updateCommentsList(it)
        })
        vm.imageUploadLiveData.observe(viewLifecycleOwner, Observer {
            updateUploadedImage(it)
        })

        vm.addCommentLiveData.observe(viewLifecycleOwner, Observer {
            updateCommentsData(it)
        })

    }

    private fun updateCommentsList(response: Resource<CommentsResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                try {
                    binding.progress.gone()
                    val list= response.data?.data?.data!!
                    val totalCommentList = ArrayList(list)
                    if (totalCommentList.size > 1)
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comments,totalCommentList.size.toString())
                    else
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comment,totalCommentList.size.toString())

                    setupAdapter(totalCommentList)
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun setupAdapter(commentsList: ArrayList<Comment>) {
        if(commentsList.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvMainFilter.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvMainFilter.gone()
        }

        binding.rvMainFilter.setUpAdapter(
            commentsList,
            R.layout.item_comment_bottom,
            ItemCommentBottomBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvProfileName.text= it.userId?.firstName.plus(" ").plus(it.userId?.lastName)
                bindingItem.tvComments.text= it.comment

                // timestamp comes back from the comments API (same field iOS shows)
                val time= it.createdAt?.takeIf { t-> t.isNotEmpty() }
                    ?.let { t-> DateUtils.getFormattedDateOfFireWire(t) } ?: ""
                bindingItem.tvDateTime.text= time
                if(time.isEmpty()) bindingItem.tvDateTime.gone() else bindingItem.tvDateTime.visible()

                // replies arrive nested on the list response; display-only until
                // reply composing is wired on Android
                val replyCount= it.replies?.size ?: 0
                when{
                    replyCount==1 -> {
                        bindingItem.tvReplies.text= getString(R.string.fw_reply_count)
                        bindingItem.tvReplies.visible()
                    }
                    replyCount>1 -> {
                        bindingItem.tvReplies.text= getString(R.string.fw_replies_count,replyCount.toString())
                        bindingItem.tvReplies.visible()
                    }
                    else -> bindingItem.tvReplies.gone()
                }

                if(!it.userId?.img.isNullOrEmpty())
                Glide.with(this)
                    .load(it.userId?.img)
                    .into(bindingItem.profileImage)
                else
                    // recycled row: clear any stale avatar so the red-tint
                    // person fallback underneath shows through
                    bindingItem.profileImage.setImageDrawable(null)

                if(it.img?.isNotEmpty() == true) bindingItem.rvImages.visible() else bindingItem.rvImages.gone()

                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)

                val imageList= ArrayList(it.img?:ArrayList())
                bindingItem.rvImages.setUpAdapter(
                    imageList,
                    R.layout.item_image,
                    ItemImageBinding::inflate,
                    { it1,pos1,subBindingItem->
                        Glide.with(this)
                            .load(it1)
                            .into(subBindingItem.ivComment)
                    },{},layoutManager

                )
            })
    }

    fun showAlert(message: String? = "") {
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .show()
    }

    override fun getImageData(uri: Uri) {
        binding.ivPhoto.setImageURI(uri)
        uploadImageData(uri)
    }

    private fun uploadImageData(uri: Uri) {
        val file = AppUtils.getImageFileFromUri(requireContext(), uri)
        if(file!=null){
            val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            vm.uploadImage(part)
        }
    }

    private fun showSnackbar(view: View, message: String) {
        // Use the view from the fragment's layout to show the Snackbar
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}