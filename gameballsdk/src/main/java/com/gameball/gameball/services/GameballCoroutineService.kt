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
    fun initializeCustomerService(tag: String, initializeCustomerRequest: InitializeCustomerRequest, callback: Callback<InitializeCustomerResponse>, gameBallApi: GameBallApi) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(tag, Gson().toJson(initializeCustomerRequest))

                gameBallApi.initializeCustomer(initializeCustomerRequest)
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