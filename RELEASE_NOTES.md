# Release Notes - Gameball Android SDK

This file contains detailed release notes for the latest version. For complete version history, see [CHANGELOG.md](CHANGELOG.md).

---

## Latest Release: v4.1.0

**Release Date**: 2026-11-19
**Version**: 4.1.0
**Type**: Minor Release

---

## 📱 What's New

v4.1.0 brings **game campaigns** to in-app messaging, detects **taps on Gameball pushes automatically**, and can report **swipe-aways and influenced opens**. It also makes in-app messaging triggers and quiet hours behave as the dashboard describes them, and fixes a crash after the first sync. There are no breaking changes.

- **Game campaigns**: Spin the Wheel, Quiz, Scratch & Win and the other Gameball games open over your app on their own in-app messaging trigger
- **Automatic push tap detection**: taps on Gameball pushes are reported with no code
- **Swipe-aways and influenced opens**: `handlePushIntent(intent)` reports pushes swiped away, and app opens a push led to without being tapped
- **Widget host on the configuration**: `GameballConfig.widgetUrlPrefix` for the profile widget and game campaigns
- **Trigger fixes**: any-condition (OR) triggers, `Greater`, `Less` and `Between` conditions, app-open repeat rules, two campaigns on one trigger, rotation, and quiet hours on the device's clock

---

## 🚀 Key Features

### 🎮 Game campaigns

A game the merchant set to open over the app arrives with your in-app messages and opens on its own trigger, inside your app, in the Gameball widget: as a card over a dimmed screen or fullscreen. There is nothing extra to call; `startInAppMessaging` and your events are enough.

```kotlin
val gameball = GameballApp.getInstance(context)
gameball.startInAppMessaging("customer-123")

// A game campaign triggered on "level_completed" opens after this event
gameball.sendEvent(
    Event.builder()
        .customerId("customer-123")
        .eventName("level_completed")
        .build(),
    callback
)
```

Games follow the message rules exactly: app-open or event triggers with their conditions, the repeat setting and minimum interval, the minimum time between campaigns, quiet hours and the end date, with no delay and no dismissal limit. The SDK draws a close button in the game's top-end corner, keeps the game out of sight until it has finished drawing (dropping one that hasn't drawn within 12 seconds), and doesn't offer a game the customer has finished again until the next successful sync. A game opens only from a fresh sync, never from the offline cache.

### ⚙️ Widget host on the configuration

```kotlin
val config = GameballConfig.builder()
    .apiKey("your-api-key")
    .widgetUrlPrefix("m.gameball.app/alpha") // no scheme; leave unset for production
    .build()
```

The host applies to the profile widget and to game campaigns. A per-call `ShowProfileRequest.widgetUrlPrefix` still overrides it for that presentation.

### 📣 Automatic push tap detection

Taps on Gameball pushes are detected and reported without any code: the SDK checks the intent of every activity that starts, and every intent a `ComponentActivity` receives in `onNewIntent`. Call `init` in `Application.onCreate` so the tap that cold-starts your app is seen too. A tap counts once, however many times it reaches the SDK.

For an intent the SDK can't see by itself, such as one a plain `Activity` receives in `onNewIntent`:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    GameballApp.getInstance(this).handlePushClick(intent)
}
```

### 👋 Swipe-aways and influenced opens

```kotlin
class MyMessagingService : FirebaseMessagingService() {

    override fun handleIntent(intent: Intent) {
        if (GameballApp.getInstance(this).handlePushIntent(intent)) return
        super.handleIntent(intent)
    }
}
```

`handlePushIntent` lets the SDK see Gameball pushes as they arrive. It shows each one the way FCM would, with a delete intent, so a swipe-away is reported as a dismissal. It also keeps the push, so an app open that no tap caused, within 30 minutes of an untapped push arriving, is reported as an open that push influenced. It returns `false` for other pushes, while your app is on screen, and whenever a push can't be shown this way, and FCM then handles the push as usual.

---

## 🐛 Fixes

- **Crash after the first sync**: in-app messaging no longer crashes the app once a sync or the cache holds a message
- **Rotation**: a message or game open during a rotation comes back on the new screen, and no longer blocks every later message
- **Two campaigns on one trigger**: the second shows once the first is closed and the minimum time between campaigns has passed
- **App-open repeat rules**: app-open campaigns honour their repeat setting and minimum interval
- **Conditions**: `Greater`, `Less` and `Between` conditions are applied, and an OR trigger shows when any condition matches
- **Quiet hours**: read on the device's local clock instead of UTC

---

## ⚠️ Breaking Changes

None. Every v4.0.0 call keeps its signature and behaviour.

### Deprecated

- `handlePushClick(payload, callback?, sessionToken?)`: taps are now detected automatically. It still works, and a tap reported both ways counts once.

---

## 🔄 Changes

- Added game campaigns to in-app messaging
- Added `GameballConfig.widgetUrlPrefix`
- Added automatic detection of taps on Gameball pushes, and `handlePushClick(intent)` for intents the SDK can't see
- Added `handlePushIntent(intent)` to report swipe-aways and influenced opens
- Changed push reporting to the `push-engagement` endpoint, which replaces `push-click` and stays on v4.0 when a session token is set
- Deprecated `handlePushClick(payload, callback?, sessionToken?)`
- Fixed in-app messaging triggers, quiet hours, rotation and a crash after the first sync

---

## 🔧 Technical Details

### Requirements

- Android API 24+ (Android 7.0)
- Kotlin 2.0.0+
- AndroidX

### Dependencies

- Added `androidx.lifecycle:lifecycle-process` 2.6.2, already resolved through `appcompat`, to detect app opens
- The SDK's manifest declares a non-exported receiver for swipe-aways; nothing needs adding to yours

---

## 📚 Migration Support

No code changes are required. To adopt the new push signals, remove calls to the deprecated `handlePushClick(payload)` whenever it suits you, and call `handlePushIntent(intent)` from your `FirebaseMessagingService.handleIntent`.

### Migration Resources
- **[Migration Guide](MIGRATION.md#migration-guide-v40x--v410)**: Step-by-step migration instructions
- **[README](README.md)**: Complete usage documentation with examples
- **[Changelog](CHANGELOG.md)**: Detailed list of all changes

### Support

- 📧 Email: support@gameball.co
- 📖 Documentation: https://developer.gameball.co/
- 🐛 Issues: https://github.com/gameballers/gb-mobile-android/issues

---

## 📦 Installation

```kotlin
dependencies {
    implementation 'com.github.gameballers:gb-mobile-android:4.1.0'
}
```

---

## Previous Release: v4.0.0

**Release Date**: 2026-10-22
**Type**: Major Release

Raised the minimum supported Android version to API 24 (Android 7.0), and added in-app messaging: modal, slideup and fullscreen campaigns from the Gameball dashboard, triggered on session start or by custom events, with `logPurchase(...)`, display hooks and automatic analytics. See [CHANGELOG.md](CHANGELOG.md) for the full history.
