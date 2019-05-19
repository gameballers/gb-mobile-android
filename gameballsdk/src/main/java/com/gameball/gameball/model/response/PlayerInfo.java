package com.gameball.gameball.model.response;

import com.gameball.gameball.model.request.PlayerInfoBody;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlayerInfo
{

    @SerializedName("DisplayName")
    @Expose
    private String displayName;
    @SerializedName("FirstName")
    @Expose
    private String firstName;
    @SerializedName("LastName")
    @Expose
    private String lastName;
    @SerializedName("Email")
    @Expose
    private String email;
    @SerializedName("Gender")
    @Expose
    private String gender;
    @SerializedName("MobileNumber")
    @Expose
    private String mobileNumber;
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

    public Integer getAge()
    {
        return age;
    }

    public String getDateOfBirth()
    {
        return dateOfBirth;
    }

    public Integer getPlayerCategoryID()
    {
        return playerCategoryID;
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

    public Integer getStatusId()
    {
        return statusId;
    }

    public Level getLevel()
    {
        return level;
    }

    public String getMobileNumber()
    {
        return mobileNumber;
    }

    public static class Builder
    {
        private String displayName;
        private String firstName;
        private String lastName;
        private String email;
        private String gender;
        private String mobileNumber;
        private String dateOfBirth;

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
            this.mobileNumber = mobileNumber;

            return this;
        }

        public Builder withDateOfBirth(String dateOfBirth)
        {
            this.dateOfBirth = dateOfBirth;

            return this;
        }

        public PlayerInfo build()
        {
            PlayerInfo playerInfo = new PlayerInfo();
            if(this.email == null || this.email.equals(""))
                playerInfo.email = null;
            else
                playerInfo.email = this.email;

            if(this.mobileNumber == null || this.mobileNumber.equals(""))
                playerInfo.mobileNumber = null;
            else
                playerInfo.mobileNumber = this.mobileNumber;

            playerInfo.firstName = this.firstName;
            playerInfo.lastName = this.lastName;
            playerInfo.dateOfBirth = this.dateOfBirth;
            playerInfo.displayName = this.displayName;
            playerInfo.gender = this.gender;
            return playerInfo;
        }
    }
}