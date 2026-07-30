package com.pioneer.nycfirewire.data
import com.pioneer.nycfirewire.model.incident.request.AddCommentRequest
import com.pioneer.nycfirewire.model.incident.request.FeatureImageSetRequest
import com.pioneer.nycfirewire.model.incident.request.ReportCommentRequest
import com.pioneer.nycfirewire.model.incident.request.mainCommentRequest
import com.pioneer.nycfirewire.model.incident.response.CommentsResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentByIdResponse
import com.pioneer.nycfirewire.model.incident.response.IncidentResponse
import com.pioneer.nycfirewire.model.locality.LocalityResponse
import com.pioneer.nycfirewire.model.payment.PaymentRequest
import com.pioneer.nycfirewire.model.user.request.DeleteUser
import com.pioneer.nycfirewire.model.user.request.ForgotPasswordRequest
import com.pioneer.nycfirewire.model.user.request.LocalityUpdate
import com.pioneer.nycfirewire.model.user.request.LoginRequest
import com.pioneer.nycfirewire.model.user.request.NotificationAreaData
import com.pioneer.nycfirewire.model.user.request.PostAreaData
import com.pioneer.nycfirewire.model.user.request.ProfileUpdateRequest
import com.pioneer.nycfirewire.model.user.request.RefreshTokenRequest
import com.pioneer.nycfirewire.model.user.request.RegisterRequest
import com.pioneer.nycfirewire.model.user.request.ResendOtpRequest
import com.pioneer.nycfirewire.model.user.request.ResetPasswordRequest
import com.pioneer.nycfirewire.model.user.request.SocialLoginRequest
import com.pioneer.nycfirewire.model.user.request.UpdatePasswordRequest
import com.pioneer.nycfirewire.model.user.request.VerifyEmailOtpRequest
import com.pioneer.nycfirewire.model.user.request.VerifyOtpRequest
import com.pioneer.nycfirewire.model.user.response.CommonResponse
import com.pioneer.nycfirewire.model.user.response.FeedResponse
import com.pioneer.nycfirewire.model.user.response.LoginResponse
import com.pioneer.nycfirewire.model.user.response.RefreshTokenResponse
import com.pioneer.nycfirewire.model.user.response.RegisterResponse
import com.pioneer.nycfirewire.model.user.response.UserResponse
import com.pioneer.nycfirewire.model.user.response.VerifyOtpResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import com.pioneer.nycfirewire.model.link.LinkResponse

interface ApiEndPoints {

    // Server-driven Menu shortcut tiles.
    // show=true is required: without it the server paginates and caps results at 10.
    @GET("api/app/link?show=true")
    suspend fun getLinks():Response<LinkResponse>



    @POST("/api/app/auth/register")
    suspend fun registerUser(@Body registerLoginRequest: RegisterRequest) : Response<RegisterResponse>

    @POST("/api/app/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest) : Response<LoginResponse>

    @POST("/api/app/auth/token/refresh")
    suspend fun refreshToken(@Body tokenRequest: RefreshTokenRequest) : Response<RefreshTokenResponse>

    @POST("/api/app/auth/token/refresh")
    fun tokenRefresh(@Body tokenRequest: RefreshTokenRequest) : Call<RefreshTokenResponse>

    @GET("/api/app/incident?sortDir=desc")
    suspend fun getIncidentList(@Query("offset") offset: Int,
                                @Query("limit") limit: Int):Response<IncidentResponse>

    @GET("api/app/incident/{incidentId}")
    suspend fun getIncident(@Path("incidentId") incidentId: String): Response<IncidentByIdResponse>

    @GET("api/app/incident?sortDir=desc")
    suspend fun getIncidentListByFilter(
        @Query("query") query: String , @Query("offset") offset: Int,
        @Query("limit") limit: Int// Pass the 'query' parameter as a JSON string
    ): Response<IncidentResponse>

   //api/app/locality?sortBy=createdAt&sortDir=desc&offset=1&limit=100?&query={"type":"notification"}
    @GET("api/app/locality?sortBy=createdAt&sortDir=desc")
    suspend fun getLocalityList(
       @Query("query") query: String
   ):Response<LocalityResponse>

    @GET("api/app/incident/comment/{incidentId}")
    suspend fun getComments(@Path("incidentId") incidentId: String,@Query("offset") offset: Int,
                            @Query("limit") limit: Int): Response<CommentsResponse>

    @GET("api/app/user/profile")
    suspend fun getUserProfile():Response<UserResponse>

    @PUT("api/app/user/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest):Response<CommonResponse>

    @PUT("api/app/user/profile")
    suspend fun deleteProfile(@Body request: DeleteUser):Response<CommonResponse>

    @POST("/api/app/auth/update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest):Response<CommonResponse>

    @Multipart
    @POST("api/common/upload") // Your server URL path for the upload endpoint
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<CommonResponse>

    @GET("api/app/feed")
    suspend fun getFeed(@Query("query") query: String, @Query("limit") limit: Int):Response<FeedResponse>

    @POST("api/app/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest):Response<CommonResponse>

    @POST("api/app/auth/otp-verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest):Response<VerifyOtpResponse>

    @POST("api/app/auth/reset-password")
    suspend fun resetPassword(@Body resetPassword: ResetPasswordRequest): Response<CommonResponse>

    @POST("api/app/incident/activity")
    suspend fun postComment(@Body request: AddCommentRequest):Response<CommonResponse>

    @POST("api/app/incident/activity")
    suspend fun postMainComment(@Body request: mainCommentRequest):Response<CommonResponse>


    @POST("api/app/auth/social-login")
    suspend fun postGoogle(@Body request: SocialLoginRequest):Response<LoginResponse>

    @PUT("api/app/user/profile")
    suspend fun updateLocalityProfile(@Body request: LocalityUpdate):Response<CommonResponse>

    @POST("api/app/user/area?sortBy=createdAt&sortDir=desc") //&offset=1&limit=100
    suspend fun postSelectedArea(@Body request: ArrayList<PostAreaData>):Response<CommonResponse>


    @POST("api/app/user/notification?sortBy=createdAt&sortDir=desc") //&offset=1&limit=100
    suspend fun postNotificationArea(@Body request: ArrayList<NotificationAreaData>):Response<CommonResponse>

    @POST("/api/app/payment") //&offset=1&limit=100
    suspend fun postPayment(@Body request: PaymentRequest):Response<CommonResponse>


    @PUT("api/app/incident/{incidentId}")
    suspend fun featureImageSetOrRemove(@Path("incidentId") incidentId: String,@Body request: FeatureImageSetRequest):Response<CommonResponse>


    @POST("api/app/incident/comment/report/{commentId}")
    suspend fun reportComment(@Path("commentId") incidentId: String,@Body request: ReportCommentRequest):Response<CommonResponse>

    @GET("noauth/create/incident")
    suspend fun postIncidentForm(@Query("form") form: String, @Query("token") token: String,@Query("theme") theme: String):Response<CommonResponse>

    @DELETE("api/app/comment/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: String):Response<CommonResponse>
    //{{url}}/api/app/comment/67c6d5df214691593b760177(comment id)

    @POST("api/app/auth/verify-email-otp")
    suspend fun verifyEmailOtp(@Body request: VerifyEmailOtpRequest):Response<LoginResponse>

    @POST("api/app/auth/resend-email-otp")
    suspend fun resendEmailOtp(@Body request: ResendOtpRequest):Response<LoginResponse>


   // https://dev-firewire.atomgroups.work/noauth/create/incident?form=add&token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY3NWE5NGNiYTcyMTMzNTRhMTk4YWM1ZCIsInJvbGUiOiJzdXBlciIsImVtYWlsIjoidmltYWxhZGV2aUBhdG9tZ3JvdXBzLmNvbSIsImlhdCI6MTc0MTQxNjA1OSwiZXhwIjoxNzQxNDUyMDU5fQ.pe7PrDNBMlBBhfLC2pVdfo1GC-jNXy8SGxvosmlFo1c&theme=light

  //  https://dev-firewire-api.atomgroups.work/api/app/locality?sortBy=createdAt&sortDir=desc&offset=1&limit=100?&query={"type":"notification"}


   //https://firewire-api.atomgroups.com/api/app/incident?sortDir=desc&offset=1&limit=10&query={"search":"new", "locality" :["6729efb097dfc3f21f13bad9"], "subLocality": ["6731c44d477cc8909d1d0123"] }




}