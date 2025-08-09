package com.gameball.gameball.services

import android.util.Log
import com.gameball.gameball.model.request.InitializeCustomerRequest
import com.gameball.gameball.model.response.InitializeCustomerResponse
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
    fun registerDevice(tag: String, registerDeviceRequest: InitializeCustomerRequest, pushProvider: String?, deviceToken: String?, callback: Callback<InitializeCustomerResponse>, gameBallApi: GameBallApi) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                
                val finalDeviceToken = deviceToken ?: PushServicesHelper.getDeviceToken(pushProvider)
                
                val finalRequest = InitializeCustomerRequest.builder()
                    .customerId(registerDeviceRequest.customerId)
                    .deviceToken(finalDeviceToken)
                    .pushProvider(pushProvider)
                    .referralCode(registerDeviceRequest.referralCode)
                    .email(registerDeviceRequest.email)
                    .mobile(registerDeviceRequest.mobile)
                    .isGuest(registerDeviceRequest.isGuest)
                    .customerAttributes(registerDeviceRequest.customerAttributes)
                    .build()

                Log.d(tag, Gson().toJson(finalRequest))

                gameBallApi.initializeCustomer(finalRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<InitializeCustomerResponse?> {
                        override fun onSubscribe(d: Disposable) {}
                        override fun onSuccess(t: InitializeCustomerResponse) {
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