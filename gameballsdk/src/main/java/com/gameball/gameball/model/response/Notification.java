package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("titleApp")
    @Expose
    private String titleApp;
    @SerializedName("bodyApp")
    @Expose
    private String bodyApp;
    @SerializedName("isRead")
    @Expose
    private Boolean isRead;
    @SerializedName("dateTime")
    @Expose
    private String dateTime;
    @SerializedName("languageCode")
    @Expose
    private String languageCode;
    @SerializedName("iconPath")
    @Expose
    private String iconPath;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitleApp() {
        return titleApp;
    }

    public void setTitleApp(String titleApp) {
        this.titleApp = titleApp;
    }

    public String getBodyApp() {
        return bodyApp;
    }

    public void setBodyApp(String bodyApp) {
        this.bodyApp = bodyApp;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
}
