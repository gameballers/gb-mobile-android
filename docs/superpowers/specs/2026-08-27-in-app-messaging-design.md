# In-App Messaging (IAM) module — Android SDK design

- **Status:** approved for implementation
- **Date:** 2026-08-27
- **Target:** `gameballers/gb-mobile-android` @ `master` (SDK 3.2.1)
- **Sources:** *Android IAM Port Guide* (25 Aug 2026) and *In-App Message UI Spec*
- **Reference impls:** `gameball-flutter @ 06164f7` (569 tests), `gameball-ios @ e4e9cc0`

---

## 1. Goal

Add in-app messaging to the Android SDK: fetch campaigns for a customer, choose at most
one in response to a trigger, draw it above the host's UI, and report impressions, clicks
and dismissals.

The feature must be **opt-in and runtime-independent**. Three client shapes must all work:

| Client | Expectation |
|---|---|
| Widget only | Upgrades the SDK version and changes nothing. Zero IAM code executes. |
| IAM only | `init` → `initializeCustomer` → `startInAppMessaging`. Never calls `showProfile`. |
| Both | Independent. Triggers fire regardless of widget state; messages render on top of the widget when its Activity is the resumed one. |

## 2. Non-goals

- Message types 4 (`htmlFullscreen`) and 5 (`emailCapture`) — parsed, marked unsupported,
  filtered at selection.
- Actions `log_event`, `log_attribute`, `request_push_permission` — parsed, unsupported.
- `content.font` — no SDK consumes it; not implemented without a contract.
- Cross-platform visual parity. Deliberately scheduled after all platforms ship. This
  design makes it a one-file diff (§13.1) rather than an audit.
- Raising `minSdk` above 21.

---

## 3. Decisions

### D1 — Isolated package inside `:gameballsdk`, not a new Gradle module

The SDK ships as a single JitPack artifact (`com.github.gameballers:gb-mobile-android`).
There is no `jitpack.yml`, so JitPack runs on defaults against a `settings.gradle` with one
subproject, which is why the single-artifact coordinate resolves. A second subproject moves
the build into JitPack's multi-module convention
(`com.github.gameballers.gb-mobile-android:<module>:<tag>`) and risks forcing every existing
client to rewrite their dependency line — a direct violation of the backward-compatibility
requirement.

The decoupling actually required is a *runtime* property, delivered by the opt-in gate in
§14, not by a build boundary. Kotlin `internal` hides the module from clients. The residual
gap — `internal` does not stop IAM from importing widget code — is handled by the one-way
dependency rule in §4.2 and code review.

### D2 — Disarm L1 by exempting the path in `HeaderInterceptor`

`HeaderInterceptor.java:41-49` rewrites any path containing `api/v4.0/` to `api/v4.1/`
whenever a session token is stored. IAM lives at `api/v4.0/integrations/inapp-messages/…`,
and v4.1 answers **401** to APIKey auth. Left alone, messaging works in testing and is dead
in production for every integration that sets a token.

Fix:

```java
if (path.contains(Config.API_V4_0) && !path.contains(IAM_PATH_SEGMENT)) { … }
```

The change is strictly subtractive: the only affected requests are those whose path contains
both `api/v4.0/` and `inapp-messages`. `Config.kt` defines `SendEvent`, `GetBotSettings`,
`InitializeCustomer` and `MobileLogs` — none contains `inapp-messages` — so that set is
empty today and every existing request evaluates the condition identically.

Rejected alternative: a dedicated OkHttp client. It duplicates `APIKey` / `Lang` /
`x-gb-agent` / `X-GB-TOKEN` logic that then drifts silently, re-implements `apiPrefix`
resolution (the exact bug iOS shipped and fixed in `e4e9cc0`, L2), and costs every opting-in
host a second connection pool.

### D3 — Kotlin, coroutines, `internal` visibility

Matches the direction of the codebase. `internal` is the enforcement mechanism for D1;
coroutines are what the guide prescribes for this state machine; Retrofit 2.9 takes `suspend`
functions with no new call adapter. Sealed classes make the `when` over action types
exhaustive.

Java interop is an existing guarantee: public entry points keep `@JvmStatic` / `@JvmOverloads`,
and the four host hooks are interfaces, not Kotlin function types.

### D4 — No `java.time`

`minSdk 21` with no core library desugaring in `gameballsdk/build.gradle`. All time is
`System.currentTimeMillis()`; all formatting is `SimpleDateFormat(pattern, Locale.US)` with
`timeZone = UTC`. Quiet-hours `HH:mm` is parsed by splitting on `:` and `toIntOrNull()`,
never by a date formatter. **L3:** on an Arabic-locale device, a locale-sensitive formatter
emits Arabic-Indic digits, which is not valid ISO-8601 and 400s the whole analytics batch.

### D5 — XML layouts, not Compose

Three views do not justify pushing the Compose runtime onto every integrator of a minSdk-21
library. Layouts live in `res/layout/` with a `gb_iam_` prefix on every resource name so a
host app's own `modal_message.xml` cannot collide. Colours arrive on the wire, so theming is
applied programmatically at bind time.

### D6 — Five interfaces, not eleven

The port guide names eleven seams. Only five need to be interfaces, because only five need a
fake in tests: `MessageSource`, `ArtworkPrefetcher`, `MessagePresenter`, `MessageAnalytics`,
`VariableSource`. The pure pieces — parser, selector, personalisation — are concrete classes
called directly with plain data. Wrapping a pure function in an interface adds indirection
and no testability.

### D7 — No new runtime dependencies

Coroutines 1.9.0, Picasso 2.71828, Retrofit 2.9.0, OkHttp 4.9.2, ConstraintLayout 2.2.1,
Material 1.12.0 and core-ktx 1.8.0 are all present. `lifecycle-process` is **not** added;
foreground/background detection is implemented by counting started Activities (§9.1), which
is what `ProcessLifecycleOwner` does internally.

---

## 4. Architecture

### 4.1 Package layout

```
com.gameball.gameball.inappmessaging/
├── GameballInAppMessaging.kt      public  entry points + hook registration
├── InAppMessagingOptions.kt       public  session timeout, navigator, hooks
├── model/                         public  what host hooks receive
│   InAppMessage · MessageButton · MessageAction · DisplayDecision
├── domain/                        internal — pure, no Android, no I/O
│   Campaign · Trigger · MetadataFilter · QuietHours · MessageContent
│   TriggerOccurrence · DisplayRecord
│   MessageSelector          selection as a pure function, clock injected
│   Personalization          token substitution + the blanking pass
├── data/                          internal
│   MessageParser            hand-walked JsonObject; never throws
│   IamApi                   Retrofit, suspend functions
│   RemoteMessageSource · RemoteVariableSource
│   IamStore                 SharedPreferences wrapper, per-customer scoped
│   DisplayHistory · CampaignCache · VariableStore · AnalyticsOutbox
├── artwork/                       internal
│   ArtworkPrefetcher (iface) · PicassoArtworkPrefetcher
├── runtime/                       internal
│   InAppMessagingService    sequencing, deferral, the pending slot
│   ActivityTracker          weak Activity ref + fg/bg counting
│   IamLog · Clock
└── ui/                            internal
    MessageMetrics           every constant from the UI spec, one file
    ColorResolver            colour parsing + close-glyph derivation
    OverlayPresenter         implements MessagePresenter
    SlideupMessageView · ModalMessageView · FullscreenMessageView
```

### 4.2 Dependency rule

IAM may depend on `network`, `local`, `utils`. **Nothing outside `inappmessaging.*` may
import it**, with one exception: `GameballApp` calls the public facade. IAM does not
observe widget lifecycle — the two features are independent and can render on the same
screen at the same time.

### 4.3 Threading

- One `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` owned by the module,
  cancelled in `stopInAppMessaging()`.
- Views on Main; parse, storage and network under `withContext(Dispatchers.IO)`.
- `SupervisorJob` so a hung artwork fetch cannot cancel the session.
- Never block Main on storage — the first `getSharedPreferences` touches disk.

### 4.4 Service boundary

`InAppMessagingService` owns **no display policy**. Selection is entirely a pure function in
`MessageSelector`; the service sequences, defers and holds the pending slot. Keeping that
boundary is what lets every rule in §7 be tested with plain data and no mocking.

---

## 5. Wire contract

Auth is the `APIKey` header. Identity is `customerId` **in the body** — there is no player
token on this surface. Base URL resolves through the existing `GameballConfig.apiPrefix`
plumbing (**L2**).

| Endpoint | When | Body |
|---|---|---|
| `POST api/v4.0/integrations/inapp-messages/sync` | once per session start | `{customerId, platform, locale, appVersion, sdkVersion}` |
| `POST api/v4.0/integrations/inapp-messages/events` | batched flushes | `{customerId, platform, events[]}` |
| `POST api/v4.0/integrations/inapp-messages/variables` | before displaying a message with tokens | `{customerId}` |

Endpoint constants go in `Config.kt`. IAM gets its own Kotlin `IamApi` rather than extending
the Java `GameBallApi`.

### 5.1 `platform` must be `2`

`1` = iOS, `2` = Android. Optional in the schema and load-bearing in fact: omit it or send
anything else and the backend returns **200 with an empty message list** — indistinguishable
from an account with no campaigns. Log loudly before sending anything other than 1 or 2.

`locale` resolves device locale → player preferred language → `en` → any available. Wrong
value silently returns the wrong translation rather than an error.

### 5.2 Sync response

Plain payload, no envelope, no success flag. Ignore unknown root keys. Root fields:
`cooldownSeconds` (default 30), `quietHours {enabled, start, end}`, `messages[]`.
`campaignOrdering` is **ignored** — see §7.3.

Failures: 400 missing customerId · 401 bad key · 404 with an `ErrorResponse` body = unknown
customer · 404 with no body = endpoint not deployed on that environment (log these
differently) · 422 `PlayerInactive` · 503 retry. Every non-2xx means "could not ask", which
is not "no campaigns" (§9.2). A successful response **replaces** the cache entirely; it is
never merged. Sync must never block app startup.

### 5.3 Events

Fields: `eventUid`, `dispatchId?`, `campaignId`, `variationId?`, `type`, `occurredAt`,
`buttonId?`, `url?`. Omit nullable fields when null.

`eventUid` **must be a real UUID v4** (`java.util.UUID.randomUUID().toString()`). A non-GUID
is a hard 400 that discards the entire batch. Never a timestamp, counter or hash. The shape
is asserted in a test.

`occurredAt` is when it happened on device, never when sent — UTC, `Z` suffix, millisecond
precision.

| Status | Action |
|---|---|
| 2xx (202 in practice) | accept, clear the batch. Body `{accepted, rejected}`; log non-zero `rejected`, never retry those |
| 400 · 401 · 404 · 422 | discard permanently, log loudly |
| 408 · 429 · 5xx · network error · timeout | retry, keep the batch |

**Defect 4:** the endpoint answers **202**. Narrowing success to exactly 200 reports every
accepted event as a failure. Accept the whole 2xx range. The existing `sendEvent` is checked
for the same bug.

### 5.4 Variables

Response is `{variables: {name: "value", …}}` — a flat map of **pre-formatted** strings
(thousand separators already applied). Insert verbatim; never re-parse or re-format. Any
failure — 404, 422, 503, timeout, no network — yields an empty map, never an exception.

Nine keys, four of which are PII (`player_email`, `player_name`, `player_last_name`,
`player_display_name`). They arrive whether or not a campaign mentions them, which is why
§12.1 persists only the tokens the held campaigns actually use.

### 5.5 Superseded document

`docs/integration/in-app-message-analytics-backend-handoff.md` in the Flutter repo describes
an analytics contract that was **proposed and never shipped**. Every field differs. The
authority is `backend-sdk-endpoints-reference.md`. Not followed here.

---

## 6. Data model and parser

`MessageParser` walks a `JsonObject` by hand. **It must never throw** — a malformed payload
returns an empty result and a log line. Gson's reflective binding is deliberately not used:
the parser's job is a list of asymmetric leniency rules, and reflection yields nulls and
exceptions instead of decisions (**L5**).

### 6.1 Campaign fields

| Field | Default | Rule |
|---|---|---|
| `campaignId` int | required | missing → drop campaign |
| `variationId` int? | null | A/B arm; report, never key on it |
| `dispatchId` string? | null | opaque; echo verbatim |
| `name` string? | null | logs only, never logic |
| `priority` int | 0 | higher wins |
| `messageType` int | required | 1 slideup · 2 modal · 3 fullscreen · 4/5 → keep, mark unsupported |
| `contentMode` | `prerendered` | anything else → drop campaign |
| `expiresAt` ISO? | null | never display at or after; checked at **selection** |
| `isTest` bool | false | displays normally, reports nothing |
| `trigger.repeatable` | false | false = once ever, enforced on device |
| `trigger.minIntervalSeconds` | null | only meaningful when repeatable; 0/null → every occurrence |

### 6.2 Message assembly

`content` carries untranslated styling and behaviour; `locale` carries translated text.
Buttons appear in both and are paired by **string `id`** — render only ids present in both
halves; drop the rest (styled-without-text has no label, translated-without-style has no
action).

- `body` ← `locale.message`, else `locale.body`. For a **slideup** the copy source is
  `locale.message` falling back to `locale.header`.
- `showCloseButton` ← `closeBehaviour` contains `button` (default true)
- `dismissOnScrimTap` ← `closeBehaviour` contains `swipe` (default true)
- `clickAction` ← `content.action`. Null means the surface is **inert** — never default to dismiss.
- `slidePosition` ← `slideFrom`, default `bottom`. Slideup only.
- `orientation` default `any`; enforced by fullscreen only.
- Modal max 2 buttons — keep the first two and log the count; never drop the campaign.
- Slideup takes none — drop them with a log naming how many.
- Fullscreen has no cap; buttons stack full-width.
- Every colour is optional and falls back to the **host theme**, never a literal.

### 6.3 Drop rules

No header, no body and no image → drop. A slideup additionally requires text.

### 6.4 Artwork resolution

- fullscreen → `content.media.url` first, then `content.imageUrl`
- everything else → `content.imageUrl` first, then `content.media.url`
- `media` is `{type, url, autoplay, muted}`; use `url` only when `type` is `image` or absent.
  Log and ignore `video`.
- Normalise blank strings to null — an empty URL otherwise reaches the loader, fails, and
  takes the whole campaign with it silently.

The live QA campaign puts its image under `media` and leaves `imageUrl` null. A parser
reading only `imageUrl` renders nothing and looks like a backend problem.

### 6.5 Layout

Modal: `text_with_image` | `image_only`. Fullscreen: `image_and_text` | `image_only`. The
first two names mean the same arrangement. Unrecognised → the type's default, campaign kept.
**Never infer layout from which fields are populated** — a campaign whose personalised copy
resolves to empty is indistinguishable from a deliberately image-only one.

One override, and only one: `image_only` **with no usable artwork falls back to the stacked
composition** on both modal and fullscreen. Otherwise it renders bare background plus
buttons, logs an impression and reports nothing wrong.

### 6.6 Actions

| type | Behaviour |
|---|---|
| `dismiss` | close |
| `open_url` | Custom Tab / in-app browser; `external: true` → `ACTION_VIEW` |
| `navigate` | hand `route` (bare name, no leading slash) + `arguments` to the host |
| `log_event` · `log_attribute` · `request_push_permission` | parsed, unsupported |

A button with no usable action falls back to `dismiss` — a dead button is worse than a
closing one. A message surface with no action stays inert. Every button tap reports a click,
including a dismiss button.

### 6.7 Triggers

Two only: `session_start` (no fields) and `event` (match on **`name`**, never `eventId`;
null or empty name → drop the campaign). Purchases are not a trigger type — a purchase
arrives as an event named `purchase` with `productId`, `price`, `currency` and `quantity` in
its properties, so ordinary filters work on them.

### 6.8 Metadata filters — the asymmetry is deliberate

Operators: `equals`, `notEquals`, `greaterThan`, `greaterThanOrEqual`, `lessThan`,
`lessThanOrEqual`, `contains`, accepting the backend's spellings case-insensitively
(`Is`, `IsNot`, …).

- Missing `name` → **drop the whole campaign**. Treating it as "always true" silently widens
  the campaign.
- Bad operator or null value → drop **that filter only**. Widening is the right response to
  one bad field.
- A missing property **never matches**. A filter is a requirement; absence is failure.
- `metadataLogicalOperator` anything but `And` → drop the campaign with a log.
- Comparisons coerce across numeric types and stringly-typed numbers (`"2"` matches `2`).
  Ordering operators on a non-number refuse and log rather than falling back to string
  comparison.

### 6.9 Colour parsing

Accepts `#RRGGBB`, `#AARRGGBB`, both without the hash, and a raw packed int. Six digits are
promoted to full opacity. Anything else logs and returns null → that one slot falls back
while the rest of the message renders. Wire order is ARGB, matching Android's `Color`; no
channel shuffling. Alpha is honoured everywhere, including on the message background —
a campaign may make a modal card translucent.

### 6.10 Text alignment

`content.textAlignment` is `{header, body}`. Case-insensitive `left`, `right`, `center`,
`start`, `end`; anything else → the type's default. `start`/`end` map to
`TEXT_ALIGNMENT_VIEW_START` / `VIEW_END`, **not** to left/right — that is the difference
between a layout that mirrors in Arabic and one that does not.

### 6.11 `extras`

Non-string values are **coerced to their string form**, not discarded. Null values are
dropped.

---

## 7. Selection

A pure function: `(occurrence, campaigns, history, now, cooldown, quietHours) → at most one
campaign`. No I/O, no `Context`, no clock of its own.

```
eligible = campaigns.filter {
    triggerMatches(it.trigger, occurrence)
    && (it.expiresAt == null || now < it.expiresAt)
    && repeatEligible(it, history, now)
    && it.messageType.isSupported
    && artworkReady(it)
}
if (eligible.isEmpty()) return null
if (quietHours?.contains(now) == true) return null      // suppress, do not defer
if (now - history.lastDisplayAt < cooldown) return null // global floor
return eligible.sortedWith(byPriorityDesc thenBy responseIndex).first()
```

### 7.1 Load-bearing details

- **Expiry is checked at selection**, not only at fetch — campaigns are cached for the
  session, so one fetched at 23:58 would otherwise fire all night.
- **Unsupported types are filtered here**, not refused at display, so a usable
  lower-priority campaign wins instead of the occurrence being wasted.
- **The tie-break is response order and it is meaningful** — the backend returns campaigns in
  the sequence the marketer arranged in the dashboard. `sortedWith` is stable, so priority
  alone would preserve it, but the index goes into the comparator explicitly with a comment
  saying why. In Flutter the comment justified it only as "deterministic", which invited
  replacing it with ascending `campaignId` — a change four existing tests all passed.
- **The global floor is checked after eligibility, before sorting.** It is not per campaign.
- **Quiet hours suppress, they do not defer.** The pending slot is in-memory and a window is
  hours long, so "retry when it ends" would never fire. Suppression costs the occurrence,
  not the campaign.

### 7.2 Quiet hours

`{enabled, start, end}` at the response root, global (verified across 78 campaign objects
that no campaign carries one).

- Times are **UTC**, confirmed with the backend team. The strings carry no zone and the
  obvious reading — the customer's local wall clock — is wrong.
- **Half-open**: the start minute is inside, the end minute is not.
- **Wraps midnight** — `22:00→08:00` is two ranges, not one. Naive `start <= m && m < end`
  is false for every minute of it.
- `start == end` is refused with a log — zero-length and twenty-four-hours look identical on
  the wire, and silencing an account over a typo is the worse reading.
- `enabled: false`, absent or malformed → no window. Log all four.
- **It must survive the cache** (§12). Rebuilding the parsed result field by field silently
  dropped it in Flutter, and going offline became a way to message somebody at 3am.

Test instants are built in UTC so the same test passes in Cairo and Los Angeles.

### 7.3 `campaignOrdering` is ignored

Despite the name, it is not the ranking. Observed sending two ids against 7–14 campaigns,
naming campaigns absent from the same response, and never covering a tied-priority pair.
Acting on it would re-rank ties the dashboard had already settled.

### 7.4 Cooldown and caps

- **Global floor** — `cooldownSeconds` from the sync response, default 30. Minimum gap
  between any two displays from any campaign. Never hardcoded; it is how the backend tunes
  marketing pressure without a client release.
- **Per-campaign repeat rule** — `repeatable` and `minIntervalSeconds`.
- **Both are recorded at impression, never at selection.** A message selected then deferred
  or suppressed must not burn its slot.
- Both survive a restart. Comparisons use wall-clock time because a monotonic clock does not
  survive process death; a device clock moved backwards can suppress messages. Accepted,
  unfixable client-side, and Braze has the same exposure.

### 7.5 Starvation is configuration, not a bug

A repeatable campaign at high priority with `minIntervalSeconds: 0` wins every session start
forever. Braze and CleverTap behave identically. Adding a fairness mechanism would diverge
from both and silently override a marketer's stated intent. Say so in the log.

---

## 8. Display

### 8.1 Where to draw

A `View` added to the current Activity's content root (`android.R.id.content`). This is what
Braze Android does and it needs no permission. **`SYSTEM_ALERT_WINDOW` /
`TYPE_APPLICATION_OVERLAY` is not used** — drawing over other apps requires a permission we
must not ask an integrator's users for.

### 8.2 Activity tracking

`registerActivityLifecycleCallbacks`, registered on opt-in and unregistered on stop. The
current Activity is held in a **`WeakReference`**, cleared in `onActivityPaused` — a strong
reference from a process-lifetime singleton is a textbook leak, and this module lives in
`GameballApp`. The `Application` comes from `context.applicationContext as Application`.

**The Activity is resolved at presentation time and never cached in the presenter.** Binding
to the first surface handle was a real Flutter trap — after a hot restart the presenter
pointed at a dead surface and messages silently never appeared. On Android it is worse,
because rotation recreates the Activity by default. (The port guide calls this "the #4
defect" in its display section, but its own numbered table has #4 as the HTTP 202 bug; it is
a separate trap, not one of the eleven.) When `present()` cannot find one it returns `false` so the
service defers — it does not throw.

### 8.3 Impression timing

Reported when the message becomes **visible**, not when the view is added:

```kotlin
view.doOnPreDraw {          // androidx.core.view, already available
    onShown()               // record the cap, log the impression,
}                           // and start the auto-dismiss timer HERE
```

Everything downstream depends on this: the frequency cap is recorded here, so a message
dismissed before it paints does not burn its slot; the auto-dismiss timer measures *time
visible*; `impressions = clicks + dismissals` holds as an identity the backend relies on; and
if the app is backgrounded in that instant the callback never fires and no impression is
reported, which is correct.

### 8.4 Dismissal accounting

Two flags per presentation: `shown` (the impression fired) and `engaged` (a tap happened).
Report `dismiss` only when `shown && !engaged`.

### 8.5 Rotation must not double-count

Rotation destroys the Activity and takes the view with it. The message was not dismissed by
the user, so it comes back — but re-presenting must not log a second impression. The
"impression already reported" flag lives on the **pending presentation**, not on the view.
The auto-dismiss timer continues measuring from the original impression rather than
restarting.

Neither Flutter nor iOS faced this — an overlay entry and a `UIWindow` both survive rotation.
This is an Android-only decision.

### 8.6 Back button

Modal and fullscreen consume back; it dismisses the message and must not pop the host's
route. **Slideup must not intercept it** — a non-blocking banner has no claim on the gesture.

Implementation: `OnBackPressedCallback` on `activity.onBackPressedDispatcher` when the
Activity is a `ComponentActivity` (likely, given appcompat 1.7.1), falling back to a focusable
root view with an `OnKeyListener` for `KEYCODE_BACK`. Removed on dismissal. The dispatcher
path is preferred because the key-listener fallback stops firing under predictive back
(`android:enableOnBackInvokedCallback="true"`).

### 8.7 Window insets

`WindowInsetsCompat.Type.systemBars()` plus `displayCutout()` applied as padding on the
slideup container. Not cosmetic: copy drawn under a cutout loses its first line, and a bottom
banner overlapping the gesture strip swallows the swipe that is the only way to dismiss it.
Android 15 enforces edge-to-edge for apps targeting it.

### 8.8 Deferral vs. suppression

| Defer — hold in the pending slot, retry | Suppress — the occurrence is spent |
|---|---|
| no Activity available | inside the cooldown floor |
| another message is already showing | the campaign's repeat rule says no |
| a fullscreen campaign's orientation does not match | artwork is not ready |
| `beforeDisplay` returned `later` | inside the quiet-hours window |
|  | `beforeDisplay` returned `discard` |

- **One pending slot, not a queue.** A newer deferral displaces an older one, with a log
  naming both.
- Retry when: the current message is dismissed, an Activity becomes available, or the
  device rotates.
- **Re-validate on retry** — ask "may this display now", not "has it ever displayed". The
  cruder question threw away every repeatable campaign that happened to be waiting (defect
  10). Re-check the floor too.
- The pending slot is in-memory only and dies with the process. Deliberate.

### 8.9 Orientation

Enforced at display time against `resources.configuration.orientation`, **fullscreen only**.
A mismatch is deferred and retried on rotation. Enforcing it for a small centred card would
suppress messages for no benefit.

---

## 9. Session lifecycle

A session starts when the host opts in, when it identifies a different customer, or when the
app returns to the foreground after more than the session timeout. Each one syncs.

### 9.1 Foreground/background detection

```kotlin
override fun onActivityStarted(a: Activity) {
    if (startedActivities == 0) service.onAppForegrounded()
    startedActivities++
}
override fun onActivityStopped(a: Activity) {
    startedActivities--
    if (startedActivities == 0) service.onAppBackgrounded()
}
```

Counting started Activities means a screen transition (start B, stop A) never dips to zero,
and neither does rotation. Treating `onActivityPaused`/`onActivityStopped` as "backgrounded"
makes every navigation look like a new session — the Android form of **defect 1**.

The pause stamp is **first-wins**, independent of getting the callback taxonomy right:

```kotlin
fun onAppBackgrounded() { if (lastPausedAt == null) lastPausedAt = now() }
fun onAppForegrounded() {
    val since = lastPausedAt ?: return
    lastPausedAt = null
    if (now() - since > sessionTimeout) startNewSession()
}
```

**Session timeout: 30 s**, deliberately equal to the display cooldown default — because a
message can only display in the foreground, aligning them guarantees the cooldown cannot
suppress a warm session-start message. It is **not** raised dynamically when the server
raises the cooldown; that was tested on Flutter and makes things worse.

### 9.2 Syncing

- Sync and read local state **concurrently**, not in series.
- **Apply the cache only when the sync failed.** This removes the race where a slow cache
  read lands after a fast sync and clobbers fresher campaigns.
- A successful but **empty** sync replaces the cache. Only a failure falls back to it.
- Order after a sync: warm artwork → declare personalisation needs → evaluate session start.

### 9.3 Accepted: a brand-new customer's first session

`initializeCustomer` and `startInAppMessaging` race on a first-ever launch; the sync 404s.
Closed as accepted behaviour — it self-heals on the first warm resume past the session
timeout. Awaiting the create call is **not** the fix: it was measured at 0.56 s, 0.56 s and
7.63 s across three probes, so awaiting it would block startup on every launch to fix a
first-session-only problem.

---

## 10. Artwork

Every held campaign's `imageUrl` and `iconUrl` is loaded at sync, before anything displays.
A campaign whose artwork failed is passed over, letting a lower-priority ready one take the
slot — Braze and CleverTap both do the same. Do not "improve" this by rendering text-only.

Picasso's `fetch()` warms the cache with no target view, which is exactly the primitive a
prefetcher needs; at display an ordinary `load().into()` hits the warm cache.

- **Warm the whole set, not just the winner** — an event trigger fires with no warning.
- **Bound at 5 s, concurrently**, so the ceiling is the slowest single image rather than the
  sum. Picasso has no per-request timeout, so it is imposed with `withTimeoutOrNull(5_000)`
  around a suspending wrapper.
- Log artwork served over `http://` — cleartext is blocked by default since API 28, so the
  load fails and the only symptom is a campaign that silently never shows.

**The failure verdict is not cached for the session.** Flutter computed it once at sync, so a
two-second blip made a campaign undisplayable for eight minutes. The failed set is
re-attempted **once per 30 s**, fired without blocking the evaluation in flight, and the retry
trigger sits **before** the "nothing displayable, return early" branch — a session where
everything failed is exactly the one that has to recover. Only the failed set is retried.

Campaigns often share an asset URL: on the QA account four campaigns (priorities 10, 7, 5, 4)
point at the same image, so one failure removes the top of the priority ladder at once and a
priority-0 filler wins. Correct behaviour, and it will look like a selection bug.

---

## 11. Analytics outbox

Three event types: `impression`, `click`, `dismiss`. There is no separate button-click type —
a button tap is a `click` carrying `buttonId`. Report `url` alongside a click whose action
opened one. **`isTest` campaigns display normally and report nothing at all.**

| Property | Value |
|---|---|
| Never blocks the caller | fire-and-forget |
| Flush interval | 30 s |
| Flush at count | 10 events |
| Events per request | 50 |
| Outbox ceiling | 500 — beyond that drop the oldest and log |
| Concurrency | one request in flight; a second call while one runs is dropped |
| Persisted | after every change |

**No exponential backoff.** A retryable failure re-arms the ordinary 30 s timer, and a
successful flush that leaves a backlog re-flushes immediately rather than waiting an interval.

**Forced flush** on app backgrounded, on `stopInAppMessaging()`, and immediately before an
action that may take the user away (`open_url` / `navigate`) bounded at ~800 ms. Expect that
bound to be hit on real networks; that is the mechanism working.

**Delivery is at-least-once**, deduplicated server-side on `eventUid`. The uid is generated
once per event and **never regenerated on retry**. A re-display of a repeatable campaign is a
new impression with a new uid, not a resend. Oldest first within a batch. A poison batch is
discarded, not retried forever — the outbox is FIFO, so one permanently rejected batch at the
front takes all analytics down.

---

## 12. Personalisation

Live, not dormant: sync sends templates and live campaigns carry `{player_name}` and
`{points_balance}` today.

**Token syntax:** `{token_name}` — single braces around `[A-Za-z_][A-Za-z0-9_]*`. Not Liquid,
no double braces, no filters, no conditionals. `{ spaced }`, `{2}` and a lone `{` are not
tokens; a loose pattern lets a value map mangle ordinary copy.

- Applies to header, body and every button label.
- Values are inserted **verbatim** — they arrive pre-formatted.
- **One pass only.** A substituted value is data, not a template. Matches are walked in
  reverse so each replacement cannot invalidate the ranges still to come.
- Cheap-scan for `{` before running the regex, so a message with no tokens costs one
  character comparison and never calls the endpoint.

**When to fetch:** immediately before display, only for a message that carries a token,
bounded at **2 s**. On timeout, error or empty result, display the text already held. Values
are cached for **60 s**, keyed by customer, and dropped **on every event and purchase**
before evaluating — but **not** on session start. The campaign this exists for is "you just
earned 200 points, you now have X": its trigger is the purchase, and a value cached before it
quotes the number from before the change.

**Unresolved tokens are blanked and the message still displays** (O22, resolved 24 Aug 2026).
Not suppressed — an unresolvable value is a backend problem to find and fix, not something
the SDK hides. Not defaulted — per-token defaults are deliberately deferred. Implemented as a
final pass immediately before display on **every path**, including ones where substitution
never ran (a timed-out fetch). `substitute()` itself leaves unknown tokens in place so a
caller can distinguish resolved from unresolved; the blanking pass makes the result
presentable. A token resolving to an empty string is treated as **resolved**.

**Put the timeout where the fallback is.** On Flutter the raw-token bug was blamed on the 2 s
budget; the real round trip was 716 ms. What broke was that the timeout fired in the service,
outside the source that owned the fallback, and the cache had already been cleared.

### 12.1 PII

After each sync, derive the set of token names the held campaigns mention and **persist only
those**. A campaign set mentioning no tokens stores nothing. The live fetch still returns all
nine keys; the filter applies to what lands on disk. Cleared on logout and customer change.

**The write-after-clear race is real.** A display must never wait on storage, so the write
after a fetch is not awaited — which means a clear issued moments later can be overtaken by
it, restoring data that was just deleted. Re-check the customer **after** acquiring storage,
not before: a check before the suspension point always passes.

---

## 13. Persistence

Four stores, all `SharedPreferences` via the existing `SharedPreferencesUtils`: display
history, campaign cache, analytics outbox, variable values.

- **Keyed per customer, discarded on mismatch at read.** Showing one person's campaigns — or
  name — to another is the single failure this scoping exists to prevent.
- A corrupt or unreadable store must not stop messaging from starting. Log, discard, carry on.
- **The campaign cache stores the raw payload**, not serialised objects. No serialiser to keep
  in step with the model, and the parser stays the only thing that reads a sync. Re-parse on
  read.
- If a parsed result is rebuilt after reading the cache, **carry every field** — Flutter
  rebuilt it field by field and silently dropped quiet hours.
- `apply()`, not `commit()`. First load happens off the main thread.
- **No read timeout.** Flutter needs one because `shared_preferences` is a platform channel;
  Android's is in-process. Porting the timeout is how a once-ever campaign displays twice.
- Display history grows without pruning, deliberately. The backend stops returning a
  non-repeatable campaign once its impression lands, so forgetting it locally could show a
  once-ever message twice. If bounded at all, cap the entry count and drop oldest — never
  prune by "no longer in the current sync".

---

## 14. Public API surface

```kotlin
GameballApp.getInstance(context).apply {
    startInAppMessaging(customerId, options)   // idempotent for the same customer
    stopInAppMessaging()
    isInAppMessagingStarted()
    logPurchase(productId, price, currency, quantity, properties)
}
```

- **start** — idempotent for the same customer (a second call logs "already running" and
  returns). A *different* customer refetches, resets caps and discards the previous
  customer's cache and stored values, so the host need not stop first.
- **stop** — dismiss what is showing → clear campaigns, caps, pending slot, artwork state,
  quiet hours → clear personalisation values including storage → flush telemetry → dispose
  the scheduler → unregister lifecycle callbacks. Flush *before* dispose.
- **observe** — a listener receiving every message *selected*, whatever happens to it next.
  An observer, not a display notification. Exposed as a Java-friendly interface.

**stop → start must fully revive.** Disposing the analytics scheduler sets a "do not
schedule" flag; if `start` reuses the same service instance — which it will — that flag must
be cleared, or after one cycle the flush timer is never armed again. Tested explicitly.

### 14.1 The four hooks

| Hook | Contract |
|---|---|
| `beforeDisplay(message) → show \| later \| discard` | synchronous; `later` defers, `discard` spends the occurrence. Throws → `show` |
| `onAction(message, button?, action) → Boolean` | `true` = host handled it. `button` is null when the surface itself was tapped. Throws → built-in handling |
| `onNavigate(route, arguments)` | for hosts whose routing the SDK cannot drive. An unknown route logs and continues, never throws |
| `logPurchase(...)` | reaches campaigns as an event named `purchase`. **Exactly one trigger occurrence per call** |

**Hooks replace the action, never the bookkeeping.** The impression, click and dismissal are
reported regardless of what any hook returns. A host that intercepts every tap must not
thereby erase its own click analytics.

Each hook is wrapped in its own try/catch that logs and swallows, placed **outside** any
existing callback chain that lacks error handling.

**Action path ordering:** report the click → ask the host → dismiss → flush telemetry →
perform the action. Dismissal comes before the action deliberately — a `navigate` starts a
transition, and leaving the message up briefly covers the screen the user just asked for.

### 14.2 Host wiring

| Existing symbol | Change |
|---|---|
| `initializeCustomer(...)` | notify the module of a customer change, in its own try/catch outside the Rx chain |
| `sendEvent(event, …)` | feed the trigger engine with the event name **and its metadata map** — omitting metadata was **defect 7**: filters fully unit-tested and completely unreachable |
| `showProfile` / `hideProfile` | unchanged; IAM does not observe widget lifecycle |
| `logPurchase` | routes through the existing `sendEvent` path, which is the single place that feeds the trigger engine. It **must not** also notify the service — that was **defect 3**, one purchase firing the trigger engine twice and displacing the pending slot. Decided 27 Aug 2026; asserted by a test counting evaluations for one purchase |

### 14.3 The compatibility invariant

Until `startInAppMessaging` is called: **no requests, no timers, no storage writes, nothing
drawn, no lifecycle callbacks registered.** That includes
`registerActivityLifecycleCallbacks` — registered on opt-in, not in `init`. Written as a test
asserting a mock OkHttp client received zero calls.

### 14.4 Test seams

`internal` + `@VisibleForTesting` injection points for the message source, analytics, artwork
prefetcher and variable source, plus an injectable clock on the evaluator and service. Without
these the e2e test hits the live API.

---

## 15. UI

All numbers live in `ui/MessageMetrics.kt` — the Android counterpart to Flutter's
`message_view_metrics.dart` and iOS's `MessageViewAttributes.swift`. Views reference it;
nothing is inlined. This is what makes the later parity pass a diff rather than an audit.

### 15.1 Constants

| Group | Name | Value |
|---|---|---|
| Shared | `defaultScrim` | `0x99000000` |
| | `closeGlyphSize` | 24 |
| | `closeGlyphOnLight` | `0xFF111827` |
| | `closeGlyphOnDark` | `0xFFFFFFFF` |
| | `closeGlyphLuminanceThreshold` | 0.179 |
| | `buttonCornerRadius` | 8 |
| Modal | `margin` | 24 all |
| | `maxWidth` | 420 |
| | `cornerRadius` | 16 (anti-aliased clipping) |
| | `contentPadding` | 20, 20, 20, 0 |
| | `headerToBodySpacing` | 8 (only when both present) |
| | `buttonsPadding` | 20, 20, 20, 16 |
| | `buttonSpacing` | 8 |
| | `buttonPadding` | h 20, v 12 |
| | `imageOnlyButtonsPadding` | 20, 0, 20, 20 |
| | `closeInset` | 4 |
| | `minImageRatio` | 0.55 |
| | `copyReserve` | 120 |
| | `imageOnlyHeightFraction` | 0.65 |
| Slideup | `margin` | 12 all, inside the safe area |
| | `maxWidth` | 480 |
| | `cornerRadius` | 12 |
| | `elevation` | 6 |
| | `contentPadding` | h 14, v 12 |
| | `maxTextLines` | 3 |
| | `iconSize` | 40 × 40 |
| | `iconCornerRadius` | 8 |
| | `iconSpacing` | end 12 |
| | `chevronSpacing` / `Size` | start 8 / 20 |
| Fullscreen | `contentPadding` | 24, 24, 24, 0 |
| | `imageHeightFraction` | 0.50 |
| | `imageOnlyButtonsPadding` | 24, 0, 24, 32 |
| | `headerToBodySpacing` | 12 |
| | `buttonsPadding` | 24, 28, 24, 24 |
| | `buttonSpacing` | 12 |
| | `buttonPadding` | v 16 |
| | `buttonFontSize` | 16 / w600 |
| | `closePadding` | 8 all |

### 15.2 Type roles

Sizes come from the Material 3 type scale, in **sp**, so enlarged system text enlarges the
message.

| Slot | Role | Size / line | Weight | Default alignment |
|---|---|---|---|---|
| Modal header | titleLarge | 22 / 28 | 700 | start |
| Modal body | bodyMedium | 14 / 20 | 400 | start |
| Modal button | labelLarge | 14 / 20 | 500 | — |
| Slideup copy | bodyMedium | 14 / 20 | 400 | start |
| Fullscreen header | headlineSmall | 24 / 32 | 700 | center |
| Fullscreen body | bodyLarge | 16 / 24 | 400 | center |
| Fullscreen button | explicit | 16 | 600 | center |

### 15.3 Colour model

| Wire key | Paints | When null |
|---|---|---|
| `colors.background` | message surface | host `colorSurface` |
| `colors.header` | header text | host default for the role |
| `colors.text` | body text; slideup copy and chevron | host theme; chevron → `onSurfaceVariant` |
| `colors.frame` | the modal scrim | constant `0x99000000` |
| `colors.closeButton` | the close glyph | derived — see 15.4 |
| `colors.border` | **nothing** — parsed and carried, never painted | — |
| `buttons[].colors.background` | button fill | host theme (transparent — a text button) |
| `buttons[].colors.text` | button label | host `colorPrimary` |
| `buttons[].colors.border` | button outline | **no border at all** — not a themed one, none |

Across all fifteen live campaigns, `closeButton`, `border`, `frame` and every button's
`colors` are null. **The fallbacks are the production path, not edge cases.**

### 15.4 The close button

Three cases in order, and nothing consults the artwork:

1. `colors.closeButton` set → use it verbatim.
2. else `colors.background` set → `#111827` if the background's relative luminance > 0.179,
   else `#FFFFFF`.
3. else → the host theme's on-surface colour.

No disc, ring or shadow. Glyph 24 inside a **48 × 48** hit target on every type that draws
one. Modal: top 4, end 4 inside the card, no safe-area handling (the 24 margin already clears
it). Fullscreen: topEnd, padding 8, **inside** the safe area. Slideup: never drawn.

Positioned to the **trailing** corner so it mirrors under RTL. The accessibility label comes
from a platform string, never a literal `"Close"`. The close control is a **sibling** of the
tappable message body, never a child — closing must never fire the click action.

A fixed white glyph measures 1.00:1 on a white card; a fixed dark glyph measures 1.00:1
against the `#111827` the live slideup uses. The derived pair clears WCAG's 3:1 for a
non-text control against every background, worst case 3.8:1.

### 15.5 Per-type composition

**Slideup** (type 1, no layout variants) — no scrim, no buttons, no close glyph; the app
underneath stays fully usable and the overlay intercepts no hit test outside its band. Copy
clamps to 3 lines then ellipsises: a **mechanism, not a number** — bounding the container's
height instead truncates where a clamp grows the banner, and they diverge at large text
scales. Icon collapses to zero **width** on failure so a broken image cannot shift the copy.
The chevron is drawn only when the campaign set a message action, and flips under RTL. Swipe
is toward its own edge only — sideways fights a horizontal scroll underneath. It survives
navigation between Activities, which is why the 8 s default is not optional.

**Modal** (type 2) — a centred card over a scrim that swallows every tap not on the card,
whether or not it dismisses. Only the copy scrolls; image and buttons sit **outside** the
scroll view so long copy can never push the call to action off. Artwork is `contain`, never
cropped, capped at `min(cardWidth / 0.55, availableHeight - 120)`. Buttons are a **wrap**
aligned trailing, not a row — two German or Arabic labels overflowed a row by 360 px in
testing. `image_only`: artwork fills the card at `cover`, capped at 65 % of screen height,
buttons stretched and stacked over it, text never drawn.

**Fullscreen** (type 3) — edge to edge, opaque, no scrim. `image_only`: `centerCrop` over the
full bounds *ignoring* the safe area, buttons anchored bottom-centre *inside* it.
`text_with_image`: the whole stack inside the safe area, the image given a **fixed 50 % of
available height** and drawn **`fitCenter`** within that box, copy scrolling in the remainder,
buttons outside the scroll view. Header and body default to **centre** here and to **start** on
a modal.

The fixed box and the fit are separate properties, and the two source documents each settle
one — see §19 Q1.

### 15.6 Motion

| Type | Entrance | Duration | Curve |
|---|---|---|---|
| Modal | fade 0→1 + scale 0.96→1.0 | 200 ms | easeOutCubic |
| Fullscreen | fade only | 200 ms | easeOut |
| Slideup | slides in from its own edge | 220 ms | easeOutCubic |

No exit animation except a swiped slideup, which animates because the gesture drives it.
**Reduce motion drops the duration to zero rather than shortening it** —
`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.

### 15.7 Dismissal

| `closeBehaviour` | Glyph | Scrim tap | Slideup | Modal | Fullscreen |
|---|---|---|---|---|---|
| `both` / null | yes | yes | swipe, 8 s | glyph, scrim, back | glyph, back |
| `button` | yes | no | swipe, 8 s | glyph, back | glyph, back |
| `swipe` | no (**yes** on fullscreen) | yes | swipe, 8 s | scrim, back | glyph, back — promoted |
| unrecognised | falls back to `both` and logs | | | | |

**`swipe` is promoted to `both` on fullscreen.** A fullscreen message has no scrim and no
swipe gesture, so obeying the field literally leaves only the system back gesture — which
does not exist on iOS. The parser promotes and logs. A port that obeys it ships a trap.

`autoDismissSeconds`: absent → **8 s on a slideup** (applied at the parser), no timer on
modal and fullscreen. An explicit **`0` means "stay until dismissed"** and is honoured —
absent and zero are different values. The timer starts at the first painted frame.

### 15.8 Degraded states

A content problem degrades the message; a contract problem drops it — never the reverse.

| Situation | Result |
|---|---|
| artwork fails before display | campaign passed over; next eligible one shows |
| artwork fails between gate and frame | image collapses; the layout closes up and still reads |
| slideup icon fails | collapses to zero width, no gap |
| copy longer than the surface | modal scrolls, fullscreen scrolls, slideup ellipsises. **No type ever clips its buttons away** |
| unresolvable `{token}` | replaced with an empty string |
| malformed colour | that one slot falls back |
| unrecognised layout | the type's default composition, logged |
| > 2 modal buttons | first two render, rest dropped with a log |
| button styled-not-translated, or vice versa | dropped |
| `http://` artwork | fails the gate; logged **by name**, or the only symptom is silence |
| `image_only` with no artwork | falls back to the stacked composition (both types) |

### 15.9 Layout resilience and accessibility

All four were real Flutter defects found on a 320×568 screen and at 2× text scale, and none
was caught by a functional test.

- Copy scrolls, never clips — clipping removes the buttons first. Measured overflows before
  the fix: 296 px on a small screen, 1,552 px at 2× text. A `NestedScrollView` sizing to
  content up to a maximum.
- **A short message must stay short** — the obvious "make it scrollable" makes every card
  full-height. Test both directions.
- Buttons wrap rather than overflow — `ConstraintLayout`'s `Flow`.
- RTL actually mirrors: `start`/`end` throughout, never `left`/`right`. The host must set
  `android:supportsRtl="true"`; we cannot force it, so degrade sanely if absent.
- Touch targets ≥ 48dp, including the close glyph. Text in **sp**. Colours absent from the
  campaign fall back to host theme attributes (`?attr/colorSurface`, `?attr/colorOnSurface`)
  so the message follows the app into dark mode — never to a literal.

---

## 16. Testing

JVM unit tests over everything that does not need a device, following the Part 15 conformance
matrix. Test-only dependencies are added (`kotlinx-coroutines-test`, `mockwebserver`,
`robolectric` where a `Context` is unavoidable); none reaches consumers.

**Parsing** — fully populated and minimal campaigns; missing `campaignId` dropped; unknown
`messageType` kept-but-unsupported; unknown `contentMode` dropped; buttons paired by id;
null trigger name dropped; filter missing `name` drops the campaign while a bad operator
drops only the filter; `Or` drops the campaign; every layout value plus an unknown one;
absent copy does not imply image-only; fullscreen prefers `media.url` and others prefer
`imageUrl`; `video` ignored; blank URL treated as absent; malformed JSON returns empty
without throwing; quiet hours wrapping midnight, same-day, disabled, absent, malformed,
`start == end` refused, `HH:mm:ss` tolerated.

**Selection** — all seven filter operators; a missing property never matches; expiry honoured
even from cache; non-repeatable never twice across a restart; `minIntervalSeconds` respected;
the floor; priority; ties breaking on response order **even when it contradicts `campaignId`
order**; unsupported types filtered so a lower-priority supported campaign wins; quiet hours
suppressing without costing the campaign.

**Analytics** — every status mapping; 2xx with `rejected > 0` still clears; `eventUid` is a
valid v4 UUID and is never regenerated on retry; outbox survives a restart; ceiling drops
oldest; batches chunk at 50; **`occurredAt` is ASCII ISO-8601 with the device locale set to
Arabic** (the L3 guard).

**Personalisation** — known token substituted; unknown left as written by `substitute()`;
malformed braces untouched; one pass only; no token means no network call; failure/empty/
timeout all display held text; TTL; dropped on event and purchase but not on session start;
only used tokens persisted; a pending write cannot resurrect cleared values; **no path renders
a raw brace**.

**Compatibility** — nothing before opt-in (asserted against a mock client with zero calls);
**the outgoing sync URL still contains `v4.0` with a session token set** (the L1 guard);
stop → start re-arms the flush timer.

**End to end** — drive the whole module through the public API only against a stubbed source,
and assert a message renders, a tap dismisses it, and analytics were reported. This is the
test that catches wiring mistakes every unit test passes — it is what would have caught
defect 7. Also parse a payload **captured from the live backend**, not one we wrote; in
Flutter that caught two defects reading the documentation did not.

### 16.1 Test discipline

- A test that passes the moment it is written has proved nothing. Break the production code,
  watch it fail, restore.
- Pair every negative assertion with a positive control — "nothing displayed" could be caps,
  cooldown, targeting, expiry or artwork.
- Device-dependent rows (rotation, insets, the Activity leak, reduce motion) are verified in
  manual QA against the alpha account, not automated in this pass.

---

## 17. Build order

Each step ends somewhere provable.

| # | Step | Done when |
|---|---|---|
| 1 | L1 guard + endpoints in `Config.kt` | a test asserts the sync URL keeps `v4.0` with a session token set |
| 2 | Models and parser | parser tests green, including a payload captured from alpha |
| 3 | Selector — pure, clock injected, quiet hours | every selection row green, no mocks |
| 4 | Persistence — history, cache, per-customer scoping | a once-ever campaign stays suppressed across a simulated restart |
| 5 | Service skeleton — sync, evaluate, pending slot, deferral | deferral and retry green against a fake presenter returning false |
| 6 | Artwork prefetcher + in-session retry | a campaign that failed once becomes displayable after the floor |
| 7 | Presentation — Activity tracking, three views, back, insets, rotation | renders on a device and survives rotation without a second impression |
| 8 | Analytics outbox | status mapping green; `eventUid` shape asserted; survives a restart |
| 9 | Personalisation + blanking pass | no path renders a raw brace |
| 10 | Host wiring | the e2e test drives everything through the public API only |
| 11 | Layout resilience — small screen, 2× text, RTL, reduce motion | all four green; verify by breaking each |
| 12 | Device QA against the alpha account | §18 |

---

## 18. Test account

- `https://api.alpha.gameball.app` — V4 endpoints are **alpha only**; production returns a
  bare 404.
- Platform code **2**. The same customer sees a different campaign set as iOS; a
  cross-platform count mismatch is expected.
- Use `moaty-survey-3` — the only identity whose session ladder steps through four campaigns
  rather than repeating one. For a pristine first-session identity, invent a new id.
- The account is shared and live: `cooldownSeconds` changed from 60 to 10 mid-run while the
  guide was being written. **Capture the sync payload at the start of every session** and
  compare against that, not against any document.
- The artwork host is flaky — all image-bearing campaigns point at one `i.ibb.co` URL that
  failed one probe in five, and because they share it one failure removes priorities 10, 7, 5
  and 4 together. Largest source of noise in the suite.
- **No storefront action fires a configured trigger.** Debug buttons are needed for
  `view_product_page`, `place_order` and `logPurchase`, or no event-triggered campaign is
  reachable at all.

Not exercisable on the account: `metadataFilters`, `expiresAt`, `isTest`, `extras`, a
message-level action, `layout: image_only`, a slideup `iconUrl`, a second button,
`orientation: landscape`, a non-prerendered `contentMode`, a purchase trigger, or types 4/5.
Metadata filters are the expensive gap — they have never met the real backend on any platform.

---

## 19. Open questions

**Q1 — fullscreen `text_with_image` scale type. RESOLVED (27 Aug 2026) as a synthesis.**

The two documents appeared to conflict: the port guide's defect 9 prescribes `fitCenter`
"where artwork shares the screen with copy, `centerCrop` only for the image-only variant";
the UI spec specifies "exactly 50 % of the available height … at `cover`" and reasons that a
fixed share "is what stops it letterboxing".

They are settling **two different properties**. The UI spec's argument is about the *height
allocation* — a fixed share rather than whatever slack the copy leaves. Defect 9 is about the
*fill within that box*. Nothing forces them to be answered together.

Decided: **fixed 50 % box from the UI spec, `fitCenter` within it from defect 9.**

Three reasons:

1. **The measured harm is severe.** With the live 384×640 asset on a 390×844 device the box
   is 390 × 375.5. `cover` scales to fill the width, producing a 650-tall image in a 375.5
   box — **42 % of the poster is cropped away, top and bottom**. That is exactly defect 9's
   finding: an offer baked into the top of a promo image, lost. `fitCenter` pillarboxes
   instead, at 225 × 375.5 with 82 px bars.
2. **Losing campaign content is a worse failure than bars.** A letterboxed poster is
   cosmetically imperfect and every pixel the marketer authored is still on screen. A cropped
   one silently deletes the call to action. This is the same asymmetry §15.8 applies
   everywhere else — degrade, do not discard.
3. **The UI spec is internally inconsistent in this exact section.** Its prose introduction
   says "the image takes whatever the copy does not need"; its composition table says
   "exactly 50 % … a fixed share, not the slack the copy leaves". Where a document contradicts
   itself, it is weak authority on the neighbouring claim.

`centerCrop` remains correct for `image_only` on both types, where bleeding to every edge is
the point. The modal is unaffected — both documents already agree its artwork is never
cropped.

To raise with the UI spec's owner, not blocking: the spec should state the fit and the box
separately, and reconcile its own prose with its table.

**Q2 — exit animations and the modal's scale** are flagged as unsettled in the UI spec: we are
the only implementation with no exit animation, and the 4 % modal scale is ours alone.
Implementing as specified; not resolving it here.

**Q3 — backend-side, identical for every SDK, not decided in this port:** O19 (V4 alpha only),
O23 (is `variables` read-after-write consistent with event processing), O4/O6 (authoritative
filter operator vocabulary; whether `Or` is reachable from the dashboard),
`campaignOrdering` (document it or stop sending it).

---

## 20. The eleven defects, and where each is addressed

| # | Defect | Here |
|---|---|---|
| 1 | warm session start never fired | §9.1 — count started Activities, first-wins pause stamp |
| 2 | new customer's first session had no campaigns | §9.3 — accepted, self-heals |
| 3 | one purchase fired the trigger engine twice | §14.2 — `logPurchase` picks one path |
| 4 | HTTP 202 read as failure | §5.3 — accept the 2xx range |
| 5 | raw `{player_name}` on screen | §12 — blanking pass on every path |
| 6 | quiet hours ignored | §7.2 — parsed, UTC, suppresses, survives the cache |
| 7 | filters built and unreachable | §14.2 — pass name **and** metadata; e2e test |
| 8 | display history skipped on cold start | §13 — no read timeout on Android |
| 9 | fullscreen cropped what the modal rendered whole | §15.5 — `fitCenter` when sharing with copy |
| 10 | deferred repeatable campaign discarded | §8.8 — reuse `repeatEligible` on retry |
| 11 | artwork failure cached for the session | §10 — retry the failed set once per 30 s |

Plus three Android-only hazards: a retry loop re-arming every frame (use the explicit
`onActivityResumed` trigger); inferring layout from populated fields (§6.5); and trusting the
documentation over the wire (§18 — probe the endpoint).
