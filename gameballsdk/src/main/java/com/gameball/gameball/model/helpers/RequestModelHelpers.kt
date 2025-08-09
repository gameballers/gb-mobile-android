package com.gameball.gameball.model.helpers

import com.gameball.gameball.model.request.CustomerAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object RequestModelHelpers {
    @JvmStatic
    fun MapFromCustomerAttributes(playerAttributes: CustomerAttributes): MutableMap<String, Any> {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

        val jsonPlayerAttributes = gson.toJson(playerAttributes)

        val mappedAttributes: MutableMap<String, Any> =
            gson.fromJson(jsonPlayerAttributes, object : TypeToken<MutableMap<String, Any>>() {}.type)

        val additionalAttributes = playerAttributes.additionalAttributes
        if (additionalAttributes != null) {
            mappedAttributes.putAll(additionalAttributes)
        }

        val customAttributes = playerAttributes.customAttributes
        if (customAttributes != null) {
            mappedAttributes["custom"] = customAttributes
        }

        return mappedAttributes
    }

    @JvmStatic
    fun MapToCustomerAtrributes(PlayerAttributesHashMap: Map<String, Any>): CustomerAttributes {
        val tempAttr = PlayerAttributesHashMap.toMutableMap()

        val attributesBuilder = CustomerAttributes.builder()

        val attrDisplayName = tempAttr["displayName"]
        if (attrDisplayName != null) {
            attributesBuilder.displayName(attrDisplayName.toString())
            tempAttr.remove("displayName")
        }

        val attrFirstName = tempAttr["firstName"]
        if (attrFirstName != null) {
            attributesBuilder.firstName(attrFirstName.toString())
            tempAttr.remove("firstName")
        }

        val attrLastName = tempAttr["lastName"]
        if (attrLastName != null) {
            attributesBuilder.lastName(attrLastName.toString())
            tempAttr.remove("lastName")
        }

        val attrEmail = tempAttr["email"]
        if (attrEmail != null) {
            attributesBuilder.email(attrEmail.toString())
            tempAttr.remove("email")
        }

        val attrGender = tempAttr["gender"]
        if (attrGender != null) {
            attributesBuilder.gender(attrGender.toString())
            tempAttr.remove("gender")
        }

        val attrMobileNumber = tempAttr["mobile"]
        if (attrMobileNumber != null) {
            attributesBuilder.mobile(attrMobileNumber.toString())
            tempAttr.remove("mobile")
        }

        val attrDateOfBirth = tempAttr["dateOfBirth"]
        if (attrDateOfBirth != null) {
            attributesBuilder.dateOfBirth(attrDateOfBirth.toString())
            tempAttr.remove("dateOfBirth")
        }

        val attrJoinDate = tempAttr["joinDate"]
        if (attrJoinDate != null) {
            attributesBuilder.joinDate(attrJoinDate.toString())
            tempAttr.remove("joinDate")
        }

        val attrPreferredLanguage = tempAttr["preferredLanguage"]
        if (attrPreferredLanguage != null) {
            attributesBuilder.preferredLanguage(attrPreferredLanguage.toString())
            tempAttr.remove("preferredLanguage")
        }

        val attrCustom = tempAttr["custom"] as? Map<String, String>
        if (attrCustom != null) {
            for ((key, value) in attrCustom) {
                attributesBuilder.addCustomAttribute(key, value)
            }
            tempAttr.remove("custom")
        }

        for ((key, _) in tempAttr) {
            val attrValue = tempAttr[key]
            if (attrValue != null) {
                attributesBuilder.addAdditionalAttribute(key, attrValue.toString())
            }
        }

        return attributesBuilder.build()
    }
}
