package com.gameball.gameball.model.request;

import com.gameball.gameball.local.SharedPreferencesUtils;
import com.gameball.gameball.utils.SHA1Hasher;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RewardPointsBody
{
    @SerializedName("PlayerUniqueID")
    @Expose
    private String playerUniqueID;
    @SerializedName("Amount")
    @Expose
    private Integer amount;
    @SerializedName("TransactionOnClientSystemId")
    @Expose
    private String transactionOnClientSystemId;
    @SerializedName("TransactionTime")
    @Expose
    private String transactionTime;
    @SerializedName("BodyHashed")
    @Expose
    private String bodyHashed;

    public RewardPointsBody(int amount, String transactionOnClientSystemId, String transactionKey)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.ENGLISH);
        SimpleDateFormat simpleDateFormatHash = new SimpleDateFormat("yyMMddHHmmss",
                Locale.ENGLISH);
        Date transactionDate = Calendar.getInstance().getTime();

        playerUniqueID = SharedPreferencesUtils.getInstance().getPlayerId();
        this.amount = amount;
        this.transactionOnClientSystemId = transactionOnClientSystemId;
        this.transactionTime = simpleDateFormat.format(transactionDate);
        try
        {
            bodyHashed = SHA1Hasher.sha1(playerUniqueID + ":" +
                    simpleDateFormatHash.format(transactionDate) + ":" + transactionKey);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
}
