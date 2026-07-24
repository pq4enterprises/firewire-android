package com.fire.wire.fragment

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.fire.wire.R
import com.fire.wire.ReplaceCallback
import com.fire.wire.adapter.Kadapter
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
import java.util.regex.Pattern


@AndroidEntryPoint
class CommentsFragment: Fragment() ,ImageDataListener {

    /** One visible row: a top-level comment (depth 0) or a reply (depth 1). */
    private data class CommentRow(
        val comment: Comment,
        val depth: Int,
        /** _id of the top-level comment this row belongs to (== own id at depth 0). */
        val topLevelId: String
    )

    private lateinit var binding: BottomSheetCommentBinding
    private var callBack: ReplaceCallback?=null
    private lateinit var vm:FireWireViewModel
    var incidentId=""
    var imageString=""

    // full nested list from the API + the flattened rows currently displayed
    private var commentsList= ArrayList<Comment>()
    private val displayRows= ArrayList<CommentRow>()
    private var commentsAdapter: Kadapter<CommentRow, ItemCommentBottomBinding>?=null

    // top-level comment ids whose replies are shown (iOS: groups start collapsed)
    private val expandedComments= HashSet<String>()

    // reply state (iOS CommentsViewController: selectedParentID + mentions)
    private var replyParentId: String?=null
    private var replyMentionUserId: String?=null
    // parent to auto-expand + reveal once the refreshed list arrives
    private var pendingExpandParentId: String?=null

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
                    imageString=""
                    // reveal the reply we just posted once the list refreshes
                    pendingExpandParentId= replyParentId
                    clearReplyState()
                    hideKeyboard()
                    // iOS re-fetches the list after a successful post
                    vm.getCommentsList(incidentId)
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
            val comment= binding.etComment.text.toString().trim()

            when{
                comment.isEmpty()-> showSnackbar(it,getString(R.string.enter_comment))
                else->{
                    // same endpoint for comments and replies; a reply carries
                    // parentId + mentions (iOS APIPayload.addComment)
                    val commentRequest= AddCommentRequest(
                        prefs.userId.toString(), incidentId,type,comment,imageString,
                        parentId = replyParentId,
                        mentions = replyMentionUserId?.let { id-> listOf(id) }
                    )
                    vm.postComment(commentRequest)
                }
            }

        }

        // iOS resets the pending reply when the composer is emptied
        binding.etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(s.isNullOrBlank()) clearReplyState()
            }
        })
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
                    commentsList = ArrayList(list)
                    if (commentsList.size > 1)
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comments,commentsList.size.toString())
                    else
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comment,commentsList.size.toString())

                    // reveal the thread a just-posted reply belongs to
                    pendingExpandParentId?.let { expandedComments.add(it) }
                    pendingExpandParentId= null

                    setupAdapter()
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

    /** Flattens top-level comments + the replies of expanded threads
     *  (the list API already returns replies nested, like iOS consumes). */
    private fun rebuildDisplayRows() {
        displayRows.clear()
        commentsList.forEach { comment->
            val id= comment._id ?: return@forEach
            displayRows.add(CommentRow(comment, 0, id))
            if(expandedComments.contains(id)){
                comment.replies?.forEach { reply->
                    displayRows.add(CommentRow(reply, 1, id))
                }
            }
        }
    }

    private fun toggleReplies(topLevelId: String) {
        if(!expandedComments.remove(topLevelId)) expandedComments.add(topLevelId)
        rebuildDisplayRows()
        commentsAdapter?.notifyDataSetChanged()
    }

    /** REPLY tapped: pre-fill the composer with the @mention and remember
     *  parentId + mentioned user (iOS FWCommentCell.replyAction). Replying to
     *  a reply threads under the same top-level parent, exactly like iOS. */
    private fun startReply(row: CommentRow) {
        replyParentId= row.topLevelId
        replyMentionUserId= row.comment.userId?._id
        val firstName= row.comment.userId?.firstName.orEmpty()
        binding.etComment.setText(getString(R.string.fw_mention_prefix, firstName))
        binding.etComment.setSelection(binding.etComment.text?.length ?: 0)
        binding.etComment.requestFocus()
        val imm= requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun clearReplyState() {
        replyParentId= null
        replyMentionUserId= null
    }

    private fun hideKeyboard() {
        val imm= requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
    }

    /** @mentions rendered bold-blue inside the body copy (iOS styledComment). */
    private fun styledComment(text: String): CharSequence {
        val spannable= SpannableString(text)
        val matcher= Pattern.compile("@\\w+").matcher(text)
        val mentionColor= ContextCompat.getColor(requireContext(), R.color.fw_info)
        while(matcher.find()){
            spannable.setSpan(ForegroundColorSpan(mentionColor),
                matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD),
                matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun setupAdapter() {
        rebuildDisplayRows()

        if(displayRows.isNotEmpty()){
            binding.tvNoData.gone()
            binding.rvMainFilter.visible()
        }else{
            binding.tvNoData.visible()
            binding.rvMainFilter.gone()
        }

        val replyIndent= (resources.displayMetrics.density * 20).toInt()

        commentsAdapter= binding.rvMainFilter.setUpAdapter(
            displayRows,
            R.layout.item_comment_bottom,
            ItemCommentBottomBinding::inflate,
            { row,pos,bindingItem->
                val it= row.comment

                // reply rows indent under their parent (iOS: depth * 20pt)
                bindingItem.clCommentRoot.updatePaddingRelative(start = replyIndent * row.depth)

                bindingItem.tvProfileName.text= it.userId?.firstName.plus(" ").plus(it.userId?.lastName)
                bindingItem.tvComments.text= styledComment(it.comment ?: "")

                // timestamp comes back from the comments API (same field iOS shows)
                val time= it.createdAt?.takeIf { t-> t.isNotEmpty() }
                    ?.let { t-> DateUtils.getFormattedDateOfFireWire(t) } ?: ""
                bindingItem.tvDateTime.text= time
                if(time.isEmpty()) bindingItem.tvDateTime.gone() else bindingItem.tvDateTime.visible()

                // replies count only on top-level rows; tapping it (or the row)
                // expands/collapses the thread, like iOS's outline disclosure
                val replyCount= it.replies?.size ?: 0
                val expanded= expandedComments.contains(row.topLevelId)
                when{
                    row.depth>0 -> bindingItem.tvReplies.gone()
                    replyCount==0 -> bindingItem.tvReplies.gone()
                    else -> {
                        bindingItem.tvReplies.text= when{
                            expanded -> getString(R.string.fw_hide_replies)
                            replyCount==1 -> getString(R.string.fw_reply_count)
                            else -> getString(R.string.fw_replies_count,replyCount.toString())
                        }
                        bindingItem.tvReplies.visible()
                    }
                }
                bindingItem.tvReplies.setOnClickListener { _-> toggleReplies(row.topLevelId) }

                bindingItem.tvReply.setOnClickListener { _-> startReply(row) }

                if(!it.userId?.img.isNullOrEmpty())
                Glide.with(this)
                    .load(it.userId?.img)
                    .into(bindingItem.profileImage)
                else
                    // recycled row: clear any stale avatar so the red-tint
                    // person fallback underneath shows through
                    bindingItem.profileImage.setImageDrawable(null)

                // drop empty-string urls (the API pads img with "" entries) —
                // an empty card here rendered as a blank white square
                val imageList= ArrayList(it.img.orEmpty().filter { u-> u.isNotEmpty() })
                if(imageList.isNotEmpty()){
                    bindingItem.rvImages.visible()
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
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
                }else{
                    bindingItem.rvImages.gone()
                }
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
