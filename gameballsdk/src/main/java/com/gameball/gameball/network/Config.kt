package com.gameball.gameball.network

/**
 * Created by Ahmed Abdelmoneam Abdelfattah on 8/23/2018.
 * Maintained by Ahmed El Monady since March 2023
 */
object Config {
    const val API_V4_0 = "api/v4.0/"
    const val API_V4_1 = "api/v4.1/"

    const val SendEvent = "api/v4.0/integrations/events"
    const val GetBotSettings = "api/v1.0/Bots/BotSettings?c=mobile"
    const val InitializeCustomer = "api/v4.0/integrations/customers"
    const val MobileLogs = "api/v4.0/integrations/mobile/logs"
    /**
     * Taps, swipe-aways and influenced opens of Gameball pushes. The backend still serves
     * push-click as the same action, for apps on 4.0.0; a backend without push-engagement answers
     * 404 instead of counting a swipe-away as a click.
     */
    const val PushEngagement = "api/v4.0/integrations/mobile/push-engagement"

    /**
     * In-app messaging lives on v4.0 only. /api/v4.1/.../inapp-messages/sync exists and answers
     * 401 to APIKey auth, so HeaderInterceptor must not version-switch these paths.
     */
    const val IAM_PATH_SEGMENT = "inapp-messages"
    private const val IAM_BASE = "api/v4.0/integrations/inapp-messages"
    const val InAppMessagesSync = "$IAM_BASE/sync"
    const val InAppMessagesEvents = "$IAM_BASE/events"
    const val InAppMessagesVariables = "$IAM_BASE/variables"
}