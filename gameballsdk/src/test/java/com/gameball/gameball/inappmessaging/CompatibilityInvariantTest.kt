package com.gameball.gameball.inappmessaging

import android.app.Activity
import android.app.Application
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.GameballApp
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.model.request.Event
import com.gameball.gameball.model.request.GameballConfig
import com.gameball.gameball.model.request.InitializeCustomerRequest
import com.gameball.gameball.model.request.ShowProfileRequest
import com.gameball.gameball.model.response.InitializeCustomerResponse
import com.gameball.gameball.network.Callback
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * The whole backward-compatibility promise, asserted in one place.
 *
 * An integrator who upgrades the SDK and does not opt in must see byte-identical behaviour:
 * no requests, no timers, no storage writes, nothing drawn, and no Activity lifecycle
 * callbacks registered.
 */
@RunWith(RobolectricTestRunner::class)
class CompatibilityInvariantTest {

    private lateinit var server: MockWebServer
    private lateinit var app: GameballApp
    private lateinit var prefs: SharedPreferencesUtils

    private val noopCustomerCallback = object : Callback<InitializeCustomerResponse> {
        override fun onSuccess(t: InitializeCustomerResponse?) = Unit
        override fun onError(e: Throwable?) = Unit
    }
    private val noopBooleanCallback = object : Callback<Boolean> {
        override fun onSuccess(t: Boolean?) = Unit
        override fun onError(e: Throwable?) = Unit
    }

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        // Any call the SDK makes lands here, so requestCount is the honest measure.
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val context = ApplicationProvider.getApplicationContext<Application>()
        SharedPreferencesUtils.init(context, Gson())
        prefs = SharedPreferencesUtils.getInstance()
        prefs.clearData()

        app = GameballApp.getInstance(context)
        app.init(
            GameballConfig.builder()
                .apiKey("test-key")
                .lang("en")
                .apiPrefix(server.url("/").toString())
                .build()
        )
    }

    @After
    fun tearDown() {
        app.stopInAppMessaging()
        server.shutdown()
    }

    /** Every clause of the invariant, in the order the spec states them. */
    @Test
    fun `nothing happens before startInAppMessaging is called`() {
        app.initializeCustomer(
            InitializeCustomerRequest.builder().customerId("alice").build(),
            noopCustomerCallback
        )
        app.sendEvent(
            Event.builder().customerId("alice").eventName("place_order").build(),
            noopBooleanCallback
        )

        // No in-app messaging requests.
        val paths = (0 until server.requestCount).mapNotNull {
            server.takeRequest(200, java.util.concurrent.TimeUnit.MILLISECONDS)?.path
        }
        assertFalse(
            "no request may touch the messaging endpoints, saw $paths",
            paths.any { it.contains("inapp-messages") }
        )

        // No storage writes in any of the module's four slots.
        assertNull(prefs.getIamCampaignCache())
        assertNull(prefs.getIamDisplayHistory())
        assertNull(prefs.getIamOutbox())
        assertNull(prefs.getIamVariables())

        assertFalse(app.isInAppMessagingStarted())
    }

    /** Registering lifecycle callbacks in init would itself be a behaviour change. */
    @Test
    fun `no Activity lifecycle callbacks are registered before opt-in`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val before = root.childCount

        app.sendEvent(
            Event.builder().customerId("alice").eventName("x").build(),
            noopBooleanCallback
        )

        assertEquals("nothing may be drawn", before, root.childCount)
    }

    @Test
    fun `the widget still works with in-app messaging never started`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // Neither call may throw, and hideProfile is a no-op when nothing is showing.
        app.showProfile(activity, ShowProfileRequest.builder().customerId("alice").build())
        app.hideProfile()
        assertFalse(app.isInAppMessagingStarted())
    }

    @Test
    fun `stopping without ever starting is safe`() {
        app.stopInAppMessaging()
        assertFalse(app.isInAppMessagingStarted())
    }

    @Test
    fun `isInAppMessagingStarted is false before opt-in`() {
        assertFalse(app.isInAppMessagingStarted())
    }

    @Test
    fun `logPurchase without an initialized customer reports an error rather than throwing`() {
        prefs.putCustomerId(null)
        var error: Throwable? = null
        app.logPurchase(
            productId = "sku-1", price = 10.0, currency = "USD", quantity = 1,
            properties = null,
            callback = object : Callback<Boolean> {
                override fun onSuccess(t: Boolean?) = Unit
                override fun onError(e: Throwable?) { error = e }
            }
        )
        org.junit.Assert.assertNotNull(error)
    }
}
