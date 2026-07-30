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

/**
 * Silent session renewal.
 *
 * OkHttp calls this whenever a request comes back 401. If we can mint a fresh access
 * token from the stored refresh token we hand OkHttp a retried request and the user
 * sees nothing at all — no spinner, no alert, no interruption. Returning null tells
 * OkHttp to give up and surface the 401.
 *
 * The user is only sent to the login screen in the one case where re-authentication is
 * genuinely unavoidable: the refresh token itself is expired, revoked, or belongs to a
 * deleted account.
 */
class TokenAuthenticator(
    private val context: Context,
    private val api: ApiEndPoints
) : Authenticator {

    companion object {
        const val EXTRA_SESSION_EXPIRED = "session_expired"

        /** One refresh attempt per originating request. See [responseCount]. */
        private const val MAX_ATTEMPTS = 1

        /** Serialises refresh across concurrent 401s. */
        private val refreshLock = Any()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // A logged-out user browsing anonymously can legitimately get a 401. That is not
        // an expired session and must never bounce them to a login screen.
        val refreshToken = prefs.refreshToken?.takeIf { it.isNotBlank() } ?: return null

        // Without this, OkHttp keeps re-invoking the authenticator on every retry that
        // also 401s, hammering the refresh endpoint up to its follow-up limit.
        if (responseCount(response) > MAX_ATTEMPTS) return null

        synchronized(refreshLock) {
            // Another thread may have refreshed while we waited on the lock. If the token
            // on disk is no longer the one this request failed with, just retry with it
            // instead of burning a second refresh and rotating the first one away.
            val currentToken = prefs.token
            if (!currentToken.isNullOrBlank() &&
                response.request.header("Authorization") != "Bearer $currentToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshResponse = try {
                api.tokenRefresh(RefreshTokenRequest(refreshToken)).execute()
            } catch (e: Exception) {
                // Network failure, not an auth failure. Keep the session and let the
                // caller see the error — signing the user out over a dropped connection
                // is exactly the forced-logout behaviour we are trying to eliminate.
                e.printStackTrace()
                return null
            }

            // The API answers a *rejected* refresh with HTTP 200 and an error body
            // (AppErrResponse) for account_not_found and for a revoked session, and with
            // 401 only when the JWT itself fails to verify. isSuccessful alone therefore
            // does not mean the refresh worked — the presence of a new token does.
            val newToken = refreshResponse.body()?.data?.token
            val newRefreshToken = refreshResponse.body()?.data?.refreshToken

            if (refreshResponse.isSuccessful && !newToken.isNullOrBlank()) {
                prefs.token = newToken
                if (!newRefreshToken.isNullOrBlank()) prefs.refreshToken = newRefreshToken

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }

            // Refresh was refused. This is the one genuine "you must sign in again" case.
            redirectToLogin()
            return null
        }
    }

    /** Number of times OkHttp has already run this request through the authenticator. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun redirectToLogin() {
        // clearSession() actually writes. The old prefs.deleteToken was a no-op, so the
        // dead credentials survived and every subsequent request re-ran this path.
        prefs.clearSession()
        val intent = Intent(context, LoginNewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            // Tells the login screen to explain why the user is looking at it, rather
            // than dumping them on a bare form with no context.
            putExtra(EXTRA_SESSION_EXPIRED, true)
        }
        context.startActivity(intent)
    }
}
