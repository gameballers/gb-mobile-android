package com.gameball.gameball.inappmessaging

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import com.gameball.gameball.inappmessaging.artwork.PicassoArtworkPrefetcher
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamApi
import com.gameball.gameball.inappmessaging.data.IamStore
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.PersistentAnalyticsOutbox
import com.gameball.gameball.inappmessaging.data.RemoteMessageSource
import com.gameball.gameball.inappmessaging.data.RemoteVariableSource
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.model.DisplayDecision
import com.gameball.gameball.inappmessaging.model.GameballMessageAction
import com.gameball.gameball.inappmessaging.model.InAppMessage
import com.gameball.gameball.inappmessaging.model.InAppMessageButton
import com.gameball.gameball.inappmessaging.runtime.ActivityTracker
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.HostHooks
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.gameball.gameball.inappmessaging.runtime.InAppMessagingService
import com.gameball.gameball.inappmessaging.runtime.MessagePresenter
import com.gameball.gameball.inappmessaging.runtime.SessionState
import com.gameball.gameball.inappmessaging.runtime.SystemClock
import com.gameball.gameball.inappmessaging.ui.OverlayPresenter
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.utils.LanguageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Owns the in-app messaging subsystem and everything it holds.
 *
 * Nothing here is constructed until [start] is called. Until then the module makes no
 * requests, arms no timers, writes no storage, draws nothing, and registers no Activity
 * lifecycle callbacks - an integrator who upgrades the SDK without opting in sees
 * byte-identical behaviour. CompatibilityInvariantTest asserts exactly that.
 */
class GameballInAppMessaging internal constructor(private val appContext: Context) {

    private var scope: CoroutineScope? = null
    private var service: InAppMessagingService? = null
    private var tracker: ActivityTracker? = null
    private var presenter: OverlayPresenter? = null
    private var options: InAppMessagingOptions? = null

    /** Test seams. Set before start; ignored once running. */
    @VisibleForTesting internal var sourceOverride: MessageSource? = null
    @VisibleForTesting internal var analyticsOverride: MessageAnalytics? = null
    @VisibleForTesting internal var artworkOverride: ArtworkPrefetcher? = null
    @VisibleForTesting internal var variablesOverride: VariableSource? = null
    @VisibleForTesting internal var presenterOverride: MessagePresenter? = null
    @VisibleForTesting internal var clockOverride: Clock? = null

    val isStarted: Boolean get() = service?.isStarted == true

    fun start(
        customerId: String,
        options: InAppMessagingOptions,
        apiPrefix: String?,
        sdkVersion: String
    ) {
        if (customerId.isBlank()) {
            IamLog.e("startInAppMessaging needs a customer id; ignoring the call")
            return
        }
        this.options = options
        val existing = service
        if (existing != null) {
            existing.setHooks(hooks(options))
            existing.start(customerId)
            return
        }

        IamImageLoader.init(appContext)
        val clock = clockOverride ?: SystemClock
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val store = IamStore(SharedPreferencesUtils.getInstance())
        val api = IamApi.create(apiPrefix)

        val activityTracker = ActivityTracker(
            appContext.applicationContext as Application,
            object : ActivityTracker.Callbacks {
                override fun onAppForegrounded() { service?.onAppForegrounded() }
                override fun onAppBackgrounded() { service?.onAppBackgrounded() }
                override fun onSurfaceAvailable() { service?.onSurfaceAvailable() }
            }
        )
        val overlay = OverlayPresenter(activityTracker, clock)

        val created = InAppMessagingService(
            scope = newScope,
            clock = clock,
            source = sourceOverride ?: RemoteMessageSource(
                api = api,
                localeProvider = { LanguageUtils.handleLanguage() },
                appVersion = options.appVersion,
                sdkVersion = sdkVersion
            ),
            cache = CampaignCache(store),
            history = DisplayHistory(store),
            artwork = artworkOverride ?: PicassoArtworkPrefetcher(newScope),
            analytics = analyticsOverride
                ?: PersistentAnalyticsOutbox(api, store, newScope, clock),
            variables = variablesOverride
                ?: RemoteVariableSource(api, store, newScope, clock),
            presenter = presenterOverride ?: overlay,
            sessionState = SessionState(clock, options.sessionTimeoutSeconds * 1000L),
            hooks = hooks(options)
        )

        scope = newScope
        service = created
        tracker = activityTracker
        presenter = overlay

        // Registered on opt-in, never in init.
        activityTracker.register()
        created.start(customerId)
    }

    fun stop() {
        service?.stop()
        tracker?.unregister()
        // The service flushes before disposing, so the scope is cancelled after it, not with it.
        scope?.cancel()
        scope = null
        service = null
        tracker = null
        presenter = null
    }

    /**
     * Follows a customer change, keeping the options - and therefore the host's hooks - that
     * the original start was given. Restarting with fresh defaults would silently drop them.
     */
    fun onCustomerChanged(customerId: String, apiPrefix: String?, sdkVersion: String) {
        val existing = options ?: return
        start(customerId, existing, apiPrefix, sdkVersion)
    }

    fun onEvent(name: String, metadata: Map<String, Any?>) {
        service?.onEvent(name, metadata)
    }

    /** Every hook is wrapped so a throwing host loses its override, not its messages. */
    private fun hooks(options: InAppMessagingOptions) = HostHooks(
        beforeDisplay = { campaign ->
            val hook = options.beforeDisplay ?: return@HostHooks DisplayDecision.SHOW
            try {
                hook.beforeDisplay(campaign.toPublic())
            } catch (t: Throwable) {
                IamLog.e("the host's beforeDisplay hook threw; showing the message", t)
                DisplayDecision.SHOW
            }
        },
        onAction = { campaign, button, action ->
            val hook = options.onAction
            val handled = if (hook == null) false else try {
                hook.onAction(campaign.toPublic(), button?.toPublic(), action.toPublic())
            } catch (t: Throwable) {
                IamLog.e("the host's onAction hook threw; falling back to built-in handling", t)
                false
            }
            if (!handled) performBuiltIn(action)
            handled
        },
        onNavigate = { route, arguments ->
            try {
                options.onNavigate?.onNavigate(route, arguments)
                    ?: IamLog.w("campaign wants route '$route' but no navigator is set")
            } catch (t: Throwable) {
                IamLog.e("the host's onNavigate hook threw for route '$route'", t)
            }
        },
        observer = { campaign ->
            try {
                options.observer?.onMessageSelected(campaign.toPublic())
            } catch (t: Throwable) {
                IamLog.e("the host's message observer threw", t)
            }
        }
    )

    /** open_url is the one action the SDK performs itself. */
    private fun performBuiltIn(action: MessageAction) {
        if (action !is MessageAction.OpenUrl) return
        val activity = tracker?.currentActivity
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
            if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            (activity ?: appContext).startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            IamLog.w("nothing on this device can open ${action.url}")
        } catch (t: Throwable) {
            IamLog.e("could not open ${action.url}", t)
        }
    }
}

// --- internal -> public mirrors ---

internal fun Campaign.toPublic() = InAppMessage(
    campaignId = campaignId,
    variationId = variationId,
    name = name,
    messageType = rawMessageType,
    header = content.header,
    body = content.body,
    buttons = content.buttons.map { it.toPublic() },
    isTest = isTest
)

internal fun MessageButton.toPublic() = InAppMessageButton(id, text)

internal fun MessageAction.toPublic(): GameballMessageAction = when (this) {
    is MessageAction.Dismiss -> GameballMessageAction.Dismiss
    is MessageAction.OpenUrl -> GameballMessageAction.OpenUrl(url, external)
    is MessageAction.Navigate -> GameballMessageAction.Navigate(route, arguments)
    // Unsupported types reach a host as a dismiss rather than leaking an internal shape.
    is MessageAction.Unsupported -> GameballMessageAction.Dismiss
}
