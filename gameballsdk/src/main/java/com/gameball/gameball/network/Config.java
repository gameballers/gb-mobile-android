package com.gameball.gameball.network;

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 */
public class Config {
    public static final String PlayerRegistration = "api/v1.0/integrations/RegisterPlayerDevice";
    public static final String Push = "api/integrations/Push";
    public static final String PlayerInfo = "api/v1.0/Bots/PlayerInfo";
    public static final String GetWithUnlocks = "api/v1.0/Bots/challenges";
    public static final String GetNextLevel = "api/Bots/GetNextLevel";
    public static final String GetLeaderBoard = "api/v1.0/Bots/leaderBoard";
    public static final String GetBotSettings = "api/v1.0/Bots/BotSettings?c=mobile";
    public static final String AddNewAction = "api/v1.0/integrations/Action";
    public static final String RewardPoints = "api/integrations/Transaction/Reward";
    public static final String HoldPoints = "api/integrations/Transaction/Hold";
    public static final String RedeemPoints = "api/integrations/Transaction/Redeem";
    public static final String GenerateOTP = "api/integrations/GenerateOTP";
    public static final String ReverseHeld = "api/integrations/Transaction/Hold";
    public static final String GetPlayerBalance = "api/integrations/Transaction/Balance";
    public static final String InitializePlayer = "api/v1.0/integrations/InitializePlayer";
    public static final String referral = "api/integrations/referral";
}
