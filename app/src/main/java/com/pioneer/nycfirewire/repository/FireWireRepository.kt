package com.pioneer.nycfirewire.repository

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.pioneer.nycfirewire.data.ApiEndPoints
import com.pioneer.nycfirewire.data.NewsEndPoint
import com.pioneer.nycfirewire.extensions.setError
import com.pioneer.nycfirewire.extensions.setLoading
import com.pioneer.nycfirewire.extensions.setSuccess
import com.pioneer.nycfirewire.model.ErrorResponse
import com.pioneer.nycfirewire.model.TrendingSearchResponseWrapper
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.response.CommentsResponse
import com.pioneer.nycfirewire.model.incident.response.FilterData
import com.pioneer.nycfirewire.model.incident.response.IncidentByIdResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentResponse
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.FeedResponse
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.resource.Resource
import com.google.gson.Gson
import com.google.gson.internal.NumberLimits
import com.pioneer.nycfirewire.fragment.PostPagingSource
import com.pioneer.nycfirewire.model.incident.request.FeatureImageSetRequest
import com.pioneer.nycfirewire.model.incident.request.ReportCommentRequest
import com.pioneer.nycfirewire.model.payment.PaymentRequest
import com.pioneer.nycfirewire.model.user.request.DeleteUser
import com.pioneer.nycfirewire.model.user.request.FilterAreaType
import com.pioneer.nycfirewire.model.user.request.LocalityUpdate
import com.pioneer.nycfirewire.model.user.request.NotificationAreaData
import com.pioneer.nycfirewire.model.user.request.PostAreaData
import com.pioneer.nycfirewire.model.user.request.ProfileUpdateRequest
import com.pioneer.nycfirewire.model.user.request.UpdatePasswordRequest
import com.pioneer.nycfirewire.model.user.response.SaltyWireResponse
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.resource.ResourceState
import com.pioneer.nycfirewire.utils.Constants
import com.pioneer.nycfirewire.utils.Constants.DARK
import com.pioneer.nycfirewire.utils.Constants.LIGHT
import com.pioneer.nycfirewire.utils.NetworkUtils
import okhttp3.MultipartBody
import retrofit2.Response
import javax.inject.Inject
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.model.incident.request.mainCommentRequest

class FireWireRepository @Inject constructor(
    private val apiEndPoint: ApiEndPoints,
    private val newsEndPoint: NewsEndPoint
) {

    suspend fun getIncidentList(
        liveData: MutableLiveData<Resource<IncidentResponse>>,
        locality: ArrayList<String>,
        subLocality: ArrayList<String>,
        offset: String,
        limit: String,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val json = createJsonStringWithGson(locality, subLocality)
            val result =  apiEndPoint.getIncidentList(offset.toInt(),limit.toInt())
                /*if (locality.isNotEmpty() && subLocality.isNotEmpty())
                apiEndPoint.getIncidentListByFilter(json,offset.toInt(),limit.toInt())
            else apiEndPoint.getIncidentList(offset.toInt(),limit.toInt())*/


            println("incident:"+result)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getNewsData(liveData: MutableLiveData<Resource<TrendingSearchResponseWrapper>>,context: Context) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = newsEndPoint.getNewsDetail()
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    suspend fun getIncidentById(
        liveData: MutableLiveData<Resource<IncidentByIdResponse>>,
        incidentId: String,
        context: Context
    ) {

        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {

            val result = apiEndPoint.getIncident(incidentId)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getLocalityList(
        liveData: MutableLiveData<Resource<LocalityResponse>>,
        type: String,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val json = createAreaType(type)
            val result = apiEndPoint.getLocalityList(json)
            print("result:"+result)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    suspend fun getCommentsList(
        liveData: MutableLiveData<Resource<CommentsResponse>>,
        incidentId: String,
        offset: Int,
        limit: Int,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.getComments(incidentId,offset,limit)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getUserDetail(
        liveData: MutableLiveData<Resource<UserResponse>>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.getUserProfile()
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun updateProfile(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        profileRequest: ProfileUpdateRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.updateProfile(profileRequest)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun updatePassword(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        passwordUpdate: UpdatePasswordRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.updatePassword(passwordUpdate)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun uploadImage(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: MultipartBody.Part,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.uploadImage(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun deleteUser(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: DeleteUser,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.deleteProfile(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getFeedList(
        liveData: MutableLiveData<Resource<FeedResponse>>,
        locality: ArrayList<String>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val json = createJsonStringWithGson(locality, arrayListOf())

            val result = apiEndPoint.getFeed(json,1000)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postComment(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: AddCommentRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.postComment(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postMainComment(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: mainCommentRequest,
        context: Context
    ){
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.postMainComment(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    suspend fun updateLocalityProfile(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: LocalityUpdate,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.updateLocalityProfile(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postSelectedArea(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: ArrayList<PostAreaData>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.postSelectedArea(request)
            println("select are post:"+request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postNotificationArea(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: ArrayList<NotificationAreaData>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.postNotificationArea(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postPayment(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: PaymentRequest,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.postPayment(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun featureImageSetOrRemove(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: FeatureImageSetRequest,
        incidentId: String,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.featureImageSetOrRemove(incidentId,request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

 suspend fun reportComment(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: ReportCommentRequest,
        commentId: String,
        context: Context
    ) {
     if (!NetworkUtils.isOnline(context)) {
         liveData.setError(context.getString(R.string.network_connection))
         return
     }
        liveData.setLoading()
        try {
            val result = apiEndPoint.reportComment(commentId,request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun deleteComment(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        commentId: String,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }
        liveData.setLoading()
        try {
            val result = apiEndPoint.deleteComment(commentId)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun postIncidentForm(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            var theme= if(prefs.isDarkMode) DARK else LIGHT
            val result = apiEndPoint.postIncidentForm("add", prefs.token.toString(),theme)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    suspend fun getContents(
        liveData: MutableLiveData<Resource<SaltyWireResponse>>,
        context: Context
    ) {
        if (!NetworkUtils.isOnline(context)) {
            liveData.setError(context.getString(R.string.network_connection))
            return
        }

        liveData.setLoading()
        try {
            val result = apiEndPoint.getContent()
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }



}



fun createJsonStringWithGson(locality: ArrayList<String>, subLocality: ArrayList<String>): String {
    val data = FilterData(
        locality = locality,
        subLocality = subLocality
    )
    val gson = Gson()
    return gson.toJson(data)
}

fun createAreaType(type: String) : String{
    val data= FilterAreaType(type= type)
    val gson = Gson()
    return gson.toJson(data)
}

private fun errorHandling(result: Response<out Any>): String {
    val error = result.errorBody()?.source()?.readUtf8()
    val gson = Gson()
    val errorMsg = gson.fromJson(error, ErrorResponse::class.java)
    return if (!errorMsg.error.isNullOrEmpty()) errorMsg.error else errorMsg?.message.toString()
}







