package com.gameball.gameball.model.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class PlayerAttributes
{
    @SerializedName("playerId")
    @Expose
    private Integer playerId;
    @SerializedName("displayName")
    @Expose
    private String displayName;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("gender")
    @Expose
    private String gender;
    @SerializedName("mobileNumber")
    @Expose
    private String mobileNumber;
    @SerializedName("dateOfBirth")
    @Expose
    private String dateOfBirth;
    @SerializedName("playerTypeId")
    @Expose
    private Integer playerTypeID;
    @SerializedName("externalId")
    @Expose
    private String externalID;
    @SerializedName("currentLevel")
    @Expose
    private Integer currentLevel;
    @SerializedName("accFrubies")
    @Expose
    private Integer accFrubies;
    @SerializedName("accPoints")
    @Expose
    private Integer accPoints;
    @SerializedName("joinDate")
    @Expose
    private String joinDate;
    @SerializedName("isActive")
    @Expose
    private boolean isActive;
    @SerializedName("level")
    @Expose
    private Level level;
    @SerializedName("referralCode")
    @Expose
    private String referralCode;
    @SerializedName("dynamicLink")
    @Expose
    private String dynamicLink;
    @SerializedName("custom")
    @Expose
    private HashMap<String, String> custom;

    public String getDisplayName()
    {
        return displayName;
    }

    public String getEmail()
    {
        return email;
    }

    public String getGender()
    {
        return gender;
    }

    public String getDateOfBirth()
    {
        return dateOfBirth;
    }

    public Integer getPlayerTypeID()
    {
        return playerTypeID;
    }

    public String getExternalID()
    {
        return externalID;
    }

    public Integer getCurrentLevel()
    {
        return currentLevel;
    }

    public Integer getAccFrubies()
    {
        return accFrubies;
    }

    public Integer getAccPoints()
    {
        return accPoints;
    }

    public Level getLevel()
    {
        return level;
    }

    public String getMobileNumber()
    {
        return mobileNumber;
    }

    public Integer getPlayerId()
    {
        return playerId;
    }

    public String getJoinDate()
    {
        return joinDate;
    }

    public boolean isActive()
    {
        return isActive;
    }

    public String getReferralCode()
    {
        return referralCode;
    }

    public String getDynamicLink()
    {
        return dynamicLink;
    }

    public HashMap<String, String> getCustom() {
        return this.custom;
    }

    public void addCustomAttribute(String key, String value) {
        if (this.custom == null)
            custom = new HashMap<>();

        custom.put(key, value);
    }

    public static class Builder
    {
        private String displayName;
        private String email;
        private String gender;
        private String mobileNumber;
        private String dateOfBirth;
        private String joinDate;

        public Builder()
        {

        }

        public Builder withDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }

        public Builder withEmail(String email)
        {
            this.email = email;
            return this;
        }

        public Builder withGender(String gender)
        {
            this.gender = gender;

            return this;
        }

        public Builder withMobileNumber(String mobileNumber)
        {
            this.mobileNumber = mobileNumber;

            return this;
        }

        public Builder withDateOfBirth(String dateOfBirth)
        {
            this.dateOfBirth = dateOfBirth;

            return this;
        }

        public Builder withJoinDate(String joinDate)
        {
            this.joinDate = joinDate;
            return this;
        }

        public PlayerAttributes build()
        {
            PlayerAttributes playerAttributes = new PlayerAttributes();
            if(this.email == null || this.email.equals(""))
                playerAttributes.email = null;
            else
                playerAttributes.email = this.email;

            if(this.mobileNumber == null || this.mobileNumber.equals(""))
                playerAttributes.mobileNumber = null;
            else
                playerAttributes.mobileNumber = this.mobileNumber;

            playerAttributes.dateOfBirth = this.dateOfBirth;
            playerAttributes.displayName = this.displayName;
            playerAttributes.gender = this.gender;
            playerAttributes.joinDate = this.joinDate;
            return playerAttributes;
        }
    }
}