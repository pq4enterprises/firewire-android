package com.fire.wire.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.fire.wire.model.TrendingSearchResponseWrapper
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.CommentsResponse
import com.fire.wire.model.incident.response.IncidentByIdResponse
import com.fire.wire.model.incident.response.IncidentResponse
import com.fire.wire.model.link.LinkResponse
import com.fire.wire.model.locality.LocalityResponse
import com.fire.wire.model.user.request.*
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.model.user.response.FeedResponse
import com.fire.wire.model.user.response.UserResponse
import com.fire.wire.repository.FireWireRepository
import com.fire.wire.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject


@HiltViewModel
class FireWireViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {

    @Inject
    lateinit var fireWireRepo: FireWireRepository
    val newsLiveData = MutableLiveData<Resource<TrendingSearchResponseWrapper>>()
    val incidentLiveData = MutableLiveData<Resource<IncidentResponse>>()
    val incidentByIdLiveData = MutableLiveData<Resource<IncidentByIdResponse>>()
    val localityLiveData = MutableLiveData<Resource<LocalityResponse>>()
    val linkLiveData = MutableLiveData<Resource<LinkResponse>>()
    val commentsLiveData = MutableLiveData<Resource<CommentsResponse>>()
    val userLiveData = MutableLiveData<Resource<UserResponse>>()
    val updateProfileLiveData = MutableLiveData<Resource<CommonResponse>>()
    val updatePasswordLiveData = MutableLiveData<Resource<CommonResponse>>()
    val deleteUserLiveData = MutableLiveData<Resource<CommonResponse>>()
    val imageUploadLiveData = MutableLiveData<Resource<CommonResponse>>()
    val feedLiveData = MutableLiveData<Resource<FeedResponse>>()
    val addCommentLiveData = MutableLiveData<Resource<CommonResponse>>()
    val updateLocalityLiveData = MutableLiveData<Resource<CommonResponse>>()



    fun getNewsDetail(){
        viewModelScope.launch {
            fireWireRepo.getNewsData(newsLiveData)
        }
    }

    fun getIncidentList(locality:ArrayList<String>, subLocality:ArrayList<String>){
        viewModelScope.launch {
            fireWireRepo.getIncidentList(incidentLiveData,locality,subLocality)
        }
    }
    fun getIncidentById(incidentId:String){
        viewModelScope.launch {
            fireWireRepo.getIncidentById(incidentByIdLiveData,incidentId)
        }
    }

    fun getLocalityList(){
        viewModelScope.launch {
            fireWireRepo.getLocalityList(localityLiveData)
        }
    }

    fun getLinks(){
        viewModelScope.launch {
            fireWireRepo.getLinks(linkLiveData)
        }
    }

    fun getCommentsList(incidentId: String){
        viewModelScope.launch {
            fireWireRepo.getCommentsList(commentsLiveData,incidentId)
        }
    }

    fun getUserDetails(){
        viewModelScope.launch {
            fireWireRepo.getUserDetail(userLiveData)
        }
    }

    fun updateProfileData(request: ProfileUpdateRequest){
        viewModelScope.launch {
            fireWireRepo.updateProfile(updateProfileLiveData,request)
        }
    }

    fun updateLocalityProfile(request: LocalityUpdate){
        viewModelScope.launch {
            fireWireRepo.updateLocalityProfile(updateLocalityLiveData,request)
        }
    }

    fun updatePasswordData(request: UpdatePasswordRequest){
        viewModelScope.launch {
            fireWireRepo.updatePassword(updatePasswordLiveData,request)
        }
    }
    fun deleteUser(request: DeleteUser){
        viewModelScope.launch {
            fireWireRepo.deleteUser(deleteUserLiveData,request)
        }
    }



    fun uploadImage(request: MultipartBody.Part){
        viewModelScope.launch {
            fireWireRepo.uploadImage(imageUploadLiveData,request)
        }
    }

    fun getFeedList(locality: ArrayList<String>){
        viewModelScope.launch {
            fireWireRepo.getFeedList(feedLiveData, locality)
        }
    }

    fun postComment(request: AddCommentRequest){
        viewModelScope.launch {
            fireWireRepo.postComment(addCommentLiveData,request)
        }
    }



}