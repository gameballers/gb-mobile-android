package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Icon {
    @SerializedName("fileName")
    @Expose
    private String fileName;

    public String getFileName()
    {
        return fileName;
    }
}