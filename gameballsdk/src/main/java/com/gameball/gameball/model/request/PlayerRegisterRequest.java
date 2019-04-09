package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class PlayerRegisterRequest {
    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("Email")
    @Expose
    private String email;
    @SerializedName("Gender")
    @Expose
    private String gender;
    @SerializedName("Age")
    @Expose
    private Integer age;
    @SerializedName("DateOfBirth")
    @Expose
    private String dateOfBirth;
    @SerializedName("PlayerCategoryID")
    @Expose
    private Integer playerCategoryID;
    @SerializedName("PlayerUniqueID")
    @Expose
    private String playerUniqueID;
    @SerializedName("CurrentLevel")
    @Expose
    private Integer currentLevel;
    @SerializedName("AccFrubies")
    @Expose
    private Integer accFrubies;
    @SerializedName("AccPoints")
    @Expose
    private Integer accPoints;
    @SerializedName("StatusId")
    @Expose
    private Integer statusId;
    @SerializedName("ClientID")
    @Expose
    private String clientID;
    @SerializedName("DeviceToken")
    @Expose
    private String deviceToken;
    @SerializedName("OSType")
    @Expose
    private String oSType = "Android";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getPlayerCategoryID() {
        return playerCategoryID;
    }

    public void setPlayerCategoryID(int playerCategoryID) {
        this.playerCategoryID = playerCategoryID;
    }

    public String getPlayerUniqueID() {
        return playerUniqueID;
    }

    public void setPlayerUniqueID(String playerUniqueID) {
        this.playerUniqueID = playerUniqueID;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getAccFrubies() {
        return accFrubies;
    }

    public void setAccFrubies(int accFrubies) {
        this.accFrubies = accFrubies;
    }

    public int getAccPoints() {
        return accPoints;
    }

    public void setAccPoints(int accPoints) {
        this.accPoints = accPoints;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
