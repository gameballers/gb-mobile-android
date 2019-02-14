package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Icon {

    @SerializedName("Id")
    @Expose
    private String id;
    @SerializedName("isDeleted")
    @Expose
    private Boolean isDeleted;
    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("FileName")
    @Expose
    private String fileName;
    @SerializedName("IconTypeId")
    @Expose
    private Integer iconTypeId;
    @SerializedName("CreationDate")
    @Expose
    private String creationDate;
    @SerializedName("LastUpdate")
    @Expose
    private String lastUpdate;
    @SerializedName("IconType")
    @Expose
    private Object iconType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getIconTypeId() {
        return iconTypeId;
    }

    public void setIconTypeId(Integer iconTypeId) {
        this.iconTypeId = iconTypeId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Object getIconType() {
        return iconType;
    }

    public void setIconType(Object iconType) {
        this.iconType = iconType;
    }

}