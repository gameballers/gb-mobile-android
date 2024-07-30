package com.gameball.gameball.model.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class PlayerAttributes
{
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
    @SerializedName("joinDate")
    @Expose
    private String joinDate;
    @SerializedName("preferredLanguage")
    @Expose
    private String preferredLanguage;
    @SerializedName("channel")
    @Expose
    private String channel = "mobile";
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
    public String getMobileNumber()
    {
        return mobile;
    }
    public String getJoinDate()
    {
        return joinDate;
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

        public Builder(){

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
            if (this.customAttributes == null) {
                customAttributes = new HashMap<>();
            }
            customAttributes.put(key, value);
            return this;
        }
        public Builder withAdditionalAttribute(String key, String value){
            if (this.additionalAttributes == null) {
                additionalAttributes = new HashMap<>();
            }
            additionalAttributes.put(key, value);
            return this;
        }
        public PlayerAttributes build()
        {
            PlayerAttributes playerAttributes = new PlayerAttributes();

            if(this.customAttributes != null) {
                playerAttributes.customAttributes = this.customAttributes;
            }

            if(this.additionalAttributes != null) {
                playerAttributes.additionalAttributes = this.additionalAttributes;
            }

            playerAttributes.dateOfBirth = this.dateOfBirth;
            playerAttributes.displayName = this.displayName;
            playerAttributes.firstName = this.firstName;
            playerAttributes.lastName = this.lastName;
            playerAttributes.email = this.email;
            playerAttributes.gender = this.gender;
            playerAttributes.mobile = this.mobile;
            playerAttributes.joinDate = this.joinDate;
            playerAttributes.preferredLanguage = this.preferredLanguage;

            return playerAttributes;
        }
    }
}