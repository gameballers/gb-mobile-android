package com.gameball.gameball.services

import android.content.Context
import android.util.Log
import com.gameball.gameball.utils.Constants.TAG
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseServices {
    private val _tokenFlow = MutableStateFlow<Result<String>?>(null)

    fun loadDeviceToken(firebaseMessagingInstance: FirebaseMessaging = FirebaseMessaging.getInstance()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = firebaseMessagingInstance.token.await()
                _tokenFlow.value = Result.success(token)
                Log.i(TAG, "Firebase Token retrieved: $token")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Firebase token: ${e.message}")
                _tokenFlow.value = Result.failure(e)
            }
        }
    }

    suspend fun getDeviceToken(): String? {
        return try {
            _tokenFlow.filterNotNull().first { it.isSuccess }.getOrThrow() // Wait for token
        } catch (e: Exception) {
            return null
        }
    }

    fun isGmsAvailable(context: Context?): Boolean {
        var isGmsAvailable = false
        if (context != null) {
            val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            isGmsAvailable = ConnectionResult.SUCCESS == result
        }
        Log.i(TAG, "IsGmsAvailable: $isGmsAvailable")
        return isGmsAvailable
    }
}