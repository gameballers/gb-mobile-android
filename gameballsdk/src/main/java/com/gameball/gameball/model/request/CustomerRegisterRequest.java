package com.gameball.gameball.model.request;

import static com.gameball.gameball.model.helpers.RequestModelHelpers.*;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class CustomerRegisterRequest {
    @SerializedName("customerId")
    @Expose
    private String customerId;
    @SerializedName("deviceToken")
    @Expose
    private String deviceToken;
    @SerializedName("osType")
    @Expose
    private String oSType = "Android";
    @SerializedName("customerAttributes")
    @Expose
    private HashMap<String, Object> customerAttributes;
    @SerializedName("referrerCode")
    @Expose
    private String ReferrerCode;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("mobile")
    @Expose
    private String mobile;
    @SerializedName("guest")
    @Expose
    private Boolean isGuest;

    public String getCustomerId()
    {
        return customerId;
    }
    public void setCustomerId(String customerId)
    {
        this.customerId = customerId;
    }
    public String getDeviceToken()
    {
        return deviceToken;
    }
    public void setDeviceToken(String deviceToken)
    {
        this.deviceToken = deviceToken;
    }
    public CustomerAttributes getCustomerAttributes()
    {
        return MapToCustomerAtrributes(customerAttributes);
    }
    public void setCustomerAttributes(CustomerAttributes customerAttributes)
    {
        this.customerAttributes = MapFromCustomerAttributes(customerAttributes);
    }
    public void setReferrerCode(String referrerCode){
        this.ReferrerCode = referrerCode;
    }
    public String getReferrerCode(){
        return this.ReferrerCode;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getEmail(){
        return this.email;
    }
    public void setMobile(String mobile){
        this.mobile = mobile;
    }
    public String getMobile(){
        return this.mobile;
    }
    public void setIsGuest(Boolean isGuest){
        this.isGuest = isGuest;
    }
    public Boolean getIsGuest(){
        return this.isGuest;
    }
}
