package com.gameball.gameball.model.helpers;

import com.gameball.gameball.model.response.PlayerAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

public class RequestModelHelpers {
    public static HashMap<String, Object> MapFromPlayerAttributes(PlayerAttributes playerAttributes){
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        String jsonPlayerAttributes = gson.toJson(playerAttributes);

        HashMap<String, Object> mappedAttributes =
                gson.fromJson(jsonPlayerAttributes, new TypeToken<HashMap<String, Object>>() {}.getType());

        HashMap<String, String> additionalAttributes = playerAttributes.getAdditionalAttributes();
        if(additionalAttributes != null) {
            mappedAttributes.putAll(additionalAttributes);
        }

        HashMap<String, String> customAttributes = playerAttributes.getCustomAttributes();
        if(customAttributes != null){
            mappedAttributes.put("custom", customAttributes);
        }

        return mappedAttributes;
    }

    public static PlayerAttributes MapToPlayerAtrributes(HashMap<String, Object> PlayerAttributesHashMap){
        HashMap<String, Object> tempAttr = new HashMap<>(PlayerAttributesHashMap);

        PlayerAttributes.Builder attributesBuilder = new PlayerAttributes.Builder();

        Object attrDisplayName = tempAttr.get("displayName");
        if(attrDisplayName != null)
        {
            attributesBuilder.withEmail(attrDisplayName.toString());
            tempAttr.remove("displayName");
        }

        Object attrFirstName = tempAttr.get("firstName");
        if(attrFirstName != null)
        {
            attributesBuilder.withFirstName(attrFirstName.toString());
            tempAttr.remove("firstName");
        }

        Object attrLastName = tempAttr.get("lastName");
        if(attrLastName != null)
        {
            attributesBuilder.withLastName(attrLastName.toString());
            tempAttr.remove("lastName");
        }

        Object attrEmail = tempAttr.get("email");
        if(attrEmail != null)
        {
            attributesBuilder.withEmail(attrEmail.toString());
            tempAttr.remove("email");
        }

        Object attrGender = tempAttr.get("gender");
        if(attrGender != null)
        {
            attributesBuilder.withGender(attrGender.toString());
            tempAttr.remove("gender");
        }

        Object attrMobileNumber = tempAttr.get("mobile");
        if(attrMobileNumber != null)
        {
            attributesBuilder.withMobileNumber(attrMobileNumber.toString());
            tempAttr.remove("mobile");
        }

        Object attrDateOfBirth = tempAttr.get("dateOfBirth");
        if(attrDateOfBirth != null)
        {
            attributesBuilder.withDateOfBirth(attrDateOfBirth.toString());
            tempAttr.remove("dateOfBirth");
        }

        Object attrJoinDate = tempAttr.get("joinDate");
        if(attrJoinDate != null)
        {
            attributesBuilder.withJoinDate(attrJoinDate.toString());
            tempAttr.remove("joinDate");
        }

        HashMap<String, String> attrCustom = (HashMap<String, String>) tempAttr.get("custom");
        if(attrCustom != null){
            for(HashMap.Entry<String, String> attr: attrCustom.entrySet()){
                attributesBuilder.withCustomAttribute(attr.getKey(), attr.getValue());
            }
            tempAttr.remove("custom");
        }

        for(HashMap.Entry<String, Object> attr: tempAttr.entrySet()){
            Object attrValue = tempAttr.get(attr.getKey());
            if(attrValue != null){
                attributesBuilder.withAdditionalAttributes(attr.getKey(), attrValue.toString());
            }
        }

        return attributesBuilder.build();
    }

}
