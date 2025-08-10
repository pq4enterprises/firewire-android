package com.pioneer.nycfirewire.data.auth

import android.content.Context
import android.content.Intent
import com.pioneer.nycfirewire.activity.LoginNewActivity
import com.pioneer.nycfirewire.model.user.request.RefreshTokenRequest
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.data.ApiEndPoints
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val context: Context,
    private val api: ApiEndPoints
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = prefs.refreshToken ?: return null

        val refreshResponse = try {
            api.tokenRefresh(RefreshTokenRequest(refreshToken)).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        if (refreshResponse.isSuccessful) {
            val auth = refreshResponse.body()?.data ?: return null

            prefs.token = auth.token
            prefs.refreshToken = auth.refreshToken

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${auth.token}")
                .build()
        } else {
            redirectToLogin()
            return null
        }
    }

    private fun redirectToLogin() {
        prefs.deleteToken
        val intent = Intent(context, LoginNewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
