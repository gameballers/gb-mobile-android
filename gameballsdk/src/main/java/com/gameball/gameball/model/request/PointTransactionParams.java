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

public class PointTransactionParams
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

    public PointTransactionParams(String transactionKey)
    {
        playerUniqueID = SharedPreferencesUtils.getInstance().getPlayerId();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.ENGLISH);
        SimpleDateFormat simpleDateFormatHash = new SimpleDateFormat("yyMMddHHmmss",
                Locale.ENGLISH);
        Date transactionDate = Calendar.getInstance().getTime();

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

    public String getPlayerUniqueID()
    {
        return playerUniqueID;
    }

    public void setPlayerUniqueID(String playerUniqueID)
    {
        this.playerUniqueID = playerUniqueID;
    }

    public Integer getAmount()
    {
        return amount;
    }

    public void setAmount(Integer amount)
    {
        this.amount = amount;
    }

    public String getTransactionOnClientSystemId()
    {
        return transactionOnClientSystemId;
    }

    public void setTransactionOnClientSystemId(String transactionOnClientSystemId)
    {
        this.transactionOnClientSystemId = transactionOnClientSystemId;
    }

    public String getTransactionTime()
    {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime)
    {
        this.transactionTime = transactionTime;
    }

    public String getBodyHashed()
    {
        return bodyHashed;
    }

    public void setBodyHashed(String bodyHashed)
    {
        this.bodyHashed = bodyHashed;
    }
}
