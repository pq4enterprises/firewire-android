package com.fire.wire.data
import com.fire.wire.model.incident.request.AddCommentRequest
import com.fire.wire.model.incident.response.CommentsResponse
import com.fire.wire.model.incident.response.IncidentByIdResponse
import com.fire.wire.model.incident.response.IncidentResponse
import com.fire.wire.model.locality.LocalityResponse
import com.fire.wire.model.user.request.*
import com.fire.wire.model.user.response.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiEndPoints {


    @POST("/api/app/auth/register")
    suspend fun registerUser(@Body registerLoginRequest: RegisterRequest) : Response<RegisterResponse>

    @POST("/api/app/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest) : Response<LoginResponse>

    @POST("/api/app/auth/token/refresh")
    suspend fun refreshToken(@Body tokenRequest: RefreshTokenRequest) : Response<RefreshTokenResponse>

    @GET("/api/app/incident?sortDir=desc")
    suspend fun getIncidentList():Response<IncidentResponse>

    @GET("api/app/incident/{incidentId}")
    suspend fun getIncident(@Path("incidentId") incidentId: String): Response<IncidentByIdResponse>

    @GET("api/app/incident?sortDir=desc")
    suspend fun getIncidentListByFilter(
        @Query("query") query: String // Pass the 'query' parameter as a JSON string
    ): Response<IncidentResponse>

    @GET("api/app/locality?sortBy=createdAt&sortDir=desc")
    suspend fun getLocalityList():Response<LocalityResponse>

    @GET("api/app/incident/comment/{incidentId}")
    suspend fun getComments(@Path("incidentId") incidentId: String): Response<CommentsResponse>

    @GET("api/app/user/profile")
    suspend fun getUserProfile():Response<UserResponse>

    @PUT("api/app/user/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest):Response<CommonResponse>

    @PUT("api/app/user/profile")
    suspend fun deleteProfile(@Body request: DeleteUser):Response<CommonResponse>

    @POST("/api/app/auth/update-password")
    suspend fun updatePassword(@Body request:UpdatePasswordRequest):Response<CommonResponse>

    @Multipart
    @POST("api/common/upload") // Your server URL path for the upload endpoint
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<CommonResponse>

    @GET("api/app/feed")
    suspend fun getFeed(@Query("query") query: String):Response<FeedResponse>

    @POST("api/app/auth/forgot-password")
    suspend fun forgotPassword(@Body request:ForgotPasswordRequest):Response<CommonResponse>

    @POST("api/app/auth/otp-verify")
    suspend fun verifyOtp(@Body request:VerifyOtpRequest):Response<VerifyOtpResponse>

    @POST("api/app/auth/reset-password")
    suspend fun resetPassword(@Body resetPassword:ResetPasswordRequest): Response<CommonResponse>

    @POST("api/app/incident/activity")
    suspend fun postComment(@Body request:AddCommentRequest):Response<CommonResponse>


    @POST("api/app/auth/social-login")
    suspend fun postGoogle(@Body request:SocialLoginRequest):Response<LoginResponse>

    @PUT("api/app/user/profile")
    suspend fun updateLocalityProfile(@Body request: LocalityUpdate):Response<CommonResponse>


   // https://firewire-api.atomgroups.com/api/app/incident?sortDir=desc&offset=1&limit=10&query={"search":"new", "locality" :["6729efb097dfc3f21f13bad9"], "subLocality": ["6731c44d477cc8909d1d0123"] }




}