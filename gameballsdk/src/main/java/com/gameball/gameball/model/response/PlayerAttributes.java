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
    @SerializedName("firstName")
    @Expose
    private String firstName;
    @SerializedName("lastName")
    @Expose
    private String lastName;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("gender")
    @Expose
    private String gender;
    @SerializedName("mobile")
    @Expose
    private String mobile;
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
    @SerializedName("preferredLanguage")
    @Expose
    private String preferredLanguage;
    @SerializedName("custom")
    private HashMap<String, String> customAttributes;
    private HashMap<String, String> additionalAttributes;

    public String getDisplayName()
    {
        return displayName;
    }
    public String getFirstName()
    {
        return firstName;
    }
    public String getLastName()
    {
        return lastName;
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
        return mobile;
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

    public String getPreferredLanguage()
    {
        return preferredLanguage;
    }

    public HashMap<String, String> getCustomAttributes() {
        return this.customAttributes;
    }

    public HashMap<String, String> getAdditionalAttributes() {
        return this.additionalAttributes;
    }

    public static class Builder
    {
        private String displayName;
        private String firstName;
        private String lastName;
        private String email;
        private String gender;
        private String mobile;
        private String dateOfBirth;
        private String joinDate;
        private String preferredLanguage;
        private HashMap<String, String> customAttributes;
        private HashMap<String, String> additionalAttributes;

        public Builder()
        {

        }

        public Builder withDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }
        public Builder withFirstName(String firstName)
        {
            this.firstName = firstName;
            return this;
        }
        public Builder withLastName(String lastName)
        {
            this.lastName = lastName;
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
            this.mobile = mobileNumber;

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

        public Builder withPreferredLanguage(String preferredLanguage){
            this.preferredLanguage = preferredLanguage;
            return this;
        }

        public Builder withCustomAttribute(String key, String value){
            if (this.customAttributes == null)
                customAttributes = new HashMap<>();

            customAttributes.put(key, value);

            return this;
        }

        public Builder withAdditionalAttributes(String key, String value){
            if (this.additionalAttributes == null)
                additionalAttributes = new HashMap<>();

            additionalAttributes.put(key, value);

            return this;
        }

        public PlayerAttributes build()
        {
            PlayerAttributes playerAttributes = new PlayerAttributes();

            if(this.email == null || this.email.equals(""))
                playerAttributes.email = null;
            else
                playerAttributes.email = this.email;

            if(this.mobile == null || this.mobile.equals(""))
                playerAttributes.mobile = null;
            else
                playerAttributes.mobile = this.mobile;

            if(this.customAttributes != null)
                playerAttributes.customAttributes = this.customAttributes;


            if(this.additionalAttributes != null)
                playerAttributes.additionalAttributes = this.additionalAttributes;

            playerAttributes.dateOfBirth = this.dateOfBirth;
            playerAttributes.displayName = this.displayName;
            playerAttributes.firstName = this.firstName;
            playerAttributes.lastName = this.lastName;
            playerAttributes.gender = this.gender;
            playerAttributes.joinDate = this.joinDate;
            playerAttributes.preferredLanguage = this.preferredLanguage;

            return playerAttributes;
        }
    }
}