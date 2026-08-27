# In-App Messaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in in-app messaging subsystem to the Gameball Android SDK that syncs campaigns, selects at most one per trigger, draws it above the host's UI, and reports impressions, clicks and dismissals — without changing behaviour for any existing widget integration.

**Architecture:** An isolated `com.gameball.gameball.inappmessaging` package inside `:gameballsdk`. Everything is `internal` except four entry points on `GameballApp`, a small public model, and four host hooks. Pure domain logic (parser, selector, personalisation) has no Android dependency and is unit-tested directly; the five I/O boundaries (`MessageSource`, `ArtworkPrefetcher`, `MessagePresenter`, `MessageAnalytics`, `VariableSource`) are interfaces with fakes in tests.

**Tech Stack:** Kotlin 2.0, coroutines 1.9.0, Retrofit 2.9 (suspend functions), OkHttp 4.9.2, Gson (hand-walked `JsonObject`, never reflective binding), Picasso 2.71828, ConstraintLayout 2.2.1, Material 1.12.0, XML layouts. Test-only: JUnit 4.13.2, Robolectric, MockWebServer, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-08-27-in-app-messaging-design.md` — read it alongside this plan. Every task references its sections.

## Global Constraints

Every task's requirements implicitly include all of these.

- **minSdk 21, no core library desugaring.** `java.time` (`Instant`, `LocalTime`, `Duration`) is unavailable. Use `System.currentTimeMillis()` and `SimpleDateFormat`. Do not raise `minSdk`.
- **Every formatter and every number on the wire is `Locale.US`.** On an Arabic-locale device a default-locale formatter emits Arabic-Indic digits, which is not valid ISO-8601 and 400s the whole analytics batch. Quiet-hours `HH:mm` is parsed by splitting on `:` and `toIntOrNull()`, never by a date formatter.
- **No new runtime dependencies.** Test-only (`testImplementation`) additions are fine.
- **Everything under `inappmessaging/` is `internal`** except `GameballInAppMessaging`, `InAppMessagingOptions`, the four hook interfaces, and the `model/` package. Kotlin forbids `internal` types in public signatures, which is why `model/` mirrors the internal domain types.
- **Nothing outside `inappmessaging.*` may import it**, except `GameballApp`, which calls the public facade.
- **The compatibility invariant:** until `startInAppMessaging` is called there must be no requests, no timers, no storage writes, nothing drawn, and no lifecycle callbacks registered. Task 20 asserts this.
- **The parser must never throw.** Malformed input returns an empty result plus a log line.
- **Diagnostics go through `IamLog`, never `GameballLogger`** — the latter posts telemetry to the Gameball backend.
- **`platform` is always `2`** on every request body. Log loudly before sending anything else.
- **All IAM endpoints are pinned to `api/v4.0/`.** v4.1 answers 401 to APIKey auth.
- **Java interop is an existing guarantee.** Public entry points keep `@JvmStatic` / `@JvmOverloads`; hooks are interfaces, not Kotlin function types.
- **Resource names are prefixed `gb_iam_`** so a host app's own `modal_message.xml` cannot collide.
- **Text sizes in `sp`, dimensions in `dp`, touch targets ≥ 48dp, `start`/`end` never `left`/`right`.**

---

## File Structure

### Modified (three files, all additive)

| File | Change |
|---|---|
| `gameballsdk/build.gradle` | test-only dependencies + `testOptions` |
| `gameballsdk/src/main/java/com/gameball/gameball/network/interceptor/HeaderInterceptor.java` | one condition — the L1 exemption |
| `gameballsdk/src/main/java/com/gameball/gameball/network/Config.kt` | three endpoint constants + one path segment |
| `gameballsdk/src/main/java/com/gameball/gameball/local/SharedPreferencesUtils.kt` | four key accessor pairs for IAM storage |
| `gameballsdk/src/main/java/com/gameball/gameball/GameballApp.kt` | four entry points + three wiring call-sites |

### Created

```
inappmessaging/
  GameballInAppMessaging.kt      public facade, owns the service instance
  InAppMessagingOptions.kt       public options + the four hook interfaces
  model/InAppMessage.kt          public mirrors: InAppMessage, InAppMessageButton,
                                 GameballMessageAction, DisplayDecision
  runtime/IamLog.kt              [GameballIAM]-tagged developer log
  runtime/Clock.kt               injectable clock
  runtime/ActivityTracker.kt     lifecycle callbacks, weak Activity ref, fg/bg counting
  runtime/InAppMessagingService.kt  sequencing, pending slot, deferral
  domain/Model.kt                Campaign, MessageContent, MessageButton, colours, enums
  domain/QuietHours.kt           parse + contains, UTC, half-open, wraps midnight
  domain/Trigger.kt              Trigger, MetadataFilter, FilterOperator
  domain/FilterEvaluator.kt      the seven operators
  domain/MessageSelector.kt      pure selection
  domain/Personalization.kt      token scan, substitute, blanking pass
  data/IamTime.kt                Locale.US ISO-8601 UTC format + parse
  data/ColorParser.kt            #RRGGBB / #AARRGGBB / packed int
  data/MessageParser.kt          hand-walked JsonObject, never throws
  data/IamApi.kt                 Retrofit suspend interface + DTOs
  data/MessageSource.kt          interface + RemoteMessageSource
  data/VariableSource.kt         interface + RemoteVariableSource (60s cache, 2s bound)
  data/IamStore.kt               per-customer scoped SharedPreferences wrapper
  data/DisplayHistory.kt         impression records, survives restart
  data/CampaignCache.kt          raw payload, re-parsed on read
  data/AnalyticsOutbox.kt        MessageAnalytics interface + implementation
  artwork/ArtworkPrefetcher.kt   interface + PicassoArtworkPrefetcher
  ui/MessageMetrics.kt           every constant from the UI spec, one file
  ui/ColorResolver.kt            close-glyph derivation, theme fallbacks
  ui/MessageViewFactory.kt       builds buttons + close glyph shared across types
  ui/SlideupMessageView.kt
  ui/ModalMessageView.kt
  ui/FullscreenMessageView.kt
  ui/OverlayPresenter.kt         MessagePresenter impl; content-root attach
res/layout/
  gb_iam_slideup.xml
  gb_iam_modal.xml
  gb_iam_fullscreen.xml
res/values/gb_iam_strings.xml    close-button content description
res/values-ar/gb_iam_strings.xml
```

### Test files

```
src/test/java/com/gameball/gameball/
  network/interceptor/HeaderInterceptorTest.kt
  inappmessaging/data/IamTimeTest.kt
  inappmessaging/data/ColorParserTest.kt
  inappmessaging/domain/QuietHoursTest.kt
  inappmessaging/data/MessageParserEnvelopeTest.kt
  inappmessaging/data/MessageParserContentTest.kt
  inappmessaging/data/MessageParserTriggerTest.kt
  inappmessaging/data/MessageParserLivePayloadTest.kt
  inappmessaging/domain/FilterEvaluatorTest.kt
  inappmessaging/domain/MessageSelectorTest.kt
  inappmessaging/data/PersistenceTest.kt
  inappmessaging/data/RemoteMessageSourceTest.kt
  inappmessaging/data/AnalyticsOutboxTest.kt
  inappmessaging/domain/PersonalizationTest.kt
  inappmessaging/data/RemoteVariableSourceTest.kt
  inappmessaging/artwork/ArtworkPrefetcherTest.kt
  inappmessaging/runtime/SessionLifecycleTest.kt
  inappmessaging/runtime/InAppMessagingServiceTest.kt
  inappmessaging/ui/ColorResolverTest.kt
  inappmessaging/EndToEndTest.kt
  inappmessaging/CompatibilityInvariantTest.kt
src/test/resources/
  live_sync_payload.json         captured from api.alpha.gameball.app
```

---

## Task 1: Foundations and the L1 guard

Spec §3 D2, §5, §14.3. This is first because it invalidates work you would otherwise do: every IAM request 401s until the interceptor is disarmed.

**Files:**
- Modify: `gameballsdk/build.gradle`
- Modify: `gameballsdk/src/main/java/com/gameball/gameball/network/Config.kt`
- Modify: `gameballsdk/src/main/java/com/gameball/gameball/network/interceptor/HeaderInterceptor.java:41-49`
- Create: `gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/runtime/IamLog.kt`
- Create: `gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/runtime/Clock.kt`
- Test: `gameballsdk/src/test/java/com/gameball/gameball/network/interceptor/HeaderInterceptorTest.kt`

**Interfaces:**
- Produces: `Config.InAppMessagesSync`, `Config.InAppMessagesEvents`, `Config.InAppMessagesVariables`, `Config.IAM_PATH_SEGMENT` (all `const val String`); `IamLog.d/w/e(String)`, `IamLog.e(String, Throwable?)`; `Clock` (`fun interface`, `fun nowMillis(): Long`) and `SystemClock: Clock`.

- [ ] **Step 1: Add test-only dependencies and unit-test options**

In `gameballsdk/build.gradle`, inside the existing `android { }` block, after `buildFeatures { }`:

```gradle
    testOptions {
        unitTests {
            includeAndroidResources = true
            returnDefaultValues = true
        }
    }
```

And in `dependencies { }`, alongside the existing `testImplementation 'junit:junit:4.13.2'`:

```gradle
    testImplementation 'org.robolectric:robolectric:4.11.1'
    testImplementation 'androidx.test:core:1.5.0'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0'
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.9.2'
```

These are `testImplementation`, so nothing reaches consumers.

- [ ] **Step 2: Add the endpoint constants**

In `network/Config.kt`, add inside `object Config`:

```kotlin
    /**
     * In-app messaging lives on v4.0 only. /api/v4.1/.../inapp-messages/sync exists and answers
     * 401 to APIKey auth, so HeaderInterceptor must not version-switch these paths.
     */
    const val IAM_PATH_SEGMENT = "inapp-messages"
    private const val IAM_BASE = "api/v4.0/integrations/inapp-messages"
    const val InAppMessagesSync = "$IAM_BASE/sync"
    const val InAppMessagesEvents = "$IAM_BASE/events"
    const val InAppMessagesVariables = "$IAM_BASE/variables"
```

`const val` initialised from another `const val` is still a compile-time constant, so these are usable in Retrofit annotations.

- [ ] **Step 3: Write the failing L1 test**

Create `src/test/java/com/gameball/gameball/network/interceptor/HeaderInterceptorTest.kt`:

```kotlin
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
```

- [ ] **Step 4: Run it and watch the first two tests fail**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*HeaderInterceptorTest*'`
Expected: `iam sync stays on v4_0…` and `iam events and variables…` FAIL, asserting `/api/v4.1/…` was seen. The two control tests pass. **This failure is the bug in production today** — do not skip watching it.

- [ ] **Step 5: Apply the exemption**

In `HeaderInterceptor.java`, change the rewrite condition:

```java
                // Switch to secure endpoint (v4.1) if sessionToken is present.
                // In-app messaging is exempt: v4.1 exists for those paths and answers 401 to
                // APIKey auth, which would silently kill messaging for every integration that
                // sets a token. See Config.IAM_PATH_SEGMENT.
                String path = request.url().encodedPath();
                if (path.contains(Config.API_V4_0) && !path.contains(Config.IAM_PATH_SEGMENT)) {
                    String newPath = path.replace(Config.API_V4_0, Config.API_V4_1);
                    builder.url(request.url().newBuilder().encodedPath(newPath).build());
                }
```

- [ ] **Step 6: Run the tests and verify all four pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*HeaderInterceptorTest*'`
Expected: PASS, 4 tests.

- [ ] **Step 7: Add IamLog**

Create `inappmessaging/runtime/IamLog.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.runtime

import android.util.Log

/**
 * Developer-facing diagnostics for the in-app messaging module.
 *
 * Deliberately separate from [com.gameball.gameball.logging.GameballLogger], which posts
 * telemetry to the Gameball backend. Nothing here leaves the device.
 *
 * Filter with: adb logcat -s GameballIAM
 */
internal object IamLog {
    private const val TAG = "GameballIAM"

    /** Host apps can silence the module's logs; on by default so integrators can diagnose. */
    @Volatile
    @JvmStatic
    var enabled: Boolean = true

    fun d(message: String) { if (enabled) Log.d(TAG, message) }
    fun w(message: String) { if (enabled) Log.w(TAG, message) }
    fun e(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
```

- [ ] **Step 8: Add the injectable clock**

Create `inappmessaging/runtime/Clock.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.runtime

/**
 * Wall-clock time, injectable so selection and session logic are testable without waiting.
 *
 * Wall clock rather than a monotonic one is deliberate: repeat rules and the cooldown floor
 * must survive process death, and a monotonic clock does not. A customer who moves the device
 * clock backwards can suppress messages. Accepted and unfixable client-side.
 */
internal fun interface Clock {
    fun nowMillis(): Long
}

internal val SystemClock = Clock { System.currentTimeMillis() }
```

- [ ] **Step 9: Verify the whole module still compiles and existing tests pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest`
Expected: PASS, including the pre-existing `ExampleUnitTest`.

- [ ] **Step 10: Commit**

```bash
git add gameballsdk/build.gradle \
        gameballsdk/src/main/java/com/gameball/gameball/network/Config.kt \
        gameballsdk/src/main/java/com/gameball/gameball/network/interceptor/HeaderInterceptor.java \
        gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/runtime/ \
        gameballsdk/src/test/java/com/gameball/gameball/network/interceptor/HeaderInterceptorTest.kt
git commit -m "fix: pin in-app messaging endpoints to v4.0 in HeaderInterceptor

The interceptor rewrites any api/v4.0/ path to api/v4.1/ whenever a session
token is stored. The IAM endpoints live under api/v4.0/integrations/inapp-messages/,
and v4.1 answers 401 to APIKey auth, so messaging would have been dead in
production for every integration that sets a token while working perfectly in
testing.

Exempts the inapp-messages path segment and adds the regression test. Existing
endpoints are unaffected: no path in Config contains the segment, so the added
condition cannot change their behaviour.

Also adds IamLog and Clock, plus test-only dependencies."
```

---

## Task 2: Time, colour and quiet hours

Spec §3 D4, §6.9, §7.2. Three pure value parsers with no dependency on anything else. Each has a minSdk-21 trap in it.

**Files:**
- Create: `inappmessaging/data/IamTime.kt`
- Create: `inappmessaging/data/ColorParser.kt`
- Create: `inappmessaging/domain/QuietHours.kt`
- Test: `inappmessaging/data/IamTimeTest.kt`, `inappmessaging/data/ColorParserTest.kt`, `inappmessaging/domain/QuietHoursTest.kt`

**Interfaces:**
- Consumes: `IamLog` (Task 1).
- Produces:
  - `IamTime.toIso8601Utc(millis: Long): String`, `IamTime.parseIso8601(value: String?): Long?`
  - `ColorParser.parse(value: Any?): Int?`
  - `QuietHours(startMinute: Int, endMinute: Int)` with `contains(nowMillis: Long): Boolean`; `QuietHours.from(enabled: Boolean?, start: String?, end: String?): QuietHours?`; `QuietHours.minuteOfDayUtc(millis: Long): Int`

- [ ] **Step 1: Write the failing time test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/IamTimeTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class IamTimeTest {

    /** 2026-08-27T10:30:00.123Z */
    private val instant = 1787826600123L

    @Test
    fun `formats as ISO-8601 UTC with millisecond precision`() {
        assertEquals("2026-08-27T10:30:00.123Z", IamTime.toIso8601Utc(instant))
    }

    /**
     * L3: on an Arabic-locale device a default-locale SimpleDateFormat emits Arabic-Indic
     * digits, which is not valid ISO-8601 and 400s the entire analytics batch.
     */
    @Test
    fun `formats in ASCII digits with the default locale set to Arabic`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale("ar", "EG"))
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            val formatted = IamTime.toIso8601Utc(instant)
            assertEquals("2026-08-27T10:30:00.123Z", formatted)
            assertTrue(
                "expected ASCII digits, got $formatted",
                formatted.all { it.code < 128 }
            )
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun `parses the format it emits`() {
        assertEquals(instant, IamTime.parseIso8601("2026-08-27T10:30:00.123Z"))
    }

    @Test
    fun `parses without milliseconds`() {
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T10:30:00Z"))
    }

    @Test
    fun `parses a numeric offset, normalising the colon form`() {
        // 13:30 at +03:00 is the same instant as 10:30Z
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T13:30:00+03:00"))
    }

    @Test
    fun `treats a missing zone as UTC`() {
        assertEquals(1787826600000L, IamTime.parseIso8601("2026-08-27T10:30:00"))
    }

    @Test
    fun `returns null rather than throwing on junk`() {
        assertNull(IamTime.parseIso8601(null))
        assertNull(IamTime.parseIso8601(""))
        assertNull(IamTime.parseIso8601("   "))
        assertNull(IamTime.parseIso8601("not a date"))
        assertNull(IamTime.parseIso8601("2026-13-45T99:99:99Z"))
    }
}
```

Add the import `org.junit.Assert.assertTrue` at the top.

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*IamTimeTest*'`
Expected: FAIL — unresolved reference `IamTime`.

- [ ] **Step 3: Implement IamTime**

Create `inappmessaging/data/IamTime.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ISO-8601 UTC formatting and lenient parsing for the wire.
 *
 * Every formatter here pins [Locale.US]. On an Arabic-locale device a default-locale
 * SimpleDateFormat emits Arabic-Indic digits, which is neither valid ISO-8601 nor a valid
 * integer, and the whole analytics batch 400s.
 *
 * SimpleDateFormat is not thread-safe, hence the ThreadLocal.
 */
internal object IamTime {

    private const val EMIT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /**
     * The ISO 'XXX' offset pattern is API 24+, so offsets are normalised to the RFC-822 'Z'
     * form ("+0000") before parsing. Order matters: most specific first.
     */
    private val PARSE_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )

    private val OFFSET_WITH_COLON = Regex("""([+-]\d{2}):(\d{2})$""")

    private val emitter = ThreadLocal.withInitial {
        SimpleDateFormat(EMIT_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun toIso8601Utc(millis: Long): String = emitter.get()!!.format(Date(millis))

    /** Returns null for anything unparseable. Never throws. */
    fun parseIso8601(value: String?): Long? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        var normalised = trimmed
        if (normalised.endsWith("Z") || normalised.endsWith("z")) {
            normalised = normalised.dropLast(1) + "+0000"
        }
        normalised = OFFSET_WITH_COLON.replace(normalised) { m ->
            m.groupValues[1] + m.groupValues[2]
        }

        for (pattern in PARSE_PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            try {
                val parsed = format.parse(normalised) ?: continue
                // Reject trailing junk that a lenient-ish parse would otherwise swallow.
                return parsed.time
            } catch (_: ParseException) {
                // try the next pattern
            }
        }
        return null
    }
}
```

- [ ] **Step 4: Run the time tests and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*IamTimeTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 5: Write the failing colour test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/ColorParserTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorParserTest {

    @Test
    fun `six hex digits are promoted to full opacity`() {
        assertEquals(0xFFFFFFFF.toInt(), ColorParser.parse("#FFFFFF"))
        assertEquals(0xFF111827.toInt(), ColorParser.parse("#111827"))
    }

    @Test
    fun `the hash is optional`() {
        assertEquals(0xFF111827.toInt(), ColorParser.parse("111827"))
        assertEquals(0x99000000.toInt(), ColorParser.parse("99000000"))
    }

    @Test
    fun `eight hex digits are read as ARGB, not RGBA`() {
        // #80FF0000 is a half-transparent red: alpha 0x80, red 0xFF.
        val parsed = ColorParser.parse("#80FF0000")!!
        assertEquals(0x80, (parsed ushr 24) and 0xFF)
        assertEquals(0xFF, (parsed ushr 16) and 0xFF)
        assertEquals(0x00, (parsed ushr 8) and 0xFF)
        assertEquals(0x00, parsed and 0xFF)
    }

    @Test
    fun `a raw packed integer is accepted as Braze encodes it`() {
        assertEquals(0xFF111827.toInt(), ColorParser.parse(0xFF111827.toInt()))
        assertEquals(0xFF111827.toInt(), ColorParser.parse(0xFF111827L))
    }

    @Test
    fun `whitespace is tolerated`() {
        assertEquals(0xFFFFFFFF.toInt(), ColorParser.parse("  #FFFFFF  "))
    }

    @Test
    fun `alpha is honoured and never clamped to opaque`() {
        assertEquals(0x99000000.toInt(), ColorParser.parse("#99000000"))
    }

    @Test
    fun `malformed values return null so that one slot falls back`() {
        assertNull(ColorParser.parse(null))
        assertNull(ColorParser.parse(""))
        assertNull(ColorParser.parse("#FFF"))          // wrong length
        assertNull(ColorParser.parse("#GGGGGG"))       // non-hex
        assertNull(ColorParser.parse("red"))           // named colour
        assertNull(ColorParser.parse("rgb(1,2,3)"))
        assertNull(ColorParser.parse(true))
    }
}
```

- [ ] **Step 6: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*ColorParserTest*'`
Expected: FAIL — unresolved reference `ColorParser`.

- [ ] **Step 7: Implement ColorParser**

Create `inappmessaging/data/ColorParser.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Parses every colour the wire can carry: #RRGGBB, #AARRGGBB, either without the hash, and a
 * raw packed 32-bit integer.
 *
 * The wire is ARGB and Android's Color is ARGB, so there is no channel shuffling. Alpha is
 * honoured everywhere, including on the message background — a campaign may deliberately make
 * a modal card translucent.
 *
 * Anything else logs and returns null, which means that one slot falls back to the host theme
 * while the rest of the message renders normally. A malformed colour never costs the customer
 * the message.
 */
internal object ColorParser {

    fun parse(value: Any?): Int? = when (value) {
        null -> null
        is Int -> value
        is Long -> value.toInt()
        is Number -> value.toInt()
        is String -> parseString(value)
        else -> {
            IamLog.w("ignoring malformed colour of type ${value.javaClass.simpleName}: $value")
            null
        }
    }

    private fun parseString(raw: String): Int? {
        var hex = raw.trim().removePrefix("#")
        if (hex.length == 6) hex = "FF$hex"
        if (hex.length != 8) {
            IamLog.w("ignoring malformed colour: '$raw'")
            return null
        }
        val parsed = hex.toLongOrNull(16)
        if (parsed == null) {
            IamLog.w("ignoring malformed colour: '$raw'")
            return null
        }
        return parsed.toInt()
    }
}
```

- [ ] **Step 8: Run the colour tests and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*ColorParserTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 9: Write the failing quiet-hours test**

Create `src/test/java/com/gameball/gameball/inappmessaging/domain/QuietHoursTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class QuietHoursTest {

    /**
     * Builds an instant from a UTC wall clock. Test instants must be built in UTC — a local
     * literal is judged by its UTC equivalent, so the same test would pass in Cairo and fail
     * in Los Angeles.
     */
    private fun utc(hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.AUGUST, 27, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `a same-day window contains only its own hours`() {
        val window = QuietHours.from(true, "09:00", "17:00")!!
        assertFalse(window.contains(utc(8, 59)))
        assertTrue(window.contains(utc(9, 0)))
        assertTrue(window.contains(utc(12, 0)))
        assertTrue(window.contains(utc(16, 59)))
        assertFalse(window.contains(utc(17, 0)))
    }

    @Test
    fun `the window is half-open so adjacent windows do not overlap`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue("the start minute is inside", window.contains(utc(22, 0)))
        assertFalse("the end minute is not", window.contains(utc(8, 0)))
    }

    /** 22:00 -> 08:00 is what the backend actually sends. It is two ranges, not one. */
    @Test
    fun `a window wrapping midnight covers both sides`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertTrue(window.contains(utc(22, 30)))
        assertTrue(window.contains(utc(23, 59)))
        assertTrue(window.contains(utc(0, 0)))
        assertTrue(window.contains(utc(3, 0)))
        assertTrue(window.contains(utc(7, 59)))
        assertFalse(window.contains(utc(8, 1)))
        assertFalse(window.contains(utc(12, 0)))
        assertFalse(window.contains(utc(21, 59)))
    }

    @Test
    fun `seconds in the wire value are tolerated`() {
        val window = QuietHours.from(true, "22:00:00", "08:00:00")!!
        assertTrue(window.contains(utc(23, 0)))
        assertFalse(window.contains(utc(9, 0)))
    }

    /**
     * Zero-length and twenty-four-hours look identical on the wire. Silencing an entire
     * account over a typo is the worse reading, so the window is refused.
     */
    @Test
    fun `start equal to end is refused`() {
        assertNull(QuietHours.from(true, "08:00", "08:00"))
    }

    @Test
    fun `disabled, absent and malformed all mean no window`() {
        assertNull(QuietHours.from(false, "22:00", "08:00"))
        assertNull(QuietHours.from(null, "22:00", "08:00"))
        assertNull(QuietHours.from(true, null, "08:00"))
        assertNull(QuietHours.from(true, "22:00", null))
        assertNull(QuietHours.from(true, "", ""))
        assertNull(QuietHours.from(true, "25:00", "08:00"))
        assertNull(QuietHours.from(true, "22:61", "08:00"))
        assertNull(QuietHours.from(true, "ten o'clock", "08:00"))
        assertNull(QuietHours.from(true, "2200", "0800"))
    }

    @Test
    fun `minute of day is computed in UTC regardless of the device timezone`() {
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(22 * 60 + 30, QuietHours.minuteOfDayUtc(utc(22, 30)))
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            assertEquals(22 * 60 + 30, QuietHours.minuteOfDayUtc(utc(22, 30)))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }
}
```

- [ ] **Step 10: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*QuietHoursTest*'`
Expected: FAIL — unresolved reference `QuietHours`.

- [ ] **Step 11: Implement QuietHours**

Create `inappmessaging/domain/QuietHours.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * A global suppression window, sent at the sync response root as
 * {enabled, start, end} alongside cooldownSeconds. No campaign carries its own.
 *
 * The times are UTC — confirmed with the backend team. The strings carry no zone and the
 * obvious reading, the customer's local wall clock, is wrong: at UTC+3 the two
 * interpretations disagree for six hours of every day, in both directions.
 *
 * The window is half-open (the start minute is inside, the end minute is not) and it wraps
 * midnight, so 22:00 -> 08:00 is two ranges rather than one.
 */
internal data class QuietHours(
    val startMinute: Int,
    val endMinute: Int
) {

    fun contains(nowMillis: Long): Boolean {
        val minute = minuteOfDayUtc(nowMillis)
        return if (startMinute < endMinute) {
            minute >= startMinute && minute < endMinute
        } else {
            // wraps midnight
            minute >= startMinute || minute < endMinute
        }
    }

    companion object {

        private const val MINUTES_PER_DAY = 1440L

        /**
         * Minute of day in UTC. Math.floorMod is API 24, hence the manual wrap.
         */
        fun minuteOfDayUtc(millis: Long): Int {
            var minutes = (millis / 60_000L) % MINUTES_PER_DAY
            if (minutes < 0) minutes += MINUTES_PER_DAY
            return minutes.toInt()
        }

        /**
         * Returns null — meaning "no window" — for every unusable input, logging which.
         */
        fun from(enabled: Boolean?, start: String?, end: String?): QuietHours? {
            if (enabled != true) {
                if (enabled == null) IamLog.d("quiet hours absent or not enabled; no window")
                return null
            }
            val startMinute = parseMinutes(start)
            val endMinute = parseMinutes(end)
            if (startMinute == null || endMinute == null) {
                IamLog.w("quiet hours malformed (start='$start', end='$end'); no window")
                return null
            }
            if (startMinute == endMinute) {
                IamLog.w(
                    "quiet hours refused: start == end ('$start'). Zero-length and " +
                        "twenty-four-hours are indistinguishable on the wire; no window"
                )
                return null
            }
            return QuietHours(startMinute, endMinute)
        }

        /**
         * Parses "HH:mm", tolerating "HH:mm:ss". Split and toIntOrNull rather than a date
         * formatter, which would emit Arabic-Indic digits on an Arabic-locale device.
         */
        private fun parseMinutes(value: String?): Int? {
            val parts = value?.trim()?.split(":") ?: return null
            if (parts.size < 2) return null
            val hours = parts[0].toIntOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null
            if (hours !in 0..23 || minutes !in 0..59) return null
            return hours * 60 + minutes
        }
    }
}
```

- [ ] **Step 12: Run the quiet-hours tests and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*QuietHoursTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 13: Prove the wrap test can fail**

Temporarily change `contains` to the naive `minute >= startMinute && minute < endMinute` for both branches. Re-run. Expected: `a window wrapping midnight covers both sides` FAILS. Restore.

This is the check worth doing by hand — the naive form is false for every minute of the window the backend actually sends.

- [ ] **Step 14: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/ \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/
git commit -m "feat(iam): add time, colour and quiet-hours value parsing

IamTime pins Locale.US on every formatter, guarding against Arabic-Indic
digits reaching the wire, and normalises offsets to the RFC-822 form because
the ISO XXX pattern is API 24+.

QuietHours is UTC, half-open, and wraps midnight; start == end is refused
because zero-length and twenty-four-hours are indistinguishable on the wire.
Minute-of-day is computed manually since Math.floorMod is API 24."
```

---

## Task 3: The domain model

Spec §6.1, §6.6, §6.7, §6.8. Plain data with two pieces of behaviour worth testing: the two enum mappings that decide whether a campaign survives.

**Files:**
- Create: `inappmessaging/domain/Model.kt`
- Create: `inappmessaging/domain/Trigger.kt`
- Test: `inappmessaging/domain/ModelTest.kt`

**Interfaces:**
- Consumes: `QuietHours` (Task 2).
- Produces: `MessageType`, `SlidePosition`, `MessageOrientation`, `MessageLayout`, `TextAlign`, `MessageAction` (sealed), `MessageColors`, `ButtonColors`, `MessageButton`, `MessageContent`, `Campaign`, `SyncResult`, `DEFAULT_COOLDOWN_SECONDS`, `TriggerType`, `Trigger`, `FilterOperator`, `MetadataFilter`, `TriggerOccurrence` (sealed). Exact shapes below — later tasks construct these by name.

- [ ] **Step 1: Write the failing enum-mapping test**

Create `src/test/java/com/gameball/gameball/inappmessaging/domain/ModelTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `the three drawable types map to themselves`() {
        assertEquals(MessageType.SLIDEUP, MessageType.from(1))
        assertEquals(MessageType.MODAL, MessageType.from(2))
        assertEquals(MessageType.FULLSCREEN, MessageType.from(3))
        assertTrue(MessageType.from(1).isSupported)
    }

    /**
     * 4 (htmlFullscreen) and 5 (emailCapture) are known-but-unimplemented; anything else is
     * a type this SDK version predates. Both keep the campaign and mark it unsupported, so
     * selection can filter it and let a lower-priority campaign win.
     */
    @Test
    fun `known-but-unimplemented and unknown types are both unsupported`() {
        assertFalse(MessageType.from(4).isSupported)
        assertFalse(MessageType.from(5).isSupported)
        assertFalse(MessageType.from(99).isSupported)
        assertFalse(MessageType.from(0).isSupported)
        assertFalse(MessageType.from(-1).isSupported)
    }

    @Test
    fun `filter operators accept the backend spellings case-insensitively`() {
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("equals"))
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("Is"))
        assertEquals(FilterOperator.EQUALS, FilterOperator.from("EQUALS"))
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.from("notEquals"))
        assertEquals(FilterOperator.NOT_EQUALS, FilterOperator.from("IsNot"))
        assertEquals(FilterOperator.GREATER_THAN, FilterOperator.from("greaterThan"))
        assertEquals(FilterOperator.GREATER_OR_EQUAL, FilterOperator.from("greaterThanOrEqual"))
        assertEquals(FilterOperator.GREATER_OR_EQUAL, FilterOperator.from("greater_than_or_equals"))
        assertEquals(FilterOperator.LESS_THAN, FilterOperator.from("lessThan"))
        assertEquals(FilterOperator.LESS_OR_EQUAL, FilterOperator.from("lessThanOrEqual"))
        assertEquals(FilterOperator.CONTAINS, FilterOperator.from("Contains"))
    }

    @Test
    fun `an unrecognised operator is null so that one filter drops, not the campaign`() {
        assertNull(FilterOperator.from("startsWith"))
        assertNull(FilterOperator.from(""))
        assertNull(FilterOperator.from(null))
    }

    @Test
    fun `text alignment accepts all five spellings case-insensitively`() {
        assertEquals(TextAlign.START, TextAlign.from("start"))
        assertEquals(TextAlign.END, TextAlign.from("END"))
        assertEquals(TextAlign.CENTER, TextAlign.from("Center"))
        assertEquals(TextAlign.LEFT, TextAlign.from("left"))
        assertEquals(TextAlign.RIGHT, TextAlign.from("right"))
        assertNull(TextAlign.from("justified"))
        assertNull(TextAlign.from(null))
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*ModelTest*'`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Create the trigger model**

Create `inappmessaging/domain/Trigger.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

internal enum class TriggerType { SESSION_START, EVENT }

/**
 * There are exactly two trigger types. Purchases are not one of them: a purchase arrives as
 * an event named "purchase" with productId, price, currency and quantity folded into its
 * properties, so ordinary metadata filters work on them.
 */
internal data class Trigger(
    val type: TriggerType,
    /** Required for [TriggerType.EVENT]. Matching is on name — never on the backend's eventId. */
    val eventName: String? = null,
    val filters: List<MetadataFilter> = emptyList(),
    /** false means once ever, enforced on device. */
    val repeatable: Boolean = false,
    /** Only meaningful when [repeatable]. 0 or null means every occurrence. */
    val minIntervalSeconds: Int? = null
)

internal enum class FilterOperator {
    EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_OR_EQUAL, LESS_THAN, LESS_OR_EQUAL, CONTAINS;

    companion object {
        private val BY_NAME: Map<String, FilterOperator> = mapOf(
            "equals" to EQUALS, "is" to EQUALS, "eq" to EQUALS,
            "notequals" to NOT_EQUALS, "isnot" to NOT_EQUALS, "neq" to NOT_EQUALS,
            "greaterthan" to GREATER_THAN, "gt" to GREATER_THAN,
            "greaterthanorequal" to GREATER_OR_EQUAL,
            "greaterthanorequals" to GREATER_OR_EQUAL,
            "gte" to GREATER_OR_EQUAL,
            "lessthan" to LESS_THAN, "lt" to LESS_THAN,
            "lessthanorequal" to LESS_OR_EQUAL,
            "lessthanorequals" to LESS_OR_EQUAL,
            "lte" to LESS_OR_EQUAL,
            "contains" to CONTAINS
        )

        /** Null for anything unrecognised — the caller drops that filter, not the campaign. */
        fun from(raw: String?): FilterOperator? =
            BY_NAME[raw?.trim()?.lowercase()?.replace("_", "")]
    }
}

/**
 * A requirement on the triggering event's metadata. A filter that cannot be named cannot be
 * evaluated, and treating it as "always true" would silently widen the campaign — so a
 * missing name drops the whole campaign, while a bad operator drops only the filter.
 */
internal data class MetadataFilter(
    val name: String,
    val operator: FilterOperator,
    val value: Any
)

/** What just happened, offered to the selector. */
internal sealed class TriggerOccurrence {
    object SessionStart : TriggerOccurrence()
    data class Event(
        val name: String,
        val metadata: Map<String, Any?> = emptyMap()
    ) : TriggerOccurrence()
}
```

- [ ] **Step 4: Create the message model**

Create `inappmessaging/domain/Model.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

/** The backend's default when the sync response omits cooldownSeconds. */
internal const val DEFAULT_COOLDOWN_SECONDS = 30

internal enum class MessageType(val wire: Int) {
    SLIDEUP(1),
    MODAL(2),
    FULLSCREEN(3),

    /**
     * Covers 4 (htmlFullscreen) and 5 (emailCapture), which are specified but not implemented,
     * and any future type this SDK version predates. The campaign is kept and filtered at
     * selection so a usable lower-priority campaign can win the occurrence.
     */
    UNSUPPORTED(-1);

    val isSupported: Boolean get() = this != UNSUPPORTED

    companion object {
        fun from(wire: Int): MessageType = when (wire) {
            1 -> SLIDEUP
            2 -> MODAL
            3 -> FULLSCREEN
            else -> UNSUPPORTED
        }
    }
}

internal enum class SlidePosition { TOP, BOTTOM }

internal enum class MessageOrientation { ANY, PORTRAIT, LANDSCAPE }

/** A rendering hint, never a contract. An unrecognised value falls back to DEFAULT. */
internal enum class MessageLayout { DEFAULT, IMAGE_ONLY }

internal enum class TextAlign {
    START, END, CENTER, LEFT, RIGHT;

    companion object {
        fun from(raw: String?): TextAlign? = when (raw?.trim()?.lowercase()) {
            "start" -> START
            "end" -> END
            "center", "centre" -> CENTER
            "left" -> LEFT
            "right" -> RIGHT
            else -> null
        }
    }
}

internal sealed class MessageAction {
    object Dismiss : MessageAction()

    data class OpenUrl(val url: String, val external: Boolean) : MessageAction()

    /** [route] is a bare name with no leading slash. */
    data class Navigate(val route: String, val arguments: Map<String, Any?>?) : MessageAction()

    /** log_event, log_attribute, request_push_permission and anything unknown. */
    data class Unsupported(val type: String) : MessageAction()
}

/**
 * Every colour is optional; absent means fall back to the host's theme, never to a literal.
 * [border] is parsed and carried but never painted — no message-level surface draws one.
 */
internal data class MessageColors(
    val background: Int? = null,
    val text: Int? = null,
    val header: Int? = null,
    val closeButton: Int? = null,
    val border: Int? = null,
    val frame: Int? = null
) {
    companion object { val EMPTY = MessageColors() }
}

internal data class ButtonColors(
    val background: Int? = null,
    val text: Int? = null,
    /** No default outline: a border is drawn only when this is set. */
    val border: Int? = null
)

/** Paired across content and locale by [id]; unmatched ids are dropped at parse. */
internal data class MessageButton(
    val id: String,
    val text: String,
    val action: MessageAction,
    val colors: ButtonColors? = null
)

internal data class MessageContent(
    val header: String?,
    val body: String?,
    /** Already resolved per type: fullscreen prefers media.url, others prefer imageUrl. */
    val imageUrl: String?,
    /** Slideup only. */
    val iconUrl: String?,
    val layout: MessageLayout,
    val colors: MessageColors,
    val buttons: List<MessageButton>,
    /** Null means the surface is inert. Never defaulted to dismiss. */
    val clickAction: MessageAction?,
    val showCloseButton: Boolean,
    val dismissOnScrimTap: Boolean,
    val slidePosition: SlidePosition,
    val orientation: MessageOrientation,
    /** Null means no timer. A slideup receives the 8 s default at parse. */
    val autoDismissMillis: Long?,
    val headerAlign: TextAlign?,
    val bodyAlign: TextAlign?,
    val extras: Map<String, String>
) {
    val hasArtwork: Boolean get() = !imageUrl.isNullOrBlank()
    val hasIcon: Boolean get() = !iconUrl.isNullOrBlank()
    val hasText: Boolean get() = !header.isNullOrBlank() || !body.isNullOrBlank()
}

internal data class Campaign(
    val campaignId: Int,
    val variationId: Int?,
    val dispatchId: String?,
    /** Logs and debug UI only. Never used in logic. */
    val name: String?,
    val priority: Int,
    val messageType: MessageType,
    /** The wire value, retained for logs when [messageType] is UNSUPPORTED. */
    val rawMessageType: Int,
    val expiresAtMillis: Long?,
    /** Displays normally; reports nothing at all. */
    val isTest: Boolean,
    val trigger: Trigger,
    val content: MessageContent,
    /**
     * Position in the response's messages array. The marketer's dashboard ordering, and the
     * tie-break for equal priorities — meaningful, not merely deterministic.
     */
    val responseIndex: Int
)

internal data class SyncResult(
    val campaigns: List<Campaign>,
    val cooldownSeconds: Int,
    val quietHours: QuietHours?
) {
    companion object {
        val EMPTY = SyncResult(emptyList(), DEFAULT_COOLDOWN_SECONDS, null)
    }
}
```

- [ ] **Step 5: Run the tests and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*ModelTest*'`
Expected: PASS, 5 tests.

- [ ] **Step 6: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/domain/ \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/domain/ModelTest.kt
git commit -m "feat(iam): add the in-app messaging domain model

Pure data with no Android dependency. Two mappings carry behaviour and are
tested: unknown message types resolve to UNSUPPORTED so the campaign survives
to be filtered at selection, and filter operators accept the backend's
spellings case-insensitively."
```

---

## Task 4: Parser — envelope and drop rules

Spec §6, §6.3, §7.2. The parser must never throw; a malformed payload returns an empty result and a log line.

**Files:**
- Create: `inappmessaging/data/JsonExt.kt`
- Create: `inappmessaging/data/MessageParser.kt` (envelope only; Task 5 fills in content)
- Test: `inappmessaging/data/MessageParserEnvelopeTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 2 and 3.
- Produces: `MessageParser.parse(rawJson: String?): SyncResult`; `MessageParser.parse(root: JsonObject): SyncResult`; internal `parseCampaign(obj: JsonObject, index: Int): Campaign?`; and the `JsonObject` extensions `obj`, `arr`, `str`, `int`, `long`, `bool`, `scalar` used by Tasks 5 and 6.

- [ ] **Step 1: Write the failing envelope test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserEnvelopeTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.DEFAULT_COOLDOWN_SECONDS
import com.gameball.gameball.inappmessaging.domain.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserEnvelopeTest {

    /** A campaign with only the fields the parser requires. */
    private fun minimalCampaign(
        campaignId: String = "2055",
        messageType: String = "2",
        extra: String = ""
    ) = """
        {
          "campaignId": $campaignId,
          "messageType": $messageType,
          "contentMode": "prerendered",
          "trigger": { "type": "session_start" },
          "content": {},
          "locale": { "header": "Hello" }
          $extra
        }
    """.trimIndent()

    private fun payload(vararg campaigns: String, root: String = "") = """
        { "cooldownSeconds": 10 $root, "messages": [ ${campaigns.joinToString(",")} ] }
    """.trimIndent()

    @Test
    fun `a minimal campaign parses`() {
        val result = MessageParser.parse(payload(minimalCampaign()))
        assertEquals(1, result.campaigns.size)
        val campaign = result.campaigns.first()
        assertEquals(2055, campaign.campaignId)
        assertEquals(MessageType.MODAL, campaign.messageType)
        assertEquals(0, campaign.priority)
        assertEquals(false, campaign.isTest)
        assertNull(campaign.expiresAtMillis)
        assertEquals(0, campaign.responseIndex)
    }

    @Test
    fun `cooldownSeconds is read from the root and defaults to 30`() {
        assertEquals(10, MessageParser.parse(payload(minimalCampaign())).cooldownSeconds)
        assertEquals(
            DEFAULT_COOLDOWN_SECONDS,
            MessageParser.parse("""{ "messages": [] }""").cooldownSeconds
        )
    }

    @Test
    fun `quiet hours are read from the root`() {
        val json = """
            {
              "cooldownSeconds": 30,
              "quietHours": { "enabled": true, "start": "22:00", "end": "08:00" },
              "messages": []
            }
        """.trimIndent()
        val window = MessageParser.parse(json).quietHours
        assertNotNull(window)
        assertEquals(22 * 60, window!!.startMinute)
        assertEquals(8 * 60, window.endMinute)
    }

    @Test
    fun `unknown root keys are ignored`() {
        val json = """
            {
              "cooldownSeconds": 30,
              "campaignOrdering": [2052, 2053],
              "somethingTheBackendAddedLastWeek": { "a": 1 },
              "messages": [ ${minimalCampaign()} ]
            }
        """.trimIndent()
        assertEquals(1, MessageParser.parse(json).campaigns.size)
    }

    @Test
    fun `a missing campaignId drops the campaign`() {
        val json = """
            { "messages": [ { "messageType": 2, "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `a missing messageType drops the campaign`() {
        val json = """
            { "messages": [ { "campaignId": 1, "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `an unknown messageType is kept and marked unsupported`() {
        val result = MessageParser.parse(payload(minimalCampaign(messageType = "99")))
        assertEquals(1, result.campaigns.size)
        assertEquals(MessageType.UNSUPPORTED, result.campaigns.first().messageType)
        assertEquals(99, result.campaigns.first().rawMessageType)
    }

    @Test
    fun `a non-prerendered contentMode drops the campaign`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2, "contentMode": "remote",
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `an absent contentMode defaults to prerendered and is kept`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        assertEquals(1, MessageParser.parse(json).campaigns.size)
    }

    @Test
    fun `a campaign with no header, body or image is dropped`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {}, "locale": {} } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `a slideup with an icon but no text is dropped`() {
        val json = """
            { "messages": [ { "campaignId": 1, "messageType": 1,
              "trigger": { "type": "session_start" },
              "content": { "iconUrl": "https://x/i.png" }, "locale": {} } ] }
        """.trimIndent()
        assertTrue(MessageParser.parse(json).campaigns.isEmpty())
    }

    @Test
    fun `expiresAt is parsed and a null one stays null`() {
        val withExpiry = minimalCampaign(extra = """, "expiresAt": "2026-12-31T23:59:59Z"""")
        assertEquals(
            IamTime.parseIso8601("2026-12-31T23:59:59Z"),
            MessageParser.parse(payload(withExpiry)).campaigns.first().expiresAtMillis
        )
        val nullExpiry = minimalCampaign(extra = """, "expiresAt": null""")
        assertNull(MessageParser.parse(payload(nullExpiry)).campaigns.first().expiresAtMillis)
    }

    @Test
    fun `response index records the marketer's dashboard order`() {
        val result = MessageParser.parse(
            payload(minimalCampaign("10"), minimalCampaign("20"), minimalCampaign("30"))
        )
        assertEquals(listOf(10, 20, 30), result.campaigns.map { it.campaignId })
        assertEquals(listOf(0, 1, 2), result.campaigns.map { it.responseIndex })
    }

    @Test
    fun `one bad campaign does not take the others with it`() {
        val bad = """{ "messageType": 2, "trigger": { "type": "session_start" } }"""
        val result = MessageParser.parse(payload(bad, minimalCampaign("77")))
        assertEquals(1, result.campaigns.size)
        assertEquals(77, result.campaigns.first().campaignId)
    }

    // --- the parser must never throw ---

    @Test
    fun `malformed json returns an empty result`() {
        assertEquals(0, MessageParser.parse("{ not json").campaigns.size)
        assertEquals(DEFAULT_COOLDOWN_SECONDS, MessageParser.parse("{ not json").cooldownSeconds)
    }

    @Test
    fun `a non-object root returns an empty result`() {
        assertTrue(MessageParser.parse("[]").campaigns.isEmpty())
        assertTrue(MessageParser.parse("\"a string\"").campaigns.isEmpty())
        assertTrue(MessageParser.parse("null").campaigns.isEmpty())
    }

    @Test
    fun `a missing or wrongly typed messages array returns an empty result`() {
        assertTrue(MessageParser.parse("{}").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": null }""").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": {} }""").campaigns.isEmpty())
        assertTrue(MessageParser.parse("""{ "messages": "nope" }""").campaigns.isEmpty())
    }

    @Test
    fun `null and empty input return an empty result`() {
        assertTrue(MessageParser.parse(null).campaigns.isEmpty())
        assertTrue(MessageParser.parse("").campaigns.isEmpty())
    }

    @Test
    fun `wrongly typed scalars do not throw`() {
        val json = """
            { "cooldownSeconds": "ten",
              "quietHours": "yes",
              "messages": [ { "campaignId": "not a number", "messageType": 2,
                "trigger": { "type": "session_start" }, "content": {},
                "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        val result = MessageParser.parse(json)
        assertEquals(DEFAULT_COOLDOWN_SECONDS, result.cooldownSeconds)
        assertNull(result.quietHours)
        assertTrue(result.campaigns.isEmpty())
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserEnvelopeTest*'`
Expected: FAIL — unresolved reference `MessageParser`.

- [ ] **Step 3: Add the JSON access helpers**

Create `inappmessaging/data/JsonExt.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Total accessors over Gson's tree model.
 *
 * The sync payload is walked by hand rather than bound reflectively: the parser's job is a
 * long list of asymmetric leniency rules, and reflective binding gives nulls and exceptions
 * where decisions are needed. Every accessor here returns null instead of throwing, so a
 * field of the wrong type behaves exactly like an absent one.
 */

internal fun JsonObject.child(name: String): JsonElement? =
    get(name)?.takeUnless { it.isJsonNull }

internal fun JsonObject.obj(name: String): JsonObject? =
    child(name)?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonObject.arr(name: String): JsonArray? =
    child(name)?.takeIf { it.isJsonArray }?.asJsonArray

/** Blank strings are normalised to null: an empty URL otherwise reaches the image loader. */
internal fun JsonObject.str(name: String): String? =
    child(name)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }

internal fun JsonObject.int(name: String): Int? = longOrNull(name)?.toInt()

internal fun JsonObject.long(name: String): Long? = longOrNull(name)

private fun JsonObject.longOrNull(name: String): Long? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isNumber -> primitive.asNumber.toLong()
        primitive.isString -> primitive.asString.trim().toLongOrNull()
        else -> null
    }
}

internal fun JsonObject.bool(name: String): Boolean? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isString -> primitive.asString.trim().lowercase().toBooleanStrictOrNull()
        else -> null
    }
}

internal fun JsonObject.double(name: String): Double? {
    val primitive = child(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
    return when {
        primitive.isNumber -> primitive.asNumber.toDouble()
        primitive.isString -> primitive.asString.trim().toDoubleOrNull()
        else -> null
    }
}

/** A Long, Double, Boolean or String — whatever the primitive actually is. Used for colours,
 *  filter values and extras, where the wire type is not fixed. */
internal fun JsonElement?.scalar(): Any? {
    val primitive = this?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonPrimitive }
        ?.asJsonPrimitive ?: return null
    return primitive.toScalar()
}

internal fun JsonObject.scalar(name: String): Any? = child(name).scalar()

private fun JsonPrimitive.toScalar(): Any = when {
    isBoolean -> asBoolean
    isNumber -> {
        val text = asNumber.toString()
        text.toLongOrNull() ?: text.toDouble()
    }
    else -> asString
}
```

- [ ] **Step 4: Implement the parser envelope**

Create `inappmessaging/data/MessageParser.kt`. Task 5 replaces the two stubs at the bottom; everything above them is final.

```kotlin
package com.gameball.gameball.inappmessaging.data

import androidx.annotation.VisibleForTesting
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.DEFAULT_COOLDOWN_SECONDS
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageType
import com.gameball.gameball.inappmessaging.domain.QuietHours
import com.gameball.gameball.inappmessaging.domain.SyncResult
import com.gameball.gameball.inappmessaging.domain.Trigger
import com.gameball.gameball.inappmessaging.runtime.IamLog
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Turns a sync payload into domain objects, applying every leniency rule.
 *
 * The rules are deliberately asymmetric: a contract problem drops the campaign, a content
 * problem degrades it. This class must never throw — a malformed payload returns
 * [SyncResult.EMPTY] and a log line, because a parser that throws takes messaging down for
 * a payload the backend can fix in a minute.
 */
internal object MessageParser {

    private const val PRERENDERED = "prerendered"

    fun parse(rawJson: String?): SyncResult {
        if (rawJson.isNullOrBlank()) {
            IamLog.w("sync payload was null or empty")
            return SyncResult.EMPTY
        }
        val root = try {
            JsonParser.parseString(rawJson)
        } catch (t: Throwable) {
            IamLog.e("sync payload is not valid JSON; ignoring it", t)
            return SyncResult.EMPTY
        }
        if (root == null || !root.isJsonObject) {
            IamLog.w("sync payload root is not an object; ignoring it")
            return SyncResult.EMPTY
        }
        return parse(root.asJsonObject)
    }

    @VisibleForTesting
    fun parse(root: JsonObject): SyncResult = try {
        val cooldown = root.int("cooldownSeconds")?.takeIf { it >= 0 } ?: DEFAULT_COOLDOWN_SECONDS

        val quietHoursObject = root.obj("quietHours")
        val quietHours = quietHoursObject?.let {
            QuietHours.from(it.bool("enabled"), it.str("start"), it.str("end"))
        }

        val messages = root.arr("messages")
        if (messages == null) {
            IamLog.w("sync payload has no usable messages array")
            return SyncResult(emptyList(), cooldown, quietHours)
        }

        val campaigns = ArrayList<Campaign>(messages.size())
        messages.forEachIndexed { index, element ->
            if (!element.isJsonObject) {
                IamLog.w("messages[$index] is not an object; dropped")
                return@forEachIndexed
            }
            // responseIndex is the position in the array, not the surviving index: it is the
            // marketer's dashboard ordering and must not shift when a sibling is dropped.
            parseCampaign(element.asJsonObject, index)?.let(campaigns::add)
        }

        IamLog.d("parsed ${campaigns.size}/${messages.size()} campaigns, cooldown ${cooldown}s")
        SyncResult(campaigns, cooldown, quietHours)
    } catch (t: Throwable) {
        // Defensive: no path above is expected to throw, and if one ever does, messaging must
        // degrade to "no campaigns" rather than take the host's sync call down with it.
        IamLog.e("unexpected failure parsing the sync payload; ignoring it", t)
        SyncResult.EMPTY
    }

    @VisibleForTesting
    fun parseCampaign(obj: JsonObject, index: Int): Campaign? {
        val campaignId = obj.int("campaignId") ?: run {
            IamLog.w("messages[$index] has no campaignId; dropped")
            return null
        }

        val contentMode = obj.str("contentMode") ?: PRERENDERED
        if (!contentMode.equals(PRERENDERED, ignoreCase = true)) {
            IamLog.w("campaign $campaignId has contentMode '$contentMode'; dropped")
            return null
        }

        val rawMessageType = obj.int("messageType") ?: run {
            IamLog.w("campaign $campaignId has no messageType; dropped")
            return null
        }
        val messageType = MessageType.from(rawMessageType)
        if (!messageType.isSupported) {
            IamLog.w(
                "campaign $campaignId has messageType $rawMessageType, which this SDK version " +
                    "cannot draw; kept and marked unsupported"
            )
        }

        val trigger = parseTrigger(obj.obj("trigger"), campaignId) ?: return null

        val content = parseContent(obj, campaignId, messageType) ?: return null

        return Campaign(
            campaignId = campaignId,
            variationId = obj.int("variationId"),
            dispatchId = obj.str("dispatchId"),
            name = obj.str("name"),
            priority = obj.int("priority") ?: 0,
            messageType = messageType,
            rawMessageType = rawMessageType,
            expiresAtMillis = IamTime.parseIso8601(obj.str("expiresAt")),
            isTest = obj.bool("isTest") ?: false,
            trigger = trigger,
            content = content,
            responseIndex = index
        )
    }

    // Task 5 implements this.
    private fun parseContent(
        campaignObject: JsonObject,
        campaignId: Int,
        messageType: MessageType
    ): MessageContent? = TODO("Task 5")

    // Task 6 implements this.
    private fun parseTrigger(triggerObject: JsonObject?, campaignId: Int): Trigger? =
        TODO("Task 6")
}
```

> The two `TODO(...)` calls are scaffolding that exists only between this task and Task 6. They are the one exception to the no-placeholder rule in this plan, and Task 6 ends with a grep asserting none remain.

- [ ] **Step 5: Implement just enough of the two stubs to run the envelope tests**

Replace the two stubbed functions with these temporary bodies so the envelope tests can run. Task 5 and Task 6 replace them properly.

```kotlin
    private fun parseContent(
        campaignObject: JsonObject,
        campaignId: Int,
        messageType: MessageType
    ): MessageContent? {
        val content = campaignObject.obj("content") ?: JsonObject()
        val locale = campaignObject.obj("locale") ?: JsonObject()
        val header = locale.str("header")
        val body = locale.str("message") ?: locale.str("body")
        val imageUrl = content.str("imageUrl")
        val iconUrl = content.str("iconUrl")

        if (header == null && body == null && imageUrl == null) {
            IamLog.w("campaign $campaignId has no header, body or image; dropped")
            return null
        }
        if (messageType == MessageType.SLIDEUP && header == null && body == null) {
            IamLog.w("campaign $campaignId is a slideup with no text; dropped")
            return null
        }

        return MessageContent(
            header = header,
            body = body,
            imageUrl = imageUrl,
            iconUrl = iconUrl,
            layout = com.gameball.gameball.inappmessaging.domain.MessageLayout.DEFAULT,
            colors = com.gameball.gameball.inappmessaging.domain.MessageColors.EMPTY,
            buttons = emptyList(),
            clickAction = null,
            showCloseButton = true,
            dismissOnScrimTap = true,
            slidePosition = com.gameball.gameball.inappmessaging.domain.SlidePosition.BOTTOM,
            orientation = com.gameball.gameball.inappmessaging.domain.MessageOrientation.ANY,
            autoDismissMillis = null,
            headerAlign = null,
            bodyAlign = null,
            extras = emptyMap()
        )
    }

    private fun parseTrigger(triggerObject: JsonObject?, campaignId: Int): Trigger? {
        val type = triggerObject?.str("type")?.lowercase()
        return when (type) {
            "session_start" -> Trigger(
                com.gameball.gameball.inappmessaging.domain.TriggerType.SESSION_START
            )
            "event" -> Trigger(
                com.gameball.gameball.inappmessaging.domain.TriggerType.EVENT,
                eventName = triggerObject.str("name")
            )
            else -> null
        }
    }
```

- [ ] **Step 6: Run the envelope tests and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserEnvelopeTest*'`
Expected: PASS, 19 tests.

- [ ] **Step 7: Prove the never-throw guarantee is real**

Temporarily replace the body of `parse(root: JsonObject)`'s `catch` with `throw t`. Re-run. Expected: still PASS — nothing in the happy path throws. Now temporarily make `parseCampaign` throw on the first campaign and re-run `malformed json returns an empty result` and `one bad campaign does not take the others with it`. Expected: the latter FAILS, showing the outer catch is load-bearing. Restore.

- [ ] **Step 8: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/data/ \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserEnvelopeTest.kt
git commit -m "feat(iam): parse the sync envelope and campaign drop rules

Hand-walks the payload with total accessors rather than binding it
reflectively, so every leniency rule is a decision rather than a null or an
exception. Missing campaignId, missing messageType and a non-prerendered
contentMode drop the campaign; an unknown messageType keeps it and marks it
unsupported so selection can pass over it and let a lower-priority campaign
win.

The parser never throws: malformed JSON, a non-object root and a missing
messages array all return an empty result with a log line."
```

---

## Task 5: Parser — content, buttons, artwork, layout, dismissal

Spec §6.2, §6.4, §6.5, §6.6, §6.9, §6.10, §6.11, §15.7. Replaces the temporary `parseContent` from Task 4.

**Files:**
- Modify: `inappmessaging/data/MessageParser.kt`
- Test: `inappmessaging/data/MessageParserContentTest.kt`

**Interfaces:**
- Consumes: `JsonExt`, `ColorParser`, the domain model.
- Produces: a fully populated `MessageContent` on every parsed campaign.

**Rules this task implements.** Each has a test below.

| Rule | Behaviour |
|---|---|
| Buttons | paired across `content.buttons` and `locale.buttons` by string `id`; an id present in only one half is dropped |
| Modal button cap | keep the first two, log the count — never drop the campaign, never render three |
| Slideup buttons | dropped with a log naming how many |
| Fullscreen buttons | uncapped |
| Button with no usable action | falls back to `dismiss` — a dead button is worse than a closing one |
| Message-level action | `content.action`; null leaves the surface **inert**, never defaulted to dismiss |
| Artwork, fullscreen | `content.media.url` first, then `content.imageUrl` |
| Artwork, others | `content.imageUrl` first, then `content.media.url` |
| `media.type == "video"` | logged and ignored — an ImageView draws a broken frame |
| Blank URL | normalised to null (handled by `JsonObject.str`) |
| Slideup copy | `locale.message`, falling back to `locale.header` |
| Other copy | `body` ← `locale.message`, else `locale.body` |
| `layout` | `text_with_image`/`image_and_text` → DEFAULT; `image_only` → IMAGE_ONLY; unrecognised → DEFAULT + log |
| `image_only` with no artwork | forced to DEFAULT — the only place the declared layout may be overridden |
| `closeBehaviour` | `both`/null → glyph + scrim; `button` → glyph only; `swipe` → scrim only; unrecognised → `both` + log |
| `swipe` on fullscreen | **promoted to `both`** and logged — a fullscreen has no scrim, so obeying it literally ships an unclosable message |
| `autoDismissSeconds` absent | slideup gets the 8 s default; modal and fullscreen get no timer |
| `autoDismissSeconds` == 0 | honoured as "stay until dismissed" on every type |
| `extras` | non-string values coerced to their string form; null values dropped |

- [ ] **Step 1: Write the failing content test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserContentTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.domain.TextAlign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserContentTest {

    /** Parses one campaign of [messageType] with the given content and locale bodies. */
    private fun parseOne(
        messageType: Int = 2,
        content: String = "{}",
        locale: String = """{ "header": "Hello" }"""
    ): Campaign? = MessageParser.parse(
        """
        { "messages": [ {
            "campaignId": 1, "messageType": $messageType, "contentMode": "prerendered",
            "trigger": { "type": "session_start" },
            "content": $content, "locale": $locale
        } ] }
        """.trimIndent()
    ).campaigns.firstOrNull()

    // --- buttons ---

    @Test
    fun `buttons are paired across content and locale by id`() {
        val campaign = parseOne(
            content = """
                { "buttons": [
                    { "id": "ok", "action": { "type": "dismiss" } },
                    { "id": "cancel", "action": { "type": "dismiss" } } ] }
            """.trimIndent(),
            locale = """
                { "header": "Hi", "buttons": [
                    { "id": "ok", "text": "Track my order" },
                    { "id": "cancel", "text": "Not now" } ] }
            """.trimIndent()
        )!!
        assertEquals(listOf("ok", "cancel"), campaign.content.buttons.map { it.id })
        assertEquals("Track my order", campaign.content.buttons.first().text)
    }

    @Test
    fun `a button styled but not translated is dropped`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok", "action": { "type": "dismiss" } },
                                        { "id": "ghost", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" } ] }"""
        )!!
        assertEquals(listOf("ok"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a button translated but not styled is dropped`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" },
                                                       { "id": "orphan", "text": "Nowhere" } ] }"""
        )!!
        assertEquals(listOf("ok"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a modal keeps the first two buttons and does not drop the campaign`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } },
                                        { "id": "b", "action": { "type": "dismiss" } },
                                        { "id": "c", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "a", "text": "A" },
                                                       { "id": "b", "text": "B" },
                                                       { "id": "c", "text": "C" } ] }"""
        )!!
        assertEquals(listOf("a", "b"), campaign.content.buttons.map { it.id })
    }

    @Test
    fun `a fullscreen has no button cap`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } },
                                        { "id": "b", "action": { "type": "dismiss" } },
                                        { "id": "c", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "a", "text": "A" },
                                                       { "id": "b", "text": "B" },
                                                       { "id": "c", "text": "C" } ] }"""
        )!!
        assertEquals(3, campaign.content.buttons.size)
    }

    @Test
    fun `a slideup drops every button`() {
        val campaign = parseOne(
            messageType = 1,
            content = """{ "buttons": [ { "id": "a", "action": { "type": "dismiss" } } ] }""",
            locale = """{ "message": "Nice pick", "buttons": [ { "id": "a", "text": "A" } ] }"""
        )!!
        assertTrue(campaign.content.buttons.isEmpty())
    }

    @Test
    fun `a button with no usable action falls back to dismiss`() {
        val campaign = parseOne(
            content = """{ "buttons": [ { "id": "ok" } ] }""",
            locale = """{ "header": "Hi", "buttons": [ { "id": "ok", "text": "Go" } ] }"""
        )!!
        assertEquals(MessageAction.Dismiss, campaign.content.buttons.first().action)
    }

    // --- actions ---

    @Test
    fun `a null message action leaves the surface inert`() {
        assertNull(parseOne(content = """{ "action": null }""")!!.content.clickAction)
        assertNull(parseOne(content = "{}")!!.content.clickAction)
    }

    @Test
    fun `navigate carries a bare route with no leading slash`() {
        val action = parseOne(
            content = """{ "action": { "type": "navigate", "route": "orders" } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.Navigate("orders", null), action)
    }

    @Test
    fun `open_url carries the url and the external flag`() {
        val action = parseOne(
            content = """{ "action": { "type": "open_url", "url": "https://x/y", "external": true } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.OpenUrl("https://x/y", true), action)
    }

    @Test
    fun `external defaults to false`() {
        val action = parseOne(
            content = """{ "action": { "type": "open_url", "url": "https://x/y" } }"""
        )!!.content.clickAction
        assertEquals(MessageAction.OpenUrl("https://x/y", false), action)
    }

    @Test
    fun `the unimplemented action types parse as unsupported`() {
        listOf("log_event", "log_attribute", "request_push_permission").forEach { type ->
            val action = parseOne(content = """{ "action": { "type": "$type" } }""")!!
                .content.clickAction
            assertEquals(MessageAction.Unsupported(type), action)
        }
    }

    // --- artwork resolution ---

    @Test
    fun `fullscreen prefers media url over imageUrl`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "imageUrl": "https://x/a.jpg",
                           "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `fullscreen falls back to imageUrl when media is absent`() {
        val campaign = parseOne(messageType = 3, content = """{ "imageUrl": "https://x/a.jpg" }""")!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a modal prefers imageUrl over media url`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "imageUrl": "https://x/a.jpg",
                           "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    /** The live QA campaign puts its image under media and leaves imageUrl null. */
    @Test
    fun `a modal falls back to media url when imageUrl is absent`() {
        val campaign = parseOne(
            messageType = 2,
            content = """{ "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a media type of video is ignored`() {
        val campaign = parseOne(
            messageType = 3,
            content = """{ "media": { "type": "video", "url": "https://x/v.mp4" },
                           "imageUrl": "https://x/a.jpg" }"""
        )!!
        assertEquals("https://x/a.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a media entry with no type is treated as an image`() {
        val campaign = parseOne(messageType = 3, content = """{ "media": { "url": "https://x/b.jpg" } }""")!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    @Test
    fun `a blank url is treated as absent`() {
        val campaign = parseOne(
            content = """{ "imageUrl": "   ", "media": { "type": "image", "url": "https://x/b.jpg" } }"""
        )!!
        assertEquals("https://x/b.jpg", campaign.content.imageUrl)
    }

    // --- layout ---

    @Test
    fun `every layout spelling maps and an unknown one falls back to the default`() {
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "layout": "text_with_image", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(messageType = 3, content = """{ "layout": "image_and_text", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.IMAGE_ONLY,
            parseOne(content = """{ "layout": "image_only", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "layout": "carousel", "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
        assertEquals(MessageLayout.DEFAULT,
            parseOne(content = """{ "imageUrl": "https://x/a.jpg" }""")!!.content.layout)
    }

    /**
     * The one place a declared layout may be overridden. image_only never references header
     * or body, so with no artwork it renders bare background plus buttons, counts an
     * impression and reports nothing wrong.
     */
    @Test
    fun `image_only with no artwork falls back to the stacked composition`() {
        val campaign = parseOne(content = """{ "layout": "image_only" }""")!!
        assertEquals(MessageLayout.DEFAULT, campaign.content.layout)
        assertEquals("Hello", campaign.content.header)
    }

    @Test
    fun `absent copy does not imply image_only`() {
        val campaign = parseOne(
            content = """{ "imageUrl": "https://x/a.jpg" }""",
            locale = "{}"
        )!!
        assertEquals(MessageLayout.DEFAULT, campaign.content.layout)
    }

    // --- copy sources ---

    @Test
    fun `body comes from locale message, else locale body`() {
        assertEquals("From message",
            parseOne(locale = """{ "header": "H", "message": "From message" }""")!!.content.body)
        assertEquals("From body",
            parseOne(locale = """{ "header": "H", "body": "From body" }""")!!.content.body)
    }

    @Test
    fun `slideup copy falls back to the header so a mis-filled campaign still says something`() {
        val campaign = parseOne(messageType = 1, locale = """{ "header": "Only a header" }""")!!
        assertEquals("Only a header", campaign.content.body)
    }

    // --- dismissal ---

    @Test
    fun `closeBehaviour both and null give a glyph and a dismissing scrim`() {
        listOf(""""both"""", "null").forEach { value ->
            val content = parseOne(content = """{ "closeBehaviour": $value }""")!!.content
            assertTrue(content.showCloseButton)
            assertTrue(content.dismissOnScrimTap)
        }
    }

    @Test
    fun `closeBehaviour button gives a glyph and a non-dismissing scrim`() {
        val content = parseOne(content = """{ "closeBehaviour": "button" }""")!!.content
        assertTrue(content.showCloseButton)
        assertFalse(content.dismissOnScrimTap)
    }

    @Test
    fun `closeBehaviour swipe on a modal gives a dismissing scrim and no glyph`() {
        val content = parseOne(messageType = 2, content = """{ "closeBehaviour": "swipe" }""")!!.content
        assertFalse(content.showCloseButton)
        assertTrue(content.dismissOnScrimTap)
    }

    /**
     * A fullscreen has no scrim and no swipe gesture, so obeying "swipe" literally leaves only
     * the system back gesture — which does not exist on iOS. The parser promotes and logs
     * rather than shipping a trap.
     */
    @Test
    fun `closeBehaviour swipe is promoted to both on a fullscreen`() {
        val content = parseOne(messageType = 3, content = """{ "closeBehaviour": "swipe" }""")!!.content
        assertTrue("a fullscreen must keep its glyph", content.showCloseButton)
    }

    @Test
    fun `an unrecognised closeBehaviour falls back to both`() {
        val content = parseOne(content = """{ "closeBehaviour": "telepathy" }""")!!.content
        assertTrue(content.showCloseButton)
        assertTrue(content.dismissOnScrimTap)
    }

    // --- auto dismiss ---

    @Test
    fun `a slideup with no duration gets the 8 second default`() {
        assertEquals(8_000L, parseOne(messageType = 1, locale = """{ "message": "Hi" }""")!!
            .content.autoDismissMillis)
    }

    @Test
    fun `an explicit zero means stay until dismissed and is not overridden`() {
        assertNull(parseOne(
            messageType = 1,
            content = """{ "autoDismissSeconds": 0 }""",
            locale = """{ "message": "Hi" }"""
        )!!.content.autoDismissMillis)
    }

    @Test
    fun `modal and fullscreen get no timer when none is set`() {
        assertNull(parseOne(messageType = 2)!!.content.autoDismissMillis)
        assertNull(parseOne(messageType = 3)!!.content.autoDismissMillis)
    }

    @Test
    fun `a fractional duration is rounded to milliseconds`() {
        assertEquals(2_500L,
            parseOne(content = """{ "autoDismissSeconds": 2.5 }""")!!.content.autoDismissMillis)
    }

    // --- colours, alignment, slide position, orientation, extras ---

    @Test
    fun `colours are parsed into their slots and absent ones stay null`() {
        val colors = parseOne(
            content = """{ "colors": { "background": "#FFFFFF", "text": "#1F2937",
                                       "header": "#111827", "closeButton": null,
                                       "border": null, "frame": null } }"""
        )!!.content.colors
        assertEquals(0xFFFFFFFF.toInt(), colors.background)
        assertEquals(0xFF1F2937.toInt(), colors.text)
        assertEquals(0xFF111827.toInt(), colors.header)
        assertNull(colors.closeButton)
        assertNull(colors.border)
        assertNull(colors.frame)
    }

    @Test
    fun `a malformed colour costs only its own slot`() {
        val colors = parseOne(
            content = """{ "colors": { "background": "chartreuse", "text": "#1F2937" } }"""
        )!!.content.colors
        assertNull(colors.background)
        assertEquals(0xFF1F2937.toInt(), colors.text)
    }

    @Test
    fun `text alignment is read per slot`() {
        val content = parseOne(
            content = """{ "textAlignment": { "header": "center", "body": "start" } }"""
        )!!.content
        assertEquals(TextAlign.CENTER, content.headerAlign)
        assertEquals(TextAlign.START, content.bodyAlign)
    }

    @Test
    fun `slideFrom defaults to bottom`() {
        assertEquals(SlidePosition.BOTTOM, parseOne(messageType = 1, locale = """{ "message": "Hi" }""")!!
            .content.slidePosition)
        assertEquals(SlidePosition.TOP, parseOne(
            messageType = 1,
            content = """{ "slideFrom": "top" }""",
            locale = """{ "message": "Hi" }"""
        )!!.content.slidePosition)
    }

    @Test
    fun `extras coerce non-string values and drop nulls`() {
        val extras = parseOne(
            content = """{ "extras": { "a": "text", "b": 42, "c": true, "d": 1.5, "e": null } }"""
        )!!.content.extras
        assertEquals("text", extras["a"])
        assertEquals("42", extras["b"])
        assertEquals("true", extras["c"])
        assertEquals("1.5", extras["d"])
        assertFalse(extras.containsKey("e"))
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserContentTest*'`
Expected: FAIL on most tests — the Task 4 stub returns defaults for everything.

- [ ] **Step 3: Replace `parseContent` with the real implementation**

In `MessageParser.kt`, replace the temporary `parseContent` with the following, and add the helpers below it.

```kotlin
    private fun parseContent(
        campaignObject: JsonObject,
        campaignId: Int,
        messageType: MessageType
    ): MessageContent? {
        val content = campaignObject.obj("content") ?: JsonObject()
        val locale = campaignObject.obj("locale") ?: JsonObject()

        val header = locale.str("header")
        // A slideup shows one line and falls back to the header, so a campaign that filled
        // the wrong field still says something.
        val body = if (messageType == MessageType.SLIDEUP) {
            locale.str("message") ?: header
        } else {
            locale.str("message") ?: locale.str("body")
        }

        val imageUrl = resolveArtwork(content, messageType, campaignId)
        val iconUrl = content.str("iconUrl")

        // Drawing an empty box is worse than showing nothing.
        if (header == null && body == null && imageUrl == null) {
            IamLog.w("campaign $campaignId has no header, body or image; dropped")
            return null
        }
        // A 40dp icon with no words is not a message.
        if (messageType == MessageType.SLIDEUP && body.isNullOrBlank()) {
            IamLog.w("campaign $campaignId is a slideup with no text; dropped")
            return null
        }

        val buttons = parseButtons(content, locale, messageType, campaignId)
        val (showClose, dismissOnScrim) = parseCloseBehaviour(
            content.str("closeBehaviour"), messageType, campaignId
        )

        return MessageContent(
            header = header,
            body = body,
            imageUrl = imageUrl,
            iconUrl = iconUrl,
            layout = parseLayout(content.str("layout"), imageUrl, campaignId),
            colors = parseColors(content.obj("colors")),
            buttons = buttons,
            clickAction = parseAction(content.obj("action")),
            showCloseButton = showClose,
            dismissOnScrimTap = dismissOnScrim,
            slidePosition = if (content.str("slideFrom")?.lowercase() == "top") {
                SlidePosition.TOP
            } else {
                SlidePosition.BOTTOM
            },
            orientation = when (content.str("orientation")?.lowercase()) {
                "portrait" -> MessageOrientation.PORTRAIT
                "landscape" -> MessageOrientation.LANDSCAPE
                else -> MessageOrientation.ANY
            },
            autoDismissMillis = parseAutoDismiss(content, messageType),
            headerAlign = TextAlign.from(content.obj("textAlignment")?.str("header")),
            bodyAlign = TextAlign.from(content.obj("textAlignment")?.str("body")),
            extras = parseExtras(content.obj("extras"))
        )
    }

    /**
     * Fullscreen prefers media.url; every other type prefers imageUrl. Each falls back to the
     * other. A parser reading only imageUrl finds nothing on the live QA campaign, renders
     * nothing, and looks like a backend problem.
     */
    private fun resolveArtwork(
        content: JsonObject,
        messageType: MessageType,
        campaignId: Int
    ): String? {
        val direct = content.str("imageUrl")
        val media = content.obj("media")
        val mediaType = media?.str("type")?.lowercase()
        val fromMedia = when {
            media == null -> null
            mediaType == null || mediaType == "image" -> media.str("url")
            else -> {
                // Handing a video URL to an ImageView draws a broken frame.
                IamLog.w("campaign $campaignId has media.type '$mediaType'; ignored")
                null
            }
        }
        return if (messageType == MessageType.FULLSCREEN) fromMedia ?: direct
        else direct ?: fromMedia
    }

    /**
     * Buttons arrive split across the styled half and the translated half and are paired by
     * string id. One without the other has no action or no label; either way it is dropped.
     */
    private fun parseButtons(
        content: JsonObject,
        locale: JsonObject,
        messageType: MessageType,
        campaignId: Int
    ): List<MessageButton> {
        val styled = content.arr("buttons").orEmpty()
            .mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
            .mapNotNull { obj -> obj.str("id")?.let { it to obj } }
            .toMap()
        val translated = locale.arr("buttons").orEmpty()
            .mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
            .mapNotNull { obj -> obj.str("id")?.let { it to obj } }
            .toMap()

        // Order follows the styled half, which is the payload order the dashboard arranged.
        var buttons = styled.mapNotNull { (id, styledObject) ->
            val text = translated[id]?.str("text") ?: return@mapNotNull null
            MessageButton(
                id = id,
                text = text,
                // A dead button is worse than a closing one.
                action = parseAction(styledObject.obj("action")) ?: MessageAction.Dismiss,
                colors = styledObject.obj("colors")?.let { colors ->
                    ButtonColors(
                        background = ColorParser.parse(colors.scalar("background")),
                        text = ColorParser.parse(colors.scalar("text")),
                        border = ColorParser.parse(colors.scalar("border"))
                    )
                }
            )
        }

        val dropped = styled.size - buttons.size
        if (dropped > 0) IamLog.w("campaign $campaignId dropped $dropped unpaired button(s)")

        when (messageType) {
            MessageType.SLIDEUP -> if (buttons.isNotEmpty()) {
                IamLog.w(
                    "campaign $campaignId is a slideup carrying ${buttons.size} button(s); " +
                        "dropped — a slideup's whole surface is the tap target"
                )
                buttons = emptyList()
            }
            MessageType.MODAL -> if (buttons.size > MODAL_MAX_BUTTONS) {
                IamLog.w(
                    "campaign $campaignId is a modal carrying ${buttons.size} buttons; " +
                        "keeping the first $MODAL_MAX_BUTTONS"
                )
                buttons = buttons.take(MODAL_MAX_BUTTONS)
            }
            else -> Unit // fullscreen has no cap; buttons stack full-width
        }
        return buttons
    }

    /** Null means "no action" — the caller decides whether that is inert or a dismiss. */
    private fun parseAction(action: JsonObject?): MessageAction? {
        val type = action?.str("type")?.lowercase() ?: return null
        return when (type) {
            "dismiss" -> MessageAction.Dismiss
            "open_url" -> action.str("url")
                ?.let { MessageAction.OpenUrl(it, action.bool("external") ?: false) }
            "navigate" -> action.str("route")
                ?.let { MessageAction.Navigate(it, parseArguments(action.obj("arguments"))) }
            else -> MessageAction.Unsupported(type)
        }
    }

    private fun parseArguments(arguments: JsonObject?): Map<String, Any?>? =
        arguments?.entrySet()?.associate { (key, value) -> key to value.scalar() }

    private fun parseLayout(raw: String?, imageUrl: String?, campaignId: Int): MessageLayout {
        val layout = when (raw?.lowercase()) {
            null -> MessageLayout.DEFAULT
            "text_with_image", "image_and_text" -> MessageLayout.DEFAULT
            "image_only" -> MessageLayout.IMAGE_ONLY
            else -> {
                IamLog.w("campaign $campaignId has layout '$raw'; using the type's default")
                MessageLayout.DEFAULT
            }
        }
        // The full-bleed branch never references header or body, so without artwork it would
        // render a bare background, count an impression and report nothing wrong.
        if (layout == MessageLayout.IMAGE_ONLY && imageUrl.isNullOrBlank()) {
            IamLog.w("campaign $campaignId declares image_only with no artwork; using the stacked layout")
            return MessageLayout.DEFAULT
        }
        return layout
    }

    private fun parseColors(colors: JsonObject?): MessageColors {
        if (colors == null) return MessageColors.EMPTY
        return MessageColors(
            background = ColorParser.parse(colors.scalar("background")),
            text = ColorParser.parse(colors.scalar("text")),
            header = ColorParser.parse(colors.scalar("header")),
            closeButton = ColorParser.parse(colors.scalar("closeButton")),
            border = ColorParser.parse(colors.scalar("border")),
            frame = ColorParser.parse(colors.scalar("frame"))
        )
    }

    /** Returns (showCloseButton, dismissOnScrimTap). */
    private fun parseCloseBehaviour(
        raw: String?,
        messageType: MessageType,
        campaignId: Int
    ): Pair<Boolean, Boolean> {
        val behaviour = raw?.trim()?.lowercase()
        if (behaviour != null && behaviour !in KNOWN_CLOSE_BEHAVIOURS) {
            IamLog.w("campaign $campaignId has closeBehaviour '$raw'; treating it as 'both'")
            return true to true
        }
        // A fullscreen has no scrim and no swipe gesture of its own, so "swipe" would leave
        // only the system back gesture — and that does not exist on iOS. Promote and log.
        if (behaviour == "swipe" && messageType == MessageType.FULLSCREEN) {
            IamLog.w(
                "campaign $campaignId is a fullscreen with closeBehaviour 'swipe', which would " +
                    "leave no way out; promoted to 'both'"
            )
            return true to true
        }
        return when (behaviour) {
            null, "both" -> true to true
            "button" -> true to false
            "swipe" -> false to true
            else -> true to true
        }
    }

    /**
     * Absent and zero are different values. A slideup draws no glyph and has no scrim, so
     * without a timer its only exit is a gesture nobody told the customer about — hence the
     * default. An explicit 0 is an author turning the timer off and is honoured.
     */
    private fun parseAutoDismiss(content: JsonObject, messageType: MessageType): Long? {
        val seconds = content.double("autoDismissSeconds")
        if (seconds == null) {
            return if (messageType == MessageType.SLIDEUP) SLIDEUP_DEFAULT_AUTO_DISMISS_MS else null
        }
        if (seconds <= 0.0) return null
        return Math.round(seconds * 1000.0)
    }

    /**
     * Braze silently drops non-string extras and loses campaign data with no diagnostic.
     * Coerce instead. A null value is dropped.
     */
    private fun parseExtras(extras: JsonObject?): Map<String, String> {
        if (extras == null) return emptyMap()
        val result = LinkedHashMap<String, String>()
        extras.entrySet().forEach { (key, value) ->
            value.scalar()?.let { result[key] = it.toString() }
        }
        return result
    }
```

Add these to the top of the object, next to `PRERENDERED`:

```kotlin
    private const val MODAL_MAX_BUTTONS = 2
    private const val SLIDEUP_DEFAULT_AUTO_DISMISS_MS = 8_000L
    private val KNOWN_CLOSE_BEHAVIOURS = setOf("both", "button", "swipe")
```

And add the imports for `ButtonColors`, `MessageAction`, `MessageButton`, `MessageColors`, `MessageLayout`, `MessageOrientation`, `SlidePosition`, `TextAlign`.

- [ ] **Step 4: Run both parser test classes and verify they pass**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParser*Test*'`
Expected: PASS.

- [ ] **Step 5: Prove the artwork preference test can fail**

Temporarily make `resolveArtwork` always return `direct ?: fromMedia`. Re-run. Expected: `fullscreen prefers media url over imageUrl` FAILS. Restore. This is the defect that only a payload captured from the live endpoint caught in Flutter.

- [ ] **Step 6: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/data/MessageParser.kt \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserContentTest.kt
git commit -m "feat(iam): parse message content, buttons, artwork and dismissal

Buttons pair across the styled and translated halves by id; a modal keeps the
first two rather than dropping the campaign, and a slideup drops all of them
with a log. Fullscreen prefers media.url while other types prefer imageUrl,
which is what the live QA campaign requires.

closeBehaviour 'swipe' is promoted to 'both' on a fullscreen: it has no scrim
and no swipe gesture, so obeying the field literally ships a message the
customer cannot close. A slideup with no autoDismissSeconds gets the 8s
default, while an explicit 0 is honoured as 'stay until dismissed'."
```

---

## Task 6: Parser — triggers, metadata filters, and the live payload

Spec §6.7, §6.8, §18. Replaces the temporary `parseTrigger` from Task 4 and removes the last `TODO`.

**Files:**
- Modify: `inappmessaging/data/MessageParser.kt`
- Create: `src/test/resources/live_sync_payload.json`
- Test: `inappmessaging/data/MessageParserTriggerTest.kt`, `inappmessaging/data/MessageParserLivePayloadTest.kt`

**Interfaces:**
- Produces: a fully populated `Trigger` with `filters` on every parsed campaign. No new public symbols.

**Rules.** The asymmetry is deliberate and each row has a test.

| Input | Result | Why |
|---|---|---|
| `type: "session_start"` | `Trigger(SESSION_START)` | |
| `type: "event"`, non-blank `name` | `Trigger(EVENT, name)` | matching is on name, never on the backend's `eventId` |
| `type: "event"`, null or blank `name` | **drop the campaign** | the numeric id is internal to the backend |
| unknown or missing `type` | **drop the campaign** | |
| `repeatable` absent | `false` — once ever | |
| `minIntervalSeconds` absent or 0 | null — every occurrence | only meaningful when repeatable |
| filter missing `name` | **drop the whole campaign** | "always true" silently widens it — a "spent over $100" message shown to everyone |
| filter with a bad operator | **drop that filter only** | widening is the right response to one bad field |
| filter with a null value | **drop that filter only** | |
| `metadataLogicalOperator` anything but `And` | **drop the campaign** | |
| `metadataLogicalOperator` null or absent | treated as `And` | |

- [ ] **Step 1: Write the failing trigger test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserTriggerTest.kt` with a `parseOne(trigger: String)` helper mirroring Task 5's, and these tests:

```kotlin
package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.FilterOperator
import com.gameball.gameball.inappmessaging.domain.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTriggerTest {

    private fun parseOne(trigger: String): Campaign? = MessageParser.parse(
        """
        { "messages": [ {
            "campaignId": 1, "messageType": 2, "contentMode": "prerendered",
            "trigger": $trigger,
            "content": {}, "locale": { "header": "Hello" }
        } ] }
        """.trimIndent()
    ).campaigns.firstOrNull()

    @Test
    fun `session_start parses with no fields`() {
        val trigger = parseOne("""{ "type": "session_start" }""")!!.trigger
        assertEquals(TriggerType.SESSION_START, trigger.type)
        assertEquals(false, trigger.repeatable)
        assertNull(trigger.minIntervalSeconds)
    }

    @Test
    fun `an event trigger matches on name and carries its repeat rule`() {
        val trigger = parseOne(
            """{ "type": "event", "eventId": 1382, "name": "place_order",
                 "repeatable": true, "minIntervalSeconds": 300 }"""
        )!!.trigger
        assertEquals(TriggerType.EVENT, trigger.type)
        assertEquals("place_order", trigger.eventName)
        assertEquals(true, trigger.repeatable)
        assertEquals(300, trigger.minIntervalSeconds)
    }

    @Test
    fun `an event trigger with a null or blank name drops the campaign`() {
        assertNull(parseOne("""{ "type": "event", "eventId": 1382, "name": null }"""))
        assertNull(parseOne("""{ "type": "event", "eventId": 1382, "name": "  " }"""))
        assertNull(parseOne("""{ "type": "event", "eventId": 1382 }"""))
    }

    @Test
    fun `an unknown or missing trigger type drops the campaign`() {
        assertNull(parseOne("""{ "type": "geofence" }"""))
        assertNull(parseOne("""{ }"""))
        assertNull(parseOne("""null"""))
    }

    @Test
    fun `minIntervalSeconds of zero means every occurrence`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "x", "repeatable": true, "minIntervalSeconds": 0 }"""
        )!!.trigger
        assertNull(trigger.minIntervalSeconds)
    }

    @Test
    fun `filters parse with name, operator and value`() {
        val filters = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "And",
                 "metadataFilters": [ { "name": "price", "operator": "greaterThan", "value": 100 },
                                      { "name": "currency", "operator": "Is", "value": "USD" } ] }"""
        )!!.trigger.filters
        assertEquals(2, filters.size)
        assertEquals("price", filters[0].name)
        assertEquals(FilterOperator.GREATER_THAN, filters[0].operator)
        assertEquals(100L, filters[0].value)
        assertEquals(FilterOperator.EQUALS, filters[1].operator)
        assertEquals("USD", filters[1].value)
    }

    /** A filter you cannot name is one you cannot evaluate. */
    @Test
    fun `a filter with no name drops the whole campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "purchase",
                 "metadataFilters": [ { "operator": "greaterThan", "value": 100 } ] }"""
        ))
    }

    /** Widening is the right response to one bad field rather than a contract mismatch. */
    @Test
    fun `a bad operator or a null value drops only that filter`() {
        val filters = parseOne(
            """{ "type": "event", "name": "purchase",
                 "metadataFilters": [ { "name": "a", "operator": "startsWith", "value": 1 },
                                      { "name": "b", "operator": "equals", "value": null },
                                      { "name": "c", "operator": "equals", "value": 3 } ] }"""
        )!!.trigger.filters
        assertEquals(listOf("c"), filters.map { it.name })
    }

    @Test
    fun `a logical operator other than And drops the campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": "Or",
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        ))
    }

    @Test
    fun `a null or absent logical operator is treated as And`() {
        assertTrue(parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": null,
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        )!!.trigger.filters.isNotEmpty())
    }

    @Test
    fun `absent filters give an empty list, not a drop`() {
        assertTrue(parseOne("""{ "type": "event", "name": "x" }""")!!.trigger.filters.isEmpty())
        assertTrue(parseOne(
            """{ "type": "event", "name": "x", "metadataFilters": null }"""
        )!!.trigger.filters.isEmpty())
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserTriggerTest*'`
Expected: FAIL — the Task 4 stub ignores filters and repeat rules.

- [ ] **Step 3: Replace `parseTrigger`**

```kotlin
    private fun parseTrigger(triggerObject: JsonObject?, campaignId: Int): Trigger? {
        if (triggerObject == null) {
            IamLog.w("campaign $campaignId has no trigger; dropped")
            return null
        }
        return when (triggerObject.str("type")?.lowercase()) {
            "session_start" -> Trigger(TriggerType.SESSION_START)

            "event" -> {
                // Match on name, never on eventId — the numeric id is internal to the backend.
                val name = triggerObject.str("name")
                if (name == null) {
                    IamLog.w("campaign $campaignId has an event trigger with no name; dropped")
                    return null
                }
                val logicalOperator = triggerObject.str("metadataLogicalOperator")
                if (logicalOperator != null && !logicalOperator.equals("And", ignoreCase = true)) {
                    IamLog.w(
                        "campaign $campaignId uses metadataLogicalOperator " +
                            "'$logicalOperator', which this SDK cannot evaluate; dropped"
                    )
                    return null
                }
                val filters = parseFilters(triggerObject, campaignId) ?: return null
                Trigger(
                    type = TriggerType.EVENT,
                    eventName = name,
                    filters = filters,
                    repeatable = triggerObject.bool("repeatable") ?: false,
                    minIntervalSeconds = triggerObject.int("minIntervalSeconds")
                        ?.takeIf { it > 0 }
                )
            }

            else -> {
                IamLog.w(
                    "campaign $campaignId has trigger type " +
                        "'${triggerObject.str("type")}'; dropped"
                )
                null
            }
        }
    }

    /** Null means "drop the campaign"; an empty list means "no filters". */
    private fun parseFilters(trigger: JsonObject, campaignId: Int): List<MetadataFilter>? {
        val raw = trigger.arr("metadataFilters") ?: return emptyList()
        val filters = ArrayList<MetadataFilter>(raw.size())
        for (element in raw) {
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue

            // A filter that cannot be named cannot be evaluated, and treating it as always
            // true silently widens the campaign — a "spent over $100" message shown to
            // everyone. That is worse than showing nothing, so the campaign goes.
            val name = obj.str("name") ?: run {
                IamLog.w("campaign $campaignId has a metadata filter with no name; dropped")
                return null
            }

            // One bad field widens rather than narrows, which is the right response here.
            val operator = FilterOperator.from(obj.str("operator")) ?: run {
                IamLog.w(
                    "campaign $campaignId filter '$name' has operator " +
                        "'${obj.str("operator")}'; that filter dropped"
                )
                continue
            }
            val value = obj.scalar("value") ?: run {
                IamLog.w("campaign $campaignId filter '$name' has a null value; that filter dropped")
                continue
            }
            filters.add(MetadataFilter(name, operator, value))
        }
        return filters
    }
```

Add imports for `MetadataFilter`, `FilterOperator`, `TriggerType`.

- [ ] **Step 4: Run and verify**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserTriggerTest*'`
Expected: PASS, 11 tests.

- [ ] **Step 5: Assert no stubs remain**

Run: `grep -rn 'TODO(' gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/`
Expected: no output.

- [ ] **Step 6: Capture a live payload**

Against the alpha account (spec §18), capture one real sync response and save it verbatim to `src/test/resources/live_sync_payload.json`:

```bash
curl -s -X POST 'https://api.alpha.gameball.app/api/v4.0/integrations/inapp-messages/sync' \
  -H 'Content-Type: application/json; charset=UTF-8' \
  -H 'APIKey: 22af53a0243f4f5dbe49950d58fd4de2' \
  -H 'Lang: en' \
  -d '{"customerId":"moaty-survey-3","platform":2,"locale":"en","appVersion":"1.0.0","sdkVersion":"3.3.0"}' \
  | tee gameballsdk/src/test/resources/live_sync_payload.json | head -c 400
```

Do not hand-edit it. If the endpoint is unreachable, say so and commit a payload captured by someone who can reach it — a hand-written fixture defeats the purpose of this test. In Flutter this caught two defects that reading the documentation did not.

- [ ] **Step 7: Write the live payload test**

Create `src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserLivePayloadTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses a payload captured from api.alpha.gameball.app, not one we wrote. The live endpoint
 * has returned a 422 where the documentation said 2xx, required a GUID the documentation
 * never mentioned, and grown two undocumented root fields mid-development.
 */
class MessageParserLivePayloadTest {

    private val payload: String =
        javaClass.classLoader!!.getResourceAsStream("live_sync_payload.json")!!
            .bufferedReader().use { it.readText() }

    @Test
    fun `the captured payload parses without throwing and yields campaigns`() {
        val result = MessageParser.parse(payload)
        assertTrue("expected at least one campaign", result.campaigns.isNotEmpty())
        assertTrue("cooldown should be non-negative", result.cooldownSeconds >= 0)
    }

    @Test
    fun `every parsed campaign has something to render`() {
        MessageParser.parse(payload).campaigns.forEach { campaign ->
            val content = campaign.content
            assertTrue(
                "campaign ${campaign.campaignId} has nothing to render",
                content.hasText || content.hasArtwork
            )
        }
    }

    @Test
    fun `campaigns whose artwork arrives under media are resolved`() {
        // The live account's image-bearing campaigns put their asset under content.media and
        // leave imageUrl null. A parser reading only imageUrl finds nothing here.
        val withArtwork = MessageParser.parse(payload).campaigns.filter { it.content.hasArtwork }
        assertTrue("expected at least one campaign with artwork", withArtwork.isNotEmpty())
        withArtwork.forEach {
            assertFalse(it.content.imageUrl!!.isBlank())
        }
    }

    @Test
    fun `response order is preserved`() {
        val campaigns = MessageParser.parse(payload).campaigns
        assertEquals(campaigns.indices.toList(), campaigns.map { it.responseIndex })
    }

    @Test
    fun `the root quiet-hours window survives parsing when the account has one`() {
        // Not an assertion that a window exists — the account may have none — only that
        // reading it does not throw and produces a coherent value when present.
        val window = MessageParser.parse(payload).quietHours
        if (window != null) {
            assertTrue(window.startMinute in 0..1439)
            assertTrue(window.endMinute in 0..1439)
            assertNotNull(window)
        }
    }
}
```

Add `import org.junit.Assert.assertEquals`.

- [ ] **Step 8: Run the full parser suite**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParser*'`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/data/MessageParser.kt \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserTriggerTest.kt \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/data/MessageParserLivePayloadTest.kt \
        gameballsdk/src/test/resources/live_sync_payload.json
git commit -m "feat(iam): parse triggers and metadata filters

Event triggers match on name, never on the backend's eventId, and a null name
drops the campaign. Filter handling is deliberately asymmetric: a filter with
no name drops the whole campaign, because treating it as always true would
silently widen the campaign, while a bad operator or null value drops only
that filter.

Adds a test that parses a payload captured from the alpha endpoint rather than
one we wrote."
```

---

## Task 7: Filter evaluation

Spec §6.8. Pure, and the row the alpha account cannot exercise — no live campaign sets `metadataFilters`, so this has never met the real backend on any platform.

**Files:**
- Create: `inappmessaging/domain/FilterEvaluator.kt`
- Test: `inappmessaging/domain/FilterEvaluatorTest.kt`

**Interfaces:**
- Produces: `FilterEvaluator.matches(filters: List<MetadataFilter>, metadata: Map<String, Any?>): Boolean` — the conjunction of every filter (`metadataLogicalOperator` is always `And`; anything else dropped the campaign at parse).

**Rules:**
- A missing property **never matches**. A filter is a requirement; absence is failure, or filters are decorative.
- Comparisons coerce across numeric types and stringly-typed numbers, so `"quantity": "2"` matches an int `2`.
- Ordering operators (`>`, `>=`, `<`, `<=`) on a non-number **refuse and log** rather than falling back to string comparison, which would produce nonsense.
- `contains` is a case-sensitive substring test on the string forms; a list value contains an element.
- `equals` on two values that both coerce to numbers compares numerically; otherwise it compares string forms.
- An empty filter list matches.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/gameball/gameball/inappmessaging/domain/FilterEvaluatorTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterEvaluatorTest {

    private fun filter(name: String, op: FilterOperator, value: Any) =
        listOf(MetadataFilter(name, op, value))

    private fun matches(
        name: String, op: FilterOperator, value: Any, metadata: Map<String, Any?>
    ) = FilterEvaluator.matches(filter(name, op, value), metadata)

    @Test
    fun `no filters always matches`() {
        assertTrue(FilterEvaluator.matches(emptyList(), emptyMap()))
        assertTrue(FilterEvaluator.matches(emptyList(), mapOf("a" to 1)))
    }

    @Test
    fun `equals and notEquals`() {
        assertTrue(matches("c", FilterOperator.EQUALS, "USD", mapOf("c" to "USD")))
        assertFalse(matches("c", FilterOperator.EQUALS, "USD", mapOf("c" to "EUR")))
        assertTrue(matches("c", FilterOperator.NOT_EQUALS, "USD", mapOf("c" to "EUR")))
        assertFalse(matches("c", FilterOperator.NOT_EQUALS, "USD", mapOf("c" to "USD")))
    }

    @Test
    fun `the four ordering operators`() {
        assertTrue(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to 150)))
        assertFalse(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.GREATER_OR_EQUAL, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.LESS_THAN, 100, mapOf("p" to 99)))
        assertFalse(matches("p", FilterOperator.LESS_THAN, 100, mapOf("p" to 100)))
        assertTrue(matches("p", FilterOperator.LESS_OR_EQUAL, 100, mapOf("p" to 100)))
    }

    @Test
    fun `contains works on strings and on lists`() {
        assertTrue(matches("t", FilterOperator.CONTAINS, "sale", mapOf("t" to "summer sale")))
        assertFalse(matches("t", FilterOperator.CONTAINS, "sale", mapOf("t" to "summer")))
        assertTrue(matches("t", FilterOperator.CONTAINS, "b", mapOf("t" to listOf("a", "b"))))
        assertFalse(matches("t", FilterOperator.CONTAINS, "z", mapOf("t" to listOf("a", "b"))))
    }

    /** A campaign authored with "quantity": "2" must match an int 2. */
    @Test
    fun `comparisons coerce across numeric types and stringly-typed numbers`() {
        assertTrue(matches("q", FilterOperator.EQUALS, "2", mapOf("q" to 2)))
        assertTrue(matches("q", FilterOperator.EQUALS, 2, mapOf("q" to "2")))
        assertTrue(matches("q", FilterOperator.EQUALS, 2L, mapOf("q" to 2.0)))
        assertTrue(matches("p", FilterOperator.GREATER_THAN, "100", mapOf("p" to 150.5)))
        assertTrue(matches("p", FilterOperator.GREATER_THAN, 100, mapOf("p" to "150")))
    }

    /** A filter is a requirement; absence is failure, otherwise filters are decorative. */
    @Test
    fun `a missing property never matches, for any operator`() {
        FilterOperator.values().forEach { operator ->
            assertFalse(
                "$operator should not match a missing property",
                matches("absent", operator, "anything", mapOf("other" to 1))
            )
        }
    }

    @Test
    fun `a null property value never matches`() {
        FilterOperator.values().forEach { operator ->
            assertFalse(
                "$operator should not match a null property",
                matches("p", operator, "anything", mapOf("p" to null))
            )
        }
    }

    /** Falling back to string comparison here would produce nonsense: "9" > "100". */
    @Test
    fun `ordering operators refuse a non-numeric value rather than comparing strings`() {
        assertFalse(matches("t", FilterOperator.GREATER_THAN, "abc", mapOf("t" to "def")))
        assertFalse(matches("t", FilterOperator.LESS_THAN, 100, mapOf("t" to "cheap")))
        assertFalse(matches("t", FilterOperator.GREATER_OR_EQUAL, "x", mapOf("t" to 5)))
    }

    @Test
    fun `booleans compare by their string form`() {
        assertTrue(matches("b", FilterOperator.EQUALS, true, mapOf("b" to true)))
        assertTrue(matches("b", FilterOperator.EQUALS, "true", mapOf("b" to true)))
        assertFalse(matches("b", FilterOperator.EQUALS, true, mapOf("b" to false)))
    }

    @Test
    fun `every filter must pass`() {
        val filters = listOf(
            MetadataFilter("price", FilterOperator.GREATER_THAN, 100),
            MetadataFilter("currency", FilterOperator.EQUALS, "USD")
        )
        assertTrue(FilterEvaluator.matches(filters, mapOf("price" to 150, "currency" to "USD")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 150, "currency" to "EUR")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 50, "currency" to "USD")))
        assertFalse(FilterEvaluator.matches(filters, mapOf("price" to 150)))
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*FilterEvaluatorTest*'`
Expected: FAIL — unresolved reference `FilterEvaluator`.

- [ ] **Step 3: Implement**

Create `inappmessaging/domain/FilterEvaluator.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Evaluates a campaign's metadata filters against a triggering event's properties.
 *
 * Always a conjunction: metadataLogicalOperator is either "And" or the campaign was dropped
 * at parse.
 */
internal object FilterEvaluator {

    fun matches(filters: List<MetadataFilter>, metadata: Map<String, Any?>): Boolean =
        filters.all { matches(it, metadata) }

    private fun matches(filter: MetadataFilter, metadata: Map<String, Any?>): Boolean {
        // A filter is a requirement, so absence is failure. Otherwise filters are decorative:
        // a campaign targeting "spent over $100" would reach everyone whose event happened
        // not to carry the property.
        val actual = metadata[filter.name] ?: return false

        return when (filter.operator) {
            FilterOperator.EQUALS -> isEqual(actual, filter.value)
            FilterOperator.NOT_EQUALS -> !isEqual(actual, filter.value)
            FilterOperator.CONTAINS -> contains(actual, filter.value)
            FilterOperator.GREATER_THAN -> compare(actual, filter) { it > 0 }
            FilterOperator.GREATER_OR_EQUAL -> compare(actual, filter) { it >= 0 }
            FilterOperator.LESS_THAN -> compare(actual, filter) { it < 0 }
            FilterOperator.LESS_OR_EQUAL -> compare(actual, filter) { it <= 0 }
        }
    }

    /** Numeric when both sides coerce; string comparison otherwise. */
    private fun isEqual(actual: Any, expected: Any): Boolean {
        val a = asNumber(actual)
        val b = asNumber(expected)
        if (a != null && b != null) return a == b
        return actual.toString() == expected.toString()
    }

    private fun contains(actual: Any, expected: Any): Boolean = when (actual) {
        is Iterable<*> -> actual.any { it?.toString() == expected.toString() }
        else -> actual.toString().contains(expected.toString())
    }

    /**
     * Ordering refuses non-numbers rather than falling back to string comparison, under which
     * "9" > "100" and a price filter would silently invert.
     */
    private inline fun compare(
        actual: Any,
        filter: MetadataFilter,
        predicate: (Int) -> Boolean
    ): Boolean {
        val a = asNumber(actual)
        val b = asNumber(filter.value)
        if (a == null || b == null) {
            IamLog.w(
                "filter '${filter.name}' uses ${filter.operator} but " +
                    "'$actual' or '${filter.value}' is not a number; not matched"
            )
            return false
        }
        return predicate(a.compareTo(b))
    }

    private fun asNumber(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }
}
```

- [ ] **Step 4: Run and verify**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*FilterEvaluatorTest*'`
Expected: PASS, 10 tests.

- [ ] **Step 5: Prove the missing-property rule can fail**

Temporarily change `metadata[filter.name] ?: return false` to `?: return true`. Re-run. Expected: `a missing property never matches` FAILS with seven operator failures. Restore.

- [ ] **Step 6: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/domain/FilterEvaluator.kt \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/domain/FilterEvaluatorTest.kt
git commit -m "feat(iam): evaluate campaign metadata filters

A missing property never matches: a filter is a requirement, and absence has
to be failure or filters are decorative. Comparisons coerce across numeric
types and stringly-typed numbers so a campaign authored with \"2\" matches an
int 2, while ordering operators refuse a non-number rather than falling back
to string comparison, under which \"9\" > \"100\"."
```

---

## Task 8: Message selection

Spec §7. The most heavily tested unit in the module, and entirely pure — no I/O, no `Context`, no clock of its own.

**Files:**
- Create: `inappmessaging/domain/DisplayHistorySnapshot.kt`
- Create: `inappmessaging/domain/MessageSelector.kt`
- Test: `inappmessaging/domain/MessageSelectorTest.kt`

**Interfaces:**
- Consumes: `Campaign`, `Trigger`, `TriggerOccurrence`, `QuietHours`, `FilterEvaluator`.
- Produces:
  ```kotlin
  internal data class DisplayRecord(val lastDisplayAtMillis: Long, val count: Int)
  internal data class DisplayHistorySnapshot(
      val perCampaign: Map<Int, DisplayRecord> = emptyMap(),
      val lastDisplayAtMillis: Long? = null
  )
  internal object MessageSelector {
      fun select(
          occurrence: TriggerOccurrence,
          campaigns: List<Campaign>,
          history: DisplayHistorySnapshot,
          nowMillis: Long,
          cooldownSeconds: Int,
          quietHours: QuietHours?,
          isArtworkReady: (Campaign) -> Boolean
      ): Campaign?

      /** The retry question: "may this display now", not "has it ever displayed". */
      fun mayDisplayNow(
          campaign: Campaign,
          history: DisplayHistorySnapshot,
          nowMillis: Long,
          cooldownSeconds: Int,
          quietHours: QuietHours?,
          isArtworkReady: (Campaign) -> Boolean
      ): Boolean

      fun triggerMatches(trigger: Trigger, occurrence: TriggerOccurrence): Boolean
      fun repeatEligible(campaign: Campaign, history: DisplayHistorySnapshot, nowMillis: Long): Boolean
  }
  ```

- [ ] **Step 1: Write the failing selection test**

Create `src/test/java/com/gameball/gameball/inappmessaging/domain/MessageSelectorTest.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MessageSelectorTest {

    private val now = 1_800_000_000_000L   // a fixed instant; nothing here waits

    private fun content() = MessageContent(
        header = "H", body = "B", imageUrl = null, iconUrl = null,
        layout = MessageLayout.DEFAULT, colors = MessageColors.EMPTY, buttons = emptyList(),
        clickAction = null, showCloseButton = true, dismissOnScrimTap = true,
        slidePosition = SlidePosition.BOTTOM, orientation = MessageOrientation.ANY,
        autoDismissMillis = null, headerAlign = null, bodyAlign = null, extras = emptyMap()
    )

    private fun campaign(
        id: Int,
        priority: Int = 0,
        index: Int = 0,
        type: MessageType = MessageType.MODAL,
        trigger: Trigger = Trigger(TriggerType.SESSION_START),
        expiresAtMillis: Long? = null
    ) = Campaign(
        campaignId = id, variationId = null, dispatchId = null, name = "c$id",
        priority = priority, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = expiresAtMillis, isTest = false, trigger = trigger,
        content = content(), responseIndex = index
    )

    private fun select(
        campaigns: List<Campaign>,
        occurrence: TriggerOccurrence = TriggerOccurrence.SessionStart,
        history: DisplayHistorySnapshot = DisplayHistorySnapshot(),
        nowMillis: Long = now,
        cooldownSeconds: Int = 30,
        quietHours: QuietHours? = null,
        artworkReady: (Campaign) -> Boolean = { true }
    ) = MessageSelector.select(
        occurrence, campaigns, history, nowMillis, cooldownSeconds, quietHours, artworkReady
    )

    // --- trigger matching ---

    @Test
    fun `session start selects a session-start campaign`() {
        assertEquals(1, select(listOf(campaign(1)))?.campaignId)
    }

    @Test
    fun `a named event selects a campaign listening for it`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertEquals(1, select(listOf(c), TriggerOccurrence.Event("place_order"))?.campaignId)
    }

    @Test
    fun `a non-matching event selects nothing`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertNull(select(listOf(c), TriggerOccurrence.Event("view_product_page")))
    }

    @Test
    fun `an event does not fire a session-start campaign, and vice versa`() {
        val sessionStart = campaign(1)
        val event = campaign(2, trigger = Trigger(TriggerType.EVENT, eventName = "x"))
        assertNull(select(listOf(sessionStart), TriggerOccurrence.Event("x")))
        assertNull(select(listOf(event), TriggerOccurrence.SessionStart))
    }

    @Test
    fun `event names match exactly, not case-insensitively`() {
        val c = campaign(1, trigger = Trigger(TriggerType.EVENT, eventName = "place_order"))
        assertNull(select(listOf(c), TriggerOccurrence.Event("Place_Order")))
    }

    @Test
    fun `filters gate an event campaign`() {
        val c = campaign(
            1,
            trigger = Trigger(
                TriggerType.EVENT, eventName = "purchase",
                filters = listOf(MetadataFilter("price", FilterOperator.GREATER_THAN, 100))
            )
        )
        assertEquals(1, select(
            listOf(c), TriggerOccurrence.Event("purchase", mapOf("price" to 150))
        )?.campaignId)
        assertNull(select(listOf(c), TriggerOccurrence.Event("purchase", mapOf("price" to 50))))
        assertNull(select(listOf(c), TriggerOccurrence.Event("purchase", emptyMap())))
    }

    // --- expiry ---

    /**
     * Checked at selection, not only at fetch: campaigns are cached for the session, so one
     * fetched at 23:58 would otherwise fire all night and keep firing after it was paused.
     */
    @Test
    fun `an expired campaign is never selected`() {
        assertNull(select(listOf(campaign(1, expiresAtMillis = now - 1))))
        assertNull(select(listOf(campaign(1, expiresAtMillis = now))))
        assertEquals(1, select(listOf(campaign(1, expiresAtMillis = now + 1)))?.campaignId)
    }

    // --- repeat rules ---

    @Test
    fun `a non-repeatable campaign is never selected twice`() {
        val c = campaign(1)
        val history = DisplayHistorySnapshot(
            perCampaign = mapOf(1 to DisplayRecord(now - 10_000_000L, 1)),
            lastDisplayAtMillis = now - 10_000_000L
        )
        assertNull(select(listOf(c), history = history))
    }

    @Test
    fun `a repeatable campaign respects minIntervalSeconds`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = 300
        ))
        val occurrence = TriggerOccurrence.Event("x")

        val tooSoon = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 299_000L, 1)), now - 299_000L
        )
        assertNull(select(listOf(c), occurrence, tooSoon, cooldownSeconds = 0))

        val longEnough = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 300_000L, 1)), now - 300_000L
        )
        assertEquals(1, select(listOf(c), occurrence, longEnough, cooldownSeconds = 0)?.campaignId)
    }

    @Test
    fun `a repeatable campaign with no interval fires on every occurrence`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = null
        ))
        val history = DisplayHistorySnapshot(mapOf(1 to DisplayRecord(now - 1L, 5)), now - 1L)
        assertEquals(1, select(
            listOf(c), TriggerOccurrence.Event("x"), history, cooldownSeconds = 0
        )?.campaignId)
    }

    // --- the global floor ---

    @Test
    fun `inside the cooldown floor nothing is selected`() {
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 29_000L)
        assertNull(select(listOf(campaign(1)), history = history, cooldownSeconds = 30))
    }

    @Test
    fun `outside the cooldown floor the same campaign is selected`() {
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 30_000L)
        assertEquals(1, select(listOf(campaign(1)), history = history, cooldownSeconds = 30)?.campaignId)
    }

    /** The floor is global, not per campaign. */
    @Test
    fun `the floor blocks a campaign that has never displayed`() {
        val history = DisplayHistorySnapshot(
            perCampaign = mapOf(99 to DisplayRecord(now - 5_000L, 1)),
            lastDisplayAtMillis = now - 5_000L
        )
        assertNull(select(listOf(campaign(1)), history = history, cooldownSeconds = 30))
    }

    // --- ranking ---

    @Test
    fun `the highest priority wins`() {
        val campaigns = listOf(
            campaign(1, priority = 3, index = 0),
            campaign(2, priority = 7, index = 1),
            campaign(3, priority = 5, index = 2)
        )
        assertEquals(2, select(campaigns)?.campaignId)
    }

    /**
     * The tie-break is response order and it is meaningful, not merely deterministic: the
     * backend returns campaigns in the sequence the marketer arranged in the dashboard.
     * Here the response order deliberately contradicts ascending campaignId, which is the
     * comparator a well-meaning refactor would reach for.
     */
    @Test
    fun `ties break on response order even when it contradicts campaignId order`() {
        val campaigns = listOf(
            campaign(900, priority = 5, index = 0),
            campaign(100, priority = 5, index = 1),
            campaign(500, priority = 5, index = 2)
        )
        assertEquals(900, select(campaigns)?.campaignId)
    }

    @Test
    fun `response order breaks ties regardless of the order the list is built in`() {
        val campaigns = listOf(
            campaign(100, priority = 5, index = 1),
            campaign(900, priority = 5, index = 0)
        )
        assertEquals(900, select(campaigns)?.campaignId)
    }

    // --- gates that let a lower-priority campaign win ---

    /**
     * Unsupported types are filtered at selection rather than refused at display, so a usable
     * lower-priority campaign wins instead of the occurrence being wasted.
     */
    @Test
    fun `an unsupported type is filtered so a lower-priority supported campaign wins`() {
        val campaigns = listOf(
            campaign(1, priority = 10, index = 0, type = MessageType.UNSUPPORTED),
            campaign(2, priority = 1, index = 1, type = MessageType.MODAL)
        )
        assertEquals(2, select(campaigns)?.campaignId)
    }

    @Test
    fun `a campaign whose artwork is not ready is passed over`() {
        val campaigns = listOf(
            campaign(1, priority = 10, index = 0),
            campaign(2, priority = 1, index = 1)
        )
        assertEquals(2, select(campaigns, artworkReady = { it.campaignId != 1 })?.campaignId)
    }

    @Test
    fun `nothing eligible selects nothing`() {
        assertNull(select(emptyList()))
        assertNull(select(listOf(campaign(1, type = MessageType.UNSUPPORTED))))
    }

    // --- quiet hours ---

    private fun utcInstantAt(hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2026, Calendar.AUGUST, 27, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `inside quiet hours nothing displays and outside the same campaign does`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        assertNull(select(
            listOf(campaign(1)), nowMillis = utcInstantAt(23, 0), quietHours = window
        ))
        assertEquals(1, select(
            listOf(campaign(1)), nowMillis = utcInstantAt(12, 0), quietHours = window
        )?.campaignId)
    }

    /**
     * Suppression costs the occurrence, not the campaign — nothing is recorded, so the same
     * campaign is eligible again next session with no manual reset.
     */
    @Test
    fun `suppression leaves the campaign eligible once the window ends`() {
        val window = QuietHours.from(true, "22:00", "08:00")!!
        val c = campaign(1)
        assertNull(select(listOf(c), nowMillis = utcInstantAt(23, 0), quietHours = window))
        // No history was written, because nothing displayed.
        assertEquals(1, select(
            listOf(c), nowMillis = utcInstantAt(9, 0), quietHours = window
        )?.campaignId)
    }

    // --- the retry predicate ---

    /**
     * Defect 10: the retry path asked "has this ever displayed" rather than "may it show now",
     * and threw away every repeatable campaign that happened to be waiting.
     */
    @Test
    fun `mayDisplayNow keeps a deferred repeatable campaign that has already displayed`() {
        val c = campaign(1, trigger = Trigger(
            TriggerType.EVENT, eventName = "x", repeatable = true, minIntervalSeconds = 60
        ))
        val history = DisplayHistorySnapshot(
            mapOf(1 to DisplayRecord(now - 120_000L, 3)), now - 120_000L
        )
        assertTrue(MessageSelector.mayDisplayNow(c, history, now, 30, null) { true })
    }

    @Test
    fun `mayDisplayNow re-checks the floor, so a message deferred before another displayed waits`() {
        val c = campaign(1)
        val history = DisplayHistorySnapshot(lastDisplayAtMillis = now - 5_000L)
        assertFalse(MessageSelector.mayDisplayNow(c, history, now, 30, null) { true })
    }

    @Test
    fun `mayDisplayNow refuses an expired, unsupported or artwork-less campaign`() {
        val history = DisplayHistorySnapshot()
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1, expiresAtMillis = now - 1), history, now, 0, null) { true })
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1, type = MessageType.UNSUPPORTED), history, now, 0, null) { true })
        assertFalse(MessageSelector.mayDisplayNow(
            campaign(1), history, now, 0, null) { false })
    }
}
```

- [ ] **Step 2: Run it and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageSelectorTest*'`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Add the history snapshot**

Create `inappmessaging/domain/DisplayHistorySnapshot.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

/** What is known about one campaign's past displays. */
internal data class DisplayRecord(
    val lastDisplayAtMillis: Long,
    val count: Int
)

/**
 * An immutable read of the display history, passed into the pure selector.
 *
 * [lastDisplayAtMillis] is the most recent display from *any* campaign — the global cooldown
 * floor is not per campaign.
 */
internal data class DisplayHistorySnapshot(
    val perCampaign: Map<Int, DisplayRecord> = emptyMap(),
    val lastDisplayAtMillis: Long? = null
)
```

- [ ] **Step 4: Implement the selector**

Create `inappmessaging/domain/MessageSelector.kt`:

```kotlin
package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Chooses at most one campaign for an occurrence.
 *
 * Pure by design: no I/O, no Context and no clock of its own, so every rule is testable with
 * plain data and no mocking. The service sequences; this owns all the policy.
 */
internal object MessageSelector {

    fun select(
        occurrence: TriggerOccurrence,
        campaigns: List<Campaign>,
        history: DisplayHistorySnapshot,
        nowMillis: Long,
        cooldownSeconds: Int,
        quietHours: QuietHours?,
        isArtworkReady: (Campaign) -> Boolean
    ): Campaign? {
        val eligible = campaigns.filter { campaign ->
            triggerMatches(campaign.trigger, occurrence) &&
                notExpired(campaign, nowMillis) &&
                repeatEligible(campaign, history, nowMillis) &&
                campaign.messageType.isSupported &&
                isArtworkReady(campaign)
        }
        if (eligible.isEmpty()) return null

        // Quiet hours suppress rather than defer: the pending slot is in-memory and a window
        // is hours long, so "retry when it ends" would never fire. This costs the occurrence,
        // not the campaign.
        if (quietHours != null && quietHours.contains(nowMillis)) {
            IamLog.d("inside quiet hours; suppressing ${eligible.size} eligible campaign(s)")
            return null
        }

        // The global floor, checked after eligibility and before sorting. Not per campaign.
        val lastDisplay = history.lastDisplayAtMillis
        if (lastDisplay != null && nowMillis - lastDisplay < cooldownSeconds * 1000L) {
            IamLog.d("inside the ${cooldownSeconds}s display floor; nothing selected")
            return null
        }

        val winner = eligible.sortedWith(RANKING).first()
        if (eligible.size > 1) {
            IamLog.d(
                "selected campaign ${winner.campaignId} (priority ${winner.priority}) from " +
                    "${eligible.size} eligible"
            )
        }
        return winner
    }

    /**
     * Highest priority first, then response order.
     *
     * The response-order tie-break is meaningful, not merely deterministic: the backend
     * returns campaigns in the sequence the marketer arranged in the dashboard, confirmed
     * with the backend team. Kotlin's sortedWith is stable, so a comparator on priority alone
     * would preserve input order — but the index is written in explicitly so that a future
     * refactor to "ascending campaignId" is visibly a behaviour change rather than a tidy-up.
     */
    private val RANKING: Comparator<Campaign> =
        compareByDescending<Campaign> { it.priority }.thenBy { it.responseIndex }

    /**
     * The retry question. Deliberately not "has this ever displayed": asking the cruder
     * question threw away every repeatable campaign that happened to be waiting in the
     * pending slot.
     */
    fun mayDisplayNow(
        campaign: Campaign,
        history: DisplayHistorySnapshot,
        nowMillis: Long,
        cooldownSeconds: Int,
        quietHours: QuietHours?,
        isArtworkReady: (Campaign) -> Boolean
    ): Boolean {
        if (!notExpired(campaign, nowMillis)) return false
        if (!campaign.messageType.isSupported) return false
        if (!repeatEligible(campaign, history, nowMillis)) return false
        if (!isArtworkReady(campaign)) return false
        if (quietHours != null && quietHours.contains(nowMillis)) return false
        val lastDisplay = history.lastDisplayAtMillis
        if (lastDisplay != null && nowMillis - lastDisplay < cooldownSeconds * 1000L) return false
        return true
    }

    fun triggerMatches(trigger: Trigger, occurrence: TriggerOccurrence): Boolean =
        when (occurrence) {
            is TriggerOccurrence.SessionStart -> trigger.type == TriggerType.SESSION_START
            is TriggerOccurrence.Event ->
                trigger.type == TriggerType.EVENT &&
                    trigger.eventName == occurrence.name &&
                    FilterEvaluator.matches(trigger.filters, occurrence.metadata)
        }

    fun repeatEligible(
        campaign: Campaign,
        history: DisplayHistorySnapshot,
        nowMillis: Long
    ): Boolean {
        val record = history.perCampaign[campaign.campaignId] ?: return true
        if (!campaign.trigger.repeatable) return false
        val interval = campaign.trigger.minIntervalSeconds ?: return true
        return nowMillis - record.lastDisplayAtMillis >= interval * 1000L
    }

    /** Never display at or after the expiry instant. */
    private fun notExpired(campaign: Campaign, nowMillis: Long): Boolean =
        campaign.expiresAtMillis?.let { nowMillis < it } ?: true
}
```

- [ ] **Step 5: Run and verify**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageSelectorTest*'`
Expected: PASS, 22 tests.

- [ ] **Step 6: Prove the tie-break test defends the contract**

Change `RANKING` to `compareByDescending<Campaign> { it.priority }.thenBy { it.campaignId }`. Re-run. Expected: `ties break on response order even when it contradicts campaignId order` FAILS.

This is the check that matters most in this task. In Flutter four tie-break tests existed and **all four passed** under a comparator that broke the contract, because the test helper handed out ids in creation order. Restore the correct comparator.

- [ ] **Step 7: Commit**

```bash
git add gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/domain/DisplayHistorySnapshot.kt \
        gameballsdk/src/main/java/com/gameball/gameball/inappmessaging/domain/MessageSelector.kt \
        gameballsdk/src/test/java/com/gameball/gameball/inappmessaging/domain/MessageSelectorTest.kt
git commit -m "feat(iam): select at most one campaign per occurrence

A pure function over plain data with the clock injected, so every rule is
testable without waiting or mocking.

Expiry is checked here rather than only at fetch, because campaigns are cached
for the session. Unsupported types and campaigns whose artwork failed are
filtered here too, so a usable lower-priority campaign wins the occurrence
instead of it being wasted. Quiet hours suppress rather than defer, since the
pending slot is in-memory and a window is hours long.

Ties break on response order, which is the marketer's dashboard ordering. The
index is written into the comparator explicitly even though a stable sort would
preserve it, so that replacing it with campaignId reads as the behaviour change
it is."
```

---

## Task 9: Persistence

Spec §13. Four stores over the existing `SharedPreferencesUtils`, so logout's `clearData()` clears IAM state too.

**Files:**
- Modify: `local/SharedPreferencesUtils.kt` — four accessor pairs and four key constants
- Create: `inappmessaging/data/IamStore.kt`, `inappmessaging/data/DisplayHistory.kt`, `inappmessaging/data/CampaignCache.kt`
- Test: `inappmessaging/data/PersistenceTest.kt` (Robolectric — `SharedPreferencesUtils` needs a `Context`)

**Interfaces:**
```kotlin
internal class IamStore(private val prefs: SharedPreferencesUtils) {
    enum class Slot { CAMPAIGN_CACHE, DISPLAY_HISTORY, OUTBOX, VARIABLES }
    /** Returns null when the stored blob belongs to another customer, and clears it. */
    fun readScoped(slot: Slot, customerId: String): String?
    fun writeScoped(slot: Slot, customerId: String, payload: String)
    /** The outbox is not customer-scoped — see the note below. */
    fun readRaw(slot: Slot): String?
    fun writeRaw(slot: Slot, payload: String?)
    fun clear(slot: Slot)
    fun clearAll()
}

internal class DisplayHistory(private val store: IamStore) {
    fun load(customerId: String): DisplayHistorySnapshot
    fun recordImpression(customerId: String, campaignId: Int, atMillis: Long)
    fun clear()
}

internal class CampaignCache(private val store: IamStore) {
    /** Stores the raw payload, never serialised objects. */
    fun put(customerId: String, rawPayload: String)
    /** Re-parses on read, so a payload a newer SDK rejects is not resurrected as stale objects. */
    fun get(customerId: String): SyncResult?
    fun clear()
}
```

**Rules:**
- Every scoped blob is `{"customerId": "...", "data": …}`. On a mismatch at read, return null **and clear the slot** — showing one person's campaigns, or name, to another is the failure this exists to prevent.
- A corrupt or unreadable store must not stop messaging. Log, discard, carry on.
- The campaign cache stores the **raw payload**, so there is no serialiser to keep in step with the model and the parser stays the only thing that reads a sync. Re-parsing on read also means quiet hours cannot be silently dropped by a field-by-field rebuild — the Flutter bug that made going offline a way to message someone at 3am.
- `apply()`, never `commit()`. The first load happens off the main thread; there is **no read timeout** — `SharedPreferences` is in-process, and porting Flutter's 2 s bound is how a once-ever campaign displays twice.
- Display history grows without pruning. The backend stops returning a non-repeatable campaign once its impression lands, so forgetting it locally could show a once-ever message twice. If ever bounded, cap the entry count and drop oldest — never prune by "no longer in the current sync".

> **Deviation, deliberate.** Spec §13 says all four stores are customer-scoped. The outbox is not, because each entry carries the `customerId` it was produced under and flushes group by it (Task 11). Discarding the outbox on a customer change would lose analytics that are already correctly attributed, and unlike the other three the outbox is never shown to anybody. Record this in the class comment.

- [ ] **Step 1: Write the failing test**

Create `inappmessaging/data/PersistenceTest.kt`, `@RunWith(RobolectricTestRunner::class)`, with `SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())` and `clearData()` in `@Before`. Tests:

1. `a display record survives a simulated restart` — record an impression, build a **new** `DisplayHistory` over a new `IamStore`, assert the record is still there. This is the "once-ever campaign stays suppressed across a restart" row.
2. `history is scoped per customer` — record for `alice`, load for `bob`, assert empty.
3. `reading another customer's blob clears it` — write for `alice`, read for `bob` (null), then read for `alice` again and assert it is **also** gone.
4. `lastDisplayAtMillis tracks the most recent display from any campaign`.
5. `the campaign cache stores the raw payload and re-parses it on read` — put a payload with a quiet-hours window, get it back, assert `quietHours` is non-null. **This is the regression test for the Flutter bug**; assert the window explicitly, not just that campaigns came back.
6. `a corrupt blob is discarded rather than throwing` — write `"{ not json"` directly through `writeRaw`, assert `load`/`get` return empty/null and do not throw.
7. `clearAll removes every slot`.

- [ ] **Step 2: Run and verify it fails**

Run: `./gradlew :gameballsdk:testDebugUnitTest --tests '*PersistenceTest*'`

- [ ] **Step 3: Add the four accessor pairs to `SharedPreferencesUtils`**

Following the file's existing style, add to `PreferencesContract`:

```kotlin
        const val IAM_CAMPAIGN_CACHE = "IAM_CAMPAIGN_CACHE"
        const val IAM_DISPLAY_HISTORY = "IAM_DISPLAY_HISTORY"
        const val IAM_OUTBOX = "IAM_OUTBOX"
        const val IAM_VARIABLES = "IAM_VARIABLES"
```

and the matching public `putIamCampaignCache(String?)` / `getIamCampaignCache(): String?` pairs delegating to the existing private `putString` / `getString`. Purely additive; no existing behaviour changes.

- [ ] **Step 4: Implement `IamStore`, `DisplayHistory` and `CampaignCache`**

`IamStore` wraps each scoped write as `{"customerId":…,"data":…}` using the Gson instance already held by `SharedPreferencesUtils`. The scoped read:

```kotlin
    fun readScoped(slot: Slot, customerId: String): String? {
        val raw = readRaw(slot) ?: return null
        val envelope = try {
            JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject
        } catch (t: Throwable) {
            IamLog.w("$slot is unreadable; discarding it")
            clear(slot)
            return null
        }
        val owner = envelope?.str("customerId")
        if (owner == null || owner != customerId) {
            // One person's campaigns — or name — must never reach another.
            IamLog.d("$slot belongs to a different customer; discarding it")
            clear(slot)
            return null
        }
        return envelope.child("data")?.toString()
    }
```

`DisplayHistory` serialises `Map<Int, DisplayRecord>` plus `lastDisplayAtMillis`. `recordImpression` reads, updates, writes with `apply()`, and never blocks.

`CampaignCache.get` calls `MessageParser.parse(rawPayload)` — the parser is the only thing that reads a sync.

- [ ] **Step 5: Run and verify**

Expected: PASS, 7 tests.

- [ ] **Step 6: Prove the quiet-hours-through-cache test can fail**

Temporarily make `CampaignCache.get` return `SyncResult(parsed.campaigns, parsed.cooldownSeconds, null)` — the field-by-field rebuild that shipped in Flutter. Re-run. Expected: `the campaign cache stores the raw payload and re-parses it on read` FAILS. Restore.

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(iam): persist display history and the campaign cache

Both are customer-scoped and discarded on a mismatch at read, because showing
one person's campaigns to another is the failure this scoping exists to
prevent.

The campaign cache stores the raw payload and re-parses it on read rather than
serialising the model. That keeps the parser the only thing that reads a sync,
and it means a field-by-field rebuild cannot silently drop the quiet-hours
window, which in Flutter made going offline a way to message someone at 3am.

No read timeout: SharedPreferences is in-process, and porting Flutter's bound
is how a once-ever campaign displays twice."
```

---

## Task 10: The wire layer

Spec §5, §5.1, §5.2. Retrofit `suspend` functions on the existing OkHttp client, so `apiPrefix` (L2) and the shared headers come for free.

**Files:**
- Create: `inappmessaging/data/IamApi.kt` (interface + request/response DTOs)
- Create: `inappmessaging/data/MessageSource.kt` (interface + `RemoteMessageSource`)
- Test: `inappmessaging/data/RemoteMessageSourceTest.kt` (MockWebServer)

**Interfaces:**
```kotlin
internal interface IamApi {
    @POST(Config.InAppMessagesSync)
    suspend fun sync(@Body body: SyncRequest): Response<ResponseBody>
    @POST(Config.InAppMessagesEvents)
    suspend fun sendEvents(@Body body: EventBatchRequest): Response<EventBatchResponse>
    @POST(Config.InAppMessagesVariables)
    suspend fun variables(@Body body: VariablesRequest): Response<VariablesResponse>
}

internal const val PLATFORM_ANDROID = 2

internal data class SyncRequest(
    @SerializedName("customerId") val customerId: String,
    @SerializedName("platform") val platform: Int,
    @SerializedName("locale") val locale: String,
    @SerializedName("appVersion") val appVersion: String?,
    @SerializedName("sdkVersion") val sdkVersion: String
)

internal interface MessageSource { suspend fun fetch(customerId: String): SyncOutcome }

internal sealed class SyncOutcome {
    data class Success(val rawPayload: String, val result: SyncResult) : SyncOutcome()
    /** [permanent] distinguishes "stop asking" from "try again next session". */
    data class Failure(val reason: String, val permanent: Boolean) : SyncOutcome()
}
```

**Rules:**
- `platform` is **always 2**. Anything else returns 200 with an empty list — a feature that silently does nothing, indistinguishable from an account with no campaigns. Log loudly before sending another value.
- `locale` resolves device locale → player preferred language → `en` → any available. Reuse `LanguageUtils.handleLanguage()`.
- Sync returns raw `ResponseBody` because `MessageParser` hand-walks it; never bind it reflectively.
- Failure mapping: 400 / 401 → permanent, log loudly. **404 with an `ErrorResponse` body** = unknown customer (transient, self-heals). **404 with no body** = the endpoint is not deployed on this environment — log these two *differently*, or both look like a wrong base URL. 422 `PlayerInactive` → permanent. 503, timeouts, IO errors → transient.
- Every non-2xx means "could not ask", which is **not** "no campaigns". The caller falls back to the cache only on `Failure`.
- Never block app startup on this call.

- [ ] **Step 1: Write the failing test**

Create `RemoteMessageSourceTest.kt` using `MockWebServer` and a Retrofit built against `server.url("/")`. Tests:

1. `the sync body carries platform 2 and the resolved locale` — read `server.takeRequest().body.readUtf8()` and assert the JSON fields.
2. `a 200 payload is parsed into campaigns` — enqueue the live fixture from Task 6.
3. `a 200 with an empty messages array is a success, not a failure` — this is the case that must **replace** the cache rather than fall back to it.
4. `400 and 401 are permanent failures`.
5. `404 with a body and 404 without one are both failures but log differently` — assert `Failure.reason` differs.
6. `422 is a permanent failure`.
7. `503 is a transient failure`.
8. `a socket failure is a transient failure` — `server.shutdown()` before the call.
9. `a 200 with an unparseable body is a success carrying no campaigns` — the parser never throws, and the payload was served, so this is not a network failure.

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

`RemoteMessageSource.fetch` wraps the call in `withContext(Dispatchers.IO)` and a `try/catch (t: Throwable)` returning `Failure(t.message ?: "network error", permanent = false)`. Success reads `response.body()?.string()` and hands it to `MessageParser.parse`.

```kotlin
    private fun failureFor(code: Int, hasBody: Boolean): SyncOutcome.Failure = when (code) {
        400 -> SyncOutcome.Failure("400 — the sync request had no customerId", permanent = true)
        401 -> SyncOutcome.Failure("401 — the API key was rejected", permanent = true)
        // Two very different problems behind one status. Distinguishing them is the difference
        // between "this customer does not exist yet" and "you are pointed at the wrong host".
        404 -> if (hasBody) {
            SyncOutcome.Failure("404 — the backend does not know this customer yet", permanent = false)
        } else {
            SyncOutcome.Failure(
                "404 with no body — the in-app messaging endpoints are not deployed on this " +
                    "environment. The V4 endpoints are alpha-only; production returns a bare 404",
                permanent = true
            )
        }
        422 -> SyncOutcome.Failure("422 — the customer is deactivated", permanent = true)
        else -> SyncOutcome.Failure("HTTP $code", permanent = false)
    }
```

Guard the platform code at the single point it is written:

```kotlin
    init {
        if (PLATFORM_ANDROID != 1 && PLATFORM_ANDROID != 2) {
            IamLog.e(
                "platform code $PLATFORM_ANDROID is not 1 or 2; the backend will answer 200 " +
                    "with an empty message list and messaging will silently do nothing"
            )
        }
    }
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(iam): add the sync wire layer

Retrofit suspend functions on the shared OkHttp client, so apiPrefix and the
existing headers apply without duplication — the iOS port shipped a fix for
exactly the base-URL divergence a separate client would reintroduce.

Sync returns a raw body because the parser hand-walks it. The two kinds of 404
are logged differently: one means the customer does not exist yet, the other
means the V4 endpoints are not deployed on this environment, and conflating
them makes both look like a wrong base URL."
```

---

## Task 11: The analytics outbox

Spec §5.3, §11. Fire-and-forget, at-least-once, deduplicated server-side.

**Files:**
- Create: `inappmessaging/data/AnalyticsOutbox.kt` (interface `MessageAnalytics` + `PersistentAnalyticsOutbox`)
- Test: `inappmessaging/data/AnalyticsOutboxTest.kt`

**Interfaces:**
```kotlin
internal enum class IamEventType(val wire: String) {
    IMPRESSION("impression"), CLICK("click"), DISMISS("dismiss")
}

internal data class IamEvent(
    val eventUid: String,          // a real UUID v4, generated once and never regenerated
    val customerId: String,
    val dispatchId: String?,
    val campaignId: Int,
    val variationId: Int?,
    val type: IamEventType,
    val occurredAtMillis: Long,
    val buttonId: String? = null,
    val url: String? = null
)

internal interface MessageAnalytics {
    fun start()
    /** Never blocks the caller. */
    fun record(event: IamEvent)
    suspend fun flush(timeoutMillis: Long? = null)
    fun dispose()
}

internal enum class FlushOutcome { ACCEPTED, RETRY, DISCARD }
internal fun outcomeFor(code: Int): FlushOutcome
```

**Rules:**

| Property | Value |
|---|---|
| Flush interval | 30 s |
| Flush at count | 10 events |
| Events per request | 50 |
| Outbox ceiling | 500 — drop the oldest and log |
| Concurrency | one request in flight; a second call while one runs is dropped, because the in-flight one re-arms on completion |
| Persisted | after every change |
| Backoff | none — a retryable failure re-arms the ordinary 30 s timer, and a successful flush that leaves a backlog re-flushes **immediately** |

- Status mapping: 2xx → `ACCEPTED` (the endpoint answers **202**, and narrowing to exactly 200 was defect 4). 408 / 429 / 5xx / IO → `RETRY`. Every other 4xx → `DISCARD`. *(The spec lists 400/401/404/422 as discard; mapping all remaining 4xx that way rather than retrying is deliberate — a 403 retried forever is a poison batch at the head of a FIFO, and the outbox is FIFO.)*
- A 2xx with `rejected > 0` still clears the batch. Those events can never succeed; the counts are diagnostics.
- An unreadable 2xx body is still accepted.
- `eventUid` is `java.util.UUID.randomUUID().toString()` — never a timestamp, counter or hash. A non-GUID is a hard 400 that discards the **entire batch**.
- `occurredAt` is when it happened on device, formatted by `IamTime.toIso8601Utc`.
- **`isTest` campaigns report nothing at all** — the caller never constructs an event for one.
- Batches group by `customerId` (see Task 9's note) and go oldest first.
- Forced flush on background, on `stop()`, and before an outward action bounded at ~800 ms.

- [ ] **Step 1: Write the failing test**

`AnalyticsOutboxTest.kt`, Robolectric, `runTest` from `kotlinx-coroutines-test`. Tests:

1. `eventUid is a valid v4 UUID` — assert against `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`. Generate 100 and assert every one matches.
2. `the uid is never regenerated on retry` — fail the first flush with 503, succeed the second, assert both requests carried the same uid.
3. `202 is accepted` — the defect 4 regression.
4. `200 with rejected greater than zero still clears the batch`.
5. `a 2xx with an unreadable body is still accepted`.
6. `400, 401, 404 and 422 discard permanently`.
7. `403 discards rather than blocking the queue forever`.
8. `408, 429 and 503 keep the batch`.
9. `a batch is chunked at 50`.
10. `the ceiling drops the oldest and logs` — record 501, assert the first is gone and the last is present.
11. `the outbox survives a restart` — record, construct a new outbox over the same store, flush, assert the event is sent.
12. `only one request is in flight at a time`.
13. **`occurredAt is ASCII ISO-8601 with the device locale set to Arabic`** — the L3 guard. Set `Locale.setDefault(Locale("ar","EG"))`, record an event, read the request body, assert the `occurredAt` string matches `^\d{4}-\d{2}-\d{2}T` and is all ASCII.
14. `events are grouped by customerId into separate requests`.

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

```kotlin
internal fun outcomeFor(code: Int): FlushOutcome = when {
    // The endpoint answers 202, not 200. Narrowing success to exactly 200 reported every
    // accepted event to the host as a failure.
    code in 200..299 -> FlushOutcome.ACCEPTED
    code == 408 || code == 429 -> FlushOutcome.RETRY
    // Everything else in 4xx can never succeed. The outbox is FIFO, so retrying a permanently
    // rejected batch blocks every event behind it and takes all analytics down.
    code in 400..499 -> FlushOutcome.DISCARD
    else -> FlushOutcome.RETRY
}
```

The scheduler is a coroutine `while (isActive) { delay(30_000); flush() }` on the module scope. `dispose()` cancels it and sets `disposed = true`; **`start()` must clear that flag** — otherwise, after one stop/start cycle the timer is never armed again and events only go out when the batch happens to reach its count threshold.

- [ ] **Step 4: Run and verify.** Expected: PASS, 14 tests.

- [ ] **Step 5: Prove the 202 test can fail**

Temporarily change the mapping to `code == 200 -> ACCEPTED`. Re-run. Expected: `202 is accepted` FAILS. Restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): add the persistent analytics outbox

Accepts the whole 2xx range: the endpoint answers 202, and narrowing success
to exactly 200 reported every accepted event as a failure in Flutter.

eventUid is a real UUID v4 generated once and never regenerated on retry,
which is what makes at-least-once delivery safe to deduplicate server-side. A
non-GUID is a hard 400 that discards the entire batch, so the shape is
asserted in a test.

Every 4xx outside 408 and 429 discards rather than retrying: the outbox is
FIFO, so one permanently rejected batch at the head takes all analytics down."
```

---

## Task 12: Personalisation

Spec §12, §12.1. Live, not dormant — campaigns on the alpha account carry `{player_name}` and `{points_balance}` today.

**Files:**
- Create: `inappmessaging/domain/Personalization.kt`
- Create: `inappmessaging/data/VariableSource.kt` (interface + `RemoteVariableSource`)
- Test: `inappmessaging/domain/PersonalizationTest.kt`, `inappmessaging/data/RemoteVariableSourceTest.kt`

**Interfaces:**
```kotlin
internal object Personalization {
    fun hasToken(text: String?): Boolean
    fun tokenNames(vararg texts: String?): Set<String>
    /** Leaves unknown tokens in place so a caller can tell resolved from unresolved. */
    fun substitute(text: String?, values: Map<String, String>): String?
    /** The final pass. Blanks anything still unresolved. */
    fun blankUnresolved(text: String?): String?
}

internal interface VariableSource {
    /** Never throws, never blocks longer than its own bound. Empty map on any failure. */
    suspend fun values(customerId: String, needed: Set<String>): Map<String, String>
    /** Drop the freshness cache but keep the persisted fallback. */
    fun invalidate()
    fun clear()
}
```

**Rules:**
- Token syntax is `{name}` where name matches `[A-Za-z_][A-Za-z0-9_]*`. Not Liquid, no double braces, no filters. `{ spaced }`, `{2}` and a lone `{` are **not** tokens — a loose pattern lets a value map mangle ordinary copy.
- **One pass only.** A substituted value is data, not a template. Walk matches in reverse so each replacement cannot invalidate the ranges still to come.
- Cheap-scan for `{` before running the regex, so a message with no tokens costs one character comparison and **never calls the endpoint**.
- Applies to header, body and every button label.
- Values are inserted **verbatim** — they arrive pre-formatted with thousand separators applied.
- Fetch bounded at **2 s**, cached **60 s** per customer. On timeout, error or empty result, display the text already held.
- The cache is dropped **on every event and purchase**, before evaluating — but **not** on session start. The campaign this exists for is "you just earned 200 points, you now have X", whose trigger is the purchase.
- **The timeout lives where the fallback lives.** In Flutter it fired in the service, outside the source that owned the persisted fallback, and the cache had already been cleared for freshness — so a 716 ms call rendered a raw token. `invalidate()` drops freshness and **keeps** the persisted copy; only `clear()` removes it.
- Unresolved tokens are **blanked, and the message still displays**. Applied as a final pass on every display path, including the one where substitution never ran. A token resolving to `""` counts as resolved.
- **PII:** persist only the token names the held campaigns actually mention. The endpoint returns nine keys, four of which are personal data, whether or not any campaign mentions them.
- **The write-after-clear race is real.** The write following a fetch is not awaited, so a clear issued moments later can be overtaken by it. Re-check the customer **after** acquiring storage, not before — a check before the suspension point always passes.

- [ ] **Step 1: Write the failing tests**

`PersonalizationTest.kt` (pure, no Robolectric):

1. `a known token is substituted`
2. `an unknown token is left exactly as written` — `substitute` leaves `{missing}` in place
3. `malformed braces are untouched` — `{ spaced }`, `{2}`, `{`, `{}`, `{{a}}` (assert the inner one substitutes but the braces around it survive)
4. `substitution is one pass` — a value containing `{other}` is not expanded again
5. `reverse walking keeps ranges valid` — a message with three tokens where the first value is much longer than its token
6. `hasToken is false for text with no brace`
7. `tokenNames collects across header, body and button labels`
8. `blankUnresolved removes anything still in braces`
9. `blankUnresolved leaves already-substituted text alone`
10. `a token resolving to an empty string counts as resolved` — assert `Nice pick, {player_name} —` with `player_name` → `""` gives `Nice pick,  —`, not a blanked-out result

`RemoteVariableSourceTest.kt` (MockWebServer):

11. `a message with no tokens never calls the endpoint` — assert `server.requestCount == 0`
12. `values are cached for 60 seconds`
13. `the cache is dropped by invalidate but the persisted fallback survives`
14. `a timeout returns the persisted values, not an empty map`
15. `404, 422, 503 and a socket error all return the fallback`
16. `only the tokens the campaigns mention are persisted` — assert the stored blob contains `points_balance` and **not** `player_email`
17. `a pending write cannot resurrect cleared values` — start a fetch, call `clear()` before the write lands, assert storage is empty afterwards

- [ ] **Step 2: Run and verify they fail**

- [ ] **Step 3: Implement `Personalization`**

```kotlin
internal object Personalization {

    /**
     * Single braces around a bare identifier. Deliberately strict: a loose pattern lets a
     * value map mangle ordinary copy, so "{ spaced }", "{2}" and a lone "{" are not tokens.
     */
    private val TOKEN = Regex("""\{([A-Za-z_][A-Za-z0-9_]*)}""")

    fun hasToken(text: String?): Boolean =
        text != null && text.indexOf('{') >= 0 && TOKEN.containsMatchIn(text)

    fun tokenNames(vararg texts: String?): Set<String> =
        texts.filterNotNull()
            .filter { it.indexOf('{') >= 0 }
            .flatMap { TOKEN.findAll(it).map { m -> m.groupValues[1] } }
            .toSet()

    /**
     * One pass. Matches come from the original text and are applied in reverse, so a
     * replacement can neither invalidate a range still to come nor be rescanned — a
     * substituted value is data, not a template.
     */
    fun substitute(text: String?, values: Map<String, String>): String? {
        if (text == null || text.indexOf('{') < 0) return text
        val matches = TOKEN.findAll(text).toList()
        if (matches.isEmpty()) return text
        val builder = StringBuilder(text)
        for (match in matches.asReversed()) {
            val replacement = values[match.groupValues[1]] ?: continue
            builder.replace(match.range.first, match.range.last + 1, replacement)
        }
        return builder.toString()
    }

    /**
     * The guarantee that a raw template never reaches a screen. Applied on every display
     * path, including the ones where substitution never ran at all — a timed-out fetch, for
     * instance. Blanking rather than suppressing is deliberate: a value the SDK could not get
     * is a backend problem to find and fix, not one for the SDK to hide by withholding the
     * campaign.
     */
    fun blankUnresolved(text: String?): String? =
        if (text == null || text.indexOf('{') < 0) text else TOKEN.replace(text, "")
}
```

- [ ] **Step 4: Implement `RemoteVariableSource`**

`values()` cheap-scans, checks the 60 s cache, then `withTimeoutOrNull(2_000) { api.variables(...) }` **inside** the source so the fallback is reachable on timeout. The persisted write filters to `needed`:

```kotlin
        // The endpoint returns all nine keys including four pieces of PII, whether or not any
        // campaign mentions them. Only what the held campaigns actually use lands on disk.
        val toPersist = fetched.filterKeys { it in needed }
        scope.launch {
            // Re-check the customer AFTER acquiring storage: a check before the suspension
            // point always passes, because the clear has not been issued yet.
            if (currentCustomerId != customerId) return@launch
            store.writeScoped(IamStore.Slot.VARIABLES, customerId, gson.toJson(toPersist))
        }
```

- [ ] **Step 5: Run and verify.** Expected: PASS, 17 tests.

- [ ] **Step 6: Prove the blanking pass can fail**

Temporarily make `blankUnresolved` return its input. Re-run. Expected: `blankUnresolved removes anything still in braces` FAILS. Restore.

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(iam): substitute personalisation tokens and blank unresolved ones

Substitution is a single pass over matches of the original text, applied in
reverse so a replacement can neither invalidate a later range nor be rescanned.
The token pattern is deliberately strict: a loose one lets a value map mangle
ordinary copy.

The 2s timeout lives inside the source that owns the persisted fallback. In
Flutter it fired in the service instead, outside the fallback's reach, and a
716ms call rendered a raw {player_name} on screen.

Only the token names the held campaigns mention are persisted: the endpoint
returns nine keys including four pieces of PII whether or not any campaign
mentions them."
```

---

## Task 13: The artwork prefetcher

Spec §10. A campaign whose artwork failed is passed over, letting a lower-priority ready one take the slot. Braze and CleverTap both do the same — do not "improve" this by rendering text-only.

**Files:**
- Create: `inappmessaging/artwork/ArtworkPrefetcher.kt` (interface + `PicassoArtworkPrefetcher`)
- Test: `inappmessaging/artwork/ArtworkPrefetcherTest.kt`

**Interfaces:**
```kotlin
internal interface ArtworkPrefetcher {
    /** Warms the whole set concurrently, bounded at 5s overall. */
    suspend fun warm(urls: Set<String>)
    /** Null or blank counts as ready — a campaign with no artwork has nothing to wait for. */
    fun isReady(url: String?): Boolean
    /** Re-attempts only the failed set, at most once per 30s. Does not block the caller. */
    fun retryFailedIfDue(nowMillis: Long)
    fun reset()
}

internal fun ArtworkPrefetcher.isReady(content: MessageContent): Boolean =
    isReady(content.imageUrl) && isReady(content.iconUrl)
```

**Rules:**
- Picasso's `fetch()` warms the cache with no target view — precisely the prefetcher primitive. At display, an ordinary `load().into()` hits the warm cache.
- **Warm the whole set, not just the winner.** An event trigger fires with no warning and no time to fetch.
- Bounded at **5 s, concurrently**, so the ceiling is the slowest single image rather than the sum. Picasso has no per-request timeout, so impose it with `withTimeoutOrNull(5_000)` around a suspending wrapper.
- **Do not cache the failure for the session.** Flutter computed the verdict once at sync, so a two-second blip made a campaign undisplayable for eight minutes. Re-attempt the failed set at most once per 30 s, fired without blocking the evaluation in flight, and place the retry trigger **before** the "nothing displayable, return early" branch — a session where everything failed is exactly the one that has to recover.
- Re-attempt **only the failed set**. Re-fetching decoded artwork costs a request per campaign per retry for nothing.
- Log artwork served over `http://` **by name** — cleartext is blocked by default since API 28, so the load fails and the only symptom is a campaign that silently never shows.

- [ ] **Step 1: Write the failing test**

`ArtworkPrefetcherTest.kt`. Picasso needs a `Context`, so extract the actual fetch behind a `suspend (String) -> Boolean` seam the test can substitute:

```kotlin
internal class PicassoArtworkPrefetcher(
    private val scope: CoroutineScope,
    private val clock: Clock,
    @VisibleForTesting internal val fetch: suspend (String) -> Boolean = ::picassoFetch
) : ArtworkPrefetcher
```

Tests:
1. `a null or blank url is ready` — nothing to wait for.
2. `a warmed url is ready and a failed one is not`.
3. `the whole set is warmed concurrently` — record start times; assert total elapsed is bounded by the slowest, not the sum (use a `TestScope` virtual clock).
4. `a hung load is bounded at five seconds and the campaign is skipped`.
5. `the failed set is re-attempted after thirty seconds` — fail once, advance the clock 30 s, `retryFailedIfDue`, assert `isReady` becomes true.
6. `it does not re-attempt within thirty seconds` — assert the fetch count does not grow.
7. `the retry asks only about what failed` — warm two urls, fail one, retry, assert only the failed url was re-fetched.
8. `http urls are logged by name` — assert the log message contains the URL.
9. `reset clears both the ready and the failed sets`.

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

```kotlin
    override suspend fun warm(urls: Set<String>) {
        val pending = urls.filter { it.isNotBlank() && it !in ready }.toSet()
        if (pending.isEmpty()) return
        pending.filter { it.startsWith("http://") }.forEach {
            IamLog.w(
                "artwork is served over cleartext http and will fail on API 28+: $it — the " +
                    "campaign using it will silently never show"
            )
        }
        // Concurrent, so the ceiling is the slowest single image regardless of how many
        // campaigns arrive rather than the sum of all of them.
        withTimeoutOrNull(WARM_TIMEOUT_MS) {
            coroutineScope {
                pending.map { url -> async { url to fetch(url) } }.awaitAll()
            }
        }?.forEach { (url, ok) -> if (ok) markReady(url) else markFailed(url) }
            ?: pending.forEach { if (it !in ready) markFailed(it) }
    }

    override fun retryFailedIfDue(nowMillis: Long) {
        if (failed.isEmpty()) return
        if (nowMillis - lastRetryAtMillis < RETRY_INTERVAL_MS) return
        lastRetryAtMillis = nowMillis
        val toRetry = failed.toSet()   // only the failed set
        // Fired without blocking the evaluation in flight: the current trigger still misses,
        // every later one recovers.
        scope.launch { warm(toRetry) }
    }
```

with `private const val WARM_TIMEOUT_MS = 5_000L` and `RETRY_INTERVAL_MS = 30_000L`.

- [ ] **Step 4: Run and verify.** Expected: PASS, 9 tests.

- [ ] **Step 5: Prove the retry test can fail**

Temporarily make `retryFailedIfDue` a no-op. Re-run. Expected: `the failed set is re-attempted after thirty seconds` FAILS. Restore. This is defect 11: a brief blip made a campaign undisplayable for eight minutes.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): warm campaign artwork before anything displays

Every held campaign's artwork is warmed at sync, not just the winner's, because
an event trigger fires with no warning. The whole set is fetched concurrently
under one 5s bound, so the ceiling is the slowest single image rather than the
sum.

The failure verdict is not cached for the session. Flutter computed it once at
sync, and a two-second blip made a campaign undisplayable for eight minutes.
Only the failed set is re-attempted, at most once every 30s, without blocking
the evaluation in flight."
```

---

## Task 14: Activity tracking and session lifecycle

Spec §8.2, §9.1. Defect 1 lives here: treating `onActivityPaused`/`onActivityStopped` as "the app went to background" makes every screen transition look like a new session.

**Files:**
- Create: `inappmessaging/runtime/ActivityTracker.kt`
- Create: `inappmessaging/runtime/SessionState.kt`
- Test: `inappmessaging/runtime/SessionLifecycleTest.kt`

**Interfaces:**
```kotlin
internal class ActivityTracker(
    private val application: Application,
    private val callbacks: Callbacks
) : Application.ActivityLifecycleCallbacks {
    internal interface Callbacks {
        fun onAppForegrounded()
        fun onAppBackgrounded()
        /** A retry trigger: an Activity became available, or the device rotated. */
        fun onSurfaceAvailable()
    }
    /** Always resolved fresh; never cached by the presenter. */
    val currentActivity: Activity?
    fun register()
    fun unregister()
}

internal class SessionState(private val clock: Clock, private val sessionTimeoutMillis: Long) {
    fun onBackgrounded()
    /** True when the absence exceeded the timeout and a new session should start. */
    fun onForegrounded(): Boolean
    fun reset()
}
```

**Rules:**
- **Count started Activities.** A screen transition (start B, stop A) never dips to zero, and neither does rotation. `ProcessLifecycleOwner` does exactly this; it is not added as a dependency because the counting is five lines.
- **The pause stamp is first-wins.** `if (lastPausedAt == null) lastPausedAt = now()`. This is the fix that does not depend on getting the callback taxonomy exactly right — if a future refactor adds another source of "backgrounded", it stays correct.
- `currentActivity` is a **`WeakReference`**, set in `onActivityResumed` and cleared in `onActivityPaused` only when it is the same instance. A strong reference from a process-lifetime singleton is a textbook leak, and this module lives in `GameballApp`.
- The `Application` comes from `context.applicationContext as Application` — never hold the `Context` you were handed.
- **Register on opt-in, unregister on stop.** Part of the compatibility invariant.
- Session timeout is **30 s**, deliberately equal to the display cooldown default. Do **not** raise it dynamically when the server raises the cooldown; that was tested on Flutter and makes things worse.

- [ ] **Step 1: Write the failing test**

`SessionLifecycleTest.kt`, Robolectric, driving `ActivityTracker` by calling the lifecycle methods directly with `Robolectric.buildActivity(Activity::class.java).get()` instances. Tests:

1. `a cold start foregrounds once`
2. **`navigating between activities does not background the app`** — `started(A)`, `started(B)`, `stopped(A)`; assert `onAppBackgrounded` was never called.
3. **`rotation does not background the app`** — `started(A')`, `stopped(A)`.
4. `leaving the app backgrounds it exactly once`
5. `returning foregrounds it exactly once`
6. `several backgrounded notifications before one resume still measure the full absence` — call `onBackgrounded()` at t, again at t+20s, then `onForegrounded()` at t+40s with a 30 s timeout; assert it reports a new session. *(First-wins: if the last stamp won, the measured absence would be 20 s and the session would be missed.)*
7. `a resume inside the timeout does not start a new session`
8. `a resume beyond the timeout starts a new session`
9. `the current activity is held weakly and cleared on pause`
10. `pausing a different activity does not clear the current one`
11. `unregister stops all callbacks`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

```kotlin
    private var startedActivities = 0

    override fun onActivityStarted(activity: Activity) {
        // Counting started Activities means a screen transition (start B, stop A) never dips
        // to zero, and neither does a rotation. Treating onActivityPaused/onActivityStopped
        // as "backgrounded" makes every navigation look like a new session.
        if (startedActivities == 0) callbacks.onAppForegrounded()
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities <= 0) {
            startedActivities = 0
            callbacks.onAppBackgrounded()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)   // never a strong ref
        callbacks.onSurfaceAvailable()      // a retry trigger
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) current = null
    }
```

```kotlin
internal class SessionState(private val clock: Clock, private val sessionTimeoutMillis: Long) {
    private var lastPausedAtMillis: Long? = null

    /**
     * First pause wins. This is the fix that does not depend on getting the callback taxonomy
     * exactly right: if another source of "backgrounded" is ever added, the earliest stamp
     * still measures the real absence. Last-wins measured every absence as zero in Flutter,
     * and session_start only ever fired on a cold launch.
     */
    fun onBackgrounded() {
        if (lastPausedAtMillis == null) lastPausedAtMillis = clock.nowMillis()
    }

    fun onForegrounded(): Boolean {
        val since = lastPausedAtMillis ?: return false
        lastPausedAtMillis = null
        return clock.nowMillis() - since > sessionTimeoutMillis
    }

    fun reset() { lastPausedAtMillis = null }
}
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 11 tests.

- [ ] **Step 5: Prove the two Android-specific tests can fail**

Temporarily make `onActivityStopped` call `onAppBackgrounded()` unconditionally. Re-run. Expected: `navigating between activities does not background the app` and `rotation does not background the app` FAIL. Restore.

Then temporarily change the stamp to last-wins (`lastPausedAtMillis = clock.nowMillis()`). Re-run. Expected: `several backgrounded notifications before one resume still measure the full absence` FAILS. Restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): track the foreground session without false transitions

Counts started Activities rather than treating onActivityPaused/onStopped as
'the app went to background' — those fire on every screen transition and every
rotation, which would make each one look like a new session.

The pause stamp is first-wins, which is the fix that survives a future refactor
adding another source of 'backgrounded'. Last-wins measured every absence as
zero in Flutter and killed every warm-resume campaign for the life of the
install.

The current Activity is held weakly: a strong reference from a process-lifetime
singleton is a leak, and this module lives in GameballApp."
```

---

## Task 15: The orchestrating service

Spec §4.4, §8.8, §9.2. Owns sequencing, deferral and the pending slot — and no display policy.

**Files:**
- Create: `inappmessaging/runtime/InAppMessagingService.kt`
- Create: `inappmessaging/runtime/PendingPresentation.kt`
- Test: `inappmessaging/runtime/InAppMessagingServiceTest.kt`

**Interfaces:**
```kotlin
internal class InAppMessagingService(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val source: MessageSource,
    private val cache: CampaignCache,
    private val history: DisplayHistory,
    private val artwork: ArtworkPrefetcher,
    private val analytics: MessageAnalytics,
    private val variables: VariableSource,
    private val presenter: MessagePresenter,
    private val hooks: HostHooks
) {
    fun start(customerId: String)
    fun stop()
    val isStarted: Boolean
    fun onCustomerChanged(customerId: String)
    fun onEvent(name: String, metadata: Map<String, Any?>)
    fun onAppForegrounded(); fun onAppBackgrounded()
    fun onSurfaceAvailable()
    fun onWidgetOpened(); fun onWidgetClosed()
}

internal data class PendingPresentation(
    val campaign: Campaign,
    /** Carried on the presentation, not the view, so rotation re-presents without re-reporting. */
    var impressionReported: Boolean = false,
    var impressionAtMillis: Long? = null
)
```

This task also **defines** the presentation seam, which Tasks 17-20 implement against. It
lives here rather than with the views because the service is its only caller and a fake
implementation is what makes this task's 27 tests possible without a screen.

```kotlin
internal interface MessagePresenter {
    /** False when it cannot draw — the service defers. Never throws. */
    fun present(
        campaign: Campaign,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ): Boolean
    fun dismissCurrent()
    val isShowing: Boolean
}

internal interface PresentationCallbacks {
    /** The first pre-draw pass, not attachment. */
    fun onShown()
    /** [button] is null when the message surface itself was tapped. */
    fun onTapped(button: MessageButton?)
    fun onDismissed()
}

/** A campaign's copy after personalisation and the blanking pass. */
internal data class ResolvedMessage(
    val header: String?,
    val body: String?,
    /** Labels already substituted and blanked. */
    val buttons: List<MessageButton>
)

/**
 * The host's four hooks, already unwrapped from their public interfaces and each already
 * wrapped in its own try/catch by the facade — so nothing in the service has to defend
 * against a throwing host, and a buggy host loses its override rather than its messages.
 */
`DisplayDecision` below is public and lives in `model/DisplayDecision.kt`; create it here
rather than in Task 21, since the service is its first consumer:

```kotlin
enum class DisplayDecision { SHOW, LATER, DISCARD }
```

```kotlin
internal data class HostHooks(
    val beforeDisplay: (Campaign) -> DisplayDecision = { DisplayDecision.SHOW },
    val onAction: (Campaign, MessageButton?, MessageAction) -> Boolean = { _, _, _ -> false },
    val onNavigate: ((String, Map<String, Any?>?) -> Unit)? = null,
    val observer: ((Campaign) -> Unit)? = null
)
```

Task 21 then defines only the *public* hook interfaces and adapts them onto `HostHooks`.

**Rules:**

*Sync sequencing.* Sync and read local state **concurrently**, not in series. Apply the cache **only when the sync failed** — that removes the race where a slow cache read lands after a fast sync and clobbers fresher campaigns. A successful but **empty** sync replaces the cache. After a sync: warm artwork → declare personalisation needs → evaluate `session_start`, in that order.

*Deferral vs. suppression.* Defer (hold in the pending slot, retry later) for: no Activity, the host widget is open, another message is showing, a fullscreen orientation mismatch, `beforeDisplay` returned `later`. Suppress (the occurrence is spent) for: inside the cooldown floor, the repeat rule says no, artwork is not ready, inside quiet hours, `beforeDisplay` returned `discard`.

*The pending slot.* One slot, not a queue. A newer deferral displaces an older one, **with a log naming both**. In-memory only; it dies with the process, deliberately.

*Retry triggers.* The current message is dismissed, the host widget closes, an Activity becomes available, or the device rotates. **Re-validate with `MessageSelector.mayDisplayNow`** — asking "has it ever displayed" threw away every waiting repeatable campaign (defect 10) — and re-check the floor, so a message deferred before another displayed cannot slip through inside it.

*Artwork retry placement.* Call `artwork.retryFailedIfDue(now)` **before** the "nothing displayable, return early" branch.

*Cache invalidation.* Drop the variable cache on **every event and purchase**, before evaluating; **not** on session start.

*Hooks.* Each wrapped in its own try/catch that logs and swallows: `beforeDisplay` throws → `SHOW`; `onAction` throws → built-in handling. **The impression, click and dismissal are reported regardless of what any hook returns.**

*Action path ordering.* Report the click → ask the host → dismiss → flush telemetry (bounded ~800 ms) → perform the action. Dismissal comes before the action deliberately: a `navigate` starts a transition, and leaving the message up briefly covers the screen the user just asked for.

*`stop`.* Dismiss what is showing → clear campaigns, caps, pending slot, artwork state, quiet hours → clear personalisation values including storage → flush telemetry → dispose the scheduler → unregister lifecycle callbacks. **Flush before dispose.**

*`stop` → `start` must fully revive.* `dispose()` sets a "do not schedule" flag; `start()` must clear it, or after one cycle the flush timer is never armed again.

- [ ] **Step 1: Write the failing test**

`InAppMessagingServiceTest.kt` with fakes for all five interfaces (`FakeMessageSource`, `RecordingAnalytics`, `FakePrefetcher`, `FakePresenter` whose `present` returns a settable boolean, `FakeVariableSource`) and a `MutableClock`. Tests:

1. `a successful sync evaluates session start`
2. `an empty successful sync replaces the cache`
3. `a failed sync falls back to the cache`
4. `a successful sync does not consult the cache at all`
5. `artwork is warmed before session start is evaluated`
6. `no Activity defers rather than suppressing` — presenter returns false; assert the pending slot holds the campaign
7. `the widget being open defers`
8. `another message showing defers`
9. `a fullscreen orientation mismatch defers`
10. `beforeDisplay later defers and discard spends the occurrence`
11. `a newer deferral displaces an older one and logs both`
12. `dismissal retries the pending slot`
13. `the widget closing retries`
14. `an Activity becoming available retries`
15. **`retry re-checks eligibility, keeping a deferred repeatable campaign`** — defect 10
16. **`retry re-checks the floor, so a message deferred before another displayed waits`**
17. `the cap and the floor are recorded at impression, not at selection` — defer a campaign, assert no history was written
18. `an isTest campaign displays and reports nothing`
19. `a throwing beforeDisplay defaults to show`
20. `a throwing onAction falls back to built-in handling`
21. `the click is reported even when onAction returns true`
22. `the action path dismisses before performing the action`
23. `the variable cache is dropped on an event but not on session start`
24. `artwork retry fires before the nothing-displayable early return`
25. `stop dismisses, flushes, then disposes`
26. **`stop then start re-arms the flush timer`**
27. `a customer change refetches, resets caps and discards the previous cache`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

Key excerpts:

```kotlin
    private suspend fun startSession(customerId: String) {
        // Concurrently: the stored history gates the decision, not the request.
        val syncDeferred = scope.async { source.fetch(customerId) }
        val historyDeferred = scope.async { history.load(customerId) }
        val outcome = syncDeferred.await()
        val snapshot = historyDeferred.await()

        val result = when (outcome) {
            is SyncOutcome.Success -> {
                // A successful response replaces the entire cache; it is never merged, and an
                // empty one is still a replacement.
                cache.put(customerId, outcome.rawPayload)
                outcome.result
            }
            // Only a failure falls back. Applying the cache unconditionally reintroduces the
            // race where a slow cache read lands after a fast sync and clobbers it.
            is SyncOutcome.Failure -> {
                IamLog.w("sync failed (${outcome.reason}); falling back to the cache")
                cache.get(customerId) ?: SyncResult.EMPTY
            }
        }

        held = result
        artwork.warm(result.campaigns.flatMap { listOfNotNull(it.content.imageUrl, it.content.iconUrl) }.toSet())
        neededTokens = result.campaigns.flatMap {
            Personalization.tokenNames(it.content.header, it.content.body,
                *it.content.buttons.map { b -> b.text }.toTypedArray())
        }.toSet()
        evaluate(TriggerOccurrence.SessionStart, snapshot)
    }
```

```kotlin
    private fun defer(campaign: Campaign, reason: String) {
        val existing = pending
        if (existing != null && existing.campaign.campaignId != campaign.campaignId) {
            IamLog.d(
                "campaign ${campaign.campaignId} displaces campaign " +
                    "${existing.campaign.campaignId} in the pending slot ($reason)"
            )
        }
        pending = PendingPresentation(campaign)
    }

    private fun retryPending() {
        val slot = pending ?: return
        val snapshot = history.load(currentCustomerId ?: return)
        // "May this display now", not "has it ever displayed". The cruder question threw away
        // every repeatable campaign that happened to be waiting.
        if (!MessageSelector.mayDisplayNow(
                slot.campaign, snapshot, clock.nowMillis(), cooldownSeconds, quietHours
            ) { artwork.isReady(it.content) }
        ) {
            IamLog.d("pending campaign ${slot.campaign.campaignId} is no longer eligible; dropped")
            pending = null
            return
        }
        present(slot)
    }
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 27 tests.

- [ ] **Step 5: Prove three tests can fail**

Change `retryPending` to use `repeatEligible(...) && history.perCampaign[id] == null` → test 15 FAILS. Restore.
Remove the floor check from `mayDisplayNow`'s call site → test 16 FAILS. Restore.
Remove the flag reset from `start()` → test 26 FAILS. Restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): add the orchestrating service

Sequences sync, evaluation, deferral and the single pending slot, and owns no
display policy — selection stays a pure function.

Sync and the local history read run concurrently, and the cache is applied only
when the sync failed, which removes the race where a slow cache read lands after
a fast sync and clobbers fresher campaigns.

Retry asks 'may this display now' rather than 'has it ever displayed', which is
what keeps a deferred repeatable campaign alive, and it re-checks the global
floor so a message deferred before another displayed cannot slip through inside
it."
```

---

## Task 16: UI foundations — metrics and the close glyph

Spec §15.1, §15.2, §15.4. One file holds every number, so the later cross-platform parity pass is a diff rather than an audit.

**Files:**
- Create: `inappmessaging/ui/MessageMetrics.kt`
- Create: `inappmessaging/ui/ColorResolver.kt`
- Create: `res/values/gb_iam_strings.xml`, `res/values-ar/gb_iam_strings.xml`
- Test: `inappmessaging/ui/ColorResolverTest.kt`

**Interfaces:**
```kotlin
internal object MessageMetrics {
    object Shared { /* scrim, close glyph, button radius, luminance threshold */ }
    object Modal { /* margin, maxWidth, radius, paddings, minImageRatio, copyReserve, … */ }
    object Slideup { /* margin, maxWidth, radius, elevation, icon, chevron, maxTextLines */ }
    object Fullscreen { /* paddings, imageHeightFraction, button sizing, closePadding */ }
    fun dp(context: Context, value: Float): Int
    fun sp(context: Context, value: Float): Float
}

internal object ColorResolver {
    fun relativeLuminance(color: Int): Double
    /** The three cases, in order. Nothing here consults the artwork. */
    fun closeGlyphColor(campaignCloseButton: Int?, background: Int?, hostOnSurface: Int): Int
    fun resolve(campaign: Int?, themeAttr: Int, context: Context): Int
}
```

- [ ] **Step 1: Write `MessageMetrics`**

Transcribe the spec's constant table exactly. Values are dp unless the name says otherwise; text sizes are sp.

```kotlin
package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.util.TypedValue

/**
 * Every layout constant in the module, in one place.
 *
 * The Android counterpart to Flutter's message_view_metrics.dart and iOS's
 * MessageViewAttributes.swift. Views reference these; nothing is inlined. Cross-platform
 * visual parity is deliberately scheduled after all platforms ship, and keeping the numbers
 * here is what makes that pass a diff between four files rather than a hunt through four
 * widget trees.
 *
 * None of these is configurable by a host or a campaign. A campaign controls colour, text and
 * behaviour — never geometry.
 */
internal object MessageMetrics {

    object Shared {
        /** The dimmed layer behind a modal when the campaign names no colors.frame. */
        val DEFAULT_SCRIM: Int = 0x99000000.toInt()
        const val CLOSE_GLYPH_SIZE_DP = 24f
        /** Separate from the glyph: the accessibility minimum on both platforms. */
        const val CLOSE_TOUCH_TARGET_DP = 48f
        val CLOSE_GLYPH_ON_LIGHT: Int = 0xFF111827.toInt()
        val CLOSE_GLYPH_ON_DARK: Int = 0xFFFFFFFF.toInt()
        /** Where black and white contrast equally. Not a taste value. */
        const val CLOSE_GLYPH_LUMINANCE_THRESHOLD = 0.179
        const val BUTTON_CORNER_RADIUS_DP = 8f
    }

    object Modal {
        const val MARGIN_DP = 24f
        const val MAX_WIDTH_DP = 420f
        const val CORNER_RADIUS_DP = 16f
        const val CONTENT_PADDING_START_DP = 20f
        const val CONTENT_PADDING_TOP_DP = 20f
        const val CONTENT_PADDING_END_DP = 20f
        const val CONTENT_PADDING_BOTTOM_DP = 0f
        /** Applied only when both header and body are present. */
        const val HEADER_TO_BODY_SPACING_DP = 8f
        const val BUTTONS_PADDING_START_DP = 20f
        const val BUTTONS_PADDING_TOP_DP = 20f
        const val BUTTONS_PADDING_END_DP = 20f
        const val BUTTONS_PADDING_BOTTOM_DP = 16f
        /** Between buttons and between wrapped rows. */
        const val BUTTON_SPACING_DP = 8f
        const val BUTTON_PADDING_H_DP = 20f
        const val BUTTON_PADDING_V_DP = 12f
        const val IMAGE_ONLY_BUTTONS_PADDING_DP = 20f
        const val IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP = 20f
        const val CLOSE_INSET_DP = 4f
        /**
         * Artwork at or above this ratio fills the card width with no bars. A shape rather
         * than a screen fraction, which is what makes the crossover device-independent: the
         * rule this replaced slid from 1.013 on a tall phone to 1.226 on a short one, so the
         * same square image was clean on one and letterboxed on the other.
         */
        const val MIN_IMAGE_RATIO = 0.55f
        /** Height always kept for copy and buttons, so artwork cannot squeeze out the CTA. */
        const val COPY_RESERVE_DP = 120f
        const val IMAGE_ONLY_HEIGHT_FRACTION = 0.65f
        const val HEADER_TEXT_SP = 22f
        const val HEADER_LINE_SP = 28f
        const val BODY_TEXT_SP = 14f
        const val BODY_LINE_SP = 20f
        const val BUTTON_TEXT_SP = 14f
    }

    object Slideup {
        const val MARGIN_DP = 12f
        const val MAX_WIDTH_DP = 480f
        const val CORNER_RADIUS_DP = 12f
        /** It has no scrim to separate it from the app, so it needs the shadow. */
        const val ELEVATION_DP = 6f
        const val CONTENT_PADDING_H_DP = 14f
        const val CONTENT_PADDING_V_DP = 12f
        /**
         * Load-bearing, and a mechanism rather than a number: bounding the container's height
         * instead truncates where a clamp grows the banner, and the two diverge at a large
         * accessibility text scale.
         */
        const val MAX_TEXT_LINES = 3
        /** Fixed square. Sizing to the artwork's own ratio would change the banner height. */
        const val ICON_SIZE_DP = 40f
        const val ICON_CORNER_RADIUS_DP = 8f
        const val ICON_SPACING_END_DP = 12f
        const val CHEVRON_SPACING_START_DP = 8f
        const val CHEVRON_SIZE_DP = 20f
        const val COPY_TEXT_SP = 14f
        const val COPY_LINE_SP = 20f
        const val DEFAULT_AUTO_DISMISS_MS = 8_000L
    }

    object Fullscreen {
        const val CONTENT_PADDING_START_DP = 24f
        const val CONTENT_PADDING_TOP_DP = 24f
        const val CONTENT_PADDING_END_DP = 24f
        const val CONTENT_PADDING_BOTTOM_DP = 0f
        /**
         * A fixed share of the available height rather than the slack the copy leaves — that
         * is what stops it letterboxing. The fit within this box is fitCenter, not centerCrop:
         * with the live 384x640 asset, cover crops 42% of the poster away, which is the offer
         * baked into the top of a promo image being lost. See spec section 19 Q1.
         */
        const val IMAGE_HEIGHT_FRACTION = 0.50f
        const val IMAGE_ONLY_BUTTONS_PADDING_H_DP = 24f
        const val IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP = 32f
        const val HEADER_TO_BODY_SPACING_DP = 12f
        const val BUTTONS_PADDING_START_DP = 24f
        const val BUTTONS_PADDING_TOP_DP = 28f
        const val BUTTONS_PADDING_END_DP = 24f
        const val BUTTONS_PADDING_BOTTOM_DP = 24f
        const val BUTTON_SPACING_DP = 12f
        const val BUTTON_PADDING_V_DP = 16f
        const val BUTTON_TEXT_SP = 16f
        const val CLOSE_PADDING_DP = 8f
        const val HEADER_TEXT_SP = 24f
        const val HEADER_LINE_SP = 32f
        const val BODY_TEXT_SP = 16f
        const val BODY_LINE_SP = 24f
    }

    object Motion {
        const val MODAL_DURATION_MS = 200L
        const val MODAL_SCALE_FROM = 0.96f
        const val FULLSCREEN_DURATION_MS = 200L
        const val SLIDEUP_DURATION_MS = 220L
    }

    fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
    ).toInt()

    fun sp(context: Context, value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics
    )
}
```

- [ ] **Step 2: Write the failing close-glyph test**

`ColorResolverTest.kt` (pure — the two-argument overload takes no `Context`):

```kotlin
    private fun contrast(a: Int, b: Int): Double {
        val la = ColorResolver.relativeLuminance(a)
        val lb = ColorResolver.relativeLuminance(b)
        val (hi, lo) = if (la > lb) la to lb else lb to la
        return (hi + 0.05) / (lo + 0.05)
    }
```

1. `a named close colour is used verbatim, readable or not` — pass `0xFFFF0000.toInt()` over a red background and assert it comes back unchanged. *(Quietly substituting something more readable is how a brand colour becomes a colour nobody chose.)*
2. `a light background gets the dark glyph` — `#FFFFFF` → `#111827`.
3. `a dark background gets the light glyph` — `#111827` → `#FFFFFF`.
4. `a saturated background picks the contrasting half` — `#F5C518` → `#111827`.
5. `no campaign colour at all falls through to the host theme` — assert the `hostOnSurface` argument is returned.
6. **`the derived pair clears 3 to 1 against every background`** — sweep the 216 web-safe colours plus the palette the live account uses, assert `contrast(derived, background) >= 3.0` for each. This is the property the whole derivation exists for.
7. `the threshold is where the two change places` — assert a colour just above 0.179 luminance picks dark and just below picks light.

- [ ] **Step 3: Run and verify it fails**

- [ ] **Step 4: Implement `ColorResolver`**

```kotlin
    /** WCAG 2.1 relative luminance. */
    fun relativeLuminance(color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel((color shr 16) and 0xFF) +
            0.7152 * channel((color shr 8) and 0xFF) +
            0.0722 * channel(color and 0xFF)
    }

    /**
     * Three cases, in order. There is no fourth, and nothing here consults the artwork.
     *
     * Deriving rather than defaulting is not a refinement: a fixed white glyph measures 1.00:1
     * against a white card, and a fixed dark glyph measures 1.00:1 against the #111827 the live
     * slideup campaign uses. Both failures are live cases, not hypotheticals.
     */
    fun closeGlyphColor(campaignCloseButton: Int?, background: Int?, hostOnSurface: Int): Int {
        // 1. The campaign asked for exactly this. Substituting something more readable is how
        //    a brand colour becomes a colour nobody chose.
        campaignCloseButton?.let { return it }
        // 2. Whichever half of the pair contrasts with the surface the glyph sits on.
        background?.let {
            return if (relativeLuminance(it) > MessageMetrics.Shared.CLOSE_GLYPH_LUMINANCE_THRESHOLD) {
                MessageMetrics.Shared.CLOSE_GLYPH_ON_LIGHT
            } else {
                MessageMetrics.Shared.CLOSE_GLYPH_ON_DARK
            }
        }
        // 3. Material already guarantees this contrasts with the surface it sits on.
        return hostOnSurface
    }
```

- [ ] **Step 5: Fold the parser's duplicate constant into the metrics file**

Task 5 introduced `private const val SLIDEUP_DEFAULT_AUTO_DISMISS_MS = 8_000L` inside
`MessageParser`. Two sources of truth for one number is exactly what this file exists to
prevent. Delete it and reference `MessageMetrics.Slideup.DEFAULT_AUTO_DISMISS_MS` instead.

Re-run `./gradlew :gameballsdk:testDebugUnitTest --tests '*MessageParserContentTest*'` and
confirm `a slideup with no duration gets the 8 second default` still passes.

- [ ] **Step 6: Add the close-button content description**

`res/values/gb_iam_strings.xml`:

```xml
<resources>
    <!-- Content description for the in-app message close button. A hard-coded "Close" would
         be the only untranslated word in the module; Gameball serves Arabic customers. -->
    <string name="gb_iam_close">Close</string>
</resources>
```

`res/values-ar/gb_iam_strings.xml` with `<string name="gb_iam_close">إغلاق</string>`.

- [ ] **Step 7: Run and verify.** Expected: PASS, 7 tests.

- [ ] **Step 8: Prove the contrast sweep can fail**

Temporarily make `closeGlyphColor` always return `CLOSE_GLYPH_ON_DARK`. Re-run. Expected: `the derived pair clears 3 to 1 against every background` FAILS on the light half of the sweep. Restore.

- [ ] **Step 9: Commit**

```bash
git commit -m "feat(iam): add layout metrics and the close-glyph resolver

Every layout constant lives in one file, the Android counterpart to Flutter's
message_view_metrics.dart, so the cross-platform parity pass is a diff rather
than a hunt through three view classes.

The close glyph is derived from the message background's relative luminance
rather than defaulting. Neither half of the pair is safe alone and both
failures are live: a fixed white glyph is 1.00:1 on a white card, and a fixed
dark glyph is 1.00:1 on the #111827 the live slideup uses. A test sweeps 216
backgrounds and asserts the derived pair clears WCAG's 3:1 for a non-text
control against every one."
```

---

## Task 17: The slideup view

Spec §15.5, §15.9. A non-blocking banner: no scrim, no buttons, no close glyph, and the app underneath stays fully usable.

**Files:**
- Create: `res/layout/gb_iam_slideup.xml`
- Create: `inappmessaging/ui/SlideupMessageView.kt`
- Test: `inappmessaging/ui/SlideupMessageViewTest.kt` (Robolectric)

**Interfaces:**
```kotlin
internal class SlideupMessageView(context: Context) : FrameLayout(context) {
    fun bind(content: MessageContent, resolved: ResolvedMessage, callbacks: PresentationCallbacks)
    /** The banner container, so the presenter can animate and inset it. */
    val banner: View
}
```

- Consumes: `MessageContent` (Task 3), `ResolvedMessage` and `PresentationCallbacks` (Task 15),
  `MessageMetrics` and `ColorResolver` (Task 16).

**Layout.** Root `FrameLayout` (match_parent) with `android:clipChildren="false"`, containing a `MaterialCardView` banner:
- gravity `top` or `bottom` per `slidePosition`; margin 12dp all sides; `maxWidth` 480dp; corner radius 12dp; elevation 6dp
- inside, a horizontal `LinearLayout` with padding 14dp horizontal, 12dp vertical
- icon `ImageView` 40×40dp, corner radius 8dp, `centerCrop`, `layout_marginEnd` 12dp
- copy `TextView`, `maxLines=3`, `ellipsize=end`, 14sp/20sp, `textAlignment=viewStart`
- chevron `ImageView` 20dp, `layout_marginStart` 8dp, `autoMirrored=true`

**Rules:**
- **Insets.** Apply `WindowInsetsCompat.Type.systemBars() or displayCutout()` as padding on the banner container at both edges. Copy under a cutout loses its first line, and a bottom banner overlapping the gesture strip swallows the swipe that is the only way to dismiss it.
- **Hit testing.** The root must not intercept touches outside the banner — the app underneath stays usable. Return `false` from the root's `onTouchEvent`; only the banner is clickable.
- **Icon failure collapses to zero width**, leaving no gap, or every broken image shifts the copy.
- **The chevron is drawn only when the campaign set a message action**, so the affordance matches the behaviour, and it flips under RTL (`autoMirrored`).
- **Swipe only toward its own edge** — a top banner swipes up, a bottom one down. Sideways fights a horizontal scroll or ViewPager underneath. Implement with a `GestureDetector` on the banner checking the sign of the Y velocity against `slidePosition`.
- **Back is not intercepted.** A non-blocking banner has no claim on the gesture.
- Everything is directional: `marginStart`/`marginEnd`, `textAlignment=viewStart`, never left/right.

- [ ] **Step 1: Write the failing test**

Tests (Robolectric, `Robolectric.buildActivity(Activity::class.java).setup()`):
1. `copy clamps to three lines and ellipsises` — assert `maxLines == 3` and `ellipsize == END`
2. `a short message stays short` — bind one word, assert the banner height is well under half the screen
3. `a failed icon collapses to zero width and leaves no gap` — assert `visibility == GONE`, not `INVISIBLE`
4. `the chevron appears only with a message action`
5. `the whole surface is the tap target when an action is set, and inert otherwise` — assert `isClickable`
6. `buttons are never rendered even if the model somehow carries them`
7. `no close glyph is drawn`
8. `taps outside the banner are not intercepted` — dispatch a touch at the screen centre with a bottom banner, assert `onTouchEvent` returns false
9. `a top banner clears the status bar inset and a bottom one clears the navigation inset` — dispatch a `WindowInsetsCompat` with known values, assert the banner's padding
10. `under RTL the icon sits trailing and the chevron flips` — set `layoutDirection = RTL`, assert `layoutDirection` propagates and the chevron is `isAutoMirrored`
11. `a bottom banner accepts a downward swipe and ignores a sideways one`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Write the layout and the view**

Key inset code:

```kotlin
        ViewCompat.setOnApplyWindowInsetsListener(banner) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            // Not cosmetic: copy under a cutout loses its first line, and a bottom banner over
            // the gesture strip swallows the swipe that is its only exit.
            view.updatePadding(top = insets.top, bottom = insets.bottom)
            windowInsets
        }
```

Hit testing:

```kotlin
    // The overlay occupies the full screen so the banner can be positioned, but it must
    // intercept nothing outside the banner's own band — the app underneath stays usable.
    override fun onTouchEvent(event: MotionEvent): Boolean = false
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 11 tests.

- [ ] **Step 5: Prove the inset test can fail**

Remove the `setOnApplyWindowInsetsListener` block. Re-run. Expected: test 9 FAILS. Restore.

Assert against the real surface the harness runs at, not an invented screen size. In Flutter the first version of this test asserted against a screen size that did not exist and passed no matter what the layout did.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): add the slideup message view

A non-blocking banner: the overlay intercepts nothing outside its own band, so
the app underneath stays fully usable. Copy clamps to three lines rather than
bounding the container's height — the two diverge at a large accessibility text
scale, and the clamp is the behaviour the other SDKs implement.

System-bar and cutout insets are applied at both edges: copy under a cutout
loses its first line, and a bottom banner over the gesture strip swallows the
swipe that is its only way out."
```

---

## Task 18: The modal view

Spec §15.5, §15.9. A centred card over a scrim that swallows every tap not on the card.

**Files:**
- Create: `res/layout/gb_iam_modal.xml`
- Create: `inappmessaging/ui/ModalMessageView.kt`
- Test: `inappmessaging/ui/ModalMessageViewTest.kt`

**Layout.** Root `FrameLayout` with a scrim background, containing a centred `MaterialCardView`:
- margin 24dp, `maxWidth` 420dp, corner radius 16dp with `cardPreventCornerOverlap=false` and clipping so the artwork's top corners round too
- vertical `LinearLayout`: artwork `ImageView`, then a `NestedScrollView` holding the copy, then the button `Flow`
- copy padding 20/20/20/0; button block padding 20/20/20/16 **outside** the scroll view

**Rules:**
- **Only the copy scrolls.** The image and buttons sit outside the scroll view, so long copy — or any copy at an accessibility text scale — can never push the call to action off the bottom. Clipping removes the buttons first, which is exactly the wrong thing to lose.
- **A short message must stay short.** Give the `NestedScrollView` `wrap_content` with a `maxHeight`, not `match_parent`. The obvious implementation of "make it scrollable" makes every card full-height; test both directions.
- **Artwork is `fitCenter`, never cropped**, capped at `min(cardWidth / 0.55, availableHeight - 120dp)`.
- **`image_only`:** artwork fills the card at `centerCrop`, capped at 65 % of screen height; buttons stretched, stacked 8dp apart, laid over the artwork with padding 20/0/20/20; text never drawn; the card takes the artwork's own ratio; tap is opaque, **not a ripple** — a highlight across the artwork reads as a glitch.
- **Buttons wrap, they do not overflow.** `ConstraintLayout`'s `Flow` with `flow_wrapMode="chain"`, aligned to the trailing edge, 8dp between buttons and between wrapped lines. Two German or Arabic labels overflowed a row by 360 px in testing; a second line is always better than a clipped button.
- **The scrim always absorbs the tap** whether or not it dismisses — its job is to block the app beneath. It dismisses only when `dismissOnScrimTap`.
- Scrim colour is `colors.frame`, else the constant `0x99000000`.
- The close glyph is a **sibling** of the tappable card body, never a child, at top 4dp / end 4dp inside the card, 24dp glyph in a 48dp target.

- [ ] **Step 1: Write the failing test**

1. `long copy scrolls and does not clip the buttons` — `@Config(qualifiers = "w320dp-h568dp")`, bind 2,000 characters, measure, assert the button `Flow` bottom is within the card and `NestedScrollView.canScrollVertically(1)`
2. `at 2x text scale the buttons are still on screen` — `@Config(fontScale = 2.0f)`
3. `a short message produces a short card` — assert the card height is under 60 % of the screen
4. `two long labels wrap to a second line rather than overflowing` — bind German labels, assert the `Flow` reports two rows and the card is not wider than its max
5. `artwork is fitCenter and never cropped in the default layout`
6. `image_only uses centerCrop and draws no text`
7. `image_only with buttons stacks them full width over the artwork`
8. `the artwork height cap is the smaller of the shape bound and the room bound`
9. `the scrim absorbs a tap even when closeBehaviour is button`
10. `the scrim dismisses only when closeBehaviour allows it`
11. `the close glyph is not a child of the tappable card body` — assert tapping it does not fire `onTapped`
12. `a campaign colour overrides and an absent one falls back to the host theme`
13. `RTL mirrors the button row and puts the close glyph in the trailing corner`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

The artwork cap:

```kotlin
    /**
     * The smaller of two bounds:
     *   byShape - the same crossover ratio on every device, which is what a screen fraction
     *             could not give: the rule this replaced slid from 1.013 on a tall phone to
     *             1.226 on a short one, so the same square image was clean on one and
     *             letterboxed on the other.
     *   byRoom  - never let artwork squeeze out the call to action.
     */
    private fun artworkHeightCap(cardWidthPx: Int, availableHeightPx: Int): Int {
        val byShape = (cardWidthPx / MessageMetrics.Modal.MIN_IMAGE_RATIO).toInt()
        val byRoom = availableHeightPx - MessageMetrics.dp(context, MessageMetrics.Modal.COPY_RESERVE_DP)
        return minOf(byShape, byRoom)
    }
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 13 tests.

- [ ] **Step 5: Prove tests 1 and 3 can fail**

Give the `NestedScrollView` `match_parent` height → test 3 FAILS (every card becomes full height). Put the buttons *inside* the scroll view → test 1 FAILS. Restore both. Testing both directions is the point: the obvious fix for one breaks the other.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): add the modal message view

Only the copy scrolls; the image and buttons sit outside the scroll view, so
long copy or a large accessibility text scale can never push the call to action
off the bottom — clipping removes the buttons first, which is the one part that
has to stay reachable.

Buttons wrap rather than sitting in a row: two German or Arabic labels
overflowed a row by 360px in testing, and a second line always beats a clipped
button.

Artwork is capped by a shape ratio rather than a screen fraction, so the point
at which bars appear is one number on every device instead of sliding between
1.013 and 1.226 with the phone."
```

---

## Task 19: The fullscreen view

Spec §15.5, §19 Q1. Edge to edge, opaque, no scrim.

**Files:**
- Create: `res/layout/gb_iam_fullscreen.xml`
- Create: `inappmessaging/ui/FullscreenMessageView.kt`
- Test: `inappmessaging/ui/FullscreenMessageViewTest.kt`

**Rules:**
- **`image_only`:** `centerCrop` over the **full bounds, ignoring the safe area** — it runs under the notch and the home indicator, which is the point of a full-bleed poster. Buttons anchored bottom-centre **inside** the safe area, padding 24/0/24/32, stretched full width, stacked 12dp apart. Header and body are **not drawn**, even when the campaign carries them.
- **`text_with_image`:** the whole stack inside the safe area. The image gets a **fixed 50 % of available height** and is drawn **`fitCenter`** within that box. Copy takes the rest and scrolls inside it; buttons sit outside the scroll view.
- Header and body default to **centre** here and to **start** on a modal, both overridable per campaign.
- Type scale is larger than a modal's: header 24/32 w700, body 16/24, buttons 16 w600, header→body 12dp, buttons above 28dp.
- Close glyph topEnd, padding 8dp, **inside** the safe area, since the surface goes edge to edge.
- **Orientation is enforced here and only here.** A mismatch is refused so the service can defer and retry on rotation. Enforcing it for a banner or a card would suppress messages for no benefit.

> **The scale type is a decided synthesis, not an oversight.** The port guide's defect 9 prescribes `fitCenter` where artwork shares the screen with copy; the UI spec specifies `cover` at a fixed 50 %. They settle two different properties — the box and the fill — so both hold. With the live 384×640 asset in a 390×375.5 box, `centerCrop` crops 42 % of the poster away, which is precisely defect 9's finding. Spec §19 Q1 has the full argument; put a short version in the code comment.

- [ ] **Step 1: Write the failing test**

1. `image_only fills the bounds and ignores the safe area`
2. `image_only draws no header or body even when the campaign supplies them`
3. `image_only anchors buttons inside the safe area`
4. **`text_with_image gives the image a fixed half of the available height`**
5. **`text_with_image draws the image fitCenter so nothing is cropped`** — assert `scaleType == FIT_CENTER`, with a comment naming defect 9
6. `copy scrolls within its remainder and never pushes the buttons off`
7. `header and body default to centre`
8. `textAlignment overrides the default per slot`
9. `buttons stack full width, 12dp apart, in payload order`
10. `the close glyph sits inside the safe area at the trailing corner`
11. `a portrait-only campaign is refused in landscape` — assert `bind` returns false or the presenter's orientation check refuses
12. `a campaign with no orientation is accepted in both`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement.** Include the comment:

```kotlin
        // fitCenter, not centerCrop: this artwork shares the screen with copy, and cropping
        // loses whatever the designer baked into the top of the poster. With the live 384x640
        // asset in this box that is 42% of the image. centerCrop is correct only for
        // image_only, where bleeding to every edge is the point. See spec 19 Q1.
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
```

- [ ] **Step 4: Run and verify.** Expected: PASS, 12 tests.

- [ ] **Step 5: Prove test 5 can fail.** Switch to `CENTER_CROP`; re-run; restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): add the fullscreen message view

image_only is centerCrop over the full bounds ignoring the safe area, which is
the point of a full-bleed poster, with buttons anchored inside it.

text_with_image gives the image a fixed half of the available height and draws
it fitCenter. The fixed share is the UI spec's rule and stops the image
letterboxing into whatever slack the copy leaves; fitCenter is defect 9's rule
and stops the crop that loses an offer baked into the top of a promo image.
With the live asset, centerCrop in that box would discard 42% of the poster.

Orientation is enforced here and only here: a mismatch is refused so the
service can defer and retry on rotation."
```

---

## Task 20: The overlay presenter

Spec §8.1, §8.3, §8.5, §8.6, §15.6. Where the module meets the host's window.

**Files:**
- Create: `inappmessaging/ui/OverlayPresenter.kt`
- Test: `inappmessaging/ui/OverlayPresenterTest.kt`

**Interfaces:**
- Consumes: `MessagePresenter`, `PresentationCallbacks`, `ResolvedMessage` and
  `PendingPresentation` (all defined in Task 15), `ActivityTracker` (Task 14), the three views
  (Tasks 17-19), `MessageMetrics.Motion` (Task 16).
- Produces: `OverlayPresenter(activityTracker: ActivityTracker) : MessagePresenter` — the only
  implementation of the seam. The service is constructed with it in Task 21.

**Rules:**
- Attach to `activity.findViewById<ViewGroup>(android.R.id.content)`. **No `SYSTEM_ALERT_WINDOW`** — drawing over other apps needs a permission we must not ask an integrator's users for.
- **Resolve the Activity at presentation time; never cache it.** Binding to the first surface handle meant that after a hot restart the presenter pointed at a dead surface and messages silently never appeared. On Android rotation recreates the Activity by default, so it is worse here.
- **The impression fires on the first pre-draw pass, not at attach:**
  ```kotlin
  view.doOnPreDraw {
      callbacks.onShown()          // record the cap, log the impression,
      startAutoDismissTimer()      // and start the timer HERE
  }
  ```
  Insertion only schedules a frame. Counting at insert time books a view of something the customer may never see; if the app is backgrounded in that instant the callback correctly never runs.
- **Rotation re-presents without re-reporting.** The "impression already reported" flag lives on the `PendingPresentation`, not on the view, and the auto-dismiss timer continues measuring from the original impression rather than restarting.
- **Back:** modal and fullscreen consume it via `OnBackPressedCallback` on `activity.onBackPressedDispatcher` when the Activity is a `ComponentActivity`, falling back to a focusable root with an `OnKeyListener` for `KEYCODE_BACK`. Removed on dismissal. The dispatcher path is preferred because the key-listener fallback stops firing under predictive back. **A slideup never registers one.**
- **Motion:** modal fade + scale 0.96→1.0 over 200 ms `easeOutCubic`; fullscreen fade only over 200 ms; slideup slides from its own edge over 220 ms. No exit animation except a swiped slideup. **Reduce motion drops the duration to zero rather than shortening it** — `Settings.Global.getFloat(resolver, ANIMATOR_DURATION_SCALE, 1f) == 0f`.
- Dismissal accounting: `shown` and `engaged` flags per presentation; report `dismiss` only when `shown && !engaged`.

- [ ] **Step 1: Write the failing test**

1. `present returns false when there is no Activity`
2. `present does not throw when there is no Activity`
3. `the Activity is resolved at presentation time, not cached` — present, recreate the Activity, present again, assert the second attaches to the new one
4. **`the impression is reported on the first pre-draw pass, not at attach`** — assert `onShown` is not called immediately after `present` returns, then drive a layout pass and assert it is
5. **`a message dismissed before it paints reports no impression and no dismissal`**
6. `the auto-dismiss timer starts at the impression, not at insertion`
7. **`re-presenting after rotation does not report a second impression`**
8. `the auto-dismiss timer continues from the original impression after rotation`
9. `dismiss is reported only when shown and not engaged`
10. `a button tap reports a click with the buttonId and a surface tap reports one without`
11. `back dismisses a modal without popping the host route`
12. `back dismisses a fullscreen`
13. `a slideup does not register a back callback`
14. `reduce motion skips the entry animation entirely` — set `ANIMATOR_DURATION_SCALE` to 0, assert duration is 0, not merely shorter
15. `dismissCurrent removes the view and clears the back callback`
16. `no Activity leak — the reference is weak and cleared on pause`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

- [ ] **Step 4: Run and verify.** Expected: PASS, 16 tests.

- [ ] **Step 5: Prove tests 4 and 7 can fail**

Call `callbacks.onShown()` directly in `present` instead of inside `doOnPreDraw` → tests 4 and 5 FAIL. Restore.
Move the `impressionReported` flag onto the view → test 7 FAILS. Restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): present messages over the host's content root

Attaches to android.R.id.content, which needs no permission, and resolves the
Activity at presentation time rather than caching it — a cached handle points
at a dead surface after every rotation, and messages then silently never
appear.

The impression fires on the first pre-draw pass rather than at attach.
Insertion only schedules a frame, so counting there books a view of something
the customer may never see, and it is what makes impressions = clicks +
dismissals hold. The auto-dismiss timer starts on the same frame, so a
configured duration measures visible time.

Rotation re-presents without a second impression: the flag lives on the pending
presentation, not on the view, which is a decision only the Android port has to
make — an overlay entry and a UIWindow both survive rotation."
```

---

## Task 21: The public API and host wiring

Spec §14, §14.1, §14.2. The last task that touches existing code.

**Files:**
- Create: `inappmessaging/GameballInAppMessaging.kt`, `inappmessaging/InAppMessagingOptions.kt`, `inappmessaging/model/InAppMessage.kt`
- Modify: `GameballApp.kt` — four entry points and three wiring call-sites
- Test: `inappmessaging/PublicApiTest.kt`

**Interfaces:**
```kotlin
// model/InAppMessage.kt — public mirrors, because Kotlin forbids internal types in public
// signatures. Deliberately minimal: what a host hook actually needs.
data class InAppMessage(
    val campaignId: Int, val variationId: Int?, val name: String?,
    val messageType: Int, val header: String?, val body: String?,
    val buttons: List<InAppMessageButton>, val isTest: Boolean
)
data class InAppMessageButton(val id: String, val text: String)

sealed class GameballMessageAction {
    object Dismiss : GameballMessageAction()
    data class OpenUrl(val url: String, val external: Boolean) : GameballMessageAction()
    data class Navigate(val route: String, val arguments: Map<String, Any?>?) : GameballMessageAction()
}

enum class DisplayDecision { SHOW, LATER, DISCARD }

// The four hooks — interfaces, not function types, so Java callers get clean anonymous
// classes and Kotlin callers get SAM conversion.
interface BeforeDisplayHook { fun beforeDisplay(message: InAppMessage): DisplayDecision }
interface MessageActionHook {
    /** True means the host handled it and the SDK does nothing further. */
    fun onAction(message: InAppMessage, button: InAppMessageButton?, action: GameballMessageAction): Boolean
}
interface MessageNavigationHook { fun onNavigate(route: String, arguments: Map<String, Any?>?) }
interface MessageObserver { fun onMessageSelected(message: InAppMessage) }

class InAppMessagingOptions private constructor(...) {
    class Builder {
        fun sessionTimeoutSeconds(seconds: Int): Builder
        fun beforeDisplay(hook: BeforeDisplayHook?): Builder
        fun onAction(hook: MessageActionHook?): Builder
        fun onNavigate(hook: MessageNavigationHook?): Builder
        fun observer(observer: MessageObserver?): Builder
        fun build(): InAppMessagingOptions
    }
    companion object { @JvmStatic fun builder(): Builder }
}
```

On `GameballApp`:

```kotlin
    @JvmOverloads
    fun startInAppMessaging(customerId: String, options: InAppMessagingOptions? = null)
    fun stopInAppMessaging()
    fun isInAppMessagingStarted(): Boolean
    @JvmOverloads
    fun logPurchase(
        productId: String, price: Double, currency: String, quantity: Int,
        properties: Map<String, Any?>? = null
    )
```

**Rules:**
- **`start` is idempotent for the same customer** — a second call logs "already running" and returns. A *different* customer refetches, resets caps, and discards the previous customer's cache and stored values, so the host need not stop first.
- **`stop`** dismisses → clears campaigns, caps, pending slot, artwork state, quiet hours → clears personalisation values including storage → flushes telemetry → disposes the scheduler → unregisters lifecycle callbacks. Flush **before** dispose.
- **Nothing is constructed until `start`.** `GameballInAppMessaging` holds a nullable service; `init` must not touch it.
- **`initializeCustomer`** notifies the module of a customer change, in **its own try/catch, outside the existing Rx chain** — a throw inside an addition to someone else's chain escapes into their code path.
- **`sendEvent`** feeds the trigger engine with the event name **and its metadata map**. Omitting metadata was defect 7: filters fully unit-tested and completely unreachable.
- **`showProfile` / `hideProfile`** publish the widget-open signal; `hideProfile` is a retry trigger.
- **`logPurchase` routes through `sendEvent` and does not also notify the service.** Defect 3 was one purchase firing the trigger engine twice, the duplicate taking the pending slot and displacing whatever was legitimately waiting. Exactly one occurrence per call.
- Every hook is wrapped in its own try/catch that logs and swallows.

`GameballApp` additions look like:

```kotlin
    /**
     * Starts in-app messaging for [customerId]. Until this is called the module does nothing
     * at all: no requests, no timers, no storage writes, nothing drawn, and no Activity
     * lifecycle callbacks registered.
     */
    @JvmOverloads
    fun startInAppMessaging(customerId: String, options: InAppMessagingOptions? = null) {
        try {
            inAppMessaging.start(mContext, customerId, options ?: InAppMessagingOptions.builder().build())
        } catch (t: Throwable) {
            // Messaging must never take the host down.
            Log.e(TAG, "in-app messaging failed to start", t)
        }
    }
```

and inside the existing `sendEvent`, after the Rx call is dispatched:

```kotlin
        // Feed the trigger engine with the name AND the metadata. Passing only the name is
        // what made every metadata-filtered campaign unmatchable in Flutter while all its
        // unit tests passed.
        try {
            event.events.forEach { (name, metadata) -> inAppMessaging.onEvent(name, metadata) }
        } catch (t: Throwable) {
            Log.e(TAG, "in-app messaging event hook failed", t)
        }
```

- [ ] **Step 1: Write the failing test**

1. `start is idempotent for the same customer`
2. `starting with a different customer refetches and resets caps`
3. `starting with a different customer discards the previous customer's stored values`
4. `stop dismisses, flushes, then disposes, in that order`
5. `isInAppMessagingStarted reflects start and stop`
6. **`stop then start logs an event and a flush is scheduled`**
7. `sendEvent passes the event name and its metadata to the trigger engine`
8. **`one logPurchase produces exactly one trigger evaluation`**
9. `showProfile marks the widget open and hideProfile retries the pending slot`
10. `a throwing beforeDisplay hook is contained and defaults to show`
11. `a throwing onAction hook is contained and falls back to built-in handling`
12. `a throwing onNavigate hook is contained`
13. `the observer receives every selected message, including one later discarded by a hook`
14. `the click is still reported when onAction returns true`
15. `an unknown route logs and continues rather than throwing`

- [ ] **Step 2: Run and verify it fails**

- [ ] **Step 3: Implement**

- [ ] **Step 4: Run and verify.** Expected: PASS, 15 tests.

- [ ] **Step 5: Prove tests 7 and 8 can fail**

Change the `sendEvent` wiring to pass only the name → test 7 FAILS (defect 7). Restore.
Make `logPurchase` both call `sendEvent` and `inAppMessaging.onEvent("purchase", …)` → test 8 FAILS with two evaluations (defect 3). Restore.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(iam): expose the public API and wire it into GameballApp

Four entry points on GameballApp, keeping the @JvmStatic/@JvmOverloads Java
interop the SDK already guarantees, with the four host hooks as interfaces so
Java callers get clean anonymous classes.

sendEvent feeds the trigger engine with the event name and its metadata map.
Passing only the name is what made every metadata-filtered campaign unmatchable
in Flutter while all of its unit tests passed.

logPurchase routes through sendEvent and does not also notify the service, so
one purchase produces exactly one trigger occurrence. Doing both meant the
duplicate took the pending slot and displaced whatever was legitimately waiting.

Every hook is wrapped in its own try/catch outside the existing Rx chain: a
buggy host loses its override, not its messages, and a throw in an addition to
someone else's chain must not escape into their code path."
```

---

## Task 22: End to end, the compatibility invariant, and docs

Spec §14.3, §16, §17 step 12. The test that catches wiring mistakes every unit test passes.

**Files:**
- Test: `inappmessaging/EndToEndTest.kt`, `inappmessaging/CompatibilityInvariantTest.kt`
- Modify: `README.md`, `CHANGELOG.md`, `MIGRATION.md`, `gameballsdk/build.gradle` (version bump)

- [ ] **Step 1: Write the compatibility invariant test**

This is the test that protects every existing integration. Robolectric, with a `MockWebServer`-backed OkHttp client injected into `Network`, or a counting interceptor.

```kotlin
    /**
     * An integrator who upgrades the SDK and does not opt in must see byte-identical
     * behaviour. This asserts the whole of that promise in one place.
     */
    @Test
    fun `nothing happens before startInAppMessaging is called`() {
        val app = GameballApp.getInstance(context)
        app.init(GameballConfig.builder().apiKey("k").lang("en").build())
        app.initializeCustomer(request, callback)
        app.sendEvent(event, callback)

        assertEquals(0, iamRequestCount)                       // no requests to inapp-messages
        assertNull(prefs.getIamCampaignCache())                // no storage writes
        assertNull(prefs.getIamDisplayHistory())
        assertNull(prefs.getIamOutbox())
        assertNull(prefs.getIamVariables())
        assertFalse(app.isInAppMessagingStarted())
        assertEquals(0, registeredLifecycleCallbackCount)      // no callbacks registered
        assertEquals(0, shadowOf(activity).contentView.childCountAddedByIam())
    }

    @Test
    fun `the widget still works with in-app messaging never started`() { /* showProfile / hideProfile */ }

    @Test
    fun `stop leaves nothing registered or scheduled`() { /* start, stop, then re-assert the above */ }
```

- [ ] **Step 2: Write the end-to-end test**

Drives the module through the **public API only**, against a stubbed `MessageSource`.

1. `a session-start campaign renders, a tap dismisses it, and analytics are reported` — assert an `impression` and a `click` reached the recording analytics with the right `campaignId` and `eventUid`s
2. `an event with metadata selects a filtered campaign and renders it` — the defect 7 catcher, end to end
3. `a purchase selects a purchase-triggered campaign and price filters work`
4. `a personalised campaign renders substituted copy and never a raw brace`
5. `an isTest campaign renders and reports nothing`
6. `the whole flow works with the live payload fixture as the source`

- [ ] **Step 3: Run the whole suite**

Run: `./gradlew :gameballsdk:testDebugUnitTest`
Expected: PASS, everything.

- [ ] **Step 4: Bump the version**

`gameballsdk/build.gradle`: `SDK_VERSION` `"3.2.1"` → `"3.3.0"` — a minor bump, since this is additive and backward compatible.

- [ ] **Step 5: Update the docs**

`README.md`: add an **In-App Messaging** section after the widget section. Lead with the fact that it is opt-in and that existing integrations need no changes. Cover `startInAppMessaging` / `stopInAppMessaging` / `isInAppMessagingStarted` / `logPurchase`, the four hooks, a Kotlin and a Java snippet, and the note that campaigns are configured in the dashboard. Add 🔔 **In-App Messaging** to the feature list and bump the version badge.

`CHANGELOG.md`: a `## 3.3.0` entry following the existing format — Added (the module and its API), Fixed (the `HeaderInterceptor` v4.0 pin, which is a real bug fix for anyone who would have used IAM), and a note that no existing API changed.

`MIGRATION.md`: a short "3.2.1 → 3.3.0" section saying no migration is required, with the opt-in snippet for anyone who wants the new module.

- [ ] **Step 6: Verify the docs claim is true**

Run: `git diff master --stat -- gameballsdk/src/main/java/com/gameball/gameball --diff-filter=M`
Expected: exactly three modified files — `GameballApp.kt`, `network/Config.kt`, `network/interceptor/HeaderInterceptor.java`, plus `local/SharedPreferencesUtils.kt`. Confirm every change in them is additive. If anything else is modified, either justify it in the changelog or revert it.

- [ ] **Step 7: Device QA against the alpha account**

Not automated. Work through spec §18:
- Add debug buttons for `view_product_page`, `place_order` and `logPurchase` — **no storefront action fires a configured trigger**, so without these no event-triggered campaign is reachable at all.
- Capture the sync payload at the start of every session and compare against *that*, not against any document. The account is shared and live: `cooldownSeconds` changed from 60 to 10 mid-run while the guide was being written.
- Verify: rotation re-presents without a second impression · no Activity leak · slideup insets at both edges · reduce motion · 2× font scale · RTL with the device set to Arabic · the digit-corruption guard with an Arabic locale.
- Timezone switching is the ideal negative control for quiet hours: the window is UTC, so changing the device timezone must change **nothing**. If suppression follows the device clock, the implementation is reading local time.

```bash
adb shell input keyevent KEYCODE_HOME                      # background
adb shell am force-stop com.your.host                      # cold start
adb shell pm clear com.your.host                           # clear caps + cache
adb logcat -s GameballIAM
adb shell settings put global animator_duration_scale 0    # reduce-motion path
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(iam): end-to-end coverage, compatibility guard and docs for 3.3.0

The end-to-end test drives the module through the public API only, which is the
test that catches wiring mistakes every unit test passes — it is what would have
caught the Flutter defect where metadata filters were fully unit-tested and
completely unreachable.

The compatibility test asserts the whole backward-compatibility promise in one
place: before startInAppMessaging there are no requests, no storage writes,
nothing drawn and no lifecycle callbacks registered, so an integrator who
upgrades without opting in sees byte-identical behaviour.

Bumps to 3.3.0 — additive and backward compatible."
```

---

## Coverage against the spec

| Spec section | Task |
|---|---|
| §3 D1 package placement | file structure, all tasks |
| §3 D2 the L1 guard | 1 |
| §3 D3 Kotlin / coroutines / internal | all |
| §3 D4 no `java.time` | 2, and the global constraints |
| §3 D5 XML views | 17, 18, 19 |
| §3 D6 five interfaces | 10, 11, 12, 13, 20 |
| §3 D7 no new runtime dependencies | 1 |
| §4.1 package layout | file structure |
| §4.2 dependency rule | 21 |
| §4.3 threading | 15 |
| §4.4 service boundary | 8, 15 |
| §5 wire contract, §5.1 platform 2, §5.2 sync | 10 |
| §5.3 events | 11 |
| §5.4 variables | 12 |
| §6 model and parser | 3, 4, 5, 6 |
| §6.8 filters | 6 (parse), 7 (evaluate) |
| §6.9 colour, §6.10 alignment, §6.11 extras | 2, 5 |
| §7 selection, §7.2 quiet hours | 2 (window), 8 (selection) |
| §8.1–8.3, 8.5–8.7 display | 20 |
| §8.4 dismissal accounting | 20 |
| §8.8 deferral vs suppression | 15 |
| §8.9 orientation | 19, 15 |
| §9 session lifecycle | 14, 15 |
| §10 artwork | 13 |
| §11 analytics outbox | 11 |
| §12 personalisation, §12.1 PII | 12 |
| §13 persistence | 9 |
| §14 public API, §14.1 hooks, §14.2 wiring | 21 |
| §14.3 compatibility invariant | 22 |
| §14.4 test seams | 15, 21 |
| §15.1 constants, §15.2 type, §15.4 close glyph | 16 |
| §15.3 colour model | 5, 16 |
| §15.5 per-type composition | 17, 18, 19 |
| §15.6 motion | 20 |
| §15.7 dismissal | 5 (parse), 18, 20 |
| §15.8 degraded states | 5, 13, 17, 18, 19 |
| §15.9 layout resilience, a11y | 17, 18, 19 |
| §16 testing | every task |
| §17 build order | task order |
| §18 test account | 6, 22 |
| §19 Q1 fullscreen fit | 16, 19 |
| §20 the eleven defects | 1, 5, 8, 11, 12, 13, 14, 15, 20, 21 |
