package com.gameball.gameball

import android.net.Uri
import com.gameball.gameball.network.Callback
import com.google.android.gms.tasks.Task
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseSyncServices() {

    private suspend fun getToken(task: Task<String>, callback: Callback<String>): Unit{
        try{
            var deviceToken = task.await()
            callback.onSuccess(deviceToken)
        }
        catch (t: Throwable){
            callback.onError(t)
        }
    }

     fun getDeviceToken(task: Task<String>, callback: Callback<String>): Unit{
         val myScope = CoroutineScope(Dispatchers.IO)
         myScope.launch {
             getToken(task, callback)
         }
    }

    private suspend fun getlink(task: Task<PendingDynamicLinkData>, callback: Callback<String>){
        try{
            val pendingDynamicLinkData = task.await()
            var deepLink: Uri? = null
            if (pendingDynamicLinkData != null) {
                deepLink = pendingDynamicLinkData.link
                val referralCode = deepLink?.getQueryParameter("GBReferral")
                callback.onSuccess(referralCode)
            }
        }
        catch (t: Throwable){
            callback.onError(t)
        }
    }

    fun getDynamicLink(task: Task<PendingDynamicLinkData>, callback: Callback<String>){
        val myScope = CoroutineScope(Dispatchers.IO)
        myScope.launch {
            getlink(task, callback)
        }
    }
}