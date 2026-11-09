package com.gameball.gameball.network.interceptor

import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderInterceptorTest {

    @Test
    fun aSessionTokenMovesV40CallsToV41() {
        assertEquals(
            "/api/v4.1/integrations/events",
            HeaderInterceptor.versionedPath("/api/v4.0/integrations/events", true)
        )
    }

    @Test
    fun withoutASessionTokenNothingMoves() {
        assertEquals(
            "/api/v4.0/integrations/events",
            HeaderInterceptor.versionedPath("/api/v4.0/integrations/events", false)
        )
    }

    @Test
    fun pushEngagementStaysOnV40() {
        assertEquals(
            "/api/v4.0/integrations/mobile/push-engagement",
            HeaderInterceptor.versionedPath("/api/v4.0/integrations/mobile/push-engagement", true)
        )
    }

    @Test
    fun inAppMessagingStaysOnV40() {
        assertEquals(
            "/api/v4.0/integrations/inapp-messages/sync",
            HeaderInterceptor.versionedPath("/api/v4.0/integrations/inapp-messages/sync", true)
        )
    }
}
