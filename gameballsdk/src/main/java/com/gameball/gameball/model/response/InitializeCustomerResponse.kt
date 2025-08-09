package com.gameball.gameball.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class InitializeCustomerResponse(
    @SerializedName("gameballId")
    @Expose
    val gameballId: String? = null
)