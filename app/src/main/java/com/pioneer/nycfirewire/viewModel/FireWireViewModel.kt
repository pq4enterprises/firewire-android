package com.pioneer.nycfirewire.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.pioneer.nycfirewire.data.ApiEndPoints
import com.pioneer.nycfirewire.extensions.setError
import com.pioneer.nycfirewire.fragment.PagingMetadataListener
import com.pioneer.nycfirewire.fragment.PostPagingSource
import com.pioneer.nycfirewire.model.TrendingSearchResponseWrapper
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.request.FeatureImageSetRequest
import com.pioneer.nycfirewire.model.incident.request.ReportCommentRequest
import com.pioneer.nycfirewire.model.incident.request.mainCommentRequest
import com.pioneer.nycfirewire.model.incident.response.CommentsResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentByIdResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentResponse
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.model.payment.PaymentRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.FeedResponse
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.repository.FireWireRepository
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.model.user.request.DeleteUser
import com.pioneer.nycfirewire.model.user.request.LocalityUpdate
import com.pioneer.nycfirewire.model.user.request.NotificationAreaData
import com.pioneer.nycfirewire.model.user.request.PostAreaData
import com.pioneer.nycfirewire.model.user.request.ProfileUpdateRequest
import com.pioneer.nycfirewire.model.user.request.UpdatePasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import javax.inject.Inject
import com.pioneer.nycfirewire.model.link.LinkResponse


@HiltViewModel
class FireWireViewModel @Inject constructor(val context: Application,val apiEndPoints: ApiEndPoints) : AndroidViewModel(context),PagingMetadataListener {

    val linkLiveData = MutableLiveData<Resource<LinkResponse>>()

    fun getLinks(){
        viewModelScope.launch {
            fireWireRepo.getLinks(linkLiveData)
        }
    }


    @Inject
    lateinit var fireWireRepo: FireWireRepository
    val newsLiveData = MutableLiveData<Resource<TrendingSearchResponseWrapper>>()
    val incidentByIdLiveData = MutableLiveData<Resource<IncidentByIdResponse>>()
    val localityLiveData = MutableLiveData<Resource<LocalityResponse>>()
    val commentsLiveData = MutableLiveData<Resource<CommentsResponse>>()
    val userLiveData = MutableLiveData<Resource<UserResponse>>()
    val updateProfileLiveData = MutableLiveData<Resource<CommonResponse>>()
    val updatePasswordLiveData = MutableLiveData<Resource<CommonResponse>>()
    val deleteUserLiveData = MutableLiveData<Resource<CommonResponse>>()
    val imageUploadLiveData = MutableLiveData<Resource<CommonResponse>>()
    val feedLiveData = MutableLiveData<Resource<FeedResponse>>()
    val addCommentLiveData = MutableLiveData<Resource<CommonResponse>>()
    val updateLocalityLiveData = MutableLiveData<Resource<CommonResponse>>()
    val postSelectAreaLiveData = MutableLiveData<Resource<CommonResponse>>()
    val postNotificationAreaLiveData = MutableLiveData<Resource<CommonResponse>>()
    val paymentLiveData = MutableLiveData<Resource<CommonResponse>>()
    val featureImageSetLiveData = MutableLiveData<Resource<CommonResponse>>()
    val reportCommentLiveData = MutableLiveData<Resource<CommonResponse>>()
    val postFormLiveData = MutableLiveData<Resource<CommonResponse>>()
    val deleteCommentLiveData = MutableLiveData<Resource<CommonResponse>>()

    val incidentLiveData = MutableLiveData<Resource<IncidentResponse>>()


    fun getIncidentList(locality:ArrayList<String>, subLocality:ArrayList<String>,offset:String,limit:String){

        viewModelScope.launch {
            fireWireRepo.getIncidentList(incidentLiveData,locality,subLocality,offset,limit,context)
        }
    }

   /* {
        viewModelScope.launch {
            fireWireRepo.getIncidentList(incidentLiveData,locality,subLocality,offset,limit)
        }
    }*/


    fun getIncidentById(incidentId:String){
        viewModelScope.launch {
            fireWireRepo.getIncidentById(incidentByIdLiveData,incidentId,context)
        }
    }



    fun getNewsDetail(){
        viewModelScope.launch {
            fireWireRepo.getNewsData(newsLiveData,context)
        }
    }


    fun getLocalityList(type: String){
        viewModelScope.launch {
            fireWireRepo.getLocalityList(localityLiveData,type,context)
        }
    }

    fun getCommentsList(incidentId: String,offset: Int,limit: Int){
        viewModelScope.launch {
            fireWireRepo.getCommentsList(commentsLiveData,incidentId,offset,limit,context)
        }
    }

    fun getUserDetails(){
        viewModelScope.launch {
            fireWireRepo.getUserDetail(userLiveData,context)
        }
    }

    fun updateProfileData(request: ProfileUpdateRequest){
        viewModelScope.launch {
            fireWireRepo.updateProfile(updateProfileLiveData,request,context)
        }
    }

    fun updateLocalityProfile(request: LocalityUpdate){
        viewModelScope.launch {
            fireWireRepo.updateLocalityProfile(updateLocalityLiveData,request, context)
        }
    }

    fun updatePasswordData(request: UpdatePasswordRequest){
        viewModelScope.launch {
            fireWireRepo.updatePassword(updatePasswordLiveData,request,context)
        }
    }
    fun deleteUser(request: DeleteUser){
        viewModelScope.launch {
            fireWireRepo.deleteUser(deleteUserLiveData,request, context)
        }
    }



    fun uploadImage(request: MultipartBody.Part){
        viewModelScope.launch {
            fireWireRepo.uploadImage(imageUploadLiveData,request, context)
        }
    }

    fun getFeedList(locality: ArrayList<String>){
        viewModelScope.launch {
            fireWireRepo.getFeedList(feedLiveData, locality, context)
        }
    }

    fun postComment(request: AddCommentRequest){
        viewModelScope.launch {
            fireWireRepo.postComment(addCommentLiveData,request, context )
        }
    }

    fun postMainComment(request: mainCommentRequest){
        viewModelScope.launch {
            fireWireRepo.postMainComment(addCommentLiveData,request, context )
        }
    }

    fun postSelectArea(request: ArrayList<PostAreaData>){
        viewModelScope.launch {
            fireWireRepo.postSelectedArea(postSelectAreaLiveData,request, context)
        }
    }

    fun postNotificationArea(request: ArrayList<NotificationAreaData>){
        viewModelScope.launch {
            fireWireRepo.postNotificationArea(postNotificationAreaLiveData,request,context)
        }
    }


    fun paymentPost(request: PaymentRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("Billing", "➡️ Sending payment to backend: $request")

                val result = fireWireRepo.postPayment(paymentLiveData, request, context)

                Log.d("Billing", "✅ Backend API call completed successfully: $result")
            } catch (e: Exception) {
                Log.e("Billing", "❌ Error posting payment: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    paymentLiveData.setError("Payment API failed: ${e.localizedMessage}")
                }
            }
        }
    }



    fun featureImageSetOrRemove(request: FeatureImageSetRequest, incidentId: String){
        viewModelScope.launch{
            fireWireRepo.featureImageSetOrRemove(featureImageSetLiveData,request,incidentId,context)
        }
    }

    fun reportComment(request: ReportCommentRequest, commentId: String){
        viewModelScope.launch{
            fireWireRepo.reportComment(reportCommentLiveData,request,commentId,context)
        }
    }

    fun deleteComment(commentId: String){
        viewModelScope.launch{
            fireWireRepo.deleteComment(deleteCommentLiveData,commentId,context)
        }
    }

    fun postForm(){
        viewModelScope.launch{
            fireWireRepo.postIncidentForm(postFormLiveData,context)
        }
    }

    val posts = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 3, enablePlaceholders = false),
        pagingSourceFactory = { PostPagingSource(apiEndPoints,this) }
    ).flow.cachedIn(viewModelScope)

    private val _totalCount = MutableLiveData<String>()
    val totalCount: LiveData<String> get() = _totalCount

    override fun onTotalCountUpdated(total: String) {
        _totalCount.postValue(total)
    }


}