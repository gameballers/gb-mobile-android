package com.gameball.gameball.services

import android.util.Log
import com.gameball.gameball.model.request.CustomerRegisterRequest
import com.gameball.gameball.model.response.CustomerRegisterResponse
import com.gameball.gameball.network.Callback
import com.gameball.gameball.network.api.GameBallApi
import com.google.gson.Gson
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GameballCoroutineService {
    fun registerDevice(tag: String, registerDeviceRequest: CustomerRegisterRequest, pushProvider: String?, deviceToken: String?, callback: Callback<CustomerRegisterResponse>, gameBallApi: GameBallApi) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                if(deviceToken != null){
                    registerDeviceRequest.deviceToken = deviceToken
                }
                else{
                    val token = PushServicesHelper.getDeviceToken(pushProvider)

                    token?.let{
                        registerDeviceRequest.deviceToken = it
                    }
                }
                
                if(pushProvider != null){
                    registerDeviceRequest.pushProvider = pushProvider
                }

                Log.d(tag, Gson().toJson(registerDeviceRequest))

                gameBallApi.registerCustomer(registerDeviceRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<CustomerRegisterResponse?> {
                        override fun onSubscribe(d: Disposable) {}
                        override fun onSuccess(t: CustomerRegisterResponse) {
                            callback.onSuccess(t)
                        }
                        override fun onError(e: Throwable) {
                            callback.onError(e)
                        }
                    })
            } catch (e: Throwable) {
                callback.onError(e)
            }
        }
    }
}