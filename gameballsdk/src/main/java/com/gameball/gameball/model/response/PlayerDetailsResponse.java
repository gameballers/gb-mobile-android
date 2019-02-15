package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerDetailsResponse {

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
    @SerializedName("ExternalID")
    @Expose
    private String externalID;
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
    @SerializedName("Level")
    @Expose
    private Level level;

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getPlayerCategoryID() {
        return playerCategoryID;
    }

    public void setPlayerCategoryID(Integer playerCategoryID) {
        this.playerCategoryID = playerCategoryID;
    }

    public String getExternalID() {
        return externalID;
    }

    public void setExternalID(String externalID) {
        this.externalID = externalID;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Integer getAccFrubies() {
        return accFrubies;
    }

    public void setAccFrubies(Integer accFrubies) {
        this.accFrubies = accFrubies;
    }

    public Integer getAccPoints() {
        return accPoints;
    }

    public void setAccPoints(Integer accPoints) {
        this.accPoints = accPoints;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

}