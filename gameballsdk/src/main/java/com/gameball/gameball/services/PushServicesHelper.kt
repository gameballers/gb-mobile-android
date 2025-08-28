package com.gameball.gameball.services

import com.gameball.gameball.model.enums.PushProvider

object PushServicesHelper {
    suspend fun getDeviceToken(provider: PushProvider?): String? {
        return when (provider) {
            PushProvider.Firebase -> FirebaseServices.getDeviceToken()
            PushProvider.Huawei -> HuaweiServices.getDeviceToken()
            else -> null
        }
    }
}