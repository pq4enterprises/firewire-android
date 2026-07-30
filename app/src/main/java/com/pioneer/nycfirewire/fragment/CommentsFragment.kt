package com.pioneer.nycfirewire.fragment

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.pioneer.nycfirewire.ReplaceCallback
import com.pioneer.nycfirewire.adapter.setUpAdapter
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.response.Comment
import com.pioneer.nycfirewire.model.incident.response.CommentsResponse
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants.INCIDENT_ID
import com.pioneer.nycfirewire.viewModel.FireWireViewModel
import com.google.android.material.snackbar.Snackbar
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.databinding.BottomSheetCommentBinding
import com.pioneer.nycfirewire.databinding.ItemCommentBottomBinding
import com.pioneer.nycfirewire.databinding.ItemImageBinding
import com.pioneer.nycfirewire.databinding.ItemReplyContentBinding
import com.pioneer.nycfirewire.listener.OnLoadMoreListener
import com.pioneer.nycfirewire.listener.RecyclerViewLoadMoreScroll
import com.pioneer.nycfirewire.model.incident.request.FeatureImageSetRequest
import com.pioneer.nycfirewire.model.incident.request.ReportCommentRequest
import com.pioneer.nycfirewire.model.incident.request.mainCommentRequest
import com.pioneer.nycfirewire.utils.AppUtils
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.USER_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUB_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUPER
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.UiUtils.commentBindColor
import com.pioneer.nycfirewire.utils.UiUtils.setReplyText
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.utils.Constants.COMMENTS_FRAGMENT


@AndroidEntryPoint
class CommentsFragment: Fragment() , ImageDataListener {
    private lateinit var binding: BottomSheetCommentBinding
    private var callBack: ReplaceCallback?=null
    private lateinit var vm: FireWireViewModel
    var incidentId=""
    var commentId=""
    var clickedCommentImage=""
    var imageString=""
    var offset = 1
    var limit=10
    var commentsList = ArrayList<Comment>()
    var isCommand=false
    var imageUri:Uri?=null
    var isLastPage = false
    var parentId=""
    var mentions= ArrayList<String>()
    var isMainReply=false

    var mainPrefix=""
    var replyClickedPosition=-1

    private lateinit var scrollListener: RecyclerViewLoadMoreScroll
    // Track which parent comments are expanded
    private val expandedComments = mutableSetOf<String>()


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
        println("commentsFragment: entered")
        initExtra()
        initViewModel()
        clickEvents()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            binding.cvComment.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = if (imeHeight > 0) imeHeight else navBarHeight
            }
            insets
        }

    }

    override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, COMMENTS_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "CommentsFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    private fun updateCommentsData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                mentions.clear()
                binding.progress.gone()
                imageString=""
                imageUri=null
                if(response.data?.code=="comment_added") {
                   showSnackbar(binding.cvComment,response.data.message.toString())
                    binding.etComment.setText("")
                    // must match the layout's icon; resetting to ic_camera left
                    // the composer showing the pre-redesign glyph after posting
                    binding.ivPhoto.setImageResource(R.drawable.fw_ic_camera)
                    if(isMainReply) {
                        isMainReply=false
                        isLastPage= false
                    }
                    offset=1
                    vm.getCommentsList(incidentId,1,10)
//                    vm.commentsLiveData.observe(viewLifecycleOwner, Observer {
//                        updateAddedCommentsInList(it)
//                    })
                    binding.cvComment.visible()
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                binding.cvComment.visible()
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
                    val type="comment"
                    var comment= binding.etComment.text.toString()

                    if(isMainReply){
                        comment = if (comment.startsWith(mainPrefix)) {
                            comment.removePrefix(mainPrefix).trim()
                        } else {
                            comment.trim()
                        }
                    }

                    val validationCommentText=  if(comment.startsWith(mainPrefix)) comment.removePrefix(mainPrefix).trim() else comment

                    if (validationCommentText.isNotEmpty()) {
                        var imgArray = ArrayList<String>()
                        imgArray.add(imageString)

                        if (parentId.isNullOrEmpty()) {
                            val commentRequest = mainCommentRequest(
                                prefs.userId.toString(),
                                incidentId,
                                type,
                                comment,
                                imgArray,
                                mentions
                            )
                            vm.postMainComment(commentRequest)
                        } else {
                            val commentRequest = AddCommentRequest(
                                prefs.userId.toString(),
                                incidentId,
                                type,
                                comment,
                                imgArray,
                                mentions,
                                parentId
                            )
                            vm.postComment(commentRequest)
                        }
                    }else { showSnackbar(binding.cvComment, "Kindly enter comment") }

                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.cvComment.visible()
                binding.progress.gone()
                showAlert(response.message)
            }
        }
    }

    private fun clickEvents() {
        binding.ivPhoto.setOnClickListener {
                val bottomSheetFragment = BottomSheetFragment(this)
                bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }
        binding.ivSend.setOnClickListener {
                var comment = binding.etComment.text.toString()
                if(isMainReply){
                    comment = if (comment.startsWith(mainPrefix)) {
                        comment.removePrefix(mainPrefix).trim()
                    } else {
                        comment.trim()
                    }
                }

              val validationCommentText=  if(comment.startsWith(mainPrefix)) comment.removePrefix(mainPrefix).trim() else comment

              if(validationCommentText.isNotEmpty()) {
                  if (imageUri != null && !imageUri.toString().isNullOrEmpty()) {
                      uploadImageData(imageUri!!)
                      binding.cvComment.gone()
                  } else if (validationCommentText.isNotEmpty()) {
                      val type = "comment"

                      var imgArray = ArrayList<String>()

                      if (parentId.isNullOrEmpty()) {
                          val commentRequest = mainCommentRequest(
                              prefs.userId.toString(),
                              incidentId,
                              type,
                              comment,
                              imgArray,
                              mentions
                          )
                          vm.postMainComment(commentRequest)
                      } else {
                          val commentRequest = AddCommentRequest(
                              prefs.userId.toString(),
                              incidentId,
                              type,
                              comment,
                              imgArray,
                              mentions,
                              parentId
                          )
                          vm.postComment(commentRequest)
                      }

                      binding.cvComment.gone()
                  } else {
                      showSnackbar(binding.cvComment, "Kindly enter comment")
                  }
              }else{
                  showSnackbar(binding.cvComment, "Kindly enter comment")
              }

        }
        binding.tvBack.setOnClickListener{
            prefs.isRecreate=true
            activity?.finish()
        }
    }

    private fun initExtra() {
        incidentId= arguments?.getString(INCIDENT_ID)?:""
    }

    private fun initViewModel() {
        vm = ViewModelProvider(this).get(FireWireViewModel::class.java)
        vm.getCommentsList(incidentId,offset,limit)
        vm.commentsLiveData.observe(viewLifecycleOwner, Observer {
            updateCommentsList(it)
        })
        vm.imageUploadLiveData.observe(viewLifecycleOwner, Observer {
            updateUploadedImage(it)
        })

        vm.addCommentLiveData.observe(viewLifecycleOwner, Observer {
            try {
                updateCommentsData(it)
            }catch (e: Exception){
                e.printStackTrace()
            }

        })

        vm.featureImageSetLiveData.observe(viewLifecycleOwner, Observer{
            updateFeatureImage(it)
        })

        vm.reportCommentLiveData.observe(viewLifecycleOwner, Observer{
            updateReport(it,false)
        })

        vm.deleteCommentLiveData.observe(viewLifecycleOwner, Observer{
            updateReport(it,true)
        })

    }

    private fun updateReport(response: Resource<CommonResponse>, isDelete:Boolean) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()

                if(response.data?.code== Constants.CODE_SUCCESS) {
                    if (isDelete){
                        showToast(requireContext(),getString(R.string.comment_deleted_suucessfully))
                        offset=1
                        vm.getCommentsList(incidentId,1,10)
                    }else{
                        showToast(requireContext(),getString(R.string.comment_reported_suucessfully))
                    }
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                binding.cvComment.visible()
            }
        }
    }

    private fun updateFeatureImage(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()

                if(response.data?.code== Constants.CODE_SUCCESS) {
                    offset=1
                    vm.getCommentsList(incidentId,1,10)
                     showToast(requireContext(),getString(R.string.update_successfuly))
                }else{
                    showAlert(response.data?.message.toString())
                }
            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                showAlert(response.message)
                binding.cvComment.visible()
            }
        }
    }


    private fun updateCommentsList(response: Resource<CommentsResponse>) {
        when(response.state){
            ResourceState.LOADING -> if (offset == 1)
                binding.progress.visible()
            else
                binding.rvProgress.visible()
            ResourceState.SUCCESS -> {
                try {
                    val list= response.data?.data?.data!!

                    if(isCommand){
                        val commandUpdatedList = list.filter { it._id== parentId }
                        if(commandUpdatedList.isEmpty()){
                            offset= offset++
                            vm.getCommentsList(incidentId,offset,10)
                        }else {
                            val updatedItem = commandUpdatedList.firstOrNull() ?: return
                            val index = commentsList.indexOfFirst { it._id == updatedItem._id }
                            if (index != -1) {
                                commentsList[index] = updatedItem
                                expandedComments.add(updatedItem._id ?: "")
                                binding.rvMainFilter.adapter?.notifyItemChanged(index)
                            }
                            parentId = ""
                            isCommand=false
                            binding.progress.gone()
                            binding.rvProgress.gone()
                        }

                    }else{
                        if (list.isNotEmpty()) {
                            binding.progress.gone()
                            binding.rvProgress.gone()
                            if(list.isNotEmpty()){
                                if(offset==1) commentsList.clear()
                                commentsList.addAll(list)
                            }
                            if (binding.rvMainFilter.adapter == null) {
                                setupAdapter()
                            } else {
                                binding.rvMainFilter.adapter?.notifyDataSetChanged()
                            }

                            rvBasedSetup(commentsList)

                            var pageInfo= response.data.data?.pageInfo?.totalCount?.toInt()!!

                            if (pageInfo > 1)
                                binding.tvTotalComments.text =
                                    requireContext().getString(R.string.comments,pageInfo.toString())
                            else
                                binding.tvTotalComments.text =
                                    requireContext().getString(R.string.comment,pageInfo.toString())

                            prefs.commentCount= pageInfo.toString()
                            offset++
                        } else {
                            if (offset >= 2) {
                                isLastPage = true   // ✅ stop future loads
                                binding.rvProgress.gone()
                                binding.progress.gone()
                            }else{
                                binding.progress.gone()
                                rvBasedSetup(list)
                            }
                        }

                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            ResourceState.ERROR -> {
                binding.progress.gone()
                binding.rvProgress.gone()
                showAlert(response.message)
                if (response.message == getString(R.string.token_expired)) {
                    startNewActivity(LoginNewActivity::class.java)
                }

            }
        }
    }


    private fun rvBasedSetup(list: List<Comment>) {
        if(list.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvMainFilter.visible()
        }else{
            if(offset==1){
                binding.tvNoData.visible()
                binding.rvMainFilter.gone()
            }
        }

        if(list.isNotEmpty()) scrollListener.setLoaded()
    }

    private fun showCommentBox(){
        binding.etComment.requestFocus()
        // Show keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
    }
   private fun setupAdapter() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMainFilter.setHasFixedSize(true)

        binding.rvMainFilter.setUpAdapter(
            commentsList,
            R.layout.item_comment_bottom,
            ItemCommentBottomBinding::inflate,
            { item, pos, bindingItem ->

                // --- Toggle replies visibility ---
                bindingItem.tvMainTotalReplies.setOnClickListener {
                    bindingItem.rvReplies.visible()
                    bindingItem.tvHideReply.visible()
                    expandedComments.add(item._id ?: "")
                }

                bindingItem.tvHideReply.setOnClickListener {
                    bindingItem.rvReplies.gone()
                    bindingItem.tvHideReply.gone()
                    expandedComments.remove(item._id ?: "")
                }

                // --- Show replies count and setup nested replies ---
                if (item.replies?.isNotEmpty() == true) {
                    bindingItem.tvMainTotalReplies.visible()
                    val replyCount = item.replies?.size ?: 0
                    bindingItem.tvMainTotalReplies.text =
                        if (replyCount > 1) getString(R.string.fw_replies_count, replyCount.toString())
                        else getString(R.string.fw_reply_count)

                    bindingItem.rvReplies.setUpAdapter(
                        item.replies as ArrayList,
                        R.layout.item_reply_content,
                        ItemReplyContentBinding::inflate,
                        { replyItem, _, replyBinding ->

                            // --- Bind reply data ---
                            replyBinding.tvProfileNameReply.text = replyItem.userId?.firstName + " " + replyItem.userId?.lastName
                            commentBindColor(replyItem.comment.toString(), requireContext(), replyBinding.tvReplyComments)

                            if (!replyItem.createdAt.isNullOrEmpty())
                                replyBinding.tvDateTimeReply.text = DateUtils.formatDateTime(replyItem.createdAt.toString())

                            // see the main-comment overflow below — same reasoning
                            replyBinding.ivMenuDotReply.visible()

                            replyBinding.ivMenuDotReply.setOnClickListener{ view->
                                commentId= replyItem._id.toString()
                                val imageRFilterList = replyItem.img?.filter { it.isNotBlank() }
                                val filterRImg= ArrayList(imageRFilterList?: java.util.ArrayList())
                                if (!filterRImg.isNullOrEmpty()) clickedCommentImage = filterRImg.get(0).toString() else clickedCommentImage=""
                                showCustomDialog(replyItem.featuredImage, replyItem.userId?._id == prefs.userId)
                            }


                            // --- Reply click (nested reply) ---
                            replyBinding.tvReplyTextSub.setOnClickListener {
                                isMainReply = false
                                isCommand = true
                                replyClickedPosition = pos
                                parentId = replyItem.parentId.toString()
                                setReplyText(binding.etComment, replyItem.userId?.firstName.toString(), requireContext())
                                showCommentBox()
                            }

                            // --- Profile image ---
                            val imageRFilterList = replyItem.img?.filter { it.isNotBlank() }
                            val filterRImg= ArrayList(imageRFilterList?: java.util.ArrayList())
                            if(filterRImg.isNotEmpty()) replyBinding.rvReplyImages.visible() else replyBinding.rvReplyImages.gone()
                            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)

                            if(!replyItem.userId?.img.isNullOrEmpty()) {
                                Glide.with(this)
                                    .load(replyItem.userId?.img ?: "")
                                    .into(replyBinding.ivProfileImageReply)
                            } else {
                                Glide.with(this).clear(replyBinding.ivProfileImageReply)
                                replyBinding.ivProfileImageReply.setImageDrawable(null)
                            }

                            replyBinding.rvReplyImages.setUpAdapter(
                                filterRImg,
                                R.layout.item_image,
                                ItemImageBinding::inflate,
                                { it1,pos1,replyBindingItem->
                                    replyBindingItem.previewImgCv.setOnClickListener{
                                        ImageDialogFragment.newInstance(it1).show(childFragmentManager, "image_dialog")
                                    }

                                    if(it1.isNotEmpty()) {
                                        Glide.with(this)
                                            .load(it1)
                                            .into(replyBindingItem.ivComment)
                                    }else replyBindingItem.ivComment.gone()

                                },{},layoutManager

                            )


                        }, {}, LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    )

                    bindingItem.viewReplyLine.visible()
                    bindingItem.viewComment.gone()
                } else {
                    bindingItem.tvMainTotalReplies.gone()
                    bindingItem.rvReplies.gone()
                    bindingItem.tvHideReply.gone()
                    // both dividers set explicitly: on a recycled row the
                    // previous comment may have left view_comment hidden
                    bindingItem.viewReplyLine.gone()
                    bindingItem.viewComment.visible()
                }

                // --- Restore expanded/collapsed state ---
                if (expandedComments.contains(item._id)) {
                    bindingItem.rvReplies.visible()
                    bindingItem.tvHideReply.visible()
                } else {
                    bindingItem.rvReplies.gone()
                    bindingItem.tvHideReply.gone()
                }

                // --- Main comment actions ---
                bindingItem.tvMainReply.setOnClickListener {
                    isCommand = false
                    isMainReply = true
                    replyClickedPosition = pos
                    parentId = item._id.toString()
                    setReplyText(binding.etComment, item.userId?.firstName.toString(), requireContext())
                    mainPrefix= binding.etComment.text.toString()
                    showCommentBox()
                }

                // --- Bind main comment data ---
                bindingItem.tvProfileName.text = item.userId?.firstName + " " + item.userId?.lastName
                bindingItem.tvComments.text = item.comment
                // the overflow is for everyone: report is the non-admin action,
                // and showCustomDialog gates delete / featured image by role.
                // The admin check this replaces had no else branch, so it never
                // hid anything — both row layouts already default to visible.
                bindingItem.ivMenuDot.visible()

                bindingItem.ivMenuDot.setOnClickListener{ view->
                    commentId= item._id.toString()
                    if (!item.img.isNullOrEmpty()) clickedCommentImage = item.img?.get(0).toString() else clickedCommentImage=""
                    showCustomDialog(item.featuredImage, item.userId?._id == prefs.userId)
                }


                if (!item.createdAt.isNullOrEmpty())
                    bindingItem.tvDateTime.text = DateUtils.formatDateTime(item.createdAt.toString())

                // --- Profile image ---
                if(!item.userId?.img.isNullOrEmpty()){
                    Glide.with(this)
                        .load(item.userId?.img)
                        .into(bindingItem.profileImage)}else{
                    // recycled row: drop any stale avatar so the red-tint person
                    // fallback underneath shows through
                    Glide.with(this).clear(bindingItem.profileImage)
                    bindingItem.profileImage.setImageDrawable(null)
                }

                val imageFilterList = item.img?.filter { it.isNotBlank() }
                val filterImg= ArrayList(imageFilterList?: java.util.ArrayList())
                if(filterImg.isNotEmpty()) bindingItem.rvImages.visible() else bindingItem.rvImages.gone()
                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)

                bindingItem.rvImages.setUpAdapter(
                    filterImg,
                    R.layout.item_image,
                    ItemImageBinding::inflate,
                    { it1,pos1,subBindingItem->
                        subBindingItem.previewImgCv.setOnClickListener{
                            ImageDialogFragment.newInstance(it1).show(childFragmentManager, "image_dialog")
                        }

                        if(it1.isNotEmpty()) {
                            Glide.with(this)
                                .load(it1)
                                .into(subBindingItem.ivComment)
                        }else subBindingItem.ivComment.gone()

                    },{},layoutManager

                )

            }, {}, layoutManager
        )

        // --- Load More Scroll Listener ---
        scrollListener = RecyclerViewLoadMoreScroll(layoutManager)
        scrollListener.setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore() {
                if (!isLastPage) {
                    binding.rvProgress.visible()
                    vm.getCommentsList(incidentId, offset, limit)
                } else {
                    binding.rvProgress.gone()
                    binding.progress.gone()
                }
            }
        })
        binding.rvMainFilter.addOnScrollListener(scrollListener)
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
        imageUri= uri
        binding.ivPhoto.setImageURI(uri)
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
        hideKeyboard()

        // Create the Snackbar
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)

        // Optional: Set the anchor view to avoid blocking UI elements like BottomNavigationView or BottomSheet
        val bottomNavigationView = view.findViewById<View>(R.id.bottom_constraint)  // Replace with your bottom view ID
        if (bottomNavigationView != null) {
            snackbar.setAnchorView(bottomNavigationView)
        }

        // Optional: Adjust Snackbar's bottom margin to avoid overlap with the system navigation gestures
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.bottomMargin = 100  // Adjust this as necessary for your layout
        snackbar.setAction("OK",{snackbar.dismiss()})
        snackbar.setActionTextColor(requireContext().getColor(R.color.app_red))

        snackbarView.layoutParams = params
        snackbar.show()
    }
    fun Fragment.hideKeyboard() {
        // Get the current focused view (usually an EditText)
        val view = activity?.currentFocus

        // If the view is not null, hide the keyboard
        view?.let {
            val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }


    private fun showCustomDialog(isfeatureImage: Boolean, isOwnComment: Boolean) {
        // Create a dialog builder
        val dialogBuilder = AlertDialog.Builder(requireContext())

        // Inflate the custom dialog layout
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_comment_report, null)
        dialogBuilder.setView(dialogView)

        // Set up the image, comment, and buttons in the dialog
        val imageView: ImageView = dialogView.findViewById(R.id.iv_close)
        val cancelText: TextView = dialogView.findViewById(R.id.button_cancel)
        val buttonReport: Button = dialogView.findViewById(R.id.btn_report_comment)
        val buttonDelete: Button = dialogView.findViewById(R.id.btn_delete_comment)
        val buttonImageSet: Button = dialogView.findViewById(R.id.btn_feature_image)



        val isModerator = prefs.userRole==USER_ADMIN || prefs.userRole==USER_SUPER || prefs.userRole==USER_SUB_ADMIN
        if(isModerator) {
            if(clickedCommentImage.isNotEmpty()) buttonImageSet.visible() else buttonImageSet.gone()
            buttonDelete.visible()
        }else{
            buttonImageSet.gone()
            // authors can remove their own comment; everyone else still cannot
            if(isOwnComment) buttonDelete.visible() else buttonDelete.gone()
        }
        // reporting your own comment is not a thing
        if(isOwnComment) buttonReport.gone() else buttonReport.visible()


        if(isfeatureImage){
            clickedCommentImage=""
            buttonImageSet.setText(getString(R.string.fw_remove_featured_image))
        }else buttonImageSet.setText(getString(R.string.fw_set_featured_image))

        // Set up dialog
        val alertDialog = dialogBuilder.create()
        // let the card drawable supply the surface and rounded corners instead
        // of the platform dialog background showing through behind them
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

      cancelText.setOnClickListener{
          alertDialog.dismiss()
      }

        buttonDelete.setOnClickListener{
            alertDialog.dismiss()
            vm.deleteComment(commentId)
        }

        buttonReport.setOnClickListener {
             alertDialog.dismiss()
            var request= ReportCommentRequest(prefs.userId)
            vm.reportComment(request,commentId)
        }
        buttonImageSet.setOnClickListener{
            alertDialog.dismiss()
            var request= FeatureImageSetRequest(clickedCommentImage,commentId)
            vm.featureImageSetOrRemove(request,incidentId)
        }

        // Show the dialog
        alertDialog.show()
    }



}