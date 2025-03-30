package com.gameball.gameball.services

import android.content.Context
import android.util.Log
import com.gameball.gameball.utils.Constants.TAG
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object HuaweiServices {
        private val _tokenFlow = MutableStateFlow<Result<String>?>(null)

        fun loadDeviceToken(huaweiAppId: String, applicationContext: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = HmsInstanceId.getInstance(applicationContext).getToken(huaweiAppId, "HCM")
                    _tokenFlow.value = Result.success(token)
                    Log.i(TAG, "Huawei Token retrieved: $token")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get Huawei token: ${e.message}")
                    _tokenFlow.value = Result.failure(e)
                }
            }
        }

        suspend fun getDeviceToken(): String? {
            return try {
                _tokenFlow.filterNotNull().first { it.isSuccess }.getOrThrow()
            } catch (e: Exception) {
                return null
            }
        }

    fun isHmsAvailable(context: Context?): Boolean {
        var isAvailable = false
        if (null != context) {
            val result =
                HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context)
            isAvailable = ConnectionResult.SUCCESS == result
        }
        Log.i(TAG, "IsHmsAvailable: $isAvailable")
        return isAvailable
    }
}