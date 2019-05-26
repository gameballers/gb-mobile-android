package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class BaseResponse<T> {
    @SerializedName("response")
    @Expose
    private T response;
    @SerializedName("Success")
    @Expose
    private boolean success;
    @SerializedName("ErrorMsg")
    @Expose
    private String errorMsg;

    public T getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
