package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class PlayerRegisterResponse {
    @SerializedName("Name")
    @Expose
    private Object name;
    @SerializedName("Email")
    @Expose
    private Object email;
    @SerializedName("Gender")
    @Expose
    private Object gender;
    @SerializedName("Age")
    @Expose
    private int age;
    @SerializedName("DateOfBirth")
    @Expose
    private Object dateOfBirth;
    @SerializedName("PlayerTypeID")
    @Expose
    private int playerTypeID;
    @SerializedName("ExternalID")
    @Expose
    private String externalID;
    @SerializedName("CurrentLevel")
    @Expose
    private int currentLevel;
    @SerializedName("AccFrubies")
    @Expose
    private int accFrubies;
    @SerializedName("AccPoints")
    @Expose
    private int accPoints;
    @SerializedName("StatusId")
    @Expose
    private int statusId;

    public Object getName() {
        return name;
    }

    public Object getEmail() {
        return email;
    }

    public Object getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public Object getDateOfBirth() {
        return dateOfBirth;
    }

    public int getPlayerTypeID() {
        return playerTypeID;
    }

    public String getExternalID() {
        return externalID;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getAccFrubies() {
        return accFrubies;
    }

    public int getAccPoints() {
        return accPoints;
    }

    public int getStatusId() {
        return statusId;
    }
}
