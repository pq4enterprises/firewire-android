package com.pioneer.nycfirewire.fragment

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.databinding.BottomSheetCommentBinding
import com.pioneer.nycfirewire.databinding.DialogCommentReportBinding
import com.pioneer.nycfirewire.databinding.ItemCommentBottomBinding
import com.pioneer.nycfirewire.databinding.ItemImageBinding
import com.pioneer.nycfirewire.listener.OnLoadMoreListener
import com.pioneer.nycfirewire.listener.RecyclerViewLoadMoreScroll
import com.pioneer.nycfirewire.model.incident.request.FeatureImageSetRequest
import com.pioneer.nycfirewire.model.incident.request.ReportCommentRequest
import com.pioneer.nycfirewire.model.incident.response.Incident
import com.pioneer.nycfirewire.utils.AppUtils
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.COMMENTS_FRAGMENT
import com.pioneer.nycfirewire.utils.Constants.USER_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUB_ADMIN
import com.pioneer.nycfirewire.utils.Constants.USER_SUPER
import com.pioneer.nycfirewire.utils.DateUtils
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.putArgs
import com.pioneer.nycfirewire.utils.showToast
import com.pioneer.nycfirewire.utils.startNewActivity
import com.pioneer.nycfirewire.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody


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

    private lateinit var scrollListener: RecyclerViewLoadMoreScroll

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

   /* override fun onResume() {
        super.onResume()
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, COMMENTS_FRAGMENT)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "CommentsFragment")
        }

        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }*/

    private fun updateCommentsData(response: Resource<CommonResponse>) {
        when(response.state){
            ResourceState.LOADING -> binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                imageString=""
                if(response.data?.code=="comment_added") {
                   showSnackbar(binding.cvComment,response.data.message.toString())
                    binding.etComment.setText("")
                    binding.ivPhoto.setImageResource(R.drawable.ic_camera)
                    isCommand=true
                    offset=1
                    vm.getCommentsList(incidentId,1,10)
                   /* vm.commentsLiveData.observe(viewLifecycleOwner, Observer {
                        updateAddedCommentsInList(it)
                    })*/
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
                    val comment= binding.etComment.text.toString()

                    var imgArray= ArrayList<String>()
                    imgArray.add(imageString)
                    val commentRequest= AddCommentRequest(
                        prefs.userId.toString(), incidentId,type,comment,imgArray)
                    vm.postComment(commentRequest)
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

            val comment= binding.etComment.text.toString()
          /*  when{
                comment.isEmpty()-> showSnackbar(it,getString(R.string.enter_comment))
                else->{

                }
            }*/

            if(imageUri!=null && !imageUri.toString().isNullOrEmpty()){
                uploadImageData(imageUri!!)
                binding.cvComment.gone()
            }else if(comment.isNotEmpty()){
                val type="comment"

                var imgArray= ArrayList<String>()
                val commentRequest= AddCommentRequest(
                    prefs.userId.toString(), incidentId,type,comment, imgArray)
                vm.postComment(commentRequest)
                binding.cvComment.gone()
            }else{
                showSnackbar(binding.cvComment, "Kindly enter comment or select an image")
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
            updateCommentsData(it)
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


    private fun updateAddedCommentsInList(response: Resource<CommentsResponse>){
        when(response.state){
            ResourceState.LOADING ->binding.progress.visible()
            ResourceState.SUCCESS -> {
                binding.progress.gone()
                val list= response.data?.data?.data!!
                try {
                    binding.rvProgress.gone()
                    if(list.isNotEmpty()){
                        commentsList.clear()
                        commentsList.addAll(list)
                        binding.rvMainFilter.adapter?.notifyItemChanged(0)
                    }

                    if (list.size > 1)
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comments,list.size.toString())
                    else
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comment,list.size.toString())


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

    private fun updateCommentsList(response: Resource<CommentsResponse>) {
        when(response.state){
            ResourceState.LOADING -> if (offset == 1) binding.progress.visible() else binding.rvProgress.visible()
            ResourceState.SUCCESS -> {
                try {
                    binding.progress.gone()
                    binding.rvProgress.gone()
                    val list= response.data?.data?.data!!
                    if(list.isNotEmpty()){
                        if(offset==1) commentsList.clear()
                        commentsList.addAll(list)
                    }
                    if(offset==1 ) setupAdapter() else binding.rvMainFilter.adapter?.notifyDataSetChanged()
                    rvBasedSetup(commentsList)
                    //if(list.isEmpty() && offset>1){ offset= offset-1}
                    var pageInfo= response.data.data?.pageInfo?.totalCount?.toInt()!!

                    if (pageInfo > 1)
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comments,pageInfo.toString())
                    else
                        binding.tvTotalComments.text =
                            requireContext().getString(R.string.comment,pageInfo.toString())
                    prefs.commentCount= pageInfo.toString()

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

    private fun setupAdapter() {
        val linearLayoutManager1 = LinearLayoutManager(requireContext()).apply { orientation = LinearLayoutManager.VERTICAL }
        binding.rvMainFilter.setHasFixedSize(true)

        binding.rvMainFilter.setUpAdapter(
            commentsList,
            R.layout.item_comment_bottom,
            ItemCommentBottomBinding::inflate,
            { it,pos,bindingItem->
                bindingItem.tvProfileName.text= it.userId?.firstName.plus(" ").plus(it.userId?.lastName)
                bindingItem.tvComments.text= it.comment

                if(prefs.userRole?.contains("admin") == true){
                    bindingItem.ivMenuDot.visible()
                }

                bindingItem.ivMenuDot.setOnClickListener{ view->
                    commentId= it._id.toString()
                 /*   if(it.featuredImage){
                        clickedCommentImage=""
                    }else {
                        if (!it.img.isNullOrEmpty()) clickedCommentImage = it.img?.get(0).toString() else clickedCommentImage=""
                    }*/
                    if (!it.img.isNullOrEmpty()) clickedCommentImage = it.img?.get(0).toString() else clickedCommentImage=""
                    showCustomDialog(it.featuredImage)
                }

                if(!it.createdAt.isNullOrEmpty())
                    bindingItem.tvDateTime.text= DateUtils.formatDateTime(it.createdAt.toString())

                if(!it.userId?.img.isNullOrEmpty()){
                Glide.with(this)
                    .load(it.userId?.img)
                    .into(bindingItem.profileImage)}else{
                    Glide.with(this)
                        .load(R.drawable.ic_user_profile_empty)
                        .into(bindingItem.profileImage)
                    }

                if(it.img?.isNotEmpty() == true) bindingItem.rvImages.visible() else bindingItem.rvImages.gone()
                val imageList= ArrayList(it.img?:ArrayList())

                if(imageList.isNotEmpty()) bindingItem.rvImages.visible() else bindingItem.rvImages.gone()

                var filterImg= imageList.filter { it.isNotEmpty() }

                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
                bindingItem.rvImages.setUpAdapter(
                    ArrayList(filterImg),
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
            },{}, linearLayoutManager1)


        scrollListener = RecyclerViewLoadMoreScroll(linearLayoutManager1)
        scrollListener.setOnLoadMoreListener(object : OnLoadMoreListener {
            override fun onLoadMore() {
                   offset=offset+1
                binding.rvProgress.visible()
        vm.getCommentsList(incidentId, offset, limit)
      /*  vm.commentsLiveData.observe(viewLifecycleOwner, Observer {
            updateCommentsList(it)
        })*/
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


    private fun showCustomDialog(isfeatureImage: Boolean) {
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



        if((prefs.userRole==USER_ADMIN || prefs.userRole==USER_SUPER || prefs.userRole==USER_SUB_ADMIN) && clickedCommentImage.isNotEmpty() ) {
            buttonImageSet.visible()
            buttonDelete.visible()
        }else{
            buttonImageSet.gone()
         buttonDelete.gone()}


        if(isfeatureImage){
            clickedCommentImage=""
            buttonImageSet.setText("Remove Featured Image")
        }else buttonImageSet.setText(("Set Featured Image"))

        // Set up dialog
        val alertDialog = dialogBuilder.create()

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