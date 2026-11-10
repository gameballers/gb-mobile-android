package com.gameball.gameball.network.interceptor

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeaderInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        SharedPreferencesUtils.getInstance().clearData()
        SharedPreferencesUtils.getInstance().putApiKey("test-key")
    }

    @After
    fun tearDown() = server.shutdown()

    /** Fires one request through the interceptor and returns the path the server actually saw. */
    private fun pathSeenByServer(requestPath: String): String {
        server.enqueue(MockResponse().setResponseCode(200))
        OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor())
            .build()
            .newCall(Request.Builder().url(server.url(requestPath)).build())
            .execute()
            .close()
        return server.takeRequest().path!!
    }

    @Test
    fun `iam sync stays on v4_0 when a session token is set`() {
        SharedPreferencesUtils.getInstance().putSessionTokenPreference("a-session-token")
        assertEquals(
            "/api/v4.0/integrations/inapp-messages/sync",
            pathSeenByServer("/api/v4.0/integrations/inapp-messages/sync")
        )
    }

    @Test
    fun `iam events and variables stay on v4_0 when a session token is set`() {
        SharedPreferencesUtils.getInstance().putSessionTokenPreference("a-session-token")
        assertEquals(
            "/api/v4.0/integrations/inapp-messages/events",
            pathSeenByServer("/api/v4.0/integrations/inapp-messages/events")
        )
        assertEquals(
            "/api/v4.0/integrations/inapp-messages/variables",
            pathSeenByServer("/api/v4.0/integrations/inapp-messages/variables")
        )
    }

    /** Positive control: the exemption must not disable the switch for anything else. */
    @Test
    fun `existing endpoints still upgrade to v4_1 when a session token is set`() {
        SharedPreferencesUtils.getInstance().putSessionTokenPreference("a-session-token")
        assertEquals("/api/v4.1/integrations/events", pathSeenByServer("/api/v4.0/integrations/events"))
        assertEquals("/api/v4.1/integrations/customers", pathSeenByServer("/api/v4.0/integrations/customers"))
    }

    @Test
    fun `no session token means no upgrade at all`() {
        SharedPreferencesUtils.getInstance().removeSessionTokenPreference()
        assertEquals("/api/v4.0/integrations/events", pathSeenByServer("/api/v4.0/integrations/events"))
    }
}
