package com.fridgephotos.google

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await

/**
 * Gets a Google access token for the Photos picker.
 * The first time, Google shows its account and permission screens; after that it's silent.
 * Create this in the Activity before it starts (it registers a result launcher).
 */
class GoogleAuth(private val activity: ComponentActivity) {
    private var pending: CompletableDeferred<String>? = null

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val deferred = pending ?: return@registerForActivityResult
            pending = null
            if (result.resultCode != Activity.RESULT_OK) {
                deferred.completeExceptionally(IllegalStateException("Google sign-in was cancelled"))
                return@registerForActivityResult
            }
            try {
                val auth = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data)
                val token = auth.accessToken
                if (token != null) deferred.complete(token)
                else deferred.completeExceptionally(IllegalStateException("Google didn't return access"))
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }

    suspend fun accessToken(): String {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(PhotosPickerClient.SCOPE)))
            .build()
        val result = Identity.getAuthorizationClient(activity).authorize(request).await()
        val intent = result.pendingIntent
        if (result.hasResolution() && intent != null) {
            val deferred = CompletableDeferred<String>()
            pending = deferred
            launcher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
            return deferred.await()
        }
        return result.accessToken ?: throw IllegalStateException("Google didn't return access")
    }
}
