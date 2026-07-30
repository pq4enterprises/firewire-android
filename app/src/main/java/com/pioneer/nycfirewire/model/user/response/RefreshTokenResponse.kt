package com.pioneer.nycfirewire.model.user.response

/**
 * `data` is nullable because the API answers a *rejected* refresh with HTTP 200 and a
 * body carrying only `message` + `code` (AppErrResponse) — no `data` object at all.
 * Declaring it non-null never stopped Gson from leaving it null via reflection; it only
 * hid the case from the type system, which is how a refused refresh used to fail
 * silently instead of prompting the user to sign in.
 */
data class RefreshTokenResponse(
    val code: String?,
    val data: TokenInnerData?,
    val message: String?
)
