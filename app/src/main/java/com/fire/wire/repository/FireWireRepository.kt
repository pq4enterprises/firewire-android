package com.fire.wire.repository

import androidx.lifecycle.MutableLiveData
import com.fire.wire.data.ApiEndPoints
import com.fire.wire.data.NewsEndPoint
import com.fire.wire.extensions.setError
import com.fire.wire.extensions.setLoading
import com.fire.wire.extensions.setSuccess
import com.fire.wire.model.ErrorResponse
import com.fire.wire.model.TrendingSearchResponseWrapper
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.CommentsResponse
import com.fire.wire.model.incident.response.FilterData
import com.fire.wire.model.incident.response.IncidentByIdResponse
import com.fire.wire.model.incident.response.IncidentResponse
import com.fire.wire.model.link.LinkResponse
import com.fire.wire.model.locality.LocalityResponse
import com.fire.wire.model.user.request.*
import com.fire.wire.model.user.response.CommonResponse
import com.fire.wire.model.user.response.FeedResponse
import com.fire.wire.model.user.response.UserResponse
import com.fire.wire.resource.Resource
import com.google.gson.Gson
import okhttp3.MultipartBody
import retrofit2.Response
import javax.inject.Inject

class FireWireRepository @Inject constructor(
    private val apiEndPoint: ApiEndPoints,
    private val newsEndPoint: NewsEndPoint
) {

    suspend fun getIncidentList(
        liveData: MutableLiveData<Resource<IncidentResponse>>,
        locality: ArrayList<String>,
        subLocality: ArrayList<String>
    ) {
        liveData.setLoading()
        try {
            val json = createJsonStringWithGson(locality, subLocality)
            val result = if (locality.isNotEmpty() && subLocality.isNotEmpty())
                apiEndPoint.getIncidentListByFilter(json)
            else apiEndPoint.getIncidentList()
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getNewsData(liveData: MutableLiveData<Resource<TrendingSearchResponseWrapper>>) {
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
        incidentId: String
    ) {
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
        liveData: MutableLiveData<Resource<LocalityResponse>>
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.getLocalityList()
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }


    // Areas & Alerts: type "area" flags feed selections, "notification" flags alert selections
    suspend fun getLocalityListByType(
        liveData: MutableLiveData<Resource<LocalityResponse>>,
        type: String
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.getLocalityListByType("{\"type\":\"$type\"}")
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun setUserAreas(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: List<UserAreaItem>
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.setUserAreas(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun setUserNotifications(
        liveData: MutableLiveData<Resource<CommonResponse>>,
        request: List<UserNotificationItem>
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.setUserNotifications(request)
            if (result.isSuccessful) {
                liveData.setSuccess(result.body())
            } else {
                liveData.setError(errorHandling(result))
            }

        } catch (e: Exception) {
            liveData.setError(e.message.toString())
        }
    }

    suspend fun getLinks(
        liveData: MutableLiveData<Resource<LinkResponse>>
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.getLinks()
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
        incidentId: String
    ) {
        liveData.setLoading()
        try {
            val result = apiEndPoint.getComments(incidentId)
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
        liveData: MutableLiveData<Resource<UserResponse>>
    ) {
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
        profileRequest: ProfileUpdateRequest
    ) {
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
        passwordUpdate: UpdatePasswordRequest
    ) {
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
        request: MultipartBody.Part
    ) {
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
        request: DeleteUser
    ) {
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
        locality: ArrayList<String>
    ) {
        liveData.setLoading()
        try {
            val json = createJsonStringWithGson(locality, arrayListOf())

            val result = apiEndPoint.getFeed(json)
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
        request: AddCommentRequest
    ){
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




}



fun createJsonStringWithGson(locality: ArrayList<String>, subLocality: ArrayList<String>): String {
    val data = FilterData(
        locality = locality,
        subLocality = subLocality
    )
    val gson = Gson()

    println("filterData:" + gson.toJson(data))
    return gson.toJson(data)
}

private fun errorHandling(result: Response<out Any>): String {
    val error = result.errorBody()?.source()?.readUtf8()
    val gson = Gson()
    val errorMsg = gson.fromJson(error, ErrorResponse::class.java)
    return if (!errorMsg.error.isNullOrEmpty()) errorMsg.error else errorMsg?.message.toString()
}