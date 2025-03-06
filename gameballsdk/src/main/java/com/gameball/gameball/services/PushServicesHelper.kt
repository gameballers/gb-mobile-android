package com.gameball.gameball.services

object PushServicesHelper {
    suspend fun getDeviceToken(provider: String?): String? {
        return when (provider) {
            "Firebase" -> FirebaseServices.getDeviceToken()
            "Huawei" -> HuaweiServices.getDeviceToken()
            else -> null
        }
    }
}